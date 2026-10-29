package com.example.actions

import android.Manifest
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.BatteryManager
import android.provider.AlarmClock
import android.provider.MediaStore
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.example.service.JarvisNotificationListenerService

data class InstalledAppInfo(
    val appName: String,
    val packageName: String
)

data class DeviceTelemetry(
    val batteryPercent: Int = 100,
    val isCharging: Boolean = false,
    val batteryTempCelsius: Float = 28.0f,
    val networkType: String = "ONLINE",
    val isOnline: Boolean = true,
    val mediaVolumePercent: Int = 70,
    val ringVolumePercent: Int = 70,
    val alarmVolumePercent: Int = 80,
    val brightnessPercent: Int = 75,
    val isFlashlightOn: Boolean = false,
    val availableRamMb: Long = 4096,
    val totalRamMb: Long = 8192,
    val hasMicPermission: Boolean = false,
    val hasCameraPermission: Boolean = false,
    val hasNotificationListenerAccess: Boolean = false,
    val hasWriteSettingsAccess: Boolean = false
)

data class PendingConfirmationAction(
    val actionName: String,
    val target: String,
    val secondaryValue: String = "",
    val promptToUser: String,
    val language: String = "en"
)

data class ActionExecutionResult(
    val success: Boolean,
    val status: String, // "EXECUTED", "PENDING_CONFIRMATION", "PERMISSION_REQUIRED", "FAILED"
    val summary: String,
    val pendingConfirmation: PendingConfirmationAction? = null
)

class DeviceActionExecutor(private val context: Context) {

    private var torchEnabledState = false

    fun getLiveTelemetry(): DeviceTelemetry {
        val batteryIntent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val batteryPct = if (level >= 0 && scale > 0) (level * 100) / scale else 100
        val status = batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
            status == BatteryManager.BATTERY_STATUS_FULL
        val tempTenths = batteryIntent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 280) ?: 280
        val tempCelsius = tempTenths / 10f

        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val activeNetwork = cm?.activeNetwork
        val caps = activeNetwork?.let { cm.getNetworkCapabilities(it) }
        val isOnline = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
        val networkType = when {
            caps == null -> "OFFLINE"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "WI-FI LINK"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "CELLULAR 5G/LTE"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "ETHERNET"
            else -> "CONNECTED"
        }

        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        fun getVolumePct(stream: Int): Int {
            if (audioManager == null) return 50
            val cur = audioManager.getStreamVolume(stream)
            val max = audioManager.getStreamMaxVolume(stream).coerceAtLeast(1)
            return ((cur * 100f) / max).toInt().coerceIn(0, 100)
        }

        val mediaVol = getVolumePct(AudioManager.STREAM_MUSIC)
        val ringVol = getVolumePct(AudioManager.STREAM_RING)
        val alarmVol = getVolumePct(AudioManager.STREAM_ALARM)

        val brightnessRaw = try {
            Settings.System.getInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS, 190)
        } catch (_: Exception) {
            190
        }
        val brightnessPct = ((brightnessRaw * 100f) / 255f).toInt().coerceIn(0, 100)

        val actManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        actManager?.getMemoryInfo(memInfo)
        val availMb = (memInfo.availMem / (1024 * 1024)).coerceAtLeast(512)
        val totalMb = (memInfo.totalMem / (1024 * 1024)).coerceAtLeast(2048)

        val hasMic = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        val hasCamera = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        val hasNotifAccess = JarvisNotificationListenerService.isNotificationAccessGranted(context)
        val canWriteSettings = Settings.System.canWrite(context)

        return DeviceTelemetry(
            batteryPercent = batteryPct,
            isCharging = isCharging,
            batteryTempCelsius = tempCelsius,
            networkType = networkType,
            isOnline = isOnline,
            mediaVolumePercent = mediaVol,
            ringVolumePercent = ringVol,
            alarmVolumePercent = alarmVol,
            brightnessPercent = brightnessPct,
            isFlashlightOn = torchEnabledState,
            availableRamMb = availMb,
            totalRamMb = totalMb,
            hasMicPermission = hasMic,
            hasCameraPermission = hasCamera,
            hasNotificationListenerAccess = hasNotifAccess,
            hasWriteSettingsAccess = canWriteSettings
        )
    }

    fun getInstalledApps(): List<InstalledAppInfo> {
        return try {
            val pm = context.packageManager
            val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
            val resolveInfos = pm.queryIntentActivities(mainIntent, 0)
            resolveInfos.mapNotNull { info ->
                val pkg = info.activityInfo?.packageName ?: return@mapNotNull null
                val label = info.loadLabel(pm)?.toString()?.trim().orEmpty()
                if (label.isBlank() || pkg == context.packageName) return@mapNotNull null
                InstalledAppInfo(appName = label, packageName = pkg)
            }
                .distinctBy { it.packageName }
                .sortedBy { it.appName.lowercase() }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun executeValidatedToolCall(
        actionName: String,
        target: String,
        secondaryParam: String,
        requireSensitiveConfirmation: Boolean,
        language: String = "en"
    ): ActionExecutionResult {
        val cleanAction = actionName.trim().lowercase()
        val cleanTarget = target.trim()
        val cleanSecondary = secondaryParam.trim()

        if (cleanAction.isBlank() || cleanAction == "none") {
            return ActionExecutionResult(
                success = true,
                status = "NO_ACTION",
                summary = "Conversational response only"
            )
        }

        return when (cleanAction) {
            "get_device_telemetry", "check_battery", "check_network", "status_report" -> {
                val t = getLiveTelemetry()
                val chargingStr = if (t.isCharging) "Charging" else "Discharging"
                ActionExecutionResult(
                    success = true,
                    status = "EXECUTED",
                    summary = "Battery: ${t.batteryPercent}% ($chargingStr, ${t.batteryTempCelsius}°C) | Net: ${t.networkType} | RAM: ${t.availableRamMb}/${t.totalRamMb} MB | Media Vol: ${t.mediaVolumePercent}%"
                )
            }

            "toggle_flashlight", "flashlight" -> {
                val enable = when {
                    cleanTarget.contains("off", ignoreCase = true) ||
                        cleanTarget.contains("বন্ধ") ||
                        cleanTarget == "false" -> false
                    cleanTarget.contains("on", ignoreCase = true) ||
                        cleanTarget.contains("চালু") ||
                        cleanTarget == "true" -> true
                    else -> !torchEnabledState
                }
                setFlashlight(enable)
            }

            "set_volume" -> {
                val level = cleanSecondary.toIntOrNull()
                    ?: cleanTarget.filter { it.isDigit() }.toIntOrNull()
                    ?: 70
                val streamType = when {
                    cleanTarget.contains("ring", ignoreCase = true) -> "ring"
                    cleanTarget.contains("alarm", ignoreCase = true) -> "alarm"
                    else -> "media"
                }
                setDeviceVolume(streamType, level)
            }

            "set_brightness" -> {
                val level = cleanSecondary.toIntOrNull()
                    ?: cleanTarget.filter { it.isDigit() }.toIntOrNull()
                    ?: 80
                setScreenBrightness(level)
            }

            "set_alarm" -> {
                val parts = cleanTarget.split(":")
                val hour = parts.getOrNull(0)?.filter { it.isDigit() }?.toIntOrNull() ?: 7
                val minute = parts.getOrNull(1)?.filter { it.isDigit() }?.toIntOrNull() ?: 0
                val label = cleanSecondary.ifBlank { "JARVIS Alarm" }
                createAlarm(hour, minute, label)
            }

            "set_timer" -> {
                val seconds = cleanTarget.filter { it.isDigit() }.toIntOrNull() ?: 60
                val label = cleanSecondary.ifBlank { "JARVIS Timer" }
                createTimer(seconds, label)
            }

            "launch_app" -> {
                launchInstalledApp(cleanTarget)
            }

            "open_website" -> {
                openWebsiteOrSearch(cleanTarget)
            }

            "open_settings" -> {
                openAndroidSettings(cleanTarget)
            }

            "launch_camera" -> {
                launchCamera(cleanTarget)
            }

            "open_navigation" -> {
                openNavigation(cleanTarget)
            }

            "prepare_call" -> {
                if (requireSensitiveConfirmation) {
                    val prompt = if (language == "bn") {
                        "বস, আপনি কি নিশ্চিত যে আপনি $cleanTarget নম্বরে ডায়ালার খুলতে চান?"
                    } else {
                        "Boss, please confirm opening the phone dialer for $cleanTarget."
                    }
                    ActionExecutionResult(
                        success = true,
                        status = "PENDING_CONFIRMATION",
                        summary = "Awaiting Boss confirmation to dial $cleanTarget",
                        pendingConfirmation = PendingConfirmationAction(
                            actionName = "prepare_call",
                            target = cleanTarget,
                            secondaryValue = cleanSecondary,
                            promptToUser = prompt,
                            language = language
                        )
                    )
                } else {
                    executeConfirmedCall(cleanTarget)
                }
            }

            "prepare_sms" -> {
                if (requireSensitiveConfirmation) {
                    val prompt = if (language == "bn") {
                        "বস, $cleanTarget-কে মেসেজ পাঠানোর আগে অনুগ্রহ করে নিশ্চিত করুন: \"$cleanSecondary\""
                    } else {
                        "Boss, please confirm preparing an SMS to $cleanTarget: \"$cleanSecondary\""
                    }
                    ActionExecutionResult(
                        success = true,
                        status = "PENDING_CONFIRMATION",
                        summary = "Awaiting Boss confirmation for SMS to $cleanTarget",
                        pendingConfirmation = PendingConfirmationAction(
                            actionName = "prepare_sms",
                            target = cleanTarget,
                            secondaryValue = cleanSecondary,
                            promptToUser = prompt,
                            language = language
                        )
                    )
                } else {
                    executeConfirmedSms(cleanTarget, cleanSecondary)
                }
            }

            "share_text" -> {
                shareTextContent(cleanTarget, cleanSecondary)
            }

            "read_notifications" -> {
                readActiveNotifications()
            }

            else -> {
                ActionExecutionResult(
                    success = false,
                    status = "BLOCKED",
                    summary = "Blocked unrecognized tool request '$cleanAction' per JARVIS security policy."
                )
            }
        }
    }

    fun setFlashlight(enable: Boolean): ActionExecutionResult {
        return try {
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
                ?: return ActionExecutionResult(false, "FAILED", "Camera service unavailable on this device.")

            val cameraId = cameraManager.cameraIdList.firstOrNull { id ->
                val chars = cameraManager.getCameraCharacteristics(id)
                chars.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            } ?: cameraManager.cameraIdList.firstOrNull()

            if (cameraId == null) {
                return ActionExecutionResult(
                    false,
                    "FAILED",
                    "No hardware camera flash unit detected on this device/emulator."
                )
            }

            cameraManager.setTorchMode(cameraId, enable)
            torchEnabledState = enable
            val stateLabel = if (enable) "ON" else "OFF"
            ActionExecutionResult(
                true,
                "EXECUTED",
                "Flashlight torch turned $stateLabel (Camera ID: $cameraId)"
            )
        } catch (e: Exception) {
            ActionExecutionResult(
                false,
                "FAILED",
                "Could not toggle flashlight: ${e.localizedMessage ?: "Hardware/Emulator restriction"}"
            )
        }
    }

    fun setDeviceVolume(streamName: String, percent: Int): ActionExecutionResult {
        return try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
                ?: return ActionExecutionResult(false, "FAILED", "AudioManager unavailable.")
            val clamped = percent.coerceIn(0, 100)
            val stream = when (streamName.lowercase()) {
                "ring" -> AudioManager.STREAM_RING
                "alarm" -> AudioManager.STREAM_ALARM
                else -> AudioManager.STREAM_MUSIC
            }
            val max = audioManager.getStreamMaxVolume(stream).coerceAtLeast(1)
            val targetVol = ((clamped / 100f) * max).toInt().coerceIn(0, max)
            audioManager.setStreamVolume(stream, targetVol, AudioManager.FLAG_SHOW_UI)
            ActionExecutionResult(
                true,
                "EXECUTED",
                "Adjusted ${streamName.uppercase()} volume to $clamped% ($targetVol/$max)"
            )
        } catch (e: Exception) {
            ActionExecutionResult(
                false,
                "FAILED",
                "Volume adjustment restricted (Do Not Disturb or hardware policy): ${e.localizedMessage}"
            )
        }
    }

    fun setScreenBrightness(percent: Int): ActionExecutionResult {
        val clamped = percent.coerceIn(5, 100)
        return try {
            if (Settings.System.canWrite(context)) {
                val rawValue = ((clamped / 100f) * 255f).toInt().coerceIn(10, 255)
                Settings.System.putInt(
                    context.contentResolver,
                    Settings.System.SCREEN_BRIGHTNESS_MODE,
                    Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL
                )
                Settings.System.putInt(
                    context.contentResolver,
                    Settings.System.SCREEN_BRIGHTNESS,
                    rawValue
                )
                ActionExecutionResult(
                    true,
                    "EXECUTED",
                    "System screen brightness set to $clamped% ($rawValue/255)"
                )
            } else {
                openAndroidSettings("write_settings")
                ActionExecutionResult(
                    false,
                    "PERMISSION_REQUIRED",
                    "Modify System Settings permission required for system-wide brightness ($clamped%). Opened Android permission screen."
                )
            }
        } catch (e: Exception) {
            ActionExecutionResult(
                false,
                "FAILED",
                "Could not modify system brightness: ${e.localizedMessage}"
            )
        }
    }

    fun createAlarm(hour: Int, minute: Int, message: String): ActionExecutionResult {
        return try {
            val h = hour.coerceIn(0, 23)
            val m = minute.coerceIn(0, 59)
            val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
                putExtra(AlarmClock.EXTRA_HOUR, h)
                putExtra(AlarmClock.EXTRA_MINUTES, m)
                putExtra(AlarmClock.EXTRA_MESSAGE, message)
                putExtra(AlarmClock.EXTRA_SKIP_UI, false)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            val timeFormatted = String.format("%02d:%02d", h, m)
            ActionExecutionResult(
                true,
                "EXECUTED",
                "Scheduled alarm for $timeFormatted ('$message')"
            )
        } catch (e: Exception) {
            ActionExecutionResult(
                false,
                "FAILED",
                "Could not launch Clock app to set alarm: ${e.localizedMessage}"
            )
        }
    }

    fun createTimer(seconds: Int, label: String): ActionExecutionResult {
        return try {
            val safeSeconds = seconds.coerceAtLeast(5)
            val intent = Intent(AlarmClock.ACTION_SET_TIMER).apply {
                putExtra(AlarmClock.EXTRA_LENGTH, safeSeconds)
                putExtra(AlarmClock.EXTRA_MESSAGE, label)
                putExtra(AlarmClock.EXTRA_SKIP_UI, false)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            ActionExecutionResult(
                true,
                "EXECUTED",
                "Started ${safeSeconds}s timer ('$label')"
            )
        } catch (e: Exception) {
            ActionExecutionResult(
                false,
                "FAILED",
                "Could not launch Timer intent: ${e.localizedMessage}"
            )
        }
    }

    fun launchInstalledApp(appNameOrPackage: String): ActionExecutionResult {
        val query = appNameOrPackage.trim()
        if (query.isBlank()) {
            return ActionExecutionResult(false, "FAILED", "No app name specified.")
        }

        // Common system shortcuts if user asks for Settings, Camera, Browser, Maps, Phone
        val lower = query.lowercase()
        when {
            lower == "settings" || lower.contains("সেটিংস") -> return openAndroidSettings("general")
            lower == "camera" || lower.contains("ক্যামেরা") -> return launchCamera("photo")
            lower == "youtube" -> {
                val ytPkg = context.packageManager.getLaunchIntentForPackage("com.google.android.youtube")
                if (ytPkg != null) {
                    ytPkg.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(ytPkg)
                    return ActionExecutionResult(true, "EXECUTED", "Launched YouTube app")
                } else {
                    return openWebsiteOrSearch("https://www.youtube.com")
                }
            }
        }

        val installed = getInstalledApps()
        val matched = installed.firstOrNull {
            it.appName.equals(query, ignoreCase = true) ||
                it.packageName.equals(query, ignoreCase = true)
        } ?: installed.firstOrNull {
            it.appName.contains(query, ignoreCase = true) ||
                it.packageName.contains(query, ignoreCase = true)
        }

        if (matched != null) {
            val launchIntent = context.packageManager.getLaunchIntentForPackage(matched.packageName)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launchIntent)
                return ActionExecutionResult(
                    true,
                    "EXECUTED",
                    "Launched ${matched.appName} (${matched.packageName})"
                )
            }
        }

        return ActionExecutionResult(
            false,
            "FAILED",
            "App '$query' is not installed on this device."
        )
    }

    fun openWebsiteOrSearch(urlOrQuery: String): ActionExecutionResult {
        return try {
            val finalUrl = when {
                urlOrQuery.startsWith("http://", ignoreCase = true) ||
                    urlOrQuery.startsWith("https://", ignoreCase = true) -> urlOrQuery
                urlOrQuery.contains(".") && !urlOrQuery.contains(" ") -> "https://$urlOrQuery"
                else -> "https://www.google.com/search?q=${Uri.encode(urlOrQuery)}"
            }
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(finalUrl)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            ActionExecutionResult(
                true,
                "EXECUTED",
                "Opened browser: $finalUrl"
            )
        } catch (e: Exception) {
            ActionExecutionResult(
                false,
                "FAILED",
                "Failed to open browser: ${e.localizedMessage}"
            )
        }
    }

    fun openAndroidSettings(section: String): ActionExecutionResult {
        return try {
            val action = when (section.lowercase().trim()) {
                "wifi", "wi-fi", "network" -> Settings.ACTION_WIFI_SETTINGS
                "bluetooth" -> Settings.ACTION_BLUETOOTH_SETTINGS
                "location", "gps" -> Settings.ACTION_LOCATION_SOURCE_SETTINGS
                "sound", "volume", "audio" -> Settings.ACTION_SOUND_SETTINGS
                "display", "brightness" -> Settings.ACTION_DISPLAY_SETTINGS
                "battery", "power" -> Intent.ACTION_POWER_USAGE_SUMMARY
                "notification", "notifications", "notification_access", "notification_listener" ->
                    Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS
                "write_settings" -> Settings.ACTION_MANAGE_WRITE_SETTINGS
                "app", "app_details", "permissions" -> Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                else -> Settings.ACTION_SETTINGS
            }
            val intent = Intent(action).apply {
                if (action == Settings.ACTION_MANAGE_WRITE_SETTINGS ||
                    action == Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                ) {
                    data = Uri.parse("package:${context.packageName}")
                }
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            ActionExecutionResult(
                true,
                "EXECUTED",
                "Opened Android Settings ($section)"
            )
        } catch (e: Exception) {
            val fallback = Intent(Settings.ACTION_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(fallback)
            ActionExecutionResult(true, "EXECUTED", "Opened Android System Settings")
        }
    }

    fun launchCamera(mode: String): ActionExecutionResult {
        return try {
            val action = if (mode.contains("video", ignoreCase = true)) {
                MediaStore.INTENT_ACTION_VIDEO_CAMERA
            } else {
                MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA
            }
            val intent = Intent(action).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            ActionExecutionResult(
                true,
                "EXECUTED",
                "Launched Android Camera ($mode mode)"
            )
        } catch (e: Exception) {
            ActionExecutionResult(
                false,
                "FAILED",
                "No camera application found: ${e.localizedMessage}"
            )
        }
    }

    fun openNavigation(destination: String): ActionExecutionResult {
        return try {
            val encoded = Uri.encode(destination.ifBlank { "Near me" })
            val geoUri = Uri.parse("geo:0,0?q=$encoded")
            val mapIntent = Intent(Intent.ACTION_VIEW, geoUri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (mapIntent.resolveActivity(context.packageManager) != null) {
                context.startActivity(mapIntent)
            } else {
                openWebsiteOrSearch("https://www.google.com/maps/search/?api=1&query=$encoded")
            }
            ActionExecutionResult(
                true,
                "EXECUTED",
                "Opened Navigation for destination: '$destination'"
            )
        } catch (e: Exception) {
            ActionExecutionResult(
                false,
                "FAILED",
                "Navigation launch failed: ${e.localizedMessage}"
            )
        }
    }

    fun executeConfirmedCall(phoneNumber: String): ActionExecutionResult {
        return try {
            val sanitized = phoneNumber.filter { it.isDigit() || it == '+' || it == '#' || it == '*' }
            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$sanitized")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            ActionExecutionResult(
                true,
                "CONFIRMED",
                "Confirmed & opened Phone Dialer for $sanitized"
            )
        } catch (e: Exception) {
            ActionExecutionResult(
                false,
                "FAILED",
                "Could not open Phone Dialer: ${e.localizedMessage}"
            )
        }
    }

    fun executeConfirmedSms(recipient: String, body: String): ActionExecutionResult {
        return try {
            val sanitized = recipient.filter { it.isDigit() || it == '+' }
            val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$sanitized")).apply {
                putExtra("sms_body", body)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            ActionExecutionResult(
                true,
                "CONFIRMED",
                "Confirmed & prepared SMS to $recipient: \"$body\""
            )
        } catch (e: Exception) {
            ActionExecutionResult(
                false,
                "FAILED",
                "Could not open SMS composer: ${e.localizedMessage}"
            )
        }
    }

    fun shareTextContent(title: String, content: String): ActionExecutionResult {
        return try {
            val bodyText = content.ifBlank { title }
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, title.ifBlank { "Shared from JARVIS AI" })
                putExtra(Intent.EXTRA_TEXT, bodyText)
            }
            val chooser = Intent.createChooser(sendIntent, "Share via JARVIS AI").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
            ActionExecutionResult(
                true,
                "EXECUTED",
                "Opened Android Share Sheet"
            )
        } catch (e: Exception) {
            ActionExecutionResult(
                false,
                "FAILED",
                "Failed to open Share Sheet: ${e.localizedMessage}"
            )
        }
    }

    fun readActiveNotifications(): ActionExecutionResult {
        if (!JarvisNotificationListenerService.isNotificationAccessGranted(context)) {
            return ActionExecutionResult(
                false,
                "PERMISSION_REQUIRED",
                "Notification Access is not granted yet. Tap 'Grant Notification Access' in Controls or System tab so JARVIS can read notifications."
            )
        }
        val list = JarvisNotificationListenerService.notifications.value
        if (list.isEmpty()) {
            return ActionExecutionResult(
                true,
                "EXECUTED",
                "Notification Listener active: 0 unread third-party notifications in status bar."
            )
        }
        val formatted = list.take(5).joinToString(" | ") { "${it.appName}: ${it.title} - ${it.text}" }
        return ActionExecutionResult(
            true,
            "EXECUTED",
            "Read ${list.size} active notifications: $formatted"
        )
    }
}
