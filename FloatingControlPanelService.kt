package com.lugmety.orderassistant.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.app.NotificationCompat
import com.lugmety.orderassistant.LugmetyApp
import com.lugmety.orderassistant.R
import com.lugmety.orderassistant.ui.MainActivity
import kotlinx.coroutines.*

/**
 * Floating Control Panel Service
 *
 * Provides a movable overlay window on top of Lugmety and Android screens.
 * Enables drivers to:
 * - Start, Pause, and Stop the Auto Clicker
 * - Add and remove target click points dynamically with true device screen dimension calculations
 * - View current click count and monitoring status
 * - Minimize the panel into a tiny floating pill so it never obstructs screen content
 * - Emergency Stop directly cancels all pending gestures and clicks immediately
 */
class FloatingControlPanelService : Service() {

    companion object {
        const val NOTIFICATION_ID = 2002
        const val ACTION_START_PANEL = "ACTION_START_FLOATING_PANEL"
        const val ACTION_STOP_PANEL = "ACTION_STOP_FLOATING_PANEL"

        fun start(context: Context) {
            if (!Settings.canDrawOverlays(context)) {
                return
            }
            val intent = Intent(context, FloatingControlPanelService::class.java).apply {
                action = ACTION_START_PANEL
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, FloatingControlPanelService::class.java).apply {
                action = ACTION_STOP_PANEL
            }
            context.startService(intent)
        }
    }

    private lateinit var windowManager: WindowManager
    private var floatingPanelRoot: View? = null
    private var isMinimized = false
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    // Target overlay marker views
    private val targetMarkerViews = mutableListOf<View>()
    private val currentTargetPoints = mutableListOf<LugmetyAccessibilityService.ClickTargetPoint>()

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        startForeground(NOTIFICATION_ID, buildForegroundNotification())
        createFloatingPanelView()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP_PANEL) {
            stopSelf()
        }
        return START_STICKY
    }

    override fun onDestroy() {
        serviceScope.cancel()
        removeFloatingViews()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    /**
     * Accurately calculates real device screen dimensions across all Android versions
     */
    private fun getScreenDimensions(): Pair<Int, Int> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val windowMetrics = windowManager.currentWindowMetrics
            val bounds = windowMetrics.bounds
            Pair(bounds.width(), bounds.height())
        } else {
            val dm = resources.displayMetrics
            Pair(dm.widthPixels, dm.heightPixels)
        }
    }

    private fun createFloatingPanelView() {
        if (!Settings.canDrawOverlays(this)) return

        val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val (screenWidth, screenHeight) = getScreenDimensions()

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutFlag,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 30
            y = (screenHeight * 0.15f).toInt()
        }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 12, 16, 12)
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#F00B132B")) // Navy dark semi-transparent
                cornerRadius = 24f
                setStroke(2, Color.parseColor("#48CAE4")) // Cyan accent border
            }
            elevation = 16f
        }

        // Header / Drag Handle
        val headerLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val titleTv = TextView(this).apply {
            text = "⚡ Lugmety Assistant"
            setTextColor(Color.WHITE)
            textSize = 12f
            paint.isFakeBoldText = true
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        val minimizeBtn = Button(this).apply {
            text = "—"
            setTextColor(Color.parseColor("#48CAE4"))
            textSize = 12f
            setBackgroundColor(Color.TRANSPARENT)
            setPadding(8, 0, 8, 0)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        headerLayout.addView(titleTv)
        headerLayout.addView(minimizeBtn)
        root.addView(headerLayout)

        // Content Area (Collapsible when minimized)
        val contentLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        val statusTv = TextView(this).apply {
            text = "Clicks: 0 | Idle"
            setTextColor(Color.parseColor("#90E0EF"))
            textSize = 11f
            setPadding(0, 4, 0, 8)
        }
        contentLayout.addView(statusTv)

        // Action Buttons Row (Start, Pause, Stop)
        val actionsRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }

        val startBtn = Button(this).apply {
            text = "▶ Start"
            setTextColor(Color.WHITE)
            textSize = 11f
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#0077B6"))
                cornerRadius = 12f
            }
            setPadding(12, 4, 12, 4)
        }

        val pauseBtn = Button(this).apply {
            text = "❚❚ Pause"
            setTextColor(Color.WHITE)
            textSize = 11f
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#3A506B"))
                cornerRadius = 12f
            }
            setPadding(12, 4, 12, 4)
        }

        val stopBtn = Button(this).apply {
            text = "■ Stop"
            setTextColor(Color.WHITE)
            textSize = 11f
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#881337"))
                cornerRadius = 12f
            }
            setPadding(12, 4, 12, 4)
        }

        actionsRow.addView(startBtn)
        actionsRow.addView(pauseBtn)
        actionsRow.addView(stopBtn)
        contentLayout.addView(actionsRow)

        // Point Management Row (+ Point, - Point)
        val pointsRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(0, 6, 0, 0)
        }

        val addPointBtn = Button(this).apply {
            text = "+ Add Target"
            setTextColor(Color.parseColor("#00F5D4"))
            textSize = 10f
            setBackgroundColor(Color.TRANSPARENT)
        }

        val removePointBtn = Button(this).apply {
            text = "- Remove Target"
            setTextColor(Color.parseColor("#F43F5E"))
            textSize = 10f
            setBackgroundColor(Color.TRANSPARENT)
        }

        pointsRow.addView(addPointBtn)
        pointsRow.addView(removePointBtn)
        contentLayout.addView(pointsRow)

        // Emergency Kill Button on Floating Panel
        val emergencyBtn = Button(this).apply {
            text = "⚠ EMERGENCY STOP"
            setTextColor(Color.parseColor("#F43F5E"))
            textSize = 9f
            paint.isFakeBoldText = true
            setBackgroundColor(Color.TRANSPARENT)
            setPadding(0, 4, 0, 0)
        }
        contentLayout.addView(emergencyBtn)

        root.addView(contentLayout)

        // Button Interactions
        minimizeBtn.setOnClickListener {
            isMinimized = !isMinimized
            if (isMinimized) {
                contentLayout.visibility = View.GONE
                minimizeBtn.text = "+"
                titleTv.text = "⚡ Clicker"
            } else {
                contentLayout.visibility = View.VISIBLE
                minimizeBtn.text = "—"
                titleTv.text = "⚡ Lugmety Assistant"
            }
        }

        startBtn.setOnClickListener {
            LugmetyAccessibilityService.instance?.let { service ->
                if (currentTargetPoints.isEmpty()) {
                    val (w, h) = getScreenDimensions()
                    val defaultPt = LugmetyAccessibilityService.ClickTargetPoint(
                        id = "pt-1",
                        orderIndex = 1,
                        x = w * 0.5f,
                        y = h * 0.5f,
                        delayMs = 0,
                        label = "Point 1"
                    )
                    currentTargetPoints.add(defaultPt)
                    createTargetMarker(defaultPt)
                }
                service.configureClickTargets(currentTargetPoints, 1000L, 50L)
                service.startAutoClicker()
                statusTv.text = "Clicks: ${service.totalClickCount.value} | Running"
            }
        }

        pauseBtn.setOnClickListener {
            LugmetyAccessibilityService.instance?.pauseAutoClicker()
            statusTv.text = "Paused"
        }

        stopBtn.setOnClickListener {
            LugmetyAccessibilityService.instance?.stopAutoClicker()
            statusTv.text = "Stopped"
        }

        emergencyBtn.setOnClickListener {
            LugmetyAccessibilityService.instance?.emergencyStopAll()
            statusTv.text = "Emergency Halted"
        }

        addPointBtn.setOnClickListener {
            val (w, h) = getScreenDimensions()
            val newIndex = currentTargetPoints.size + 1
            val posX = (w * 0.5f + ((newIndex - 1) * 40f)).coerceIn(80f, w - 80f)
            val posY = (h * 0.45f + ((newIndex - 1) * 70f)).coerceIn(120f, h - 120f)

            val newPt = LugmetyAccessibilityService.ClickTargetPoint(
                id = "pt-$newIndex",
                orderIndex = newIndex,
                x = posX,
                y = posY,
                delayMs = 0,
                label = "Point $newIndex"
            )
            currentTargetPoints.add(newPt)
            createTargetMarker(newPt)
            LugmetyAccessibilityService.instance?.configureClickTargets(currentTargetPoints, 1000L, 50L)
            statusTv.text = "Targets: ${currentTargetPoints.size}"
        }

        removePointBtn.setOnClickListener {
            if (currentTargetPoints.isNotEmpty()) {
                currentTargetPoints.removeAt(currentTargetPoints.size - 1)
                val lastView = targetMarkerViews.removeLastOrNull()
                lastView?.let { windowManager.removeView(it) }
                LugmetyAccessibilityService.instance?.configureClickTargets(currentTargetPoints, 1000L, 50L)
                statusTv.text = "Targets: ${currentTargetPoints.size}"
            }
        }

        // Draggable Touch Listener on Floating Panel with edge clamping
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f

        root.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val (w, h) = getScreenDimensions()
                    params.x = (initialX + (event.rawX - initialTouchX).toInt()).coerceIn(0, w - 100)
                    params.y = (initialY + (event.rawY - initialTouchY).toInt()).coerceIn(40, h - 100)
                    windowManager.updateViewLayout(root, params)
                    true
                }
                else -> false
            }
        }

        windowManager.addView(root, params)
        floatingPanelRoot = root
    }

    private fun createTargetMarker(point: LugmetyAccessibilityService.ClickTargetPoint) {
        val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val (screenWidth, screenHeight) = getScreenDimensions()
        val markerSize = 100
        val halfSize = markerSize / 2

        val clampedX = point.x.toInt().coerceIn(halfSize, screenWidth - halfSize)
        val clampedY = point.y.toInt().coerceIn(halfSize, screenHeight - halfSize)

        val markerParams = WindowManager.LayoutParams(
            markerSize,
            markerSize,
            layoutFlag,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = clampedX - halfSize
            y = clampedY - halfSize
        }

        val marker = TextView(this).apply {
            text = point.orderIndex.toString()
            setTextColor(Color.WHITE)
            textSize = 14f
            gravity = Gravity.CENTER
            paint.isFakeBoldText = true
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#D90077B6"))
                cornerRadius = 60f
                setStroke(3, Color.parseColor("#00F5D4"))
            }
        }

        var initX = 0
        var initY = 0
        var touchX = 0f
        var touchY = 0f

        marker.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initX = markerParams.x
                    initY = markerParams.y
                    touchX = event.rawX
                    touchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val (w, h) = getScreenDimensions()
                    markerParams.x = (initX + (event.rawX - touchX).toInt()).coerceIn(0, w - markerSize)
                    markerParams.y = (initY + (event.rawY - touchY).toInt()).coerceIn(40, h - markerSize)
                    windowManager.updateViewLayout(marker, markerParams)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    // Update and clamp point coordinates precisely to marker center
                    val (w, h) = getScreenDimensions()
                    val updatedX = (markerParams.x + halfSize).toFloat().coerceIn(0f, w.toFloat())
                    val updatedY = (markerParams.y + halfSize).toFloat().coerceIn(0f, h.toFloat())

                    val index = currentTargetPoints.indexOfFirst { it.id == point.id }
                    if (index != -1) {
                        currentTargetPoints[index] = point.copy(x = updatedX, y = updatedY)
                        LugmetyAccessibilityService.instance?.configureClickTargets(
                            currentTargetPoints,
                            1000L,
                            50L
                        )
                    }
                    true
                }
                else -> false
            }
        }

        windowManager.addView(marker, markerParams)
        targetMarkerViews.add(marker)
    }

    private fun removeFloatingViews() {
        floatingPanelRoot?.let {
            try {
                windowManager.removeView(it)
            } catch (_: Exception) {}
            floatingPanelRoot = null
        }
        targetMarkerViews.forEach {
            try {
                windowManager.removeView(it)
            } catch (_: Exception) {}
        }
        targetMarkerViews.clear()
    }

    private fun buildForegroundNotification(): Notification {
        val appIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, LugmetyApp.CHANNEL_MONITORING)
            .setSmallIcon(R.drawable.ic_service_active)
            .setContentTitle("Lugmety Floating Controller")
            .setContentText("Overlay active. Tap to open dashboard.")
            .setContentIntent(appIntent)
            .setOngoing(true)
            .build()
    }
}
