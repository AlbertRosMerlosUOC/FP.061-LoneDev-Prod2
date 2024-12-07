package com.example.producto2

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.animation.AnimationUtils
import android.widget.ImageView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.producto2.databinding.ActivityGameBinding
import com.example.producto2.model.GameResult
import com.example.producto2.model.Player
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.util.Calendar
import kotlin.random.Random

class GameActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGameBinding
    private lateinit var database: AppDatabase
    private lateinit var mediaPlayer: MediaPlayer
    private lateinit var audioManager: AudioManager
    private var jugadorActual: Player? = null
    private val symbols = listOf(
        R.drawable.ic_reels_0,
        R.drawable.ic_reels_2,
        R.drawable.ic_reels_3,
        R.drawable.ic_reels_4,
        R.drawable.ic_reels_5,
        R.drawable.ic_reels_6
    )
    private val symbolNames = listOf(
        "s0",
        "s2",
        "s3",
        "s4",
        "s5",
        "s6"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGameBinding.inflate(layoutInflater)
        setContentView(binding.root)

        database = AppDatabase.getInstance(this)

        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        mediaPlayer = MediaPlayer.create(this, R.raw.spin) // Añadir el archivo de sonido

        val jugadorId = intent.getIntExtra("jugadorId", -1)

        lifecycleScope.launch {
            jugadorActual = withContext(Dispatchers.IO) {
                database.playerDao().getAllPlayers().find { it.id == jugadorId }
            }

            if (jugadorActual != null) {
                actualizarMonedas()
            } else {
                finish()
            }
        }

        binding.spinButton.setOnClickListener {
            spinReels()
        }

        binding.changeUserButton.setOnClickListener {
            finish()
        }

        binding.leaderboardButton.setOnClickListener {
            val intent = Intent(this, LeaderboardActivity::class.java)
            intent.putExtra("jugadorId", jugadorActual?.id)
            startActivity(intent)
        }

        binding.historyButton.setOnClickListener {
            val intent = Intent(this, HistoryActivity::class.java)
            intent.putExtra("jugadorId", jugadorId)
            startActivity(intent)
        }
    }

    private fun spinReels() {
        val spinDuration = 2000L
        val delay = 50L
        val handler = Handler(Looper.getMainLooper())
        val startTime = System.currentTimeMillis()

        if (!mediaPlayer.isPlaying) {
            mediaPlayer.start()
        }

        adjustSpinnerSoundVolume()

        handler.post(object : Runnable {
            override fun run() {
                val elapsedTime = System.currentTimeMillis() - startTime
                val symbol1 = symbols[Random.nextInt(symbols.size)]
                val symbol2 = symbols[Random.nextInt(symbols.size)]
                val symbol3 = symbols[Random.nextInt(symbols.size)]

                // Aplicamos una animación de deslizamiento a las imágenes de las fichas
                animateReel(binding.reel1, symbol1)
                animateReel(binding.reel2, symbol2)
                animateReel(binding.reel3, symbol3)

                if (elapsedTime < spinDuration) {
                    handler.postDelayed(this, delay)
                } else {
                    mediaPlayer.stop()
                    mediaPlayer.prepare()
                    val gameResult = checkResult(symbol1, symbol2, symbol3)
                    lifecycleScope.launch {
                        withContext(Dispatchers.IO) {
                            gameResult?.let {
                                AppDatabase.getInstance(this@GameActivity).gameResultDao().insertGame(it)
                            }
                        }
                    }
                }
            }
        })
    }

    private fun adjustSpinnerSoundVolume() {
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val volume = (maxVolume * 0.8).toInt()

        mediaPlayer.setVolume(volume.toFloat() / maxVolume, volume.toFloat() / maxVolume)
    }

    private fun checkResult(symbol1: Int, symbol2: Int, symbol3: Int) : GameResult? {
        val symbol1Index = symbols.indexOf(symbol1)
        val symbol2Index = symbols.indexOf(symbol2)
        val symbol3Index = symbols.indexOf(symbol3)

        val symbol1Name = if (symbol1Index != -1) symbolNames[symbol1Index] else "-1"
        val symbol2Name = if (symbol2Index != -1) symbolNames[symbol2Index] else "-1"
        val symbol3Name = if (symbol3Index != -1) symbolNames[symbol3Index] else "-1"

        var resultadoPremio = 0

        if (symbol1Name == "s0" && symbol2Name == "s0" && symbol3Name == "s0") {
            jugadorActual?.coins = jugadorActual?.coins?.plus(500) ?: 0
            actualizarTextoResultado(5, "¡Jack-o-Win! Has ganado 500 monedas")
            resultadoPremio = 500
        }
        else if (symbol1Name == "s6" && symbol2Name == "s6" && symbol3Name == "s6") {
            jugadorActual?.coins = jugadorActual?.coins?.minus(100)?.coerceAtLeast(0) ?: 0
            actualizarTextoResultado(1, "¡La muerte! Has perdido 100 monedas")
            resultadoPremio = 100
        }
        else if (symbol1Name == symbol2Name && symbol2Name == symbol3Name && symbol1Name != "s0" && symbol1Name != "s6") {
            jugadorActual?.coins = jugadorActual?.coins?.plus(100) ?: 0
            actualizarTextoResultado(4, "¡Triple! Has ganado 100 monedas")
            resultadoPremio = 100
        }
        else if ((symbol1Name == symbol2Name && symbol1Name != "s6" && symbol2Name != "s6") ||
            (symbol2Name == symbol3Name && symbol2Name != "s6" && symbol3Name != "s6") ||
            (symbol1Name == symbol3Name && symbol1Name != "s6" && symbol3Name != "s6")) {
            jugadorActual?.coins = jugadorActual?.coins?.plus(20) ?: 0
            actualizarTextoResultado(3, "¡Doble! Has ganado 20 monedas")
            resultadoPremio = 20
        }
        else {
            jugadorActual?.coins = jugadorActual?.coins?.minus(10)?.coerceAtLeast(0) ?: 0
            actualizarTextoResultado(2, "¡Inténtalo de nuevo!")
            resultadoPremio = -10
        }

        if (jugadorActual?.coins == 0) {
            showDeletePlayerDialog()
        } else {
            lifecycleScope.launch {
                withContext(Dispatchers.IO) {
                    jugadorActual?.let { database.playerDao().updatePlayer(it) }
                }
                actualizarMonedas()
            }
        }

        return jugadorActual?.id?.let { playerId ->
            val calendar = Calendar.getInstance()
            val currentDateTime = "${calendar.get(Calendar.DAY_OF_MONTH)}/${calendar.get(Calendar.MONTH) + 1}/${calendar.get(Calendar.YEAR)} " +
                    "${calendar.get(Calendar.HOUR_OF_DAY)}:${calendar.get(Calendar.MINUTE)}:${calendar.get(Calendar.SECOND)}"

            GameResult( playerId = playerId,
                        loot = resultadoPremio,
                        result1 = symbol1Name,
                        result2 = symbol2Name,
                        result3 = symbol3Name,
                        date = currentDateTime
                      )
        }
    }

    private fun actualizarMonedas() {
        jugadorActual?.let {
            binding.coinsTextView.text = "Monedas: ${it.coins}"
        }
    }

    @SuppressLint("DiscouragedApi")
    private fun actualizarTextoResultado(resultado: Int, texto: String) {
        jugadorActual?.let {
            val colorResourceName = "result_$resultado"
            val colorResId = resources.getIdentifier(colorResourceName, "color", packageName)
            binding.resultadoTextView.setTextColor(resources.getColor(colorResId, theme))
            binding.resultadoTextView.text = texto
        }
    }

    private fun showDeletePlayerDialog() {
        val dialog = android.app.AlertDialog.Builder(this)
            .setTitle("Game Over")
            .setMessage("Tus monedas han llegado a 0. El jugador será eliminado.")
            .setPositiveButton("Seguir") { dialog, _ ->
                lifecycleScope.launch {
                    withContext(Dispatchers.IO) {
                        jugadorActual?.let {
                            if (it.id != 0) {
                                database.playerDao().deletePlayer(it)
                            }
                        }
                    }
                    val intent = Intent(applicationContext, MainActivity::class.java)
                    startActivity(intent)
                    finish()
                }
                dialog.dismiss()
            }
            .create()

        dialog.show()
    }

    private fun animateReel(reelView: ImageView, symbolResId: Int) {
        val slideAnimation = AnimationUtils.loadAnimation(this, R.anim.rotate_animation)
        reelView.startAnimation(slideAnimation)

        reelView.setImageResource(symbolResId)
    }
}
