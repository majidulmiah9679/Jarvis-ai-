package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AccessAlarm
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.BrightnessHigh
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.FlashlightOff
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.actions.DeviceTelemetry
import com.example.actions.InstalledAppInfo
import com.example.data.ActionLogEntity
import com.example.service.JarvisNotificationListenerService
import com.example.service.ReadableNotification
import com.example.ui.theme.JarvisAmber
import com.example.ui.theme.JarvisCrimson
import com.example.ui.theme.JarvisCyan
import com.example.ui.theme.JarvisElectricBlue
import com.example.ui.theme.JarvisEmerald
import com.example.ui.theme.JarvisIceWhite
import com.example.ui.theme.JarvisMutedCyan
import com.example.ui.theme.JarvisObsidian
import com.example.ui.theme.JarvisSurfaceCard
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DeviceControlScreen(
    telemetry: DeviceTelemetry,
    installedApps: List<InstalledAppInfo>,
    actionLogs: List<ActionLogEntity>,
    activeNotifications: List<ReadableNotification>,
    onTriggerAction: (String, String, String) -> Unit,
    onClearActionLogs: () -> Unit,
    modifier: Modifier = Modifier
) {
    var mediaVolumeSlider by remember(telemetry.mediaVolumePercent) {
        mutableFloatStateOf(telemetry.mediaVolumePercent.toFloat())
    }
    var brightnessSlider by remember(telemetry.brightnessPercent) {
        mutableFloatStateOf(telemetry.brightnessPercent.toFloat())
    }
    var destinationInput by rememberSaveable { mutableStateOf("") }
    var appSearchQuery by rememberSaveable { mutableStateOf("") }
    val timeFormat = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp),
        contentPadding = PaddingValues(vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "ANDROID NATIVE CONTROL DECK",
                style = MaterialTheme.typography.headlineMedium,
                color = JarvisCyan
            )
            Text(
                text = "Every AI tool call is validated before execution. Test hardware & OS actions directly below.",
                style = MaterialTheme.typography.bodyMedium,
                color = JarvisMutedCyan
            )
        }

        // 1. Live Hardware Telemetry Matrix
        item {
            Surface(
                color = JarvisSurfaceCard,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, JarvisCyan.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "REAL-TIME HARDWARE TELEMETRY",
                        style = MaterialTheme.typography.labelLarge,
                        color = JarvisCyan
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TelemetryStatItem(
                            label = "BATTERY",
                            value = "${telemetry.batteryPercent}%",
                            sub = if (telemetry.isCharging) "CHARGING" else "${telemetry.batteryTempCelsius}°C"
                        )
                        TelemetryStatItem(
                            label = "NETWORK",
                            value = if (telemetry.isOnline) "ONLINE" else "OFFLINE",
                            sub = telemetry.networkType
                        )
                        TelemetryStatItem(
                            label = "RAM FREE",
                            value = "${telemetry.availableRamMb} MB",
                            sub = "OF ${telemetry.totalRamMb} MB"
                        )
                        TelemetryStatItem(
                            label = "TORCH",
                            value = if (telemetry.isFlashlightOn) "ACTIVE" else "STANDBY",
                            sub = "REAR LED"
                        )
                    }
                }
            }
        }

        // 2. Quick Hardware Action Grid
        item {
            Surface(
                color = JarvisSurfaceCard,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, JarvisElectricBlue.copy(alpha = 0.45f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "INSTANT HARDWARE & SYSTEM ACTIONS",
                        style = MaterialTheme.typography.labelLarge,
                        color = JarvisCyan
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        QuickControlTile(
                            icon = if (telemetry.isFlashlightOn) Icons.Filled.FlashlightOn else Icons.Filled.FlashlightOff,
                            title = if (telemetry.isFlashlightOn) "Torch OFF" else "Torch ON",
                            subtitle = "Camera LED",
                            accent = if (telemetry.isFlashlightOn) JarvisEmerald else JarvisCyan,
                            onClick = {
                                onTriggerAction(
                                    "toggle_flashlight",
                                    if (telemetry.isFlashlightOn) "off" else "on",
                                    ""
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("control_torch_button")
                        )
                        QuickControlTile(
                            icon = Icons.Filled.CameraAlt,
                            title = "Camera",
                            subtitle = "Still Photo",
                            accent = JarvisCyan,
                            onClick = { onTriggerAction("launch_camera", "photo", "") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("control_camera_button")
                        )
                        QuickControlTile(
                            icon = Icons.Filled.AccessAlarm,
                            title = "Set Alarm",
                            subtitle = "07:00 AM",
                            accent = JarvisElectricBlue,
                            onClick = { onTriggerAction("set_alarm", "07:00", "JARVIS Morning Briefing") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("control_alarm_button")
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        QuickControlTile(
                            icon = Icons.Filled.Timer,
                            title = "2m Timer",
                            subtitle = "120 Seconds",
                            accent = JarvisElectricBlue,
                            onClick = { onTriggerAction("set_timer", "120", "JARVIS Quick Timer") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("control_timer_button")
                        )
                        QuickControlTile(
                            icon = Icons.Filled.Settings,
                            title = "Settings",
                            subtitle = "Android OS",
                            accent = JarvisCyan,
                            onClick = { onTriggerAction("open_settings", "general", "") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("control_settings_button")
                        )
                        QuickControlTile(
                            icon = Icons.Filled.Share,
                            title = "Share Status",
                            subtitle = "Share Sheet",
                            accent = JarvisEmerald,
                            onClick = {
                                onTriggerAction(
                                    "share_text",
                                    "JARVIS Telemetry Report",
                                    "JARVIS AI Status: Battery ${telemetry.batteryPercent}%, Network ${telemetry.networkType}, RAM Free ${telemetry.availableRamMb}MB."
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("control_share_button")
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        QuickControlTile(
                            icon = Icons.Filled.Phone,
                            title = "Test Call",
                            subtitle = "Confirm Flow",
                            accent = JarvisAmber,
                            onClick = { onTriggerAction("prepare_call", "+8801700000000", "") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("control_call_button")
                        )
                        QuickControlTile(
                            icon = Icons.Filled.Sms,
                            title = "Test SMS",
                            subtitle = "Confirm Flow",
                            accent = JarvisAmber,
                            onClick = {
                                onTriggerAction(
                                    "prepare_sms",
                                    "+8801700000000",
                                    "Hello from JARVIS AI Assistant!"
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("control_sms_button")
                        )
                    }
                }
            }
        }

        // 3. Volume & Brightness Sliders
        item {
            Surface(
                color = JarvisSurfaceCard,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, JarvisCyan.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = null,
                            tint = JarvisCyan,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "MEDIA AUDIO VOLUME: ${mediaVolumeSlider.toInt()}%",
                            style = MaterialTheme.typography.labelLarge,
                            color = JarvisIceWhite
                        )
                    }
                    Slider(
                        value = mediaVolumeSlider,
                        onValueChange = { mediaVolumeSlider = it },
                        onValueChangeFinished = {
                            onTriggerAction("set_volume", "media", mediaVolumeSlider.toInt().toString())
                        },
                        valueRange = 0f..100f,
                        colors = SliderDefaults.colors(
                            thumbColor = JarvisCyan,
                            activeTrackColor = JarvisCyan
                        )
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.BrightnessHigh,
                                contentDescription = null,
                                tint = JarvisCyan,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "SYSTEM BRIGHTNESS: ${brightnessSlider.toInt()}%",
                                style = MaterialTheme.typography.labelLarge,
                                color = JarvisIceWhite
                            )
                        }
                        if (!telemetry.hasWriteSettingsAccess) {
                            Text(
                                text = "Requires Write Settings",
                                style = MaterialTheme.typography.labelSmall,
                                color = JarvisAmber
                            )
                        }
                    }
                    Slider(
                        value = brightnessSlider,
                        onValueChange = { brightnessSlider = it },
                        onValueChangeFinished = {
                            onTriggerAction("set_brightness", brightnessSlider.toInt().toString(), "")
                        },
                        valueRange = 5f..100f,
                        colors = SliderDefaults.colors(
                            thumbColor = JarvisElectricBlue,
                            activeTrackColor = JarvisElectricBlue
                        )
                    )
                }
            }
        }

        // 4. Navigation & Installed App Launcher
        item {
            Surface(
                color = JarvisSurfaceCard,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, JarvisCyan.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "NAVIGATION & INSTALLED APP LAUNCHER",
                        style = MaterialTheme.typography.labelLarge,
                        color = JarvisCyan
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = destinationInput,
                            onValueChange = { destinationInput = it },
                            placeholder = { Text("Enter destination (e.g. Dhaka, India Gate)…") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = JarvisCyan,
                                unfocusedBorderColor = JarvisCyan.copy(alpha = 0.35f)
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        Button(
                            onClick = {
                                onTriggerAction(
                                    "open_navigation",
                                    destinationInput.ifBlank { "Restaurants near me" },
                                    ""
                                )
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = JarvisCyan,
                                contentColor = JarvisObsidian
                            )
                        ) {
                            Icon(Icons.Filled.Navigation, contentDescription = "Navigate", modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("GO", fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = appSearchQuery,
                        onValueChange = { appSearchQuery = it },
                        placeholder = { Text("Filter ${installedApps.size} installed apps…") },
                        leadingIcon = { Icon(Icons.Filled.Apps, contentDescription = null, tint = JarvisCyan) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = JarvisCyan,
                            unfocusedBorderColor = JarvisCyan.copy(alpha = 0.35f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    val filteredApps = remember(installedApps, appSearchQuery) {
                        if (appSearchQuery.isBlank()) {
                            installedApps.take(20)
                        } else {
                            installedApps.filter {
                                it.appName.contains(appSearchQuery, ignoreCase = true) ||
                                    it.packageName.contains(appSearchQuery, ignoreCase = true)
                            }.take(20)
                        }
                    }

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(filteredApps, key = { it.packageName }) { app ->
                            Surface(
                                color = JarvisObsidian,
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, JarvisCyan.copy(alpha = 0.4f)),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable {
                                        onTriggerAction("launch_app", app.packageName, app.appName)
                                    }
                            ) {
                                Column(
                                    modifier = Modifier
                                        .padding(horizontal = 12.dp, vertical = 8.dp)
                                        .width(110.dp)
                                ) {
                                    Text(
                                        text = app.appName,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = JarvisIceWhite,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = app.packageName.substringAfterLast('.'),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = JarvisMutedCyan,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 5. Notification Reader Card (Explicit User Authorization)
        item {
            Surface(
                color = JarvisSurfaceCard,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, JarvisCyan.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.NotificationsActive,
                                contentDescription = null,
                                tint = if (telemetry.hasNotificationListenerAccess) JarvisEmerald else JarvisAmber
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "NOTIFICATION READER",
                                style = MaterialTheme.typography.labelLarge,
                                color = JarvisIceWhite
                            )
                        }
                        if (!telemetry.hasNotificationListenerAccess) {
                            OutlinedButton(
                                onClick = {
                                    onTriggerAction("open_settings", "notification_access", "")
                                },
                                border = BorderStroke(1.dp, JarvisAmber)
                            ) {
                                Text("AUTHORIZE ACCESS", color = JarvisAmber, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    if (!telemetry.hasNotificationListenerAccess) {
                        Text(
                            text = "Android requires explicit Notification Listener authorization in System Settings before JARVIS can read incoming notifications.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = JarvisMutedCyan
                        )
                    } else if (activeNotifications.isEmpty()) {
                        Text(
                            text = "Notification Access Granted. No active third-party notifications in status bar.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = JarvisEmerald
                        )
                    } else {
                        activeNotifications.take(4).forEach { notif ->
                            Text(
                                text = "• [${notif.appName}] ${notif.title}: ${notif.text}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = JarvisIceWhite,
                                modifier = Modifier.padding(vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }

        // 6. Validated Action Execution Audit Log
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "VALIDATED DEVICE ACTION AUDIT LOG (${actionLogs.size})",
                    style = MaterialTheme.typography.labelLarge,
                    color = JarvisCyan
                )
                if (actionLogs.isNotEmpty()) {
                    IconButton(onClick = onClearActionLogs) {
                        Icon(
                            imageVector = Icons.Filled.DeleteSweep,
                            contentDescription = "Clear Action Logs",
                            tint = JarvisMutedCyan
                        )
                    }
                }
            }
        }

        if (actionLogs.isEmpty()) {
            item {
                Text(
                    text = "No device actions executed yet. Ask JARVIS or tap any control tile above.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = JarvisMutedCyan
                )
            }
        } else {
            items(actionLogs, key = { it.id }) { log ->
                val statusColor = when (log.status) {
                    "EXECUTED", "CONFIRMED" -> JarvisEmerald
                    "PENDING_CONFIRMATION", "PERMISSION_REQUIRED" -> JarvisAmber
                    else -> JarvisCrimson
                }
                Surface(
                    color = JarvisSurfaceCard.copy(alpha = 0.75f),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, statusColor.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${log.toolName.uppercase()} -> ${log.target}",
                                style = MaterialTheme.typography.labelLarge,
                                color = JarvisIceWhite,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${log.status} • ${timeFormat.format(Date(log.timestamp))}",
                                style = MaterialTheme.typography.labelSmall,
                                color = statusColor
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = log.details,
                            style = MaterialTheme.typography.bodyMedium,
                            color = JarvisMutedCyan
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TelemetryStatItem(
    label: String,
    value: String,
    sub: String
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = JarvisMutedCyan)
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = JarvisIceWhite,
            fontWeight = FontWeight.Bold
        )
        Text(text = sub, style = MaterialTheme.typography.labelSmall, color = JarvisCyan)
    }
}

@Composable
private fun QuickControlTile(
    icon: ImageVector,
    title: String,
    subtitle: String,
    accent: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = JarvisObsidian,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.55f)),
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = accent,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = JarvisIceWhite,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = JarvisMutedCyan,
                maxLines = 1
            )
        }
    }
}
