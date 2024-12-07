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
        mediaPlayer = MediaPlayer.create(this, R.raw.music3)
        mediaPlayer?.isLooping = true
        mediaPlayer?.start() // Iniciar la música automáticamente
        isPlaying = true // Actualizar el estado
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
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    // Alternar entre reproducción y pausa
    private fun toggleMusic() {
        if (isPlaying) {
            mediaPlayer?.pause()
            isPlaying = false
        } else {
            mediaPlayer?.start()
            isPlaying = true
        }
        sendMusicState() // Actualizar el estado de la música
    }

    // Enviar el estado actual de la música
    private fun sendMusicState() {
        val intent = Intent("com.example.producto2.MUSIC_STATE")
        intent.putExtra("isPlaying", isPlaying)
        sendBroadcast(intent) // Enviar un broadcast con el estado de la música
    }

    // Cambiar la música actual
    private fun changeMusic(uri: String) {
        mediaPlayer?.reset()
        mediaPlayer = MediaPlayer.create(this, Uri.parse(uri))
        mediaPlayer?.isLooping = true
        mediaPlayer?.start()
        isPlaying = true
        sendMusicState() // Actualizar el estado de la música
    }
}
