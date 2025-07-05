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
        // URL нашего API через GitHub Pages
        private const val API_BASE_URL = "https://artemon4es.github.io/tv-channels-api"
    }

    /**
     * Проверяет статус подписки удаленно через GitHub API
     */
    suspend fun checkSubscriptionStatus(): SubscriptionResult = withContext(Dispatchers.IO) {
        try {
            val url = "$API_BASE_URL/api/config.json"
            
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 10000
            connection.readTimeout = 10000
            connection.setRequestProperty("User-Agent", "AndroidTV-App/1.0")
            connection.setRequestProperty("Cache-Control", "no-cache")
            
            val responseCode = connection.responseCode
            when (responseCode) {
                200 -> {
                    val response = connection.inputStream.bufferedReader().readText()
                    parseConfigResponse(response)
                }
                403 -> SubscriptionResult.Blocked("Доступ заблокирован администратором")
                404 -> SubscriptionResult.Error("Конфигурация не найдена")
                else -> SubscriptionResult.Error("Ошибка сервера: $responseCode")
            }
        } catch (e: Exception) {
            // При ошибке подключения разрешаем доступ (offline режим)
            SubscriptionResult.Active("Offline режим", null)
        }
    }

    /**
     * Получает уникальный ID устройства
     */
    private fun getDeviceId(): String {
        return Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
    }

    /**
     * Парсит конфигурацию GitHub API
     */
    private fun parseConfigResponse(response: String): SubscriptionResult {
        return try {
            // Проверяем доступность сервиса
            val serviceAvailable = response.contains("\"service_available\":true") || 
                                 response.contains("\"service_available\": true")
            
            // Извлекаем сообщение для пользователя
            val message = extractJsonValue(response, "message")
            
            if (serviceAvailable) {
                SubscriptionResult.Active("Сервис доступен", null)
            } else {
                val reason = message?.takeIf { it.isNotEmpty() } 
                    ?: "Сервис временно недоступен.\nОбратитесь к администратору."
                SubscriptionResult.Blocked(reason)
            }
        } catch (e: Exception) {
            // При ошибке парсинга разрешаем доступ
            SubscriptionResult.Active("Парсинг не удался, разрешаем доступ", null)
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
     * Устаревшая тестовая функция - больше не используется
     */
    @Deprecated("Используйте checkSubscriptionStatus() вместо этой функции")
    fun getTestSubscriptionStatus(): SubscriptionResult {
        // Всегда возвращаем активный статус, так как теперь управление через GitHub API
        return SubscriptionResult.Active("Тестовый режим отключен", null)
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