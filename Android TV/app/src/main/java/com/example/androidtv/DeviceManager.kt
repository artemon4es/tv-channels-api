package com.example.androidtv

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.*

/**
 * Менеджер для регистрации и мониторинга устройств
 */
class DeviceManager(private val context: Context) {
    
    companion object {
        private const val TAG = "DeviceManager"
        
        // GitHub API URLs
        private const val BASE_URL = "https://artemon4es.github.io/tv-channels-api"
        private const val DEVICE_CONFIG_URL = "$BASE_URL/api/devices/config.json"
        
        // API endpoints для работы с устройствами
        private const val REGISTER_URL = "$BASE_URL/api/devices/register.json"
        private const val PING_URL = "$BASE_URL/api/devices/ping.json"
        private const val STATUS_URL = "$BASE_URL/api/devices/status.json"
        
        private const val PREFS_NAME = "device_manager"
        private const val KEY_DEVICE_ID = "device_id"
        private const val KEY_DEVICE_NAME = "device_name"
        private const val KEY_IS_REGISTERED = "is_registered"
        private const val KEY_LAST_PING = "last_ping"
        private const val KEY_SERVICE_ENABLED = "service_enabled"
        
        // Интервал ping-запросов (5 минут)
        private const val PING_INTERVAL = 5 * 60 * 1000L
    }
    
    private val client = OkHttpClient()
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    // Данные устройства
    data class DeviceInfo(
        val deviceId: String,
        val deviceName: String,
        val androidVersion: String,
        val model: String,
        val manufacturer: String,
        val lastSeen: Long,
        val appVersion: String,
        val isOnline: Boolean,
        val serviceEnabled: Boolean
    )
    
    /**
     * Получает уникальный ID устройства
     */
    private fun getDeviceId(): String {
        var deviceId = prefs.getString(KEY_DEVICE_ID, null)
        if (deviceId == null) {
            // Генерируем уникальный ID на основе Android ID и MAC
            deviceId = try {
                val androidId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
                "androidtv_${androidId}_${UUID.randomUUID().toString().take(8)}"
            } catch (e: Exception) {
                "androidtv_${UUID.randomUUID()}"
            }
            prefs.edit().putString(KEY_DEVICE_ID, deviceId).apply()
        }
        return deviceId
    }
    
    /**
     * Получает имя устройства
     */
    private fun getDeviceName(): String {
        var deviceName = prefs.getString(KEY_DEVICE_NAME, null)
        if (deviceName == null) {
            deviceName = try {
                "${Build.MANUFACTURER} ${Build.MODEL}".trim()
            } catch (e: Exception) {
                "Android TV Device"
            }
            prefs.edit().putString(KEY_DEVICE_NAME, deviceName).apply()
        }
        return deviceName
    }
    
    /**
     * Создает JSON с информацией об устройстве
     */
    private fun createDeviceInfo(): JSONObject {
        return JSONObject().apply {
            put("device_id", getDeviceId())
            put("device_name", getDeviceName())
            put("android_version", Build.VERSION.RELEASE)
            put("api_level", Build.VERSION.SDK_INT)
            put("model", Build.MODEL)
            put("manufacturer", Build.MANUFACTURER)
            put("app_version", getAppVersion())
            put("last_seen", System.currentTimeMillis())
            put("is_online", true)
            put("registration_time", System.currentTimeMillis())
        }
    }
    
    /**
     * Получает версию приложения
     */
    private fun getAppVersion(): String {
        return try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            pInfo.versionName ?: "1.0"
        } catch (e: Exception) {
            "1.0"
        }
    }
    
    /**
     * Регистрирует устройство в системе
     */
    suspend fun registerDevice(): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Регистрация устройства...")
            
            val deviceInfo = createDeviceInfo()
            val json = deviceInfo.toString()
            
            Log.d(TAG, "Данные устройства: $json")
            
            // В реальной реализации здесь был бы POST запрос к API
            // Пока что просто логируем и сохраняем локально
            prefs.edit()
                .putBoolean(KEY_IS_REGISTERED, true)
                .putLong(KEY_LAST_PING, System.currentTimeMillis())
                .putBoolean(KEY_SERVICE_ENABLED, true)
                .apply()
            
            Log.d(TAG, "Устройство зарегистрировано: ${getDeviceId()}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка регистрации устройства: ${e.message}")
            false
        }
    }
    
    /**
     * Отправляет ping сигнал на сервер
     */
    private suspend fun sendPing(): Boolean = withContext(Dispatchers.IO) {
        try {
            val pingData = JSONObject().apply {
                put("device_id", getDeviceId())
                put("timestamp", System.currentTimeMillis())
                put("status", "online")
                put("app_version", getAppVersion())
            }
            
            Log.d(TAG, "Отправка ping: ${getDeviceId()}")
            
            // В реальной реализации здесь был бы POST запрос
            prefs.edit().putLong(KEY_LAST_PING, System.currentTimeMillis()).apply()
            
            true
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка отправки ping: ${e.message}")
            false
        }
    }
    
    /**
     * Проверяет статус устройства на сервере
     */
    suspend fun checkDeviceStatus(): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Проверка статуса устройства...")
            
            // Загружаем конфигурацию устройств
            val request = Request.Builder()
                .url(DEVICE_CONFIG_URL)
                .build()
            
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val configJson = response.body?.string()
                if (configJson != null) {
                    val config = JSONObject(configJson)
                    val deviceId = getDeviceId()
                    
                    // Проверяем, включен ли сервис для этого устройства
                    val devices = config.optJSONObject("devices") ?: JSONObject()
                    val deviceConfig = devices.optJSONObject(deviceId)
                    
                    val serviceEnabled = if (deviceConfig != null) {
                        deviceConfig.optBoolean("service_enabled", true)
                    } else {
                        // Если устройство не найдено в конфиге, используем глобальную настройку
                        config.optBoolean("default_service_enabled", true)
                    }
                    
                    prefs.edit().putBoolean(KEY_SERVICE_ENABLED, serviceEnabled).apply()
                    
                    Log.d(TAG, "Статус сервиса для устройства $deviceId: $serviceEnabled")
                    return@withContext serviceEnabled
                }
            }
            response.close()
            
            // Если не удалось получить конфигурацию, используем локальную настройку
            prefs.getBoolean(KEY_SERVICE_ENABLED, true)
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка проверки статуса устройства: ${e.message}")
            // При ошибке возвращаем локальную настройку
            prefs.getBoolean(KEY_SERVICE_ENABLED, true)
        }
    }
    
    /**
     * Проверяет, включен ли сервис для этого устройства
     */
    fun isServiceEnabled(): Boolean {
        return prefs.getBoolean(KEY_SERVICE_ENABLED, true)
    }
    
    /**
     * Проверяет, зарегистрировано ли устройство
     */
    fun isRegistered(): Boolean {
        return prefs.getBoolean(KEY_IS_REGISTERED, false)
    }
    
    /**
     * Получает ID устройства
     */
    fun getCurrentDeviceId(): String {
        return getDeviceId()
    }
    
    /**
     * Запускает мониторинг устройства (ping каждые 5 минут)
     */
    fun startDeviceMonitoring(scope: CoroutineScope) {
        scope.launch(Dispatchers.IO) {
            while (isActive) {
                try {
                    // Проверяем статус устройства
                    checkDeviceStatus()
                    
                    // Отправляем ping если сервис включен
                    if (isServiceEnabled()) {
                        sendPing()
                    }
                    
                    // Ждем следующий интервал
                    delay(PING_INTERVAL)
                } catch (e: Exception) {
                    Log.e(TAG, "Ошибка в мониторинге устройства: ${e.message}")
                    delay(PING_INTERVAL) // Ждем и пытаемся снова
                }
            }
        }
    }
    
    /**
     * Получает информацию об устройстве
     */
    fun getDeviceInfo(): DeviceInfo {
        return DeviceInfo(
            deviceId = getDeviceId(),
            deviceName = getDeviceName(),
            androidVersion = Build.VERSION.RELEASE,
            model = Build.MODEL,
            manufacturer = Build.MANUFACTURER,
            lastSeen = prefs.getLong(KEY_LAST_PING, 0),
            appVersion = getAppVersion(),
            isOnline = System.currentTimeMillis() - prefs.getLong(KEY_LAST_PING, 0) < PING_INTERVAL * 2,
            serviceEnabled = prefs.getBoolean(KEY_SERVICE_ENABLED, true)
        )
    }
}
