package com.example.producto2

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.MediaPlayer
import android.net.Uri
import android.os.IBinder
import android.telephony.TelephonyManager
import android.util.Log
import android.media.AudioManager

class MusicService : Service() {

    private var mediaPlayer: MediaPlayer? = null
    private var isPlaying = false
    private var wasPlayingBeforeEvent = false
    private val phoneStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE)
            Log.d("MusicService", "Estado de la llamada: $state")

            if (state == TelephonyManager.EXTRA_STATE_RINGING) {
                Log.d("MusicService", "Llamada entrante detectada. Pausando música.")
                pauseMusicForEvent()
            }
            else if (state == TelephonyManager.EXTRA_STATE_IDLE) {
                Log.d("MusicService", "Llamada finalizada. Reanudando música.")
                resumeMusicAfterEvent()
            }
        }
    }

    private val audioManagerReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

            if (audioManager.ringerMode == AudioManager.RINGER_MODE_SILENT) {
                Log.d("MusicService", "Modo silencioso activado. Pausando música.")
                pauseMusicForEvent()
            } else if (audioManager.ringerMode == AudioManager.RINGER_MODE_NORMAL) {
                Log.d("MusicService", "Modo sonido activado. Reanudando música.")
                resumeMusicAfterEvent()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        mediaPlayer = MediaPlayer.create(this, R.raw.music3)
        mediaPlayer?.isLooping = true
        mediaPlayer?.start()
        isPlaying = true

        val phoneFilter = IntentFilter(TelephonyManager.ACTION_PHONE_STATE_CHANGED)
        registerReceiver(phoneStateReceiver, phoneFilter)

        val audioFilter = IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
        registerReceiver(audioManagerReceiver, audioFilter)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            "com.example.producto2.TOGGLE_MUSIC" -> toggleMusic()
            "com.example.producto2.GET_MUSIC_STATE" -> sendMusicState()
            "com.example.producto2.CHANGE_MUSIC" -> {
                val uri = intent.data
                uri?.let { changeMusic(it.toString()) }
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(phoneStateReceiver)
        unregisterReceiver(audioManagerReceiver)

        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun toggleMusic() {
        if (isPlaying) {
            mediaPlayer?.pause()
            isPlaying = false
        } else {
            mediaPlayer?.start()
            isPlaying = true
        }
        sendMusicState()
    }

    private fun sendMusicState() {
        val intent = Intent("com.example.producto2.MUSIC_STATE")
        intent.putExtra("isPlaying", isPlaying)
        sendBroadcast(intent)
    }

    private fun changeMusic(uri: String) {
        mediaPlayer?.reset()
        mediaPlayer = MediaPlayer.create(this, Uri.parse(uri))
        mediaPlayer?.isLooping = true
        mediaPlayer?.start()
        isPlaying = true
        sendMusicState()
    }

    private fun pauseMusicForEvent() {
        if (isPlaying) {
            wasPlayingBeforeEvent = true
            mediaPlayer?.pause()
            isPlaying = false
        }
    }

    private fun resumeMusicAfterEvent() {
        if (wasPlayingBeforeEvent) {
            mediaPlayer?.start()
            isPlaying = true
            sendMusicState()
            wasPlayingBeforeEvent = false
        }
    }
}
