package com.example.producto2

import android.R
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ImageButton
import android.widget.PopupMenu
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.producto2.databinding.ActivityMainBinding
import com.example.producto2.model.Player
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var database: AppDatabase
    private var jugadores: List<Player> = emptyList()
    private var jugadorActual: Player? = null
    private var musicReceiver: MusicReceiver? = null

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val selectedLanguage = getSelectedLanguage(this)
        setAppLocale(this, selectedLanguage)

        val btnSelectLanguage: ImageButton = findViewById(com.example.producto2.R.id.buttonSelectLanguage)
        btnSelectLanguage.setOnClickListener { view ->
            showLanguagePopup(view)
        }

        database = AppDatabase.getInstance(this)

        cargarJugadores()

        binding.botonAddPlayer.setOnClickListener {
            mostrarDialogoAnadirJugador()
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

        binding.buttonHelp.setOnClickListener {
            startActivity(Intent(this, HelpActivity::class.java))
        }
    }

    private fun showLanguagePopup(view: View) {
        val popup = PopupMenu(this, view)
        popup.menuInflater.inflate(com.example.producto2.R.menu.language_menu, popup.menu)

        popup.setOnMenuItemClickListener { menuItem: MenuItem ->
            when (menuItem.itemId) {
                com.example.producto2.R.id.language_english -> restartActivityWithLocale("en")
                com.example.producto2.R.id.language_spanish -> restartActivityWithLocale("es")
                com.example.producto2.R.id.language_catalan -> restartActivityWithLocale("ca")
            }
            true
        }
        popup.show()
    }

    private fun setAppLocale(context: Context, languageCode: String): Context {
        val locale = Locale(languageCode)
        Locale.setDefault(locale)
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        return context.createConfigurationContext(config)
    }

    private fun restartActivityWithLocale(languageCode: String) {
        saveSelectedLanguage(this, languageCode)
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        finish()
        startActivity(intent)
    }

    override fun attachBaseContext(newBase: Context?) {
        val selectedLanguage = newBase?.let { getSelectedLanguage(it) } ?: Locale.getDefault().language
        val contextWithLocale = newBase?.let { setAppLocale(it, selectedLanguage) }
        super.attachBaseContext(contextWithLocale)
    }

    private fun saveSelectedLanguage(context: Context, languageCode: String) {
        val prefs = context.getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
        prefs.edit().putString("SelectedLanguage", languageCode).apply()
    }

    private fun getSelectedLanguage(context: Context): String {
        val prefs = context.getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
        return prefs.getString("SelectedLanguage", Locale.getDefault().language) ?: Locale.getDefault().language
    }

    private fun cargarJugadores() {
        lifecycleScope.launch {
            jugadores = withContext(Dispatchers.IO) {
                database.playerDao().getAllPlayers().sortedBy { it.name }
            }
            configurarSpinner()
        }
    }

    private fun configurarSpinner() {
        val nombres = jugadores.map { it.name }

        val adapter = ArrayAdapter(this, R.layout.simple_spinner_item, nombres)
        adapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item)
        binding.spinner.adapter = adapter

        binding.spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                jugadorActual = jugadores[position]
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
            }
        }

        binding.botonIniciarJuego.setOnClickListener {
            if (jugadorActual != null) {
                navegarPantallaJuego()
            } else {
                Toast.makeText(this,
                    getString(com.example.producto2.R.string.select_player), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun mostrarDialogoAnadirJugador() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(getString(com.example.producto2.R.string.create_new_player_title))

        val input = android.widget.EditText(this)
        input.hint = getString(com.example.producto2.R.string.player_name_hint)
        builder.setView(input)

        builder.setPositiveButton(getString(com.example.producto2.R.string.create_button_text)) { _, _ ->
            val nombre = input.text.toString().trim()
            if (nombre.isNotEmpty()) {
                lifecycleScope.launch {
                    val jugadorExistente = withContext(Dispatchers.IO) {
                        database.playerDao().findPlayerByName(nombre)
                    }

                    if (jugadorExistente == null) {
                        val nuevoJugador = Player(name = nombre, coins = 100)
                        withContext(Dispatchers.IO) {
                            database.playerDao().insertPlayer(nuevoJugador)
                        }
                        cargarJugadores()
                        Toast.makeText(this@MainActivity,
                            getString(com.example.producto2.R.string.player_created_toast), Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this@MainActivity,
                            getString(com.example.producto2.R.string.player_exists_toast), Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                Toast.makeText(this,
                    getString(com.example.producto2.R.string.empty_name_toast), Toast.LENGTH_SHORT).show()
            }
        }

        builder.setNegativeButton(getString(com.example.producto2.R.string.cancel_button_text), null)
        builder.show()
    }

    private fun navegarPantallaJuego() {
        val intent = Intent(this, GameActivity::class.java)
        intent.putExtra("jugadorId", jugadorActual?.id)
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