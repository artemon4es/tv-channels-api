 package com.example.androidtv

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import android.widget.ImageView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest

/**
 * Менеджер для динамического обновления логотипов каналов
 */
class ChannelLogoManager(private val context: Context) {
    
    companion object {
        private const val TAG = "ChannelLogoManager"
        private const val BASE_URL = "https://artemon4es.github.io/tv-channels-api/files/channel-logos"
        
        private const val PREFS_NAME = "channel_logos"
        private const val KEY_LOGOS_VERSION = "logos_version"
        private const val KEY_LAST_CHECK = "last_check"
        
        private const val CACHE_DIR = "channel_logos_cache"
        private const val CHECK_INTERVAL = 30 * 60 * 1000L // 30 минут
    }
    
    private val client = OkHttpClient()
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    /**
     * Проверяет и загружает обновления логотипов каналов
     */
    suspend fun checkAndUpdateChannelLogos(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                // Проверяем, нужно ли проверять обновления
                if (!shouldCheckForUpdates()) {
                    Log.d(TAG, "Проверка обновлений логотипов не требуется")
                    return@withContext false
                }
                
                Log.d(TAG, "Проверка обновлений логотипов каналов...")
                
                // Получаем список доступных логотипов с сервера
                val availableLogos = getAvailableLogos()
                var hasUpdates = false
                
                // Проверяем каждый логотип
                for (logoName in availableLogos) {
                    if (checkAndDownloadLogo(logoName)) {
                        hasUpdates = true
                        Log.d(TAG, "Обновлен логотип: $logoName")
                    }
                }
                
                // Обновляем время последней проверки
                prefs.edit().putLong(KEY_LAST_CHECK, System.currentTimeMillis()).apply()
                
                if (hasUpdates) {
                    Log.d(TAG, "Логотипы каналов обновлены")
                } else {
                    Log.d(TAG, "Обновлений логотипов нет")
                }
                
                hasUpdates
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка проверки обновлений логотипов: ${e.message}")
                false
            }
        }
    }
    
    /**
     * Загружает логотип канала в ImageView
     */
    suspend fun loadChannelLogo(channelName: String, imageView: ImageView) {
        withContext(Dispatchers.IO) {
            try {
                // Проверяем, есть ли логотип для этого канала
                val logoFileName = getLogoFileName(channelName)
                
                if (logoFileName == null) {
                    // Логотипа нет - скрываем ImageView
                    withContext(Dispatchers.Main) {
                        imageView.visibility = android.view.View.GONE
                        Log.d(TAG, "Логотип для $channelName не найден - скрываем ImageView")
                    }
                    return@withContext
                }
                
                val cachedFile = File(getCacheDir(), logoFileName)
                
                val bitmap = if (cachedFile.exists()) {
                    // Загружаем из кэша
                    Log.d(TAG, "Загрузка логотипа $logoFileName из кэша")
                    BitmapFactory.decodeFile(cachedFile.absolutePath)
                } else {
                    // Пытаемся загрузить из drawable ресурсов
                    Log.d(TAG, "Загрузка логотипа $logoFileName из ресурсов")
                    loadFromDrawableResources(channelName)
                }
                
                // Устанавливаем изображение в UI потоке
                withContext(Dispatchers.Main) {
                    if (bitmap != null) {
                        imageView.visibility = android.view.View.VISIBLE
                        imageView.setImageBitmap(bitmap)
                        Log.d(TAG, "Логотип $logoFileName загружен в ImageView")
                    } else {
                        // Если даже в ресурсах нет - скрываем
                        imageView.visibility = android.view.View.GONE
                        Log.d(TAG, "Логотип для $channelName не найден в ресурсах - скрываем ImageView")
                    }
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка загрузки логотипа для $channelName: ${e.message}")
                
                // В случае ошибки скрываем ImageView
                withContext(Dispatchers.Main) {
                    imageView.visibility = android.view.View.GONE
                }
            }
        }
    }
    
    /**
     * Генерирует имя файла логотипа на основе названия канала
     */
    private fun generateLogoFileName(channelName: String): String {
        return "${channelName.lowercase()
            .replace(" ", "_")
            .replace("россия", "rossiya")
            .replace("первый", "perviy")
            .replace("канал", "kanal") 
            .replace("нтв", "ntv")
            .replace("рен", "ren")
            .replace("тв", "tv")
            .replace("центр", "centr")
            .replace("звезда", "tvzvezda")
            .replace("домашний", "domashniy")
            .replace("культура", "kultura")
            .replace("пятница", "friday")
            .replace("карусель", "karusel")
            .replace("матч!", "match_tv")
            .replace("матч", "match_tv")
            .replace("мир", "mir")
            .replace("муз", "muz")
            .replace("рбк", "rbc")
            .replace("отр", "otr")
            .replace("спас", "spas")
            .replace("стс", "sts")
            .replace("тнт", "tnt")
            .replace("5 канал", "5-kanal")
            .replace("тв-3", "tv-3")
            .replace("тв центр", "tvc")
            .replace("hd", "")
            .replace("-", "_")
            .replace(".", "_")
            .replace("!", "")
            .trim('_')}.png"
    }
    
    /**
     * Проверяет наличие логотипа для канала и возвращает имя файла или null
     */
    private fun getLogoFileName(channelName: String): String? {
        val name = channelName.lowercase().trim()
        
        // Точные соответствия названий каналов и файлов логотипов
        val exactMatches = mapOf(
            "первый канал hd" to "ortl.png",
            "россия 1 hd" to "rossiya-1.png",
            "россия-24" to "rossiya-24.png",
            "россия культура" to "kultura.png",
            "нтв hd" to "ntv.png",
            "тнт hd" to "tnt.png",
            "стс hd" to "sts.png",
            "тв-3 hd" to "tv-3.png",
            "звезда hd" to "tvzvezda.png",
            "рен тв hd" to "ren_tv.png",
            "тв центр hd" to "tvc.png",
            "5 канал россия" to "5-kanal.png",
            "домашний hd" to "domashniy.png",
            "пятница hd" to "friday.png",
            "мир hd" to "mir.png",
            "отр hd" to "otr.png",
            "рбк" to "rbc.png",
            "спас" to "spas.png",
            "карусель" to "karusel.png",
            "муз тв" to "muz.png",
            "матч тв" to "match_tv.png",
            "матч! тв" to "match_tv.png"
        )
        
        // Проверяем точное соответствие
        exactMatches[name]?.let { return it }
        
        // Если точного соответствия нет, пытаемся найти по частичному совпадению
        for ((channelPattern, logoFile) in exactMatches) {
            if (name.contains(channelPattern.replace(" hd", "")) || 
                channelPattern.replace(" hd", "").contains(name)) {
                return logoFile
            }
        }
        
        // Если ничего не найдено, возвращаем null (логотип не будет показан)
        return null
    }
    
    /**
     * Загружает логотип из drawable ресурсов (fallback)
     */
    private fun loadFromDrawableResources(channelName: String): Bitmap? {
        return try {
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
                .replace("hd", "")
                .replace("-", "_")
                .replace(".", "_")
                .replace("!", "")
                .trim('_')}"
            
            val iconResId = context.resources.getIdentifier(
                iconName, "drawable", context.packageName
            )
            
            if (iconResId != 0) {
                BitmapFactory.decodeResource(context.resources, iconResId)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка загрузки из ресурсов: ${e.message}")
            null
        }
    }
    
    /**
     * Получает список доступных логотипов с сервера
     */
    private suspend fun getAvailableLogos(): List<String> {
        return try {
            // В реальной реализации можно получать список через API
            // Пока используем предопределенный список
            listOf(
                "rossiya-1.png", "ntv.png", "rossiya-24.png", "kultura.png",
                "tnt.png", "sts.png", "tv-3.png", "tvzvezda.png", "ren_tv.png",
                "tvc.png", "5-kanal.png", "domashniy.png", "friday.png", "mir.png",
                "otr.png", "rbc.png", "muz.png", "karusel.png", "spas.png", "ortl.png",
                "match_tv.png"
            )
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка получения списка логотипов: ${e.message}")
            emptyList()
        }
    }
    
    /**
     * Проверяет и загружает логотип если он изменился
     */
    private suspend fun checkAndDownloadLogo(logoFileName: String): Boolean {
        return try {
            val url = "$BASE_URL/$logoFileName"
            val request = Request.Builder()
                .url(url)
                .build()
            
            val response = client.newCall(request).execute()
            
            if (response.isSuccessful) {
                val imageBytes = response.body?.bytes()
                if (imageBytes != null) {
                    val newHash = calculateHash(imageBytes)
                    val oldHash = prefs.getString("hash_$logoFileName", "")
                    
                    if (newHash != oldHash) {
                        // Логотип изменился, сохраняем новый
                        val cacheFile = File(getCacheDir(), logoFileName)
                        FileOutputStream(cacheFile).use { it.write(imageBytes) }
                        
                        // Сохраняем новый хеш
                        prefs.edit().putString("hash_$logoFileName", newHash).apply()
                        
                        Log.d(TAG, "$logoFileName обновлен (hash: $newHash)")
                        return true
                    } else {
                        Log.d(TAG, "$logoFileName не изменился")
                    }
                }
            } else {
                Log.w(TAG, "Не удалось загрузить $url: ${response.code}")
            }
            
            response.close()
            false
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка загрузки $logoFileName: ${e.message}")
            false
        }
    }
    
    /**
     * Проверяет, нужно ли проверять обновления
     */
    private fun shouldCheckForUpdates(): Boolean {
        val lastCheck = prefs.getLong(KEY_LAST_CHECK, 0)
        val currentTime = System.currentTimeMillis()
        return (currentTime - lastCheck) > CHECK_INTERVAL
    }
    
    /**
     * Получает директорию кэша для логотипов
     */
    private fun getCacheDir(): File {
        val cacheDir = File(context.filesDir, CACHE_DIR)
        if (!cacheDir.exists()) {
            cacheDir.mkdirs()
        }
        return cacheDir
    }
    
    /**
     * Вычисляет MD5 хеш для массива байт
     */
    private fun calculateHash(bytes: ByteArray): String {
        val md = MessageDigest.getInstance("MD5")
        val digest = md.digest(bytes)
        return digest.joinToString("") { "%02x".format(it) }
    }
    
    /**
     * Очищает кэш логотипов
     */
    fun clearCache() {
        try {
            val cacheDir = getCacheDir()
            cacheDir.listFiles()?.forEach { it.delete() }
            prefs.edit().clear().apply()
            Log.d(TAG, "Кэш логотипов очищен")
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка очистки кэша: ${e.message}")
        }
    }
    
    /**
     * Принудительно обновляет все логотипы
     */
    suspend fun forceUpdateAllLogos(): Boolean {
        prefs.edit().putLong(KEY_LAST_CHECK, 0).apply()
        return checkAndUpdateChannelLogos()
    }
}