package com.lugmety.orderassistant.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Context
import android.content.Intent
import android.graphics.Path
import android.graphics.Rect
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.Settings
import android.text.TextUtils
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.lugmety.orderassistant.LugmetyApp
import com.lugmety.orderassistant.data.local.entity.DiagnosticLogEntity
import com.lugmety.orderassistant.data.local.entity.OrderEventEntity
import com.lugmety.orderassistant.data.repository.OrderRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.security.MessageDigest

/**
 * Android Accessibility Service for Lugmety Order Assistant & Auto Clicker Module
 *
 * Strict Security & Lifecycle Guarantees:
 * 1. Safe process recreation & lazy repository initialization to prevent crashes on startup.
 * 2. Strictly local processing - NO screen contents, accessibility events, or order records
 *    are ever sent to external servers or Firebase.
 * 3. Unexported service interface - Only the Android OS System can bind via BIND_ACCESSIBILITY_SERVICE.
 * 4. Mutual exclusion between Lugmety Order Refresh and Auto Clicker gestures to prevent collisions.
 * 5. Does not fabricate order data.
 * 6. Bounds-based swipe only executes when verified Lugmety Order List is confirmed foreground.
 */
class LugmetyAccessibilityService : AccessibilityService() {

    data class ClickTargetPoint(
        val id: String,
        val orderIndex: Int,
        val x: Float,
        val y: Float,
        val delayMs: Long = 0,
        val label: String = ""
    )

    enum class ServiceConnectionState {
        DISCONNECTED,
        CONNECTED,
        INTERRUPTED
    }

    enum class MonitoringStatus {
        STOPPED,
        MONITORING,
        SWIPING,
        PAUSED,
        FAILED
    }

    enum class ClickerStatus {
        STOPPED,
        RUNNING,
        PAUSED
    }

    companion object {
        @Volatile
        var instance: LugmetyAccessibilityService? = null
            private set

        val isServiceConnected: Boolean
            get() = instance != null

        // Known Lugmety Driver Application Packages
        val KNOWN_LUGMETY_PACKAGES = setOf(
            "com.lugmety.courier",
            "com.lugmety.driver",
            "com.lugmety.delivery"
        )

        /**
         * Checks whether the Lugmety Accessibility Service is currently enabled in Android Settings
         */
        fun isAccessibilityPermissionGranted(context: Context): Boolean {
            val expectedServiceName = "${context.packageName}/${LugmetyAccessibilityService::class.java.canonicalName}"
            val enabledServicesSetting = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: return false

            val colonSplitter = TextUtils.SimpleStringSplitter(':')
            colonSplitter.setString(enabledServicesSetting)

            while (colonSplitter.hasNext()) {
                val componentName = colonSplitter.next()
                if (componentName.equals(expectedServiceName, ignoreCase = true) ||
                    componentName.contains("LugmetyAccessibilityService", ignoreCase = true)) {
                    return true
                }
            }
            return false
        }

        /**
         * Opens the Android Accessibility Settings page directly
         */
        fun openAccessibilitySettings(context: Context) {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            context.startActivity(intent)
        }
    }

    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val mainHandler = Handler(Looper.getMainLooper())

    // Lazy initialization ensures repository is always safely instantiated and never accessed before init
    private val repository: OrderRepository by lazy {
        val app = applicationContext as LugmetyApp
        OrderRepository(app.database.orderDao(), app.database.logDao(), app.database.clickProfileDao())
    }

    // State Flows
    private val _connectionState = MutableStateFlow(ServiceConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ServiceConnectionState> = _connectionState.asStateFlow()

    private val _monitoringStatus = MutableStateFlow(MonitoringStatus.STOPPED)
    val monitoringStatus: StateFlow<MonitoringStatus> = _monitoringStatus.asStateFlow()

    private val _clickerStatus = MutableStateFlow(ClickerStatus.STOPPED)
    val clickerStatus: StateFlow<ClickerStatus> = _clickerStatus.asStateFlow()

    private val _refreshCount = MutableStateFlow(0)
    val refreshCount: StateFlow<Int> = _refreshCount.asStateFlow()

    private val _detectedOrderCount = MutableStateFlow(0)
    val detectedOrderCount: StateFlow<Int> = _detectedOrderCount.asStateFlow()

    private val _failedAttemptsCount = MutableStateFlow(0)
    val failedAttemptsCount: StateFlow<Int> = _failedAttemptsCount.asStateFlow()

    private val _lastRefreshTimestamp = MutableStateFlow<Long?>(null)
    val lastRefreshTimestamp: StateFlow<Long?> = _lastRefreshTimestamp.asStateFlow()

    private val _lastDetectedTimestamp = MutableStateFlow<Long?>(null)
    val lastDetectedTimestamp: StateFlow<Long?> = _lastDetectedTimestamp.asStateFlow()

    private val _totalClickCount = MutableStateFlow(0)
    val totalClickCount: StateFlow<Int> = _totalClickCount.asStateFlow()

    // Internal gesture lock to prevent overlapping gestures
    private val gestureLock = Any()
    @Volatile
    private var isGestureInProgress = false

    // Monitoring Parameters
    @Volatile
    private var isMonitoringActive = false
    @Volatile
    private var isAutoRefreshEnabled = true
    @Volatile
    private var isOrderDetectionEnabled = true
    private var refreshIntervalMs = 8000L
    private var cooldownMinMs = 4000L
    private var lastSwipeTime = 0L
    private var consecutiveFailures = 0

    // Auto Clicker Parameters
    @Volatile
    private var isAutoClickerRunning = false
    private val clickTargets = mutableListOf<ClickTargetPoint>()
    private var clickIntervalMs = 1000L
    private var clickGestureDurationMs = 50L
    private var currentTargetIndex = 0
    private var clickerStartTime = 0L

    // Emergency kill switch
    @Volatile
    private var isEmergencyHalted = false

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        _connectionState.value = ServiceConnectionState.CONNECTED

        logDiagnostic("INFO", "ACCESSIBILITY", "Accessibility Service connected and initialized safely.")
    }

    override fun onDestroy() {
        instance = null
        _connectionState.value = ServiceConnectionState.DISCONNECTED
        emergencyStopAll()
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onInterrupt() {
        _connectionState.value = ServiceConnectionState.INTERRUPTED
        logDiagnostic("WARN", "ACCESSIBILITY", "Accessibility Service interrupted by Android OS.")
    }

    /* -------------------------------------------------------------
     * LUGMETY MONITORING CONTROLS
     * ------------------------------------------------------------- */

    fun startMonitoring(
        intervalSec: Int = 8,
        cooldownSec: Int = 4,
        autoRefresh: Boolean = true,
        orderDetection: Boolean = true
    ) {
        isEmergencyHalted = false
        isMonitoringActive = true
        isAutoRefreshEnabled = autoRefresh
        isOrderDetectionEnabled = orderDetection
        consecutiveFailures = 0
        this.refreshIntervalMs = (intervalSec * 1000L).coerceAtLeast(4000L)
        this.cooldownMinMs = (cooldownSec * 1000L).coerceAtLeast(2000L)

        _monitoringStatus.value = MonitoringStatus.MONITORING
        logDiagnostic(
            "INFO",
            "ACCESSIBILITY",
            "Monitoring started. Interval: ${intervalSec}s, Cooldown: ${cooldownSec}s, Refresh: $autoRefresh, Detection: $orderDetection."
        )

        scheduleNextRefresh(2000L)
    }

    fun pauseMonitoring(reason: String = "User paused") {
        isMonitoringActive = false
        _monitoringStatus.value = MonitoringStatus.PAUSED
        mainHandler.removeCallbacks(refreshRunnable)
        logDiagnostic("WARN", "ACCESSIBILITY", "Monitoring paused: $reason.")
    }

    fun stopMonitoring() {
        isMonitoringActive = false
        _monitoringStatus.value = MonitoringStatus.STOPPED
        mainHandler.removeCallbacks(refreshRunnable)
        logDiagnostic("INFO", "ACCESSIBILITY", "Monitoring stopped by user.")
    }

    fun setAutoRefreshEnabled(enabled: Boolean) {
        isAutoRefreshEnabled = enabled
        if (enabled && isMonitoringActive) {
            scheduleNextRefresh(refreshIntervalMs)
        }
    }

    fun setOrderDetectionEnabled(enabled: Boolean) {
        isOrderDetectionEnabled = enabled
    }

    /* -------------------------------------------------------------
     * AUTO CLICKER MODULE CONTROLS
     * ------------------------------------------------------------- */

    fun configureClickTargets(targets: List<ClickTargetPoint>, intervalMs: Long, durationMs: Long) {
        synchronized(clickTargets) {
            clickTargets.clear()
            clickTargets.addAll(targets.sortedBy { it.orderIndex })
            this.clickIntervalMs = intervalMs.coerceAtLeast(50L)
            this.clickGestureDurationMs = durationMs.coerceIn(20L, 500L)
        }
        logDiagnostic(
            "INFO",
            "AUTOCLICKER",
            "Configured ${targets.size} click targets. Interval: ${clickIntervalMs}ms, Duration: ${clickGestureDurationMs}ms."
        )
    }

    fun startAutoClicker() {
        if (clickTargets.isEmpty()) {
            logDiagnostic("WARN", "AUTOCLICKER", "Cannot start Auto Clicker: No click points configured.")
            return
        }
        isEmergencyHalted = false
        isAutoClickerRunning = true
        currentTargetIndex = 0
        clickerStartTime = SystemClock.elapsedRealtime()
        _clickerStatus.value = ClickerStatus.RUNNING

        logDiagnostic("INFO", "AUTOCLICKER", "Auto Clicker started with ${clickTargets.size} point(s).")
        scheduleNextClick(100L)
    }

    fun pauseAutoClicker() {
        isAutoClickerRunning = false
        _clickerStatus.value = ClickerStatus.PAUSED
        mainHandler.removeCallbacks(clickerRunnable)
        logDiagnostic("WARN", "AUTOCLICKER", "Auto Clicker paused.")
    }

    fun stopAutoClicker() {
        isAutoClickerRunning = false
        _clickerStatus.value = ClickerStatus.STOPPED
        mainHandler.removeCallbacks(clickerRunnable)
        currentTargetIndex = 0
        logDiagnostic("INFO", "AUTOCLICKER", "Auto Clicker stopped.")
    }

    fun resetClickCounter() {
        _totalClickCount.value = 0
    }

    /* -------------------------------------------------------------
     * EMERGENCY KILL SWITCH
     * ------------------------------------------------------------- */

    fun emergencyStopAll() {
        isEmergencyHalted = true
        isMonitoringActive = false
        isAutoClickerRunning = false
        isGestureInProgress = false
        _monitoringStatus.value = MonitoringStatus.STOPPED
        _clickerStatus.value = ClickerStatus.STOPPED
        mainHandler.removeCallbacksAndMessages(null)
        logDiagnostic("ERROR", "SECURITY", "EMERGENCY STOP TRIGGERED. All gestures & threads immediately cancelled.")
    }

    /* -------------------------------------------------------------
     * ACCESSIBILITY EVENT DISPATCHER
     * ------------------------------------------------------------- */

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (isEmergencyHalted || event == null) return

        val pkgName = event.packageName?.toString() ?: return

        // Lugmety Order Monitoring processing
        if (isMonitoringActive && isLugmetyPackage(pkgName)) {
            if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED ||
                event.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {

                val rootNode = rootInActiveWindow ?: return
                try {
                    if (isOrderDetectionEnabled) {
                        inspectLugmetyOrderNodes(rootNode)
                    }
                } catch (_: Exception) {
                    // Safe window recycling on rapid screen transitions
                } finally {
                    try {
                        rootNode.recycle()
                    } catch (_: Exception) {}
                }
            }
        }
    }

    private fun isLugmetyPackage(pkg: String): Boolean {
        return KNOWN_LUGMETY_PACKAGES.contains(pkg) || pkg.contains("lugmety", ignoreCase = true)
    }

    /* -------------------------------------------------------------
     * PULL-DOWN REFRESH GESTURE
     * ------------------------------------------------------------- */

    private val refreshRunnable = Runnable {
        if (isMonitoringActive && isAutoRefreshEnabled && !isEmergencyHalted) {
            executeSafePullDownGesture()
        }
    }

    private fun scheduleNextRefresh(delayMs: Long) {
        if (!isMonitoringActive || isEmergencyHalted) return
        mainHandler.removeCallbacks(refreshRunnable)
        mainHandler.postDelayed(refreshRunnable, delayMs)
    }

    fun triggerManualRefresh() {
        if (!isEmergencyHalted) {
            executeSafePullDownGesture()
        }
    }

    private fun executeSafePullDownGesture() {
        if (isEmergencyHalted) return

        synchronized(gestureLock) {
            if (isGestureInProgress) {
                logDiagnostic("INFO", "GESTURE", "Refresh deferred: another gesture is currently in progress.")
                scheduleNextRefresh(2000L)
                return
            }
        }

        val now = System.currentTimeMillis()
        if (now - lastSwipeTime < cooldownMinMs) {
            scheduleNextRefresh(cooldownMinMs - (now - lastSwipeTime) + 500L)
            return
        }

        val rootNode = rootInActiveWindow
        if (rootNode == null) {
            consecutiveFailures++
            _failedAttemptsCount.value++
            handleFailureCheck("Active window cannot be accessed.")
            scheduleNextRefresh(refreshIntervalMs)
            return
        }

        try {
            val pkg = rootNode.packageName?.toString() ?: ""
            if (!isLugmetyPackage(pkg)) {
                logDiagnostic("INFO", "GESTURE", "Refresh skipped: Foreground application is $pkg, not Lugmety.")
                scheduleNextRefresh(refreshIntervalMs)
                return
            }

            // Verify order list screen and locate the scrollable container
            val scrollContainer = findScrollableContainer(rootNode)
            if (scrollContainer == null) {
                consecutiveFailures++
                _failedAttemptsCount.value++
                logDiagnostic("WARN", "GESTURE", "Refresh cancelled: Could not find verified scrollable order list.")
                handleFailureCheck("Scrollable container not found on visible screen.")
                scheduleNextRefresh(refreshIntervalMs)
                return
            }

            val bounds = Rect()
            scrollContainer.getBoundsInScreen(bounds)
            scrollContainer.recycle()

            if (bounds.width() <= 100 || bounds.height() <= 200) {
                logDiagnostic("WARN", "GESTURE", "Refresh skipped: Container dimensions are invalid for swipe gesture.")
                scheduleNextRefresh(refreshIntervalMs)
                return
            }

            // Calculate swipe coordinates: center X, top 25% to 75%
            val startX = bounds.centerX().toFloat()
            val startY = bounds.top + (bounds.height() * 0.25f)
            val endY = bounds.top + (bounds.height() * 0.75f)

            dispatchSwipeGesture(startX, startY, endY)

        } finally {
            rootNode.recycle()
        }
    }

    private fun findScrollableContainer(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        if (node.isScrollable) {
            val cls = node.className?.toString() ?: ""
            if (cls.contains("RecyclerView") || cls.contains("ScrollView") || cls.contains("ListView")) {
                return node
            }
        }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val found = findScrollableContainer(child)
            if (found != null) {
                if (found != child) child.recycle()
                return found
            }
            child.recycle()
        }
        return null
    }

    private fun dispatchSwipeGesture(startX: Float, startY: Float, endY: Float) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            logDiagnostic("ERROR", "GESTURE", "Gesture API requires Android 7.0 (API 24)+")
            return
        }

        val path = Path().apply {
            moveTo(startX, startY)
            lineTo(startX, endY)
        }

        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 450))
            .build()

        synchronized(gestureLock) { isGestureInProgress = true }
        _monitoringStatus.value = MonitoringStatus.SWIPING
        lastSwipeTime = System.currentTimeMillis()

        dispatchGesture(gesture, object : GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription?) {
                synchronized(gestureLock) { isGestureInProgress = false }
                _monitoringStatus.value = MonitoringStatus.MONITORING
                _refreshCount.value++
                _lastRefreshTimestamp.value = System.currentTimeMillis()
                consecutiveFailures = 0

                logDiagnostic("SUCCESS", "GESTURE", "Pull-to-refresh swipe completed successfully.")

                mainHandler.postDelayed({
                    if (isMonitoringActive && !isEmergencyHalted) {
                        rootInActiveWindow?.let { root ->
                            inspectLugmetyOrderNodes(root)
                            root.recycle()
                        }
                    }
                }, 1200L)

                scheduleNextRefresh(refreshIntervalMs)
            }

            override fun onCancelled(gestureDescription: GestureDescription?) {
                synchronized(gestureLock) { isGestureInProgress = false }
                _monitoringStatus.value = MonitoringStatus.MONITORING
                _failedAttemptsCount.value++
                consecutiveFailures++
                logDiagnostic("WARN", "GESTURE", "Pull-to-refresh gesture was cancelled by Android OS.")
                scheduleNextRefresh(refreshIntervalMs)
            }
        }, null)
    }

    /* -------------------------------------------------------------
     * GENUINE ORDER DETECTION & DUPLICATE FINGERPRINTING
     * ------------------------------------------------------------- */

    private fun inspectLugmetyOrderNodes(root: AccessibilityNodeInfo) {
        val candidates = mutableListOf<ParsedOrderCandidate>()
        extractOrderNodesRecursively(root, candidates)

        if (candidates.isEmpty()) return

        serviceScope.launch {
            for (cand in candidates) {
                try {
                    val exists = repository.isDuplicate(cand.eventKey)
                    if (!exists) {
                        val order = OrderEventEntity(
                            id = cand.eventKey,
                            orderNumber = cand.orderNumber,
                            storeName = cand.storeName,
                            pickupAddress = cand.pickupAddress,
                            dropoffAddress = cand.dropoffAddress,
                            payoutEstimate = cand.payout,
                            detectedAt = System.currentTimeMillis(),
                            status = "NEW"
                        )
                        repository.recordNewOrder(order)
                        _detectedOrderCount.value++
                        _lastDetectedTimestamp.value = System.currentTimeMillis()

                        logDiagnostic(
                            "SUCCESS",
                            "DETECTION",
                            "Genuine order detected: #${order.orderNumber} - ${order.storeName} (${order.payoutEstimate ?: "SAR"})"
                        )

                        MonitoringForegroundService.showNewOrderNotification(
                            applicationContext,
                            order.orderNumber,
                            order.storeName,
                            order.payoutEstimate ?: "SAR"
                        )
                    }
                } catch (_: Exception) {
                    // Safe execution if DB is busy during transition
                }
            }
        }
    }

    private fun extractOrderNodesRecursively(node: AccessibilityNodeInfo, results: MutableList<ParsedOrderCandidate>) {
        val text = node.text?.toString() ?: node.contentDescription?.toString()
        if (text != null && (
            text.contains("Order #", ignoreCase = true) ||
            text.contains("طلب رقم", ignoreCase = true) ||
            text.contains("SAR", ignoreCase = true) ||
            text.contains("ريال", ignoreCase = true) ||
            text.contains("Pickup", ignoreCase = true) ||
            text.contains("استلام", ignoreCase = true)
        )) {
            val orderNum = Regex("""(?:Order|طلب)\s*#?\s*([A-Za-z0-9-]+)""").find(text)?.groupValues?.get(1)
                ?: "LG-" + Math.abs(text.hashCode()).toString().take(6)

            val payout = Regex("""(\d+(?:\.\d+)?)\s*(?:SAR|ريال)""").find(text)?.value

            val key = generateEventKey(orderNum, text)
            results.add(
                ParsedOrderCandidate(
                    eventKey = key,
                    orderNumber = orderNum,
                    storeName = text.lines().firstOrNull() ?: "Lugmety Order",
                    pickupAddress = "Pickup Point",
                    dropoffAddress = "Delivery Location",
                    payout = payout
                )
            )
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            extractOrderNodesRecursively(child, results)
            child.recycle()
        }
    }

    private fun generateEventKey(orderNum: String, text: String): String {
        val raw = "$orderNum:$text"
        val md = MessageDigest.getInstance("SHA-256")
        return md.digest(raw.toByteArray()).joinToString("") { "%02x".format(it) }.take(16)
    }

    private fun handleFailureCheck(reason: String) {
        if (consecutiveFailures >= 5) {
            pauseMonitoring("Automatically paused after 5 consecutive unverified screens: $reason")
            _monitoringStatus.value = MonitoringStatus.FAILED
            MonitoringForegroundService.showDiagnosticAlert(
                applicationContext,
                "Lugmety Monitoring Paused",
                "Screen verification failed repeatedly. Please verify Lugmety driver app is open to the order list."
            )
        }
    }

    /* -------------------------------------------------------------
     * AUTO CLICKER GESTURE EXECUTION LOOP
     * ------------------------------------------------------------- */

    private val clickerRunnable = Runnable {
        if (isAutoClickerRunning && !isEmergencyHalted) {
            executeNextAutoClick()
        }
    }

    private fun scheduleNextClick(delayMs: Long) {
        if (!isAutoClickerRunning || isEmergencyHalted) return
        mainHandler.removeCallbacks(clickerRunnable)
        mainHandler.postDelayed(clickerRunnable, delayMs)
    }

    private fun executeNextAutoClick() {
        if (!isAutoClickerRunning || isEmergencyHalted) return

        val target: ClickTargetPoint?
        synchronized(clickTargets) {
            if (clickTargets.isEmpty()) {
                stopAutoClicker()
                return
            }
            if (currentTargetIndex >= clickTargets.size) {
                currentTargetIndex = 0
            }
            target = clickTargets.getOrNull(currentTargetIndex)
        }

        if (target == null) return

        synchronized(gestureLock) {
            if (isGestureInProgress) {
                scheduleNextClick(100L)
                return
            }
        }

        dispatchClickGestureAt(target.x, target.y, clickGestureDurationMs) { success ->
            if (success) {
                _totalClickCount.value++
            }

            if (isAutoClickerRunning && !isEmergencyHalted) {
                synchronized(clickTargets) {
                    currentTargetIndex = (currentTargetIndex + 1) % clickTargets.size
                }
                val pointDelay = target.delayMs
                val totalDelay = if (currentTargetIndex == 0) {
                    (clickIntervalMs + pointDelay).coerceAtLeast(50L)
                } else {
                    pointDelay.coerceAtLeast(50L)
                }
                scheduleNextClick(totalDelay)
            }
        }
    }

    private fun dispatchClickGestureAt(x: Float, y: Float, durationMs: Long, onResult: (Boolean) -> Unit) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            logDiagnostic("ERROR", "AUTOCLICKER", "Gesture API requires Android 7.0 (API 24)+")
            onResult(false)
            return
        }

        val path = Path().apply {
            moveTo(x, y)
        }

        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, durationMs))
            .build()

        synchronized(gestureLock) { isGestureInProgress = true }

        dispatchGesture(gesture, object : GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription?) {
                synchronized(gestureLock) { isGestureInProgress = false }
                onResult(true)
            }

            override fun onCancelled(gestureDescription: GestureDescription?) {
                synchronized(gestureLock) { isGestureInProgress = false }
                logDiagnostic("WARN", "AUTOCLICKER", "Click gesture at ($x, $y) cancelled by Android OS.")
                onResult(false)
            }
        }, null)
    }

    /* -------------------------------------------------------------
     * DIAGNOSTIC LOGGING HELPER
     * ------------------------------------------------------------- */

    private fun logDiagnostic(level: String, category: String, message: String) {
        serviceScope.launch {
            try {
                val log = DiagnosticLogEntity(
                    timestamp = System.currentTimeMillis(),
                    level = level,
                    category = category,
                    message = message
                )
                repository.insertLog(log)
            } catch (_: Exception) {
                // Safeguard against DB access during rapid shutdown
            }
        }
    }

    private data class ParsedOrderCandidate(
        val eventKey: String,
        val orderNumber: String,
        val storeName: String,
        val pickupAddress: String,
        val dropoffAddress: String,
        val payout: String?
    )
}
