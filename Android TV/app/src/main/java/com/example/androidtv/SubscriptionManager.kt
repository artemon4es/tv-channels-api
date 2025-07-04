package com.example.androidtv

import android.content.Context
import android.provider.Settings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.util.*

/**
 * Менеджер подписки для удаленного контроля доступа к приложению
 */
class SubscriptionManager(private val context: Context) {

    companion object {
        // Замените на ваш реальный API URL
        private const val API_BASE_URL = "https://your-api.com"
        
        // Для тестирования можно использовать Firebase Remote Config
        // или любой другой сервис
    }

    /**
     * Проверяет статус подписки удаленно
     */
    suspend fun checkSubscriptionStatus(): SubscriptionResult = withContext(Dispatchers.IO) {
        try {
            val deviceId = getDeviceId()
            val url = "$API_BASE_URL/subscription/check?device_id=$deviceId"
            
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 10000
            connection.readTimeout = 10000
            connection.setRequestProperty("User-Agent", "AndroidTV-App/1.0")
            
            val responseCode = connection.responseCode
            when (responseCode) {
                200 -> {
                    val response = connection.inputStream.bufferedReader().readText()
                    parseSubscriptionResponse(response)
                }
                403 -> SubscriptionResult.Blocked("Доступ заблокирован администратором")
                404 -> SubscriptionResult.Blocked("Устройство не найдено в системе")
                else -> SubscriptionResult.Error("Ошибка сервера: $responseCode")
            }
        } catch (e: Exception) {
            SubscriptionResult.Error("Ошибка подключения: ${e.message}")
        }
    }

    /**
     * Получает уникальный ID устройства
     */
    private fun getDeviceId(): String {
        return Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
    }

    /**
     * Парсит ответ от сервера
     * Ожидается JSON формат: {"active": true/false, "message": "text", "expires": "2024-12-31"}
     */
    private fun parseSubscriptionResponse(response: String): SubscriptionResult {
        return try {
            // Простой парсинг без JSON библиотеки
            val isActive = response.contains("\"active\":true") || response.contains("\"active\": true")
            val message = extractJsonValue(response, "message")
            val expires = extractJsonValue(response, "expires")
            
            if (isActive) {
                SubscriptionResult.Active(message ?: "Подписка активна", expires)
            } else {
                SubscriptionResult.Blocked(message ?: "Подписка не активна")
            }
        } catch (e: Exception) {
            SubscriptionResult.Error("Ошибка парсинга ответа")
        }
    }

    /**
     * Простое извлечение значения из JSON строки
     */
    private fun extractJsonValue(json: String, key: String): String? {
        val pattern = "\"$key\"\\s*:\\s*\"([^\"]*)\""
        val regex = Regex(pattern)
        return regex.find(json)?.groupValues?.get(1)
    }

    /**
     * Для тестирования - временная проверка без сервера
     */
    fun getTestSubscriptionStatus(): SubscriptionResult {
        val calendar = Calendar.getInstance()
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        
        return when (dayOfWeek) {
            Calendar.SATURDAY, Calendar.SUNDAY -> 
                SubscriptionResult.Blocked("Доступ ограничен в выходные дни")
            else -> 
                SubscriptionResult.Active("Тестовый доступ разрешен", "2024-12-31")
        }
    }
}

/**
 * Результат проверки подписки
 */
sealed class SubscriptionResult {
    data class Active(val message: String, val expiresDate: String?) : SubscriptionResult()
    data class Blocked(val reason: String) : SubscriptionResult()
    data class Error(val error: String) : SubscriptionResult()
} 