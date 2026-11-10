# Lugmety Order Assistant — Complete Android Studio Project

Native Android delivery assistant application built with **Kotlin**, **Jetpack Compose**, **Android AccessibilityService**, **Room Database (SQLite)**, and a **Movable Floating Auto Clicker Controller**.

---

## 🛠️ Project Health & Repairs Applied

1. **Complete Resource Restoration**:
   - `data_extraction_rules.xml` and `backup_rules.xml` generated in `app/src/main/res/xml/` to protect local database privacy.
   - Vector drawables created in `app/src/main/res/drawable/` (`ic_notification_order.xml`, `ic_open_app.xml`, `ic_alert.xml`, `ic_service_active.xml`, `ic_pause.xml`, `ic_stop.xml`).
   - Adaptive launcher icons and background/foreground vector assets created in `app/src/main/res/mipmap-anydpi-v26/` and `app/src/main/res/drawable/`.
   - Comprehensive color tokens defined in `app/src/main/res/values/colors.xml` matching `themes.xml`.
   - `proguard-rules.pro` and `gradle.properties` created.
   - Standard `gradle/wrapper/gradle-wrapper.properties` created for Android Studio Gradle 8.7 & AGP 8.4.1.

2. **AndroidManifest.xml Hardening**:
   - Validated package visibility queries for `com.lugmety.courier`, `com.lugmety.driver`, and `com.lugmety.delivery`.
   - Enforced `android:permission="android.permission.BIND_ACCESSIBILITY_SERVICE"` on `LugmetyAccessibilityService`.
   - Declared `SYSTEM_ALERT_WINDOW` permission for the movable floating auto clicker panel.
   - Registered `FloatingControlPanelService` and `MonitoringForegroundService` with `foregroundServiceType="specialUse"`.

3. **Room Database Preservation**:
   - Upgraded to schema `version = 2` with explicit `MIGRATION_1_2`.
   - Preserves 100% of existing `order_events` and `diagnostic_logs` records.
   - Adds `click_profiles` and `click_points` tables for local auto clicker profiles.

4. **Accessibility Service Lifecycle & Security**:
   - Safe bounds-based pull-down refresh only executes on verified Lugmety scrollable containers (`RecyclerView`, `ScrollView`, `ListView`).
   - Mutual exclusion lock (`gestureLock`) between swipe gestures and auto clicker dispatches to prevent collision.
   - Strict local-only processing. Zero external network transmissions.

---

## 🚀 How to Build the Debug APK

### Option A: In Android Studio (Standard Desktop Build)
1. Open **Android Studio** (Hedgehog, Iguana, Jellyfish, or newer).
2. Select **File > Open** and choose the `LugmetyOrderAssistant` folder.
3. Wait for Gradle Sync to complete.
4. Select **Build > Build Bundle(s) / APK(s) > Build APK(s)**.
5. The debug APK will be generated at:
   ```
   app/build/outputs/apk/debug/app-debug.apk
   ```

### Option B: 1-Click Free Cloud Build on Your Android Phone (Zero Computer Required)
1. Download the complete project ZIP via the web portal (**Download Android Project (.zip)**).
2. In your mobile browser, go to [github.com/new](https://github.com/new) and create a repository named `lugmety-assistant`.
3. Extract the ZIP on your phone (e.g. using Google Files) and upload all files (including the `.github/workflows/build-apk.yml` file).
4. Tap the **Actions** tab on your GitHub repository → select **Build Lugmety Order Assistant APK** → tap **Run workflow**.
5. In ~2 minutes, the workflow finishes. Tap the completed run, scroll to **Artifacts**, and download **LugmetyOrderAssistant-debug-apk** directly to your phone.

### Option C: On-Device Compilation with Termux (Android Phone)
```bash
pkg update && pkg install openjdk-17 gradle git unzip -y
unzip LugmetyOrderAssistant.zip -d ~/lugmety
cd ~/lugmety
./gradlew assembleDebug
# Generated APK: app/build/outputs/apk/debug/app-debug.apk
```

---

## Required Android Permissions
1. **Accessibility Service (`Settings.ACTION_ACCESSIBILITY_SETTINGS`)**:
   - Enables Lugmety order list screen inspection and pull-down swipe gestures.
   - If marked "Restricted setting" on Android 13/14: *Settings > Apps > Lugmety Order Assistant > 3 dots (top right) > Allow restricted settings*.
2. **Display Over Other Apps (`SYSTEM_ALERT_WINDOW`)**:
   - Enables the movable floating auto clicker control panel and target pins over Lugmety.
3. **Post Notifications (`POST_NOTIFICATIONS`)**:
   - Delivers high-priority sound alerts and the ongoing foreground service status notification.
4. **Battery Optimization Whitelist (`REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`)**:
   - Prevents Android Doze Mode from killing background monitoring during delivery shifts.
