package com.example.androidtv

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.*
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class RemoteConfigManager(private val context: Context) {
    
    private val client = OkHttpClient()
    private val prefs: SharedPreferences = context.getSharedPreferences("remote_config", Context.MODE_PRIVATE)
    
    companion object {
        private const val TAG = "RemoteConfigManager"
        // GitHub Pages API URL
        private const val BASE_URL = "https://artemon4es.github.io/tv-channels-api"
        private const val CONFIG_URL = "$BASE_URL/api/config.json"
        private const val CHANNELS_URL = "$BASE_URL/files/channels.m3u8"
        private const val SECURITY_URL = "$BASE_URL/files/security_config.xml"
        
        // Ключи для SharedPreferences
        private const val KEY_LAST_CHECK = "last_check_time"
        private const val KEY_CHANNEL_VERSION = "channel_version"
        private const val KEY_LAST_CONFIG = "last_config"
        
        // Безопасное хранение токена
        private const val PREFS_NAME = "secure_config"
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_LAST_UPDATE = "last_update"
        private const val KEY_CONFIG_CACHE = "config_cache"
        private const val KEY_CHANNELS_VERSION = "channels_version"
    }
    
    data class RemoteConfig(
        val serviceAvailable: Boolean,
        val message: String,
        val maintenanceMode: Boolean,
        val appLatestVersion: String,
        val appVersionCode: Int,
        val downloadUrl: String,
        val updateRequired: Boolean,
        val changelog: String,
        val channelsVersion: Int,
        val channelsUrl: String,
        val securityConfigUrl: String,
        val splashVersion: Int,
        val splashLogoUrl: String,
        val splashBackgroundUrl: String
    )
    
    /**
     * Установить токен доступа безопасно
     * ВАЖНО: Токен должен передаваться через безопасные каналы
     */
    fun setAccessToken(token: String) {
        prefs.edit().putString(KEY_ACCESS_TOKEN, token).apply()
        Log.d(TAG, "Токен доступа установлен безопасно")
    }
    
    /**
     * Получить токен доступа из безопасного хранилища
     */
    private fun getAccessToken(): String? {
        return prefs.getString(KEY_ACCESS_TOKEN, null)
    }
    
    /**
     * Проверить авторизацию с сервером
     */
    private suspend fun verifyAuth(): Boolean {
        val token = getAccessToken()
        if (token.isNullOrEmpty()) {
            Log.w(TAG, "Токен доступа не установлен")
            return false
        }
        
        return withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url("https://api.github.com/user")
                    .header("Authorization", "token $token")
                    .build()
                
                val response = client.newCall(request).execute()
                val isValid = response.isSuccessful
                
                if (isValid) {
                    Log.d(TAG, "Авторизация успешна")
                } else {
                    Log.w(TAG, "Авторизация не удалась: ${response.code}")
                }
                
                response.close()
                isValid
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка проверки авторизации", e)
                false
            }
        }
    }
    
    /**
     * Проверяет удаленную конфигурацию
     */
    suspend fun checkRemoteConfig(): RemoteConfig? {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Проверка удаленной конфигурации с URL: $CONFIG_URL")
                
                val request = Request.Builder()
                    .url(CONFIG_URL)
                    .addHeader("Cache-Control", "no-cache")
                    .addHeader("Pragma", "no-cache")
                    .addHeader("Expires", "0")
                    .build()
                
                val response = client.newCall(request).execute()
                Log.d(TAG, "Ответ сервера: ${response.code}")
                
                if (response.isSuccessful) {
                    val jsonString = response.body?.string()
                    response.close()
                    
                    if (jsonString != null) {
                        Log.d(TAG, "Получена конфигурация: ${jsonString.take(200)}...")
                        val json = JSONObject(jsonString)
                        val config = parseConfig(json)
                        
                        Log.d(TAG, "Статус сервиса: ${config.serviceAvailable}, сообщение: '${config.message}'")
                        
                        // Кэшируем конфигурацию
                        prefs.edit().putString(KEY_CONFIG_CACHE, jsonString).apply()
                        
                        Log.d(TAG, "Конфигурация получена и кэширована")
                        return@withContext config
                    }
                } else {
                    Log.e(TAG, "Ошибка получения конфигурации: ${response.code} ${response.message}")
                }
                
                // Если не удалось получить, используем кэш
                val cachedConfig = prefs.getString(KEY_CONFIG_CACHE, null)
                if (cachedConfig != null) {
                    Log.d(TAG, "Используется кэшированная конфигурация")
                    return@withContext parseConfig(JSONObject(cachedConfig))
                }
                
                response.close()
                null
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка получения конфигурации", e)
                
                // Возвращаем кэшированную конфигурацию при ошибке
                val cachedConfig = prefs.getString(KEY_CONFIG_CACHE, null)
                if (cachedConfig != null) {
                    Log.d(TAG, "Используется кэшированная конфигурация после ошибки")
                    return@withContext parseConfig(JSONObject(cachedConfig))
                }
                
                null
            }
        }
    }
    
    /**
     * Загружает список каналов
     */
    suspend fun downloadChannelList(): String? = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Загрузка списка каналов...")
            
            val request = Request.Builder()
                .url(CHANNELS_URL)
                .addHeader("Cache-Control", "no-cache")
                .build()
                
            val response = client.newCall(request).execute()
            
            if (response.isSuccessful) {
                val channelList = response.body?.string()
                if (channelList != null) {
                    saveChannelListToCache(channelList)
                    Log.d(TAG, "Список каналов загружен и сохранен")
                }
                channelList
            } else {
                Log.e(TAG, "Ошибка загрузки каналов: ${response.code}")
                getCachedChannelList()
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка загрузки каналов: ${e.message}")
            getCachedChannelList()
        }
    }
    
    /**
     * Загружает конфигурацию безопасности
     */
    suspend fun downloadSecurityConfig(): String? = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Загрузка конфигурации безопасности...")
            
            val request = Request.Builder()
                .url(SECURITY_URL)
                .addHeader("Cache-Control", "no-cache")
                .build()
                
            val response = client.newCall(request).execute()
            
            if (response.isSuccessful) {
                val securityConfig = response.body?.string()
                if (securityConfig != null) {
                    saveSecurityConfigToCache(securityConfig)
                    Log.d(TAG, "Конфигурация безопасности загружена и сохранена")
                }
                securityConfig
            } else {
                Log.e(TAG, "Ошибка загрузки конфигурации безопасности: ${response.code}")
                null
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка загрузки конфигурации безопасности: ${e.message}")
            null
        }
    }
    
    /**
     * Проверяет, нужно ли обновлять каналы
     */
    fun shouldUpdateChannels(remoteVersion: Int): Boolean {
        val localVersion = prefs.getInt(KEY_CHANNEL_VERSION, 0)
        return remoteVersion > localVersion
    }
    
    /**
     * Обновляет версию каналов в локальном хранилище
     */
    fun updateChannelVersion(version: Int) {
        prefs.edit().putInt(KEY_CHANNEL_VERSION, version).apply()
        Log.d(TAG, "Версия каналов обновлена до: $version")
    }
    
    /**
     * Получает текущую версию каналов
     */
    fun getChannelVersion(): Int {
        return prefs.getInt(KEY_CHANNEL_VERSION, 0)
    }
    
    /**
     * Получает время последней проверки
     */
    fun getLastCheckTime(): Long {
        return prefs.getLong(KEY_LAST_CHECK, 0)
    }
    
    /**
     * Проверяет, нужно ли проверять обновления (раз в 30 минут)
     */
    fun shouldCheckForUpdates(): Boolean {
        val lastCheck = getLastCheckTime()
        val currentTime = System.currentTimeMillis()
        val thirtyMinutes = 30 * 60 * 1000L
        return (currentTime - lastCheck) > thirtyMinutes
    }
    
    private fun parseConfig(json: JSONObject): RemoteConfig {
        val appInfo = json.optJSONObject("app_info") ?: JSONObject()
        val serviceConfig = json.optJSONObject("service_config") ?: JSONObject()
        val channelsConfig = json.optJSONObject("channels_config") ?: JSONObject()
        val splashConfig = json.optJSONObject("splash_config") ?: JSONObject()
        
        return RemoteConfig(
            serviceAvailable = serviceConfig.optBoolean("service_available", true),
            message = serviceConfig.optString("message", ""),
            maintenanceMode = serviceConfig.optBoolean("maintenance_mode", false),
            appLatestVersion = appInfo.optString("latest_version", "1.0"),
            appVersionCode = appInfo.optInt("version_code", 1),
            downloadUrl = appInfo.optString("download_url", ""),
            updateRequired = appInfo.optBoolean("update_required", false),
            changelog = appInfo.optString("changelog", ""),
            channelsVersion = channelsConfig.optInt("version", 1),
            channelsUrl = channelsConfig.optString("url", CHANNELS_URL),
            securityConfigUrl = channelsConfig.optString("security_config_url", SECURITY_URL),
            splashVersion = splashConfig.optInt("version", 1),
            splashLogoUrl = splashConfig.optString("logo_url", "$BASE_URL/files/splash/logo_app.png"),
            splashBackgroundUrl = splashConfig.optString("background_url", "$BASE_URL/files/splash/background.png")
        )
    }
    
    private fun getLastCachedConfig(): RemoteConfig? {
        val cachedJson = prefs.getString(KEY_LAST_CONFIG, null)
        return if (cachedJson != null) {
            try {
                parseConfig(JSONObject(cachedJson))
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка парсинга кэшированной конфигурации: ${e.message}")
                null
            }
        } else {
            null
        }
    }
    
    private fun saveChannelListToCache(channelList: String) {
        try {
            val file = File(context.filesDir, "cached_channels.m3u8")
            file.writeText(channelList)
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка сохранения списка каналов: ${e.message}")
        }
    }
    
    private fun getCachedChannelList(): String? {
        return try {
            val file = File(context.filesDir, "cached_channels.m3u8")
            if (file.exists()) {
                file.readText()
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка чтения кэшированного списка каналов: ${e.message}")
            null
        }
    }
    
    private fun saveSecurityConfigToCache(securityConfig: String) {
        try {
            val file = File(context.filesDir, "cached_security_config.xml")
            file.writeText(securityConfig)
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка сохранения конфигурации безопасности: ${e.message}")
        }
    }
    
    /**
     * Выполняет административное действие
     */
    suspend fun performAdminAction(action: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val token = getAccessToken()
                if (token.isNullOrEmpty()) {
                    Log.w(TAG, "Токен доступа не установлен")
                    return@withContext false
                }
                
                // Проверяем токен через GitHub API
                val request = Request.Builder()
                    .url("https://api.github.com/user")
                    .header("Authorization", "token $token")
                    .build()
                
                val response = client.newCall(request).execute()
                val isValid = response.isSuccessful
                
                if (isValid) {
                    Log.d(TAG, "Административное действие '$action' выполнено успешно")
                } else {
                    Log.w(TAG, "Административное действие '$action' не удалось: ${response.code}")
                }
                
                response.close()
                isValid
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка выполнения административного действия", e)
                false
            }
        }
    }
    
    /**
     * Очищает токен доступа
     */
    fun clearAccessToken() {
        prefs.edit().remove(KEY_ACCESS_TOKEN).apply()
        Log.d(TAG, "Токен доступа очищен")
    }
    
    /**
     * Принудительно очищает кэш конфигурации
     */
    fun clearConfigCache() {
        prefs.edit().remove(KEY_CONFIG_CACHE).apply()
        Log.d(TAG, "Кэш конфигурации очищен")
    }
    
    /**
     * Проверяет удаленную конфигурацию без использования кэша
     */
    suspend fun forceCheckRemoteConfig(): RemoteConfig? {
        clearConfigCache()
        return checkRemoteConfig()
    }
} 