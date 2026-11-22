package com.teena.assistant.core.device

import android.Manifest
import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import javax.inject.Inject
import javax.inject.Singleton

data class LivePermissionState(
    val recordAudioGranted: Boolean,
    val postNotificationsGranted: Boolean,
    val batteryOptimizationIgnored: Boolean,
    val internetGranted: Boolean,
    val isColorOsOem: Boolean,
    val restrictedSettingsApplies: Boolean
)

@Singleton
class DeviceCompatibilityHelper @Inject constructor() {

    fun inspectPermissions(context: Context): LivePermissionState {
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val audioOk = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        val notifyOk = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

        val internetOk = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.INTERNET
        ) == PackageManager.PERMISSION_GRANTED

        val brand = "${Build.MANUFACTURER} ${Build.BRAND}".lowercase()
        val isColorOs = brand.contains("oppo") || brand.contains("realme") || brand.contains("oneplus")

        return LivePermissionState(
            recordAudioGranted = audioOk,
            postNotificationsGranted = notifyOk,
            batteryOptimizationIgnored = pm.isIgnoringBatteryOptimizations(context.packageName),
            internetGranted = internetOk,
            isColorOsOem = isColorOs,
            restrictedSettingsApplies = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
        )
    }

    fun isNotificationListenerEnabled(context: Context): Boolean =
        NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)

    fun openAppDetailsSettings(context: Context): Boolean {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return launchSafely(context, intent)
    }

    @SuppressLint("BatteryLife")
    fun openBatteryOptimizationSettings(context: Context): Boolean {
        val directIntent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = Uri.parse("package:${context.packageName}")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        if (launchSafely(context, directIntent)) return true

        val fallback = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return launchSafely(context, fallback)
    }

    fun openColorOsAutoStart(context: Context): Boolean {
        val candidates = listOf(
            ComponentName("com.coloros.safecenter", "com.coloros.safecenter.startupapp.StartupAppListActivity"),
            ComponentName("com.oplus.safecenter", "com.oplus.safecenter.startupapp.StartupAppListActivity"),
            ComponentName("com.coloros.safecenter", "com.coloros.safecenter.permission.startup.StartupAppListActivity"),
            ComponentName("com.oppo.safe", "com.oppo.safe.permission.startup.StartupAppListActivity"),
            ComponentName("com.coloros.oppoguardelf", "com.coloros.powermanager.fuelgaue.PowerUsageModelActivity")
        )
        for (component in candidates) {
            val intent = Intent().apply {
                this.component = component
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (launchSafely(context, intent)) return true
        }
        return openAppDetailsSettings(context)
    }

    fun openAccessibilitySettings(context: Context): Boolean =
        launchSafely(
            context,
            Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        )

    fun openNotificationListenerSettings(context: Context): Boolean =
        launchSafely(
            context,
            Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        )

    private fun launchSafely(context: Context, intent: Intent): Boolean {
        return runCatching {
            val resolved = intent.resolveActivity(context.packageManager) ?: return false
            context.startActivity(intent)
            resolved != null
        }.getOrDefault(false)
    }
}
