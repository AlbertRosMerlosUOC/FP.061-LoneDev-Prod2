package com.example.producto2

import android.app.Service
import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.os.IBinder
import android.util.Log

class MusicService : Service() {

    private var mediaPlayer: MediaPlayer? = null
    private var isPlaying = false // Control del estado de la música

    override fun onCreate() {
        super.onCreate()
        Log.d("MusicService", "Servicio creado")
        startDefaultMusic() // Reproducir la música predeterminada al iniciar el servicio
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            "com.example.producto2.TOGGLE_MUSIC" -> toggleMusic()
            "com.example.producto2.GET_MUSIC_STATE" -> sendMusicState()
            "com.example.producto2.CHANGE_MUSIC" -> {
                val uri = intent.data
                uri?.let {
                    Log.d("MusicService", "Cambiando música a: $uri")
                    changeMusic(it.toString())
                } ?: Log.e("MusicService", "URI de música no válido")
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("MusicService", "Servicio destruido")
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun startDefaultMusic() {
        mediaPlayer = MediaPlayer.create(this, R.raw.music3)
        mediaPlayer?.apply {
            isLooping = true
            start()
        }
        isPlaying = true
        sendMusicState()
    }

    private fun toggleMusic() {
        mediaPlayer?.let {
            if (isPlaying) {
                it.pause()
                isPlaying = false
            } else {
                it.start()
                isPlaying = true
            }
            sendMusicState()
        } ?: Log.e("MusicService", "MediaPlayer no inicializado")
    }

    private fun sendMusicState() {
        val intent = Intent("com.example.producto2.MUSIC_STATE")
        intent.putExtra("isPlaying", isPlaying)
        sendBroadcast(intent)
        Log.d("MusicService", "Estado de la música enviado: $isPlaying")
    }

    private fun changeMusic(uri: String) {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.reset()
            mediaPlayer = MediaPlayer.create(this, Uri.parse(uri))
            mediaPlayer?.apply {
                isLooping = true
                start()
            }
            isPlaying = true
            sendMusicState()
        } catch (e: Exception) {
            Log.e("MusicService", "Error al cambiar la música: ${e.message}", e)
        }
    }
}
