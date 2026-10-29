package com.example.util

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SignalWifiOff
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.service.JarvisNotificationListenerService
import com.example.ui.theme.JarvisAmber
import com.example.ui.theme.JarvisCrimson
import com.example.ui.theme.JarvisCyan
import com.example.ui.theme.JarvisDeepNavy
import com.example.ui.theme.JarvisElectricBlue
import com.example.ui.theme.JarvisEmerald
import com.example.ui.theme.JarvisIceWhite
import com.example.ui.theme.JarvisMutedCyan
import com.example.ui.theme.JarvisObsidian
import com.example.ui.theme.JarvisSurfaceCard

enum class PermissionGrantStatus {
    GRANTED,
    NOT_REQUESTED,
    SHOULD_SHOW_RATIONALE,
    PERMANENTLY_DENIED
}

data class NetworkAccessSnapshot(
    val hasInternetManifestPermission: Boolean = true,
    val hasNetworkStateManifestPermission: Boolean = true,
    val isConnected: Boolean = true,
    val isValidatedInternet: Boolean = true,
    val transportLabel: String = "WI-FI LINK",
    val isMetered: Boolean = false
)

data class JarvisPermissionsSnapshot(
    val micStatus: PermissionGrantStatus = PermissionGrantStatus.NOT_REQUESTED,
    val cameraStatus: PermissionGrantStatus = PermissionGrantStatus.NOT_REQUESTED,
    val notificationPermissionStatus: PermissionGrantStatus = PermissionGrantStatus.GRANTED,
    val hasWriteSettingsAccess: Boolean = false,
    val hasNotificationListenerAccess: Boolean = false,
    val network: NetworkAccessSnapshot = NetworkAccessSnapshot()
) {
    val isMicGranted: Boolean get() = micStatus == PermissionGrantStatus.GRANTED
    val isCameraGranted: Boolean get() = cameraStatus == PermissionGrantStatus.GRANTED
    val isReadyForVoiceOnline: Boolean get() = isMicGranted && network.isConnected
}

sealed interface PermissionDialogState {
    data object Hidden : PermissionDialogState
    data object MicRationale : PermissionDialogState
    data object MicPermanentlyDenied : PermissionDialogState
    data object NetworkOffline : PermissionDialogState
}

class JarvisPermissionsController(
    val snapshot: JarvisPermissionsSnapshot,
    val dialogState: PermissionDialogState,
    val requestMicPermission: (onGranted: (() -> Unit)?) -> Unit,
    val requestCameraPermission: () -> Unit,
    val requestVoiceSuitePermissions: (onAllReady: (() -> Unit)?) -> Unit,
    val verifyNetworkOrPrompt: (onOnline: () -> Unit) -> Unit,
    val openAppPermissionSettings: () -> Unit,
    val openWifiSettings: () -> Unit,
    val openWirelessSettings: () -> Unit,
    val dismissDialog: () -> Unit,
    val refreshNow: () -> Unit
)

private fun Context.findActivity(): Activity? {
    var ctx = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

fun inspectNetworkAccess(context: Context): NetworkAccessSnapshot {
    val hasInternetPerm = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.INTERNET
    ) == PackageManager.PERMISSION_GRANTED

    val hasNetStatePerm = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_NETWORK_STATE
    ) == PackageManager.PERMISSION_GRANTED

    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
    val activeNetwork = cm?.activeNetwork
    val caps = activeNetwork?.let { cm.getNetworkCapabilities(it) }

    val hasInternetCap = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
    val isValidated = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) == true
    val isMetered = cm?.isActiveNetworkMetered ?: false

    val transport = when {
        caps == null || !hasInternetCap -> "OFFLINE"
        caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "WI-FI LINK"
        caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "CELLULAR 5G/LTE"
        caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "ETHERNET"
        caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> "VPN TUNNEL"
        else -> "ONLINE"
    }

    return NetworkAccessSnapshot(
        hasInternetManifestPermission = hasInternetPerm,
        hasNetworkStateManifestPermission = hasNetStatePerm,
        isConnected = hasInternetCap,
        isValidatedInternet = isValidated || hasInternetCap,
        transportLabel = transport,
        isMetered = isMetered
    )
}

fun evaluatePermissionStatus(
    context: Context,
    permission: String,
    hasRequestedBefore: Boolean
): PermissionGrantStatus {
    val granted = ContextCompat.checkSelfPermission(
        context,
        permission
    ) == PackageManager.PERMISSION_GRANTED
    if (granted) return PermissionGrantStatus.GRANTED

    val activity = context.findActivity()
    val showRationale = activity != null &&
        ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)

    return when {
        showRationale -> PermissionGrantStatus.SHOULD_SHOW_RATIONALE
        hasRequestedBefore -> PermissionGrantStatus.PERMANENTLY_DENIED
        else -> PermissionGrantStatus.NOT_REQUESTED
    }
}

@Composable
fun rememberJarvisPermissionsController(
    onPermissionsUpdated: () -> Unit = {}
): JarvisPermissionsController {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val prefs = remember {
        context.getSharedPreferences("jarvis_permission_tracker", Context.MODE_PRIVATE)
    }

    var micRequestedBefore by remember {
        mutableStateOf(prefs.getBoolean("mic_requested", false))
    }
    var cameraRequestedBefore by remember {
        mutableStateOf(prefs.getBoolean("camera_requested", false))
    }
    var notifRequestedBefore by remember {
        mutableStateOf(prefs.getBoolean("notif_requested", false))
    }

    var refreshTrigger by remember { mutableIntStateOf(0) }
    var dialogState by remember { mutableStateOf<PermissionDialogState>(PermissionDialogState.Hidden) }
    var pendingMicSuccessCallback by remember { mutableStateOf<(() -> Unit)?>(null) }

    var networkSnapshot by remember { mutableStateOf(inspectNetworkAccess(context)) }

    val snapshot = remember(
        refreshTrigger,
        networkSnapshot,
        micRequestedBefore,
        cameraRequestedBefore,
        notifRequestedBefore
    ) {
        val micStat = evaluatePermissionStatus(
            context,
            Manifest.permission.RECORD_AUDIO,
            micRequestedBefore
        )
        val camStat = evaluatePermissionStatus(
            context,
            Manifest.permission.CAMERA,
            cameraRequestedBefore
        )
        val notifStat = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            evaluatePermissionStatus(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
                notifRequestedBefore
            )
        } else {
            PermissionGrantStatus.GRANTED
        }

        JarvisPermissionsSnapshot(
            micStatus = micStat,
            cameraStatus = camStat,
            notificationPermissionStatus = notifStat,
            hasWriteSettingsAccess = Settings.System.canWrite(context),
            hasNotificationListenerAccess = JarvisNotificationListenerService.isNotificationAccessGranted(context),
            network = networkSnapshot
        )
    }

    // Single Microphone Permission Launcher
    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        prefs.edit().putBoolean("mic_requested", true).apply()
        micRequestedBefore = true
        refreshTrigger++
        onPermissionsUpdated()

        if (isGranted) {
            dialogState = PermissionDialogState.Hidden
            pendingMicSuccessCallback?.invoke()
            pendingMicSuccessCallback = null
        } else {
            val activity = context.findActivity()
            val showRationale = activity != null &&
                ActivityCompat.shouldShowRequestPermissionRationale(
                    activity,
                    Manifest.permission.RECORD_AUDIO
                )
            dialogState = if (showRationale) {
                PermissionDialogState.MicRationale
            } else {
                PermissionDialogState.MicPermanentlyDenied
            }
            pendingMicSuccessCallback = null
        }
    }

    // Camera Permission Launcher
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {
        prefs.edit().putBoolean("camera_requested", true).apply()
        cameraRequestedBefore = true
        refreshTrigger++
        onPermissionsUpdated()
    }

    // Multi-permission Launcher (Microphone + Post Notifications on Android 13+)
    val multiVoicePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        prefs.edit()
            .putBoolean("mic_requested", true)
            .putBoolean("notif_requested", true)
            .apply()
        micRequestedBefore = true
        notifRequestedBefore = true
        refreshTrigger++
        onPermissionsUpdated()

        val micGranted = results[Manifest.permission.RECORD_AUDIO] == true ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED

        if (micGranted) {
            dialogState = PermissionDialogState.Hidden
            pendingMicSuccessCallback?.invoke()
            pendingMicSuccessCallback = null
        } else {
            dialogState = PermissionDialogState.MicRationale
            pendingMicSuccessCallback = null
        }
    }

    // Real-time ConnectivityManager.NetworkCallback + Lifecycle ON_RESUME observer
    DisposableEffect(lifecycleOwner, context) {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                networkSnapshot = inspectNetworkAccess(context)
                onPermissionsUpdated()
            }

            override fun onLost(network: Network) {
                networkSnapshot = inspectNetworkAccess(context)
                onPermissionsUpdated()
            }

            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities
            ) {
                networkSnapshot = inspectNetworkAccess(context)
                onPermissionsUpdated()
            }
        }

        try {
            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()
            cm?.registerNetworkCallback(request, networkCallback)
        } catch (_: Exception) {
        }

        val lifecycleObserver = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                networkSnapshot = inspectNetworkAccess(context)
                refreshTrigger++
                onPermissionsUpdated()
            }
        }
        lifecycleOwner.lifecycle.addObserver(lifecycleObserver)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(lifecycleObserver)
            try {
                cm?.unregisterNetworkCallback(networkCallback)
            } catch (_: Exception) {
            }
        }
    }

    val openAppSettings: () -> Unit = {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:${context.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {
        }
    }

    val openWifiSettings: () -> Unit = {
        try {
            val intent = Intent(Settings.ACTION_WIFI_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {
        }
    }

    val openWirelessSettings: () -> Unit = {
        try {
            val intent = Intent(Settings.ACTION_WIRELESS_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            openWifiSettings()
        }
    }

    val requestMic: ((() -> Unit)?) -> Unit = { onGranted ->
        val currentlyGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (currentlyGranted) {
            onGranted?.invoke()
        } else {
            pendingMicSuccessCallback = onGranted
            val currentStatus = evaluatePermissionStatus(
                context,
                Manifest.permission.RECORD_AUDIO,
                micRequestedBefore
            )
            if (currentStatus == PermissionGrantStatus.SHOULD_SHOW_RATIONALE) {
                dialogState = PermissionDialogState.MicRationale
            } else {
                micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }

    val requestVoiceSuite: ((() -> Unit)?) -> Unit = { onReady ->
        val permissionsToRequest = buildList {
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.RECORD_AUDIO
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                add(Manifest.permission.RECORD_AUDIO)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        if (permissionsToRequest.isEmpty()) {
            onReady?.invoke()
        } else {
            pendingMicSuccessCallback = onReady
            multiVoicePermissionLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }

    val verifyNetwork: (() -> Unit) -> Unit = { onOnline ->
        val latestNet = inspectNetworkAccess(context)
        networkSnapshot = latestNet
        if (latestNet.isConnected) {
            onOnline()
        } else {
            dialogState = PermissionDialogState.NetworkOffline
        }
    }

    // Render interactive HUD dialog when rationale, permanent denial, or offline recovery is triggered
    when (dialogState) {
        PermissionDialogState.Hidden -> Unit

        PermissionDialogState.MicRationale -> {
            AlertDialog(
                onDismissRequest = { dialogState = PermissionDialogState.Hidden },
                containerColor = JarvisDeepNavy,
                titleContentColor = JarvisCyan,
                textContentColor = JarvisIceWhite,
                icon = {
                    Icon(
                        imageVector = Icons.Filled.Mic,
                        contentDescription = null,
                        tint = JarvisCyan,
                        modifier = Modifier.size(32.dp)
                    )
                },
                title = {
                    Text(
                        text = "MICROPHONE ACCESS REQUIRED",
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                text = {
                    Text(
                        text = "Boss, JARVIS requires Android RECORD_AUDIO permission to listen to your voice commands in English and বাংলা and animate the live voice waveform. Audio is only processed when you actively tap the Microphone or enable Hands-Free mode.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            dialogState = PermissionDialogState.Hidden
                            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = JarvisCyan,
                            contentColor = JarvisObsidian
                        ),
                        modifier = Modifier.testTag("rationale_grant_mic_btn")
                    ) {
                        Text("ALLOW MICROPHONE", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    OutlinedButton(
                        onClick = { dialogState = PermissionDialogState.Hidden },
                        border = BorderStroke(1.dp, JarvisMutedCyan)
                    ) {
                        Text("NOT NOW", color = JarvisMutedCyan)
                    }
                }
            )
        }

        PermissionDialogState.MicPermanentlyDenied -> {
            AlertDialog(
                onDismissRequest = { dialogState = PermissionDialogState.Hidden },
                containerColor = JarvisDeepNavy,
                titleContentColor = JarvisAmber,
                textContentColor = JarvisIceWhite,
                icon = {
                    Icon(
                        imageVector = Icons.Filled.MicOff,
                        contentDescription = null,
                        tint = JarvisAmber,
                        modifier = Modifier.size(32.dp)
                    )
                },
                title = {
                    Text(
                        text = "ENABLE MICROPHONE IN SETTINGS",
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                text = {
                    Text(
                        text = "Microphone permission was denied with 'Don't ask again'. Please tap 'Open App Settings' -> Permissions -> Microphone -> Allow while using the app so JARVIS can hear your voice commands.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            dialogState = PermissionDialogState.Hidden
                            openAppSettings()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = JarvisAmber,
                            contentColor = JarvisObsidian
                        ),
                        modifier = Modifier.testTag("open_app_settings_mic_btn")
                    ) {
                        Icon(Icons.Filled.Settings, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("OPEN APP SETTINGS", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    OutlinedButton(
                        onClick = { dialogState = PermissionDialogState.Hidden },
                        border = BorderStroke(1.dp, JarvisMutedCyan)
                    ) {
                        Text("CANCEL", color = JarvisMutedCyan)
                    }
                }
            )
        }

        PermissionDialogState.NetworkOffline -> {
            AlertDialog(
                onDismissRequest = { dialogState = PermissionDialogState.Hidden },
                containerColor = JarvisDeepNavy,
                titleContentColor = JarvisCrimson,
                textContentColor = JarvisIceWhite,
                icon = {
                    Icon(
                        imageVector = Icons.Filled.SignalWifiOff,
                        contentDescription = null,
                        tint = JarvisCrimson,
                        modifier = Modifier.size(32.dp)
                    )
                },
                title = {
                    Text(
                        text = "NETWORK LINK OFFLINE",
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                text = {
                    Text(
                        text = "Boss, JARVIS's Gemini AI Brain and ElevenLabs neural voice require an active Wi-Fi or Cellular internet connection. Please enable Wi-Fi or Mobile Data to restore online intelligence.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            dialogState = PermissionDialogState.Hidden
                            openWifiSettings()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = JarvisCyan,
                            contentColor = JarvisObsidian
                        ),
                        modifier = Modifier.testTag("open_wifi_settings_btn")
                    ) {
                        Icon(Icons.Filled.Wifi, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("WI-FI SETTINGS", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    OutlinedButton(
                        onClick = { dialogState = PermissionDialogState.Hidden },
                        border = BorderStroke(1.dp, JarvisMutedCyan)
                    ) {
                        Text("DISMISS", color = JarvisMutedCyan)
                    }
                }
            )
        }
    }

    return remember(snapshot, dialogState) {
        JarvisPermissionsController(
            snapshot = snapshot,
            dialogState = dialogState,
            requestMicPermission = requestMic,
            requestCameraPermission = {
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            },
            requestVoiceSuitePermissions = requestVoiceSuite,
            verifyNetworkOrPrompt = verifyNetwork,
            openAppPermissionSettings = openAppSettings,
            openWifiSettings = openWifiSettings,
            openWirelessSettings = openWirelessSettings,
            dismissDialog = { dialogState = PermissionDialogState.Hidden },
            refreshNow = {
                networkSnapshot = inspectNetworkAccess(context)
                refreshTrigger++
                onPermissionsUpdated()
            }
        )
    }
}

@Composable
fun JarvisPermissionAndNetworkBanner(
    snapshot: JarvisPermissionsSnapshot,
    onGrantMicClick: () -> Unit,
    onOpenAppSettingsClick: () -> Unit,
    onOpenNetworkSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val needsMic = !snapshot.isMicGranted
    val isOffline = !snapshot.network.isConnected

    if (!needsMic && !isOffline) return

    Surface(
        color = JarvisSurfaceCard.copy(alpha = 0.95f),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(
            1.dp,
            if (isOffline) JarvisCrimson else JarvisAmber
        ),
        modifier = modifier
            .fillMaxWidth()
            .testTag("permission_network_banner")
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            if (isOffline) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.SignalWifiOff,
                            contentDescription = "Offline Warning",
                            tint = JarvisCrimson,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "NETWORK ACCESS OFFLINE",
                                style = MaterialTheme.typography.labelLarge,
                                color = JarvisCrimson,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Connect to Wi-Fi or Cellular for Gemini AI Brain & ElevenLabs.",
                                style = MaterialTheme.typography.labelSmall,
                                color = JarvisIceWhite
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedButton(
                        onClick = onOpenNetworkSettingsClick,
                        border = BorderStroke(1.dp, JarvisCrimson),
                        modifier = Modifier.testTag("banner_network_settings_btn")
                    ) {
                        Text("CONNECT", color = JarvisCrimson, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            if (isOffline && needsMic) {
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (needsMic) {
                val isPermanentlyDenied = snapshot.micStatus == PermissionGrantStatus.PERMANENTLY_DENIED
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.MicOff,
                            contentDescription = "Microphone Permission Needed",
                            tint = JarvisAmber,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = if (isPermanentlyDenied) {
                                    "MICROPHONE BLOCKED IN OS SETTINGS"
                                } else {
                                    "MICROPHONE PERMISSION REQUIRED"
                                },
                                style = MaterialTheme.typography.labelLarge,
                                color = JarvisAmber,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (isPermanentlyDenied) {
                                    "Enable Microphone in App Settings for voice commands."
                                } else {
                                    "Grant RECORD_AUDIO to speak with JARVIS in English & বাংলা."
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = JarvisIceWhite
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = if (isPermanentlyDenied) onOpenAppSettingsClick else onGrantMicClick,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = JarvisAmber,
                            contentColor = JarvisObsidian
                        ),
                        modifier = Modifier.testTag("banner_grant_mic_btn")
                    ) {
                        Text(
                            text = if (isPermanentlyDenied) "SETTINGS" else "GRANT MIC",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun NetworkAndMicDiagnosticsCard(
    snapshot: JarvisPermissionsSnapshot,
    onRequestMic: () -> Unit,
    onOpenAppSettings: () -> Unit,
    onOpenWifiSettings: () -> Unit,
    onRefreshDiagnostics: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = JarvisSurfaceCard,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, JarvisCyan.copy(alpha = 0.45f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (snapshot.isReadyForVoiceOnline) {
                            Icons.Filled.CheckCircle
                        } else {
                            Icons.Filled.Wifi
                        },
                        contentDescription = null,
                        tint = if (snapshot.isReadyForVoiceOnline) JarvisEmerald else JarvisCyan
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "VOICE & NETWORK ACCESS HANDLER",
                        style = MaterialTheme.typography.labelLarge,
                        color = JarvisCyan,
                        fontWeight = FontWeight.Bold
                    )
                }
                OutlinedButton(
                    onClick = onRefreshDiagnostics,
                    border = BorderStroke(1.dp, JarvisElectricBlue)
                ) {
                    Text("RE-SCAN", color = JarvisIceWhite, style = MaterialTheme.typography.labelSmall)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            val micLabel = when (snapshot.micStatus) {
                PermissionGrantStatus.GRANTED -> "GRANTED (RECORD_AUDIO ACTIVE)"
                PermissionGrantStatus.SHOULD_SHOW_RATIONALE -> "NEEDS RATIONALE APPROVAL"
                PermissionGrantStatus.PERMANENTLY_DENIED -> "DENIED (ENABLE IN APP SETTINGS)"
                PermissionGrantStatus.NOT_REQUESTED -> "NOT REQUESTED YET"
            }

            Text(
                text = "• Microphone State: $micLabel",
                style = MaterialTheme.typography.bodyMedium,
                color = if (snapshot.isMicGranted) JarvisEmerald else JarvisAmber
            )
            Text(
                text = "• Network Link: ${snapshot.network.transportLabel} (Connected: ${snapshot.network.isConnected}, Validated: ${snapshot.network.isValidatedInternet}, Metered: ${snapshot.network.isMetered})",
                style = MaterialTheme.typography.bodyMedium,
                color = if (snapshot.network.isConnected) JarvisEmerald else JarvisCrimson
            )
            Text(
                text = "• Manifest Permissions: INTERNET=${snapshot.network.hasInternetManifestPermission}, ACCESS_NETWORK_STATE=${snapshot.network.hasNetworkStateManifestPermission}",
                style = MaterialTheme.typography.labelSmall,
                color = JarvisMutedCyan
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!snapshot.isMicGranted) {
                    val isPermDenied = snapshot.micStatus == PermissionGrantStatus.PERMANENTLY_DENIED
                    Button(
                        onClick = if (isPermDenied) onOpenAppSettings else onRequestMic,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = JarvisCyan,
                            contentColor = JarvisObsidian
                        ),
                        modifier = Modifier.testTag("system_grant_mic_handler_btn")
                    ) {
                        Text(
                            text = if (isPermDenied) "OPEN APP SETTINGS" else "REQUEST MIC ACCESS",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
                OutlinedButton(
                    onClick = onOpenWifiSettings,
                    border = BorderStroke(1.dp, JarvisCyan.copy(alpha = 0.6f)),
                    modifier = Modifier.testTag("system_network_settings_btn")
                ) {
                    Text(
                        text = "NETWORK SETTINGS",
                        color = JarvisCyan,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
    }
}
