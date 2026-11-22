package com.teena.assistant.core.voice

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.teena.assistant.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class GeminiLiveService : Service() {

    @Inject
    lateinit var liveVoiceController: LiveVoiceController

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var stateObserverJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        ensureChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                liveVoiceController.stopSession()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_TOGGLE_MUTE -> {
                liveVoiceController.toggleMute()
            }
            ACTION_START -> {
                val notification = buildNotification("Microphone active — Gemini Live", muted = false)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    startForeground(
                        NOTIFICATION_ID,
                        notification,
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
                    )
                } else {
                    startForeground(NOTIFICATION_ID, notification)
                }
                observeVoiceState()
            }
        }
        return START_NOT_STICKY
    }

    private fun observeVoiceState() {
        stateObserverJob?.cancel()
        stateObserverJob = serviceScope.launch {
            liveVoiceController.snapshot.collect { snap ->
                val text = when (val s = snap.state) {
                    is LiveVoiceState.Connecting -> "Connecting (${s.modelId})..."
                    is LiveVoiceState.Listening -> if (s.muted) "Muted (${s.modelId})" else "Listening (${s.modelId})"
                    is LiveVoiceState.Thinking -> "Thinking (${s.modelId})..."
                    is LiveVoiceState.Speaking -> "Speaking (${s.modelId})"
                    is LiveVoiceState.Reconnecting -> "Reconnecting (#${s.attempt})..."
                    else -> null
                }
                if (text != null) {
                    val canNotify = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                        ContextCompat.checkSelfPermission(
                            this@GeminiLiveService,
                            Manifest.permission.POST_NOTIFICATIONS
                        ) == PackageManager.PERMISSION_GRANTED
                    if (canNotify) {
                        val manager = getSystemService(NotificationManager::class.java)
                        manager.notify(NOTIFICATION_ID, buildNotification(text, snap.isMuted))
                    }
                }
            }
        }
    }

    private fun buildNotification(statusText: String, muted: Boolean): Notification {
        val openAppIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val stopIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, GeminiLiveService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val muteIntent = PendingIntent.getService(
            this,
            2,
            Intent(this, GeminiLiveService::class.java).setAction(ACTION_TOGGLE_MUTE),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentTitle("TEENA AI • Gemini Live")
            .setContentText(statusText)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(openAppIntent)
            .addAction(0, if (muted) "Unmute" else "Mute", muteIntent)
            .addAction(0, "Stop", stopIntent)
            .build()
    }

    private fun ensureChannel() {
        val manager = getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CHANNEL_ID,
            "TEENA Live Voice",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Persistent indicator while TEENA Gemini Live microphone session is active."
        }
        manager.createNotificationChannel(channel)
    }

    override fun onDestroy() {
        runCatching { stopForeground(STOP_FOREGROUND_REMOVE) }
        serviceScope.cancel()
        super.onDestroy()
    }

    companion object {
        const val ACTION_START = "com.teena.assistant.voice.START"
        const val ACTION_STOP = "com.teena.assistant.voice.STOP"
        const val ACTION_TOGGLE_MUTE = "com.teena.assistant.voice.TOGGLE_MUTE"
        private const val CHANNEL_ID = "teena_live_voice"
        private const val NOTIFICATION_ID = 4102
    }
}
