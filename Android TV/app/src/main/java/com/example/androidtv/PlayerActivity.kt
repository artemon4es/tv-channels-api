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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_player)

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
        Toast.makeText(this, "📺 $name", Toast.LENGTH_SHORT).show()
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

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (channelList.isNotEmpty()) {
            when (keyCode) {
                // Только Channel Up/Down для переключения каналов
                KeyEvent.KEYCODE_CHANNEL_UP -> {
                    nextChannel()
                    return true
                }
                KeyEvent.KEYCODE_CHANNEL_DOWN -> {
                    previousChannel()
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
    }
} 