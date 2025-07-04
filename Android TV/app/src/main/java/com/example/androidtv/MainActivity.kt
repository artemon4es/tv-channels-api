package com.example.androidtv

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.*
import android.widget.Toast
import android.widget.ImageView
import android.widget.TextView
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.LinearLayoutManager

import java.io.BufferedReader
import java.io.InputStreamReader
import androidx.window.layout.WindowMetricsCalculator
import kotlinx.coroutines.*

class MainActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ChannelAdapter
    private lateinit var remoteConfigManager: RemoteConfigManager
    private lateinit var autoUpdateManager: AutoUpdateManager
    private var channels = mutableListOf<Channel>()
    
    companion object {
        private const val TAG = "MainActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        // Инициализируем менеджеры
        remoteConfigManager = RemoteConfigManager(this)
        autoUpdateManager = AutoUpdateManager(this)

        recyclerView = findViewById(R.id.recyclerView)
        
        // Получаем ширину экрана современным способом
        val windowMetrics = WindowMetricsCalculator.getOrCreate().computeCurrentWindowMetrics(this)
        val screenWidth = windowMetrics.bounds.width()
        val cardWidth = resources.getDimensionPixelSize(R.dimen.channel_card_width) + 2 * resources.getDimensionPixelSize(R.dimen.grid_spacing)
        val spanCount = (screenWidth / cardWidth).coerceAtLeast(1)
        val gridLayoutManager = GridLayoutManager(this, spanCount)
        recyclerView.layoutManager = gridLayoutManager
        
        // Add spacing between items
        recyclerView.addItemDecoration(object : RecyclerView.ItemDecoration() {
            override fun getItemOffsets(
                outRect: Rect,
                view: View,
                parent: RecyclerView,
                state: RecyclerView.State
            ) {
                val spacing = resources.getDimensionPixelSize(R.dimen.grid_spacing)
                outRect.set(spacing, spacing, spacing, spacing)
            }
        })
        
        adapter = ChannelAdapter(channels) { channel, _ ->
            playChannel(channel)
        }
        recyclerView.adapter = adapter

        // Загрузка каналов
        loadChannels()
        
        // Запускаем проверку удаленной конфигурации
        lifecycleScope.launch {
            initializeRemoteConfig()
        }
    }
    
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_check_updates -> {
                lifecycleScope.launch {
                    checkForUpdates()
                }
                true
            }
            R.id.action_refresh_channels -> {
                lifecycleScope.launch {
                    refreshChannels()
                }
                true
            }
            R.id.action_service_status -> {
                lifecycleScope.launch {
                    showServiceStatus()
                }
                true
            }
            R.id.action_admin_login -> {
                showAdminLoginDialog()
                true
            }
            R.id.action_admin_logout -> {
                logoutAdmin()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        autoUpdateManager.cleanup()
    }
    
    /**
     * Инициализация удаленной конфигурации
     */
    private suspend fun initializeRemoteConfig() {
        try {
            Log.d(TAG, "Инициализация удаленной конфигурации...")
            
            val remoteConfig = remoteConfigManager.checkRemoteConfig()
            
            if (remoteConfig != null) {
                // Проверяем доступность сервиса
                if (!remoteConfig.serviceAvailable) {
                    showServiceUnavailableDialog(remoteConfig.message)
                    return
                }
                
                // Проверяем режим обслуживания
                if (remoteConfig.maintenanceMode) {
                    showMaintenanceDialog(remoteConfig.message)
                    return
                }
                
                // Проверяем обновления приложения (автоматически, без диалога)
                autoUpdateManager.checkForUpdates(remoteConfig, false)
                
                // Проверяем обновления каналов
                if (remoteConfigManager.shouldUpdateChannels(remoteConfig.channelsVersion)) {
                    updateChannelsFromRemote()
                } else {
                    loadChannelsFromCacheOrAssets()
                }
            } else {
                // Если удаленная конфигурация недоступна, загружаем локальные данные
                Log.w(TAG, "Удаленная конфигурация недоступна, используем локальные данные")
                loadChannelsFromCacheOrAssets()
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка инициализации удаленной конфигурации: ${e.message}")
            loadChannelsFromCacheOrAssets()
        }
    }
    
    /**
     * Проверка обновлений по запросу пользователя
     */
    private suspend fun checkForUpdates() {
        try {
            withContext(Dispatchers.Main) {
                Toast.makeText(this@MainActivity, "Проверка обновлений...", Toast.LENGTH_SHORT).show()
            }
            
            val remoteConfig = remoteConfigManager.checkRemoteConfig()
            
            if (remoteConfig != null) {
                autoUpdateManager.checkForUpdates(remoteConfig, true)
            } else {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "Ошибка проверки обновлений", Toast.LENGTH_LONG).show()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка проверки обновлений: ${e.message}")
            withContext(Dispatchers.Main) {
                Toast.makeText(this@MainActivity, "Ошибка: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
    
    /**
     * Обновление каналов по запросу пользователя
     */
    private suspend fun refreshChannels() {
        try {
            withContext(Dispatchers.Main) {
                Toast.makeText(this@MainActivity, "Обновление каналов...", Toast.LENGTH_SHORT).show()
            }
            
            val channelList = remoteConfigManager.downloadChannelList()
            
            if (channelList != null) {
                val parsedChannels = parseM3U8(channelList)
                
                // Обновляем версию каналов
                val remoteConfig = remoteConfigManager.checkRemoteConfig()
                remoteConfig?.let {
                    remoteConfigManager.updateChannelVersion(it.channelsVersion)
                }
                
                withContext(Dispatchers.Main) {
                    updateChannelList(parsedChannels)
                    Toast.makeText(this@MainActivity, "Каналы обновлены", Toast.LENGTH_SHORT).show()
                }
            } else {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "Ошибка обновления каналов", Toast.LENGTH_LONG).show()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка обновления каналов: ${e.message}")
            withContext(Dispatchers.Main) {
                Toast.makeText(this@MainActivity, "Ошибка: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
    
    /**
     * Показывает статус сервиса
     */
    private suspend fun showServiceStatus() {
        try {
            val remoteConfig = remoteConfigManager.checkRemoteConfig()
            val lastCheck = remoteConfigManager.getLastCheckTime()
            val lastCheckTime = if (lastCheck > 0) {
                java.text.SimpleDateFormat("dd.MM.yyyy HH:mm", java.util.Locale.getDefault())
                    .format(java.util.Date(lastCheck))
            } else {
                "Никогда"
            }
            
            val message = if (remoteConfig != null) {
                """
                🟢 Сервис: ${if (remoteConfig.serviceAvailable) "Доступен" else "Недоступен"}
                📱 Версия приложения: ${remoteConfig.appLatestVersion}
                📺 Версия каналов: ${remoteConfig.channelsVersion}
                🕒 Последняя проверка: $lastCheckTime
                
                ${if (remoteConfig.message.isNotEmpty()) "📢 " + remoteConfig.message else ""}
                """.trimIndent()
            } else {
                """
                🔴 Сервис: Недоступен
                🕒 Последняя проверка: $lastCheckTime
                
                ℹ️ Работаем в автономном режиме
                """.trimIndent()
            }
            
            withContext(Dispatchers.Main) {
                AlertDialog.Builder(this@MainActivity)
                    .setTitle("📊 Статус сервиса")
                    .setMessage(message)
                    .setPositiveButton("OK", null)
                    .show()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка получения статуса: ${e.message}")
            withContext(Dispatchers.Main) {
                Toast.makeText(this@MainActivity, "Ошибка: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
    
    /**
     * Обновляет каналы из удаленного источника
     */
    private suspend fun updateChannelsFromRemote() {
        try {
            Log.d(TAG, "Обновление каналов из удаленного источника...")
            
            val channelList = remoteConfigManager.downloadChannelList()
            
            if (channelList != null) {
                val parsedChannels = parseM3U8(channelList)
                
                val remoteConfig = remoteConfigManager.checkRemoteConfig()
                remoteConfig?.let {
                    remoteConfigManager.updateChannelVersion(it.channelsVersion)
                }
                
                withContext(Dispatchers.Main) {
                    updateChannelList(parsedChannels)
                    Toast.makeText(this@MainActivity, "Список каналов обновлен", Toast.LENGTH_SHORT).show()
                }
                
                Log.d(TAG, "Каналы успешно обновлены")
            } else {
                Log.w(TAG, "Не удалось загрузить каналы, используем кэш")
                loadChannelsFromCacheOrAssets()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка обновления каналов: ${e.message}")
            loadChannelsFromCacheOrAssets()
        }
    }
    
    /**
     * Загружает каналы из кэша или assets
     */
    private suspend fun loadChannelsFromCacheOrAssets() {
        try {
            // Сначала пробуем загрузить из кэша
            val cachedChannels = getCachedChannelList()
            
            if (cachedChannels != null) {
                Log.d(TAG, "Загрузка каналов из кэша")
                val parsedChannels = parseM3U8(cachedChannels)
                withContext(Dispatchers.Main) {
                    updateChannelList(parsedChannels)
                }
            } else {
                Log.d(TAG, "Загрузка каналов из assets")
                val localChannels = loadLocalChannels()
                withContext(Dispatchers.Main) {
                    updateChannelList(localChannels)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка загрузки каналов: ${e.message}")
            withContext(Dispatchers.Main) {
                val localChannels = loadLocalChannels()
                updateChannelList(localChannels)
            }
        }
    }
    
    /**
     * Показывает диалог недоступности сервиса
     */
    private fun showServiceUnavailableDialog(message: String) {
        runOnUiThread {
            val displayMessage = message.ifEmpty { 
                "Сервис временно не доступен.\nОбратитесь к администратору." 
            }
            
            AlertDialog.Builder(this)
                .setTitle("⚠️ Сервис недоступен")
                .setMessage(displayMessage)
                .setPositiveButton("OK") { _, _ ->
                    finish()
                }
                .setCancelable(false)
                .show()
        }
    }
    

    
    /**
     * Получает кэшированный список каналов
     */
    private fun getCachedChannelList(): String? {
        return try {
            val file = java.io.File(filesDir, "cached_channels.m3u8")
            if (file.exists()) {
                file.readText()
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка чтения кэшированных каналов: ${e.message}")
            null
        }
    }



    private fun loadChannels() {
        lifecycleScope.launch {
            try {
                // Сначала пытаемся загрузить из локального файла
                val localChannels = loadLocalChannels()
                if (localChannels.isNotEmpty()) {
                    updateChannelList(localChannels)
                }
                
                // Затем пытаемся обновить из удаленного источника
                val remoteChannels = remoteConfigManager.downloadChannelList()
                if (remoteChannels != null && remoteChannels.isNotEmpty()) {
                    val channelList = parseM3U8(remoteChannels)
                    updateChannelList(channelList)
                    showToast("Каналы обновлены")
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "Ошибка загрузки каналов", e)
                showToast("Ошибка загрузки каналов")
            }
        }
    }
    
    private fun loadLocalChannels(): List<Channel> {
        return try {
            val inputStream = assets.open("custom.m3u8")
            val content = inputStream.bufferedReader().use { it.readText() }
            parseM3U8(content)
        } catch (e: Exception) {
            Log.e("MainActivity", "Ошибка загрузки локальных каналов", e)
            emptyList()
        }
    }
    
    private fun parseM3U8(content: String): List<Channel> {
        val channels = mutableListOf<Channel>()
        val lines = content.split("\n")
        
        var i = 0
        while (i < lines.size) {
            val line = lines[i].trim()
            if (line.startsWith("#EXTINF:")) {
                val info = line.substring(8)
                val parts = info.split(",")
                if (parts.size >= 2) {
                    val name = parts[1].trim()
                    
                    if (i + 1 < lines.size) {
                        val url = lines[i + 1].trim()
                        if (url.isNotEmpty() && !url.startsWith("#")) {
                            channels.add(Channel(name, R.drawable.channel_placeholder, url))
                        }
                    }
                }
                i++
            }
            i++
        }
        
        return channels
    }
    
    private fun updateChannelList(newChannels: List<Channel>) {
        channels.clear()
        channels.addAll(newChannels)
        adapter.notifyDataSetChanged()
    }
    
    private fun playChannel(channel: Channel) {
        val intent = Intent(this, PlayerActivity::class.java)
        intent.putExtra("channel_name", channel.name)
        intent.putExtra("channel_url", channel.streamUrl)
        startActivity(intent)
    }
    
    private fun showMaintenanceDialog(message: String) {
        val dialog = AlertDialog.Builder(this)
            .setTitle("Техническое обслуживание")
            .setMessage(message.ifEmpty { "Проводится техническое обслуживание. Попробуйте позже." })
            .setPositiveButton("OK") { _, _ ->
                finish()
            }
            .setCancelable(false)
            .create()
        dialog.show()
    }
    
    /**
     * Показать диалог для ввода токена администратора
     */
    private fun showAdminLoginDialog() {
        val builder = AlertDialog.Builder(this)
        val input = EditText(this)
        input.inputType = InputType.TYPE_TEXT_VARIATION_PASSWORD
        input.hint = "Введите токен администратора"
        
        builder.setTitle("🔐 Авторизация администратора")
        builder.setMessage("Введите GitHub Personal Access Token для выполнения административных действий")
        builder.setView(input)
        
        builder.setPositiveButton("Войти") { _, _ ->
            val token = input.text.toString().trim()
            if (token.isNotEmpty()) {
                loginAdmin(token)
            } else {
                showToast("Токен не может быть пустым")
            }
        }
        
        builder.setNegativeButton("Отмена", null)
        builder.show()
    }
    
    /**
     * Выполнить авторизацию администратора
     */
    private fun loginAdmin(token: String) {
        lifecycleScope.launch {
            try {
                showToast("Проверка токена...")
                
                // Устанавливаем токен
                remoteConfigManager.setAccessToken(token)
                
                // Проверяем авторизацию
                if (remoteConfigManager.performAdminAction("login_test")) {
                    showToast("✅ Авторизация успешна")
                    // Обновляем меню для показа административных функций
                    invalidateOptionsMenu()
                } else {
                    showToast("❌ Неверный токен")
                    remoteConfigManager.clearAccessToken()
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "Ошибка авторизации администратора", e)
                showToast("Ошибка авторизации")
                remoteConfigManager.clearAccessToken()
            }
        }
    }
    
    /**
     * Выйти из режима администратора
     */
    private fun logoutAdmin() {
        remoteConfigManager.clearAccessToken()
        showToast("Выход из административного режима")
        invalidateOptionsMenu()
    }
    
    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}

// Адаптер для каналов
class ChannelAdapter(
    private val channels: List<Channel>,
    private val onClick: (Channel, Int) -> Unit
) : RecyclerView.Adapter<ChannelAdapter.ChannelViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChannelViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_channel, parent, false)
        return ChannelViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChannelViewHolder, position: Int) {
        val channel = channels[position]
        holder.bind(channel)
        holder.itemView.setOnClickListener { onClick(channel, position) }
        
        // Минимальный эффект увеличения
        holder.itemView.setOnFocusChangeListener { v, hasFocus ->
            v.isSelected = hasFocus
            if (hasFocus) {
                v.animate().scaleX(1.05f).scaleY(1.05f).z(10f).setDuration(150).start()
            } else {
                v.animate().scaleX(1.0f).scaleY(1.0f).z(0f).setDuration(150).start()
            }
        }
    }

    override fun getItemCount() = channels.size

    class ChannelViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val logo: ImageView = itemView.findViewById(R.id.channelLogo)
        private val name: TextView = itemView.findViewById(R.id.channelName)
        
        fun bind(channel: Channel) {
            // Автоматическая загрузка иконки канала
            loadChannelIcon(channel.name, logo)
            name.text = channel.name
        }
        
        private fun loadChannelIcon(channelName: String, imageView: ImageView) {
            // Создаем имя файла иконки на основе названия канала
            val iconName = "channel_${channelName.lowercase()
                .replace(" ", "_")
                .replace("россия", "russia")
                .replace("первый", "perviy")
                .replace("канал", "kanal") 
                .replace("нтв", "ntv")
                .replace("рен", "ren")
                .replace("тв", "tv")
                .replace("центр", "centr")
                .replace("звезда", "zvezda")
                .replace("домашний", "domashniy")
                .replace("культура", "kultura")
                .replace("пятница", "pyatnica")
                .replace("карусель", "karusel")
                .replace("матч!", "match")
                .replace("матч", "match")
                .replace("мир", "mir")
                .replace("муз", "muz")
                .replace("рбк", "rbk")
                .replace("отр", "otr")
                .replace("спас", "spas")
                .replace("стс", "sts")
                .replace("тнт", "tnt")
                .replace("рtr_планета_европа", "rtr_planeta_evropa")
                .replace("hd", "")
                .replace("-", "_")
                .replace(".", "_")
                .replace("!", "")
                .trim('_')}"
            
            // Ищем ресурс по имени
            val iconResId = itemView.context.resources.getIdentifier(
                iconName, "drawable", itemView.context.packageName
            )
            
            if (iconResId != 0) {
                // Если иконка найдена, используем её
                imageView.setImageResource(iconResId)
            } else {
                // Иначе используем заглушку
                imageView.setImageResource(R.drawable.channel_placeholder)
            }
        }
    }
} 