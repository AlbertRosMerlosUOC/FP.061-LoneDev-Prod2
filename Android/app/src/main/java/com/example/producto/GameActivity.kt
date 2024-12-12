package com.example.producto2

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.producto2.databinding.ActivityGameBinding
import com.example.producto2.model.GameResult
import com.example.producto2.model.Player
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar
import kotlin.random.Random
import android.os.Build
import android.widget.ImageButton
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.OnCompleteListener

class GameActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGameBinding
    private lateinit var database: AppDatabase
    private lateinit var mediaPlayer: MediaPlayer
    private lateinit var audioManager: AudioManager
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var musicReceiver: MusicReceiver? = null
    private var jugadorActual: Player? = null
    private val CALENDAR_PERMISSION_REQUEST_CODE = 101
    private val NOTIFICATIONS_PERMISSION_REQUEST_CODE = 102
    private val LOCATION_PERMISSION_REQUEST_CODE = 103
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

    @SuppressLint("NewApi")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGameBinding.inflate(layoutInflater)
        setContentView(binding.root)

        database = AppDatabase.getInstance(this)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        solicitarPermisosCalendario()

        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        mediaPlayer = MediaPlayer.create(this, R.raw.spin)

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
            navegarPantallaInicio()
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

        binding.buttonScreenshot.setOnClickListener {
            val screenshot = captureScreenshot()
            if (screenshot != null) {
                saveImageToGallery(screenshot)
            } else {
                showToast("No se pudo capturar la pantalla.")
            }
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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), NOTIFICATIONS_PERMISSION_REQUEST_CODE)
            }

            if (checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST_CODE)
            }

            if (checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(android.Manifest.permission.ACCESS_COARSE_LOCATION), LOCATION_PERMISSION_REQUEST_CODE)
            }
        }

        binding.buttonHelp.setOnClickListener {
            startActivity(Intent(this, HelpActivity::class.java))
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
                    checkResult(symbol1, symbol2, symbol3) { gameResult ->
                        gameResult?.let {
                            lifecycleScope.launch(Dispatchers.IO) {
                                AppDatabase.getInstance(this@GameActivity).gameResultDao().insertGame(it)
                            }
                        }
                    }
                }
            }
        })

        crearCanalNotificacion()

    }

    private fun checkResult(symbol1: Int, symbol2: Int, symbol3: Int, callback: (GameResult?) -> Unit) {
        val screenshotLinearLayout = findViewById<LinearLayout>(R.id.screenshotLinearLayout)
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
            screenshotLinearLayout.visibility = View.VISIBLE
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                (checkSelfPermission(android.Manifest.permission.WRITE_CALENDAR) == PackageManager.PERMISSION_GRANTED &&
                        checkSelfPermission(android.Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED)) {

                registrarVictoriaEnCalendario("Victoria en el juego", "Has ganado una partida con un premio ¡Jack-o-Win!")
            } else {
                solicitarPermisosCalendario()
            }
            mostrarNotificacionVictoria()

        } else if (symbol1Name == "s6" && symbol2Name == "s6" && symbol3Name == "s6") {
            jugadorActual?.coins = jugadorActual?.coins?.minus(100)?.coerceAtLeast(0) ?: 0
            actualizarTextoResultado(1, "¡La muerte! Has perdido 100 monedas")
            resultadoPremio = -100
            screenshotLinearLayout.visibility = View.INVISIBLE
        } else if (symbol1Name == symbol2Name && symbol2Name == symbol3Name && symbol1Name != "s0" && symbol1Name != "s6") {
            jugadorActual?.coins = jugadorActual?.coins?.plus(100) ?: 0
            actualizarTextoResultado(4, "¡Triple! Has ganado 100 monedas")
            resultadoPremio = 100
            screenshotLinearLayout.visibility = View.VISIBLE
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                (checkSelfPermission(android.Manifest.permission.WRITE_CALENDAR) == PackageManager.PERMISSION_GRANTED &&
                        checkSelfPermission(android.Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED)) {

                registrarVictoriaEnCalendario("Victoria en el juego", "Has ganado una partida con un premio ¡Triple!")
            } else {
                solicitarPermisosCalendario()
            }
            mostrarNotificacionVictoria()

        } else if ((symbol1Name == symbol2Name && symbol1Name != "s6" && symbol2Name != "s6") ||
            (symbol2Name == symbol3Name && symbol2Name != "s6" && symbol3Name != "s6") ||
            (symbol1Name == symbol3Name && symbol1Name != "s6" && symbol3Name != "s6")) {
            jugadorActual?.coins = jugadorActual?.coins?.plus(20) ?: 0
            actualizarTextoResultado(3, "¡Doble! Has ganado 20 monedas")
            resultadoPremio = 20
            screenshotLinearLayout.visibility = View.VISIBLE
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                (checkSelfPermission(android.Manifest.permission.WRITE_CALENDAR) == PackageManager.PERMISSION_GRANTED &&
                        checkSelfPermission(android.Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED)) {

                registrarVictoriaEnCalendario("Victoria en el juego", "Has ganado una partida con un premio ¡Doble!")
            } else {
                solicitarPermisosCalendario()
            }
            mostrarNotificacionVictoria()

        } else {
            jugadorActual?.coins = jugadorActual?.coins?.minus(10)?.coerceAtLeast(0) ?: 0
            actualizarTextoResultado(2, "¡Inténtalo de nuevo!")
            resultadoPremio = -10
            screenshotLinearLayout.visibility = View.INVISIBLE
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

        obtenerUbicacion { location ->
            val gameResult = jugadorActual?.id?.let { playerId ->
                val calendar = Calendar.getInstance()
                val currentDateTime = "${calendar.get(Calendar.DAY_OF_MONTH)}/${calendar.get(Calendar.MONTH) + 1}/${calendar.get(Calendar.YEAR)} " +
                        "${calendar.get(Calendar.HOUR_OF_DAY)}:${calendar.get(Calendar.MINUTE)}:${calendar.get(Calendar.SECOND)}"

                GameResult(
                    playerId = playerId,
                    loot = resultadoPremio,
                    result1 = symbol1Name,
                    result2 = symbol2Name,
                    result3 = symbol3Name,
                    date = currentDateTime,
                    location = location
                )
            }

            println("La localización del usuario es " + gameResult?.location)

            callback(gameResult)
        }
    }

    private fun adjustSpinnerSoundVolume() {
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val volume = (maxVolume * 0.8).toInt()

        mediaPlayer.setVolume(volume.toFloat() / maxVolume, volume.toFloat() / maxVolume)
    }

    private fun obtenerUbicacion(callback: (String) -> Unit) {
        if (checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.lastLocation.addOnCompleteListener { task ->
                if (task.isSuccessful && task.result != null) {
                    val location = task.result
                    callback("${location.latitude},${location.longitude}")
                } else {
                    callback("Location unavailable")
                }
            }
        } else {
            callback("Permission not granted")
        }
    }



    private fun navegarPantallaInicio() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
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

    private fun captureScreenshot(): Bitmap {
        val rootView = window.decorView.rootView
        rootView.isDrawingCacheEnabled = true
        val bitmap = Bitmap.createBitmap(rootView.drawingCache)
        rootView.isDrawingCacheEnabled = false
        return bitmap
    }

    private fun saveImageToGallery(bitmap: Bitmap) {
        val contentResolver = contentResolver
        val filename = "Screenshot_${System.currentTimeMillis()}.png"
        val imageCollection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }

        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, filename)
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Screenshots")
            }
        }

        try {
            val uri = contentResolver.insert(imageCollection, contentValues)
            uri?.let {
                contentResolver.openOutputStream(it).use { outputStream ->
                    if (outputStream != null) {
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                    }
                }
            }
            showToast("Captura guardada en la galería.")
        } catch (e: Exception) {
            showToast("Error al guardar la captura: ${e.message}")
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
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
            musicButton.setImageResource(R.drawable.ic_music_pause)
        } else {
            musicButton.setImageResource(R.drawable.ic_music_play)
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

    private fun solicitarPermisosCalendario() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(android.Manifest.permission.WRITE_CALENDAR) != PackageManager.PERMISSION_GRANTED ||
                checkSelfPermission(android.Manifest.permission.READ_CALENDAR) != PackageManager.PERMISSION_GRANTED) {

                requestPermissions(
                    arrayOf(
                        android.Manifest.permission.WRITE_CALENDAR,
                        android.Manifest.permission.READ_CALENDAR
                    ),
                    CALENDAR_PERMISSION_REQUEST_CODE
                )
            }
        }
    }

    private fun registrarVictoriaEnCalendario(titulo: String, descripcion: String) {
        val contentResolver = contentResolver
        val calendarUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            android.provider.CalendarContract.Events.CONTENT_URI
        } else {
            Uri.parse("content://com.android.calendar/events")
        }

        val calendarsUri = Uri.parse("content://com.android.calendar/calendars")
        val cursor = contentResolver.query(
            calendarsUri,
            arrayOf("_id", "account_name"),
            null,
            null,
            null
        )

        if (cursor != null) {
            var calendarId: Long? = null
            while (cursor.moveToNext()) {
                val accountName = cursor.getString(cursor.getColumnIndexOrThrow("account_name"))
                if (accountName.contains("uoc.edu", ignoreCase = true)) {
                    calendarId = cursor.getLong(cursor.getColumnIndexOrThrow("_id"))
                    break
                }
            }
            cursor.close()

            if (calendarId != null) {
                val uniqueTitle = "$titulo - ${System.currentTimeMillis()}"

                val values = ContentValues().apply {
                    put(android.provider.CalendarContract.Events.DTSTART, System.currentTimeMillis())
                    put(android.provider.CalendarContract.Events.DTEND, System.currentTimeMillis() + 60 * 60 * 1000) // 1 hora de duración
                    put(android.provider.CalendarContract.Events.TITLE, uniqueTitle)
                    put(android.provider.CalendarContract.Events.DESCRIPTION, descripcion)
                    put(android.provider.CalendarContract.Events.CALENDAR_ID, calendarId)
                    put(android.provider.CalendarContract.Events.EVENT_TIMEZONE, java.util.TimeZone.getDefault().id)
                }

                val uri = contentResolver.insert(calendarUri, values)

                if (uri != null) {
                    Toast.makeText(this, "Victoria registrada en el calendario", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Error al registrar la victoria en el calendario", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "No se encontró un calendario con cuenta 'uoc.edu'", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "Error al acceder a los calendarios", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == CALENDAR_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permisos concedidos para el calendario", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Permisos denegados para el calendario", Toast.LENGTH_SHORT).show()
            }
        }

        if (requestCode == NOTIFICATIONS_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permiso concedido para notificaciones", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Permiso denegado para notificaciones", Toast.LENGTH_SHORT).show()
            }
        }

        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permiso concedido para localización", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Permiso denegado para localización", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun crearCanalNotificacion() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nombre = "Victorias"
            val descripcion = "Notificaciones de victorias en el juego"
            val importancia = NotificationManager.IMPORTANCE_DEFAULT
            val canal = NotificationChannel("victoria_channel", nombre, importancia).apply {
                description = descripcion
            }

            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(canal)
        }
    }

    @SuppressLint("NotificationPermission")
    private fun mostrarNotificacionVictoria() {
        val notificationId = 1
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val builder = NotificationCompat.Builder(this, "victoria_channel")
            .setSmallIcon(R.drawable.ic_trophy)
            .setContentTitle("¡Victoria!")
            .setContentText("¡Felicidades! Has ganado una partida.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)

        notificationManager.notify(notificationId, builder.build())
    }

}