package com.sakinah.mobile

import android.app.*
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.pedro.common.ConnectChecker
import com.pedro.encoder.input.sources.audio.InternalAudioSource
import com.pedro.encoder.input.sources.audio.MicrophoneSource
import com.pedro.encoder.input.sources.audio.MixAudioSource
import com.pedro.encoder.input.sources.video.NoVideoSource
import com.pedro.encoder.input.sources.video.ScreenSource
import com.pedro.library.generic.GenericStream

class BroadcastService : Service(), ConnectChecker {
    enum class AudioMode { INTERNAL, MICROPHONE, MIX }

    companion object {
        const val ACTION_STOP = "com.sakinah.mobile.STOP"
        const val CHANNEL_ID = "sakinah_broadcast"
        const val NOTIFY_ID = 7086
        var INSTANCE: BroadcastService? = null
    }

    private lateinit var stream: GenericStream
    private var projection: MediaProjection? = null
    private val projectionManager by lazy { getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager }
    private var listener: ConnectChecker? = null
    private var prepared = false

    override fun onCreate() {
        super.onCreate()
        INSTANCE = this
        createChannel()
        stream = GenericStream(baseContext, this, NoVideoSource(), MicrophoneSource()).apply {
            getGlInterface().setForceRender(true, 30)
        }
        prepared = stream.prepareVideo(720, 1280, 2_700_000, rotation = 0) &&
            stream.prepareAudio(44_100, true, 128_000, echoCanceler = true, noiseSuppressor = true)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopStream(); stopSelf(); return START_NOT_STICKY
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null
    fun projectionIntent(): Intent = projectionManager.createScreenCaptureIntent()
    fun setListener(value: ConnectChecker?) { listener = value }
    fun isStreaming(): Boolean = ::stream.isInitialized && stream.isStreaming

    fun prepareProjection(resultCode: Int, data: Intent, audioMode: AudioMode): Boolean {
        if (!prepared) return false
        startAsForeground(); stopStream(); projection?.stop()
        projection = projectionManager.getMediaProjection(resultCode, data) ?: return false
        return try {
            stream.changeVideoSource(ScreenSource(applicationContext, projection!!))
            when (audioMode) {
                AudioMode.INTERNAL -> stream.changeAudioSource(InternalAudioSource(projection!!))
                AudioMode.MIX -> stream.changeAudioSource(MixAudioSource(projection!!))
                AudioMode.MICROPHONE -> stream.changeAudioSource(MicrophoneSource())
            }
            true
        } catch (_: Exception) { false }
    }

    fun startStream(endpoint: String) { if (!stream.isStreaming) stream.startStream(endpoint) }
    fun stopStream() { if (::stream.isInitialized && stream.isStreaming) stream.stopStream() }

    private fun startAsForeground() {
        val stopIntent = Intent(this, BroadcastService::class.java).setAction(ACTION_STOP)
        val pending = PendingIntent.getService(this, 0, stopIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val open = PendingIntent.getActivity(
            this, 1, Intent(this, SakinahLiveActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.presence_video_online)
            .setContentTitle("SAKINAH LIVE — البث يعمل")
            .setContentText("اضغط للعودة إلى الاستوديو")
            .setContentIntent(open)
            .setOngoing(true)
            .addAction(android.R.drawable.ic_media_pause, "إيقاف البث", pending)
            .build()
        startForeground(NOTIFY_ID, notification)
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(NotificationChannel(CHANNEL_ID, "Sakinah Broadcast", NotificationManager.IMPORTANCE_LOW))
        }
    }

    override fun onDestroy() {
        stopStream(); if (::stream.isInitialized) stream.release(); projection?.stop(); projection = null; INSTANCE = null; super.onDestroy()
    }

    override fun onConnectionStarted(url: String) { listener?.onConnectionStarted(url) }
    override fun onConnectionSuccess() { listener?.onConnectionSuccess() }
    override fun onConnectionFailed(reason: String) { listener?.onConnectionFailed(reason) }
    override fun onNewBitrate(bitrate: Long) { listener?.onNewBitrate(bitrate) }
    override fun onDisconnect() { listener?.onDisconnect() }
    override fun onAuthError() { listener?.onAuthError() }
    override fun onAuthSuccess() { listener?.onAuthSuccess() }
}
