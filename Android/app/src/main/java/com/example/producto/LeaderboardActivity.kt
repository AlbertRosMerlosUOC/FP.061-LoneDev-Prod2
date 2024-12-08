package com.example.producto2

import android.annotation.SuppressLint
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.producto2.databinding.ActivityLeaderboardBinding
import com.example.producto2.model.Player
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LeaderboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLeaderboardBinding
    private lateinit var database: AppDatabase
    private var musicReceiver: MusicReceiver? = null
    private var jugadorActual: Player? = null

    @SuppressLint("NewApi")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLeaderboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        database = AppDatabase.getInstance(this)

        val jugadorId = intent.getIntExtra("jugadorId", -1)

        lifecycleScope.launch {
            jugadorActual = withContext(Dispatchers.IO) {
                database.playerDao().getAllPlayers().find { it.id == jugadorId }
            }
        }

        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        actualizarClasificacion()

        binding.botonIniciarJuego.setOnClickListener {
            if (jugadorActual != null) {
                navegarPantallaJuego()
            } else {
                Toast.makeText(this, "Selecciona un jugador primero", Toast.LENGTH_SHORT).show()
            }
        }

        binding.historyButton.setOnClickListener {
            navegarPantallaHistorial()
        }

        binding.changeUserButton.setOnClickListener {
            navegarPantallaInicio()
        }

        val musicIntent = Intent(this, MusicService::class.java)
        startService(musicIntent)

        binding.buttonToggleMusic.setOnClickListener {
            toggleMusic()
        }

        binding.buttonSelectMusic.setOnClickListener {
            selectMusicLauncher.launch(arrayOf("audio/*"))
        }

        musicReceiver = MusicReceiver { isPlaying ->
            updateMusicButtonIcon(isPlaying)
        }
        val filter = IntentFilter("com.example.producto2.MUSIC_STATE")
        registerReceiver(musicReceiver, filter, RECEIVER_EXPORTED)

        checkMusicState()
    }

    private fun actualizarClasificacion() {
        CoroutineScope(Dispatchers.IO).launch {
            val jugadoresOrdenados = database.playerDao().getAllPlayers().sortedByDescending { it.coins }

            withContext(Dispatchers.Main) {
                val adapter = LeaderboardAdapter(jugadoresOrdenados)
                binding.recyclerView.adapter = adapter
            }
        }
    }

    private fun navegarPantallaJuego() {
        val intent = Intent(this, GameActivity::class.java)
        intent.putExtra("jugadorId", jugadorActual?.id)
        startActivity(intent)
    }

    private fun navegarPantallaHistorial() {
        val intent = Intent(this, HistoryActivity::class.java)
        intent.putExtra("jugadorId", jugadorActual?.id)
        startActivity(intent)
    }

    private fun navegarPantallaInicio() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(musicReceiver)
    }

    private fun toggleMusic() {
        val musicIntent = Intent(this, MusicService::class.java)
        musicIntent.action = "com.example.producto2.TOGGLE_MUSIC"
        startService(musicIntent)
    }

    private fun updateMusicButtonIcon(isPlaying: Boolean) {
        val musicButton: ImageButton = binding.buttonToggleMusic
        if (isPlaying) {
            musicButton.setImageResource(com.example.producto2.R.drawable.ic_music_pause)
        } else {
            musicButton.setImageResource(com.example.producto2.R.drawable.ic_music_play)
        }
    }

    private fun checkMusicState() {
        val musicIntent = Intent(this, MusicService::class.java)
        musicIntent.action = "com.example.producto2.GET_MUSIC_STATE"
        startService(musicIntent)
    }

    private fun selectMusicFromDevice() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            type = "audio/*"
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        startActivityForResult(intent, 200)
    }

    private val selectMusicLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            val musicIntent = Intent(this, MusicService::class.java).apply {
                action = "com.example.producto2.CHANGE_MUSIC"
                data = uri
            }
            startService(musicIntent)
        }
    }

}