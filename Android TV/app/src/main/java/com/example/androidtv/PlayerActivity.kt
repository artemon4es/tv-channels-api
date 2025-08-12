package com.example.androidtv

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource

import androidx.media3.ui.PlayerView
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import java.net.URI

@OptIn(UnstableApi::class)
class PlayerActivity : AppCompatActivity() {
    private var player: ExoPlayer? = null
    private lateinit var playerView: PlayerView
    private val errorHandler = Handler(Looper.getMainLooper())
    private val uiHandler = Handler(Looper.getMainLooper())
    private var errorShown = false
    private lateinit var trackSelector: DefaultTrackSelector
    private var subtitlesEnabled = false
    private lateinit var channelIndicator: LinearLayout
    private lateinit var channelNumberText: TextView

    private lateinit var channelList: ArrayList<Channel>
    private var channelIndex: Int = 0
    private var digitBuffer = "" // Буфер для набора номера канала
    private val digitInputHandler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_player)
        
        // Предотвращаем выключение экрана во время просмотра видео
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        playerView = findViewById(R.id.playerView)
        playerView.useController = true // чтобы работала кнопка субтитров
        // Не вызываем showController(), чтобы не показывать контроллер
        channelIndicator = findViewById(R.id.channelIndicator)
        channelNumberText = findViewById(R.id.channelNumberText)
        
        // Получаем список каналов и индекс
        channelList = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableArrayListExtra("CHANNEL_LIST", Channel::class.java) ?: arrayListOf()
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableArrayListExtra<Channel>("CHANNEL_LIST") ?: arrayListOf()
        }
        channelIndex = intent.getIntExtra("CHANNEL_INDEX", 0)

        if (channelList.isEmpty() || channelIndex !in channelList.indices) {
            showErrorAndExit()
            return
        }

        // Обновить индикатор канала
        updateChannelIndicator()
        
        // Скрыть индикатор канала через 3 секунды
        uiHandler.postDelayed({
            channelIndicator.animate()
                .alpha(0f)
                .setDuration(500)
                .start()
        }, 3000)

        playChannel(channelIndex)
    }

    @SuppressLint("SetTextI18n")
    private fun updateChannelIndicator() {
        // Обновить текст индикатора канала
        channelNumberText.text = "${channelIndex + 1}/${channelList.size}"
        
        // Показать индикатор
        channelIndicator.alpha = 1f
        channelIndicator.visibility = View.VISIBLE
        
        // Скрыть через 3 секунды
        uiHandler.removeCallbacks(hideChannelIndicatorRunnable)
        uiHandler.postDelayed(hideChannelIndicatorRunnable, 3000)
    }
    
    private val hideChannelIndicatorRunnable = Runnable {
        channelIndicator.animate()
            .alpha(0f)
            .setDuration(500)
            .start()
    }

    private fun playChannel(index: Int) {
        val channel = channelList[index]
        player?.release()
        
        // Используем современный способ создания трек-селектора
        trackSelector = DefaultTrackSelector(this)
        // Отключаем субтитры современным способом
        trackSelector.parameters = trackSelector.parameters
            .buildUpon()
            .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, !subtitlesEnabled)
            .build()
        
        // Runtime whitelist: если host не в списке, блокируем воспроизведение
        try {
            val host = URI(channel.streamUrl).host ?: ""
            val allowed = RemoteConfigManager.RuntimeSecurity.allowedHosts
            if (allowed.isNotEmpty() && host.isNotEmpty() && !allowed.contains(host)) {
                Toast.makeText(this, "Поток заблокирован политикой безопасности: $host", Toast.LENGTH_LONG).show()
                showErrorAndExit()
                return
            }
        } catch (_: Exception) { }

        player = ExoPlayer.Builder(this)
            .setTrackSelector(trackSelector)
            .build().also { exoPlayer ->
            playerView.player = exoPlayer
            val dataSourceFactory = DefaultHttpDataSource.Factory()
                .setUserAgent("Mozilla/5.0 (Android TV ExoPlayer)")
                .setAllowCrossProtocolRedirects(true)
            val mediaItem = MediaItem.fromUri(channel.streamUrl.toUri())
            val mediaSource = HlsMediaSource.Factory(dataSourceFactory).createMediaSource(mediaItem)
            exoPlayer.setMediaSource(mediaSource)
            exoPlayer.prepare()
            exoPlayer.playWhenReady = true
            exoPlayer.addListener(object : Player.Listener {
                override fun onPlayerError(error: PlaybackException) {
                    if (!errorShown) {
                        errorShown = true
                        showErrorAndExit()
                    }
                }
            })
        }
        showChannelNameToast(channel.name)
        
        // Обновить индикатор канала
        updateChannelIndicator()
    }

    private fun showChannelNameToast(name: String) {
        // Простой Toast без кастомного view (современный подход)
        Toast.makeText(this, "$name", Toast.LENGTH_SHORT).show()
    }

    /**
     * Переключение на следующий канал
     */
    private fun nextChannel() {
        if (channelList.isNotEmpty()) {
            channelIndex = (channelIndex + 1) % channelList.size
            playChannel(channelIndex)
        }
    }

    /**
     * Переключение на предыдущий канал
     */
    private fun previousChannel() {
        if (channelList.isNotEmpty()) {
            channelIndex = if (channelIndex - 1 < 0) channelList.size - 1 else channelIndex - 1
            playChannel(channelIndex)
        }
    }

    /**
     * Обработка цифрового ввода для прямого переключения на канал
     */
    private fun handleDigitInput(digit: String) {
        digitBuffer += digit
        
        // Показываем номер канала который набирается
        channelNumberText.text = "Канал: $digitBuffer"
        channelIndicator.alpha = 1f
        channelIndicator.visibility = View.VISIBLE
        
        // Убираем предыдущий таймер
        digitInputHandler.removeCallbacks(processDigitInput)
        
        // Если набрали больше 3 цифр, сразу переключаемся
        if (digitBuffer.length >= 3) {
            processDigitInput.run()
        } else {
            // Ждем 2 секунды после последней цифры
            digitInputHandler.postDelayed(processDigitInput, 2000)
        }
    }
    
    private val processDigitInput = Runnable {
        if (digitBuffer.isNotEmpty()) {
            val channelNumber = digitBuffer.toIntOrNull()
            if (channelNumber != null && channelNumber > 0 && channelNumber <= channelList.size) {
                channelIndex = channelNumber - 1 // Переводим в 0-based индекс
                playChannel(channelIndex)
            } else {
                Toast.makeText(this, "Канал $digitBuffer не найден", Toast.LENGTH_SHORT).show()
                updateChannelIndicator() // Вернуть обычный индикатор
            }
            digitBuffer = ""
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (channelList.isNotEmpty()) {
            when (keyCode) {
                // Переключение каналов - поддержка разных клавиш пульта
                KeyEvent.KEYCODE_CHANNEL_UP,
                KeyEvent.KEYCODE_DPAD_UP,
                KeyEvent.KEYCODE_MEDIA_NEXT,
                KeyEvent.KEYCODE_PLUS -> {
                    nextChannel()
                    return true
                }
                KeyEvent.KEYCODE_CHANNEL_DOWN,
                KeyEvent.KEYCODE_DPAD_DOWN,
                KeyEvent.KEYCODE_MEDIA_PREVIOUS,
                KeyEvent.KEYCODE_MINUS -> {
                    previousChannel()
                    return true
                }
                // Переключение каналов влево/вправо
                KeyEvent.KEYCODE_DPAD_LEFT -> {
                    previousChannel()
                    return true
                }
                KeyEvent.KEYCODE_DPAD_RIGHT -> {
                    nextChannel()
                    return true
                }
                // Цифровые клавиши для прямого переключения на канал
                KeyEvent.KEYCODE_0 -> {
                    handleDigitInput("0")
                    return true
                }
                KeyEvent.KEYCODE_1 -> {
                    handleDigitInput("1")
                    return true
                }
                KeyEvent.KEYCODE_2 -> {
                    handleDigitInput("2")
                    return true
                }
                KeyEvent.KEYCODE_3 -> {
                    handleDigitInput("3")
                    return true
                }
                KeyEvent.KEYCODE_4 -> {
                    handleDigitInput("4")
                    return true
                }
                KeyEvent.KEYCODE_5 -> {
                    handleDigitInput("5")
                    return true
                }
                KeyEvent.KEYCODE_6 -> {
                    handleDigitInput("6")
                    return true
                }
                KeyEvent.KEYCODE_7 -> {
                    handleDigitInput("7")
                    return true
                }
                KeyEvent.KEYCODE_8 -> {
                    handleDigitInput("8")
                    return true
                }
                KeyEvent.KEYCODE_9 -> {
                    handleDigitInput("9")
                    return true
                }
                // Выход из плеера
                KeyEvent.KEYCODE_BACK, KeyEvent.KEYCODE_ESCAPE -> {
                    finish()
                    return true
                }
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun showErrorAndExit() {
        Toast.makeText(this, "Ошибка воспроизведения потока", Toast.LENGTH_LONG).show()
        errorHandler.postDelayed({
            finish()
        }, 2000)
    }

    override fun onStop() {
        super.onStop()
        player?.release()
        player = null
        // Удалить все отложенные задачи
        uiHandler.removeCallbacksAndMessages(null)
        errorHandler.removeCallbacksAndMessages(null)
        digitInputHandler.removeCallbacksAndMessages(null)
    }
} 