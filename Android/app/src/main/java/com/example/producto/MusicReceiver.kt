package com.example.producto2

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class MusicReceiver(private val onMusicStateChanged: (Boolean) -> Unit) : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        val isPlaying = intent?.getBooleanExtra("isPlaying", false) ?: false
        onMusicStateChanged(isPlaying)
    }
}
