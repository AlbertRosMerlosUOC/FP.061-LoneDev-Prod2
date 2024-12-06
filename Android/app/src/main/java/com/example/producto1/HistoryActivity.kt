package com.example.producto2

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.producto2.databinding.ActivityHistoryBinding
import com.example.producto2.model.Player
import com.example.producto2.model.GameResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HistoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHistoryBinding
    private lateinit var database: AppDatabase
    private var jugadorActual: Player? = null
    private lateinit var gameResultList: List<GameResult>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        database = AppDatabase.getInstance(this)

        val jugadorId = intent.getIntExtra("jugadorId", -1)

        binding.recyclerView.layoutManager = LinearLayoutManager(this)

        lifecycleScope.launch {
            jugadorActual = withContext(Dispatchers.IO) {
                database.playerDao().getAllPlayers().find { it.id == jugadorId }
            }
            if (jugadorActual != null) {
                gameResultList = withContext(Dispatchers.IO) {
                    database.gameResultDao().getHistoryByPlayer(jugadorId)
                }
                actualizarHistorial(gameResultList)
            } else {
                finish()
            }
        }

        binding.botonIniciarJuego.setOnClickListener {
            if (jugadorActual != null) {
                navegarPantallaJuego()
            } else {
                Toast.makeText(this, "Selecciona un jugador primero", Toast.LENGTH_SHORT).show()
            }
        }

        binding.leaderboardButton.setOnClickListener {
            navegarPantallaLeaderboard()
        }

        binding.changeUserButton.setOnClickListener {
            navegarPantallaInicio()
        }
    }


    private fun actualizarHistorial(historial: List<GameResult>) {
        val historialOrdenado = historial.sortedByDescending { it.id }

        val adapter = HistoryAdapter(historialOrdenado)
        binding.recyclerView.adapter = adapter
    }


    private fun navegarPantallaJuego() {
        val intent = Intent(this, GameActivity::class.java)
        intent.putExtra("jugadorId", jugadorActual?.id)
        startActivity(intent)
    }

    private fun navegarPantallaLeaderboard() {
        val intent = Intent(this, LeaderboardActivity::class.java)
        intent.putExtra("jugadorId", jugadorActual?.id)
        startActivity(intent)
    }

    private fun navegarPantallaInicio() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
    }
}
