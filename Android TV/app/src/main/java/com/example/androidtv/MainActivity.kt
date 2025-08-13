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
import kotlinx.coroutines.delay

class MainActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ChannelAdapter
    private lateinit var remoteConfigManager: RemoteConfigManager
    private lateinit var autoUpdateManager: AutoUpdateManager
    internal lateinit var channelLogoManager: ChannelLogoManager
    private lateinit var deviceManager: DeviceManager
    private var channels = mutableListOf<Channel>()
    private var periodicConfigCheckJob: Job? = null
    
    companion object {
        private const val TAG = "MainActivity"
        private const val CONFIG_CHECK_INTERVAL = 300000L // 5 минут
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        // Предотвращаем выключение экрана во время использования приложения
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        
        // Инициализируем менеджеры
        remoteConfigManager = RemoteConfigManager(this)
        autoUpdateManager = AutoUpdateManager(this)
        channelLogoManager = ChannelLogoManager(this)
        deviceManager = DeviceManager(this)

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
        
        // Запускаем периодическую проверку конфигурации
        startPeriodicConfigCheck()
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
        stopPeriodicConfigCheck()
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
                    showServiceUnavailableDialog("Сервис временно недоступен.\nОбратитесь к администратору.")
                    return
                }
                
                // Проверяем режим обслуживания
                if (remoteConfig.maintenanceMode) {
                    showMaintenanceDialog(remoteConfig.message)
                    return
                }
                
                // Проверяем обновления приложения (автоматически, без диалога)
                autoUpdateManager.checkForUpdates(remoteConfig, false)
                
                // ВСЕГДА проверяем и обновляем каналы при запуске
                Log.d(TAG, "Проверка обновлений каналов при запуске...")
                
                // Сначала принудительно загружаем mapping логотипов из GitHub
                Log.d(TAG, "Принудительная загрузка mapping логотипов...")
                channelLogoManager.forceUpdateMapping()
                
                // Затем проверяем обновления логотипов
                channelLogoManager.checkAndUpdateChannelLogos()
                
                updateChannelsFromRemote()

                // Загружаем runtime whitelist доменов из удаленного security_config.xml
                lifecycleScope.launch(Dispatchers.IO) {
                    try {
                        remoteConfigManager.downloadSecurityConfig()
                    } catch (_: Exception) { }
                }
                
                // Инициализируем систему учета устройств
                lifecycleScope.launch(Dispatchers.IO) {
                    try {
                        // Регистрируем устройство если еще не зарегистрировано
                        if (!deviceManager.isRegistered()) {
                            deviceManager.registerDevice()
                        }
                        
                        // Запускаем мониторинг устройства
                        deviceManager.startDeviceMonitoring(lifecycleScope)
                        
                        Log.d(TAG, "Device ID: ${deviceManager.getCurrentDeviceId()}")
                        Log.d(TAG, "Service enabled: ${deviceManager.isServiceEnabled()}")
                    } catch (e: Exception) {
                        Log.e(TAG, "Ошибка инициализации DeviceManager: ${e.message}")
                    }
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
        // Проверяем, разрешена ли работа сервиса для этого устройства
        if (::deviceManager.isInitialized && !deviceManager.isServiceEnabled()) {
            Log.d(TAG, "Сервис отключен для этого устройства, пропускаем обновление каналов")
            return
        }
        
        try {
            Log.d(TAG, "Проверка обновлений каналов из удаленного источника...")
            
            // Получаем текущую конфигурацию для проверки версии
            val remoteConfig = remoteConfigManager.checkRemoteConfig()
            
            if (remoteConfig != null) {
                val remoteVersion = remoteConfig.channelsVersion
                val localVersion = remoteConfigManager.getChannelVersion()
                
                Log.d(TAG, "Версия каналов: локальная=$localVersion, удаленная=$remoteVersion")
                
                // Загружаем каналы если версия изменилась ИЛИ если локальных каналов нет
                val cachedChannels = getCachedChannelList()
                val shouldUpdate = remoteVersion > localVersion || cachedChannels == null
                
                if (shouldUpdate) {
                    Log.d(TAG, "Обновление каналов необходимо, загружаем...")
                    val channelList = remoteConfigManager.downloadChannelList()
                    
                    if (channelList != null) {
                        val parsedChannels = parseM3U8(channelList)
                        
                        // Обновляем версию каналов
                        remoteConfigManager.updateChannelVersion(remoteVersion)
                        
                        withContext(Dispatchers.Main) {
                            updateChannelList(parsedChannels)
                            if (remoteVersion > localVersion) {
                                Toast.makeText(this@MainActivity, "Каналы обновлены до версии $remoteVersion", Toast.LENGTH_SHORT).show()
                            }
                        }
                        
                        Log.d(TAG, "Каналы успешно обновлены до версии $remoteVersion")
                    } else {
                        Log.w(TAG, "Не удалось загрузить каналы, используем кэш")
                        loadChannelsFromCacheOrAssets()
                    }
                } else {
                    Log.d(TAG, "Каналы актуальны (версия $remoteVersion), загружаем из кэша")
                    loadChannelsFromCacheOrAssets()
                }
            } else {
                Log.w(TAG, "Не удалось получить конфигурацию, используем кэш")
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
        
        // Передаем весь список каналов и индекс выбранного канала
        intent.putParcelableArrayListExtra("CHANNEL_LIST", ArrayList(channels))
        intent.putExtra("CHANNEL_INDEX", channels.indexOf(channel))
        
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
     * Показывает экран недоступности сервиса
     */
    private fun showServiceUnavailableDialog(message: String) {
        val serviceMessage = message.ifEmpty { "Сервис временно недоступен.\nОбратитесь к администратору." }
        Log.d(TAG, "Показываем экран недоступности сервиса: $serviceMessage")
        ServiceUnavailableActivity.start(this, serviceMessage)
        finish() // Закрываем MainActivity
    }
    
    /**
     * Запускает периодическую проверку конфигурации сервиса
     */
    private fun startPeriodicConfigCheck() {
        periodicConfigCheckJob = lifecycleScope.launch {
            while (isActive) {
                try {
                    delay(CONFIG_CHECK_INTERVAL)
                    
                    if (isActive) {
                        checkServiceConfigPeriodically()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Ошибка периодической проверки конфигурации: ${e.message}")
                }
            }
        }
    }
    
    /**
     * Останавливает периодическую проверку конфигурации
     */
    private fun stopPeriodicConfigCheck() {
        periodicConfigCheckJob?.cancel()
        periodicConfigCheckJob = null
    }
    
    /**
     * Выполняет периодическую проверку конфигурации сервиса
     */
    private suspend fun checkServiceConfigPeriodically() {
        try {
            Log.d(TAG, "Периодическая проверка конфигурации...")
            val remoteConfig = remoteConfigManager.forceCheckRemoteConfig() // Принудительная проверка без кэша
            
            if (remoteConfig != null) {
                Log.d(TAG, "Получена конфигурация - сервис доступен: ${remoteConfig.serviceAvailable}, обслуживание: ${remoteConfig.maintenanceMode}")
                
                // Проверяем доступность сервиса
                if (!remoteConfig.serviceAvailable) {
                    Log.w(TAG, "Сервис стал недоступен во время работы - перенаправляем на экран блокировки")
                    withContext(Dispatchers.Main) {
                        showServiceUnavailableDialog("Сервис временно недоступен.\nОбратитесь к администратору.")
                    }
                    return
                }
                
                // Проверяем режим обслуживания
                if (remoteConfig.maintenanceMode) {
                    Log.w(TAG, "Включен режим обслуживания во время работы - перенаправляем на экран обслуживания")
                    withContext(Dispatchers.Main) {
                        showMaintenanceDialog(remoteConfig.message)
                    }
                    return
                }
                
                // Проверяем обновления каналов
                val remoteVersion = remoteConfig.channelsVersion
                val localVersion = remoteConfigManager.getChannelVersion()
                
                if (remoteVersion > localVersion) {
                    Log.i(TAG, "Обнаружены обновления каналов: $localVersion -> $remoteVersion")
                    updateChannelsFromRemote()
                } else {
                    Log.d(TAG, "Каналы актуальны, версия: $remoteVersion")
                }
                
                // Проверяем обновления заставки и логотипов каналов
                checkSplashUpdates(remoteConfig)
                checkChannelLogosUpdates()
                
            } else {
                Log.w(TAG, "Не удалось получить конфигурацию при периодической проверке - возможно проблемы с сетью")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка периодической проверки конфигурации: ${e.message}", e)
        }
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
    
    /**
     * Проверяет обновления заставки
     */
    private suspend fun checkSplashUpdates(remoteConfig: RemoteConfigManager.RemoteConfig) {
        try {
            // Здесь можно добавить логику проверки версии заставки
            Log.d(TAG, "Проверка обновлений заставки...")
            // SplashImageManager уже проверяет обновления автоматически
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка проверки обновлений заставки: ${e.message}")
        }
    }
    
    /**
     * Проверяет обновления логотипов каналов
     */
    private suspend fun checkChannelLogosUpdates() {
        try {
            Log.d(TAG, "Проверка обновлений логотипов каналов...")
            val hasUpdates = channelLogoManager.checkAndUpdateChannelLogos()
            if (hasUpdates) {
                Log.d(TAG, "Логотипы каналов обновлены")
                // Обновляем адаптер для перезагрузки логотипов
                withContext(Dispatchers.Main) {
                    adapter.notifyDataSetChanged()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка проверки обновлений логотипов: ${e.message}")
        }
    }
    
    /**
     * Публичный метод для загрузки логотипа канала
     * Используется из ChannelAdapter
     */
    fun loadChannelLogoIntoView(channelName: String, imageView: ImageView) {
        lifecycleScope.launch {
            channelLogoManager.loadChannelLogo(channelName, imageView)
        }
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
            // Используем публичный метод MainActivity для загрузки логотипов
            val context = itemView.context
            if (context is MainActivity) {
                // Используем публичный метод вместо прямого доступа к channelLogoManager
                context.loadChannelLogoIntoView(channelName, imageView)
            } else {
                // Fallback к старому методу если контекст не MainActivity
                loadChannelIconFallback(channelName, imageView)
            }
        }
        
        private fun loadChannelIconFallback(channelName: String, imageView: ImageView) {
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