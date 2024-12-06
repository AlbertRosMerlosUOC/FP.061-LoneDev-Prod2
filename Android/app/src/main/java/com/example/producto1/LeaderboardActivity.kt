package com.example.producto2

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
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
    private var jugadorActual: Player? = null

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
}
