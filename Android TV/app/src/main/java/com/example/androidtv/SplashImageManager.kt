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
 * Менеджер для динамического обновления заставки приложения
 */
class SplashImageManager(private val context: Context) {
    
    companion object {
        private const val TAG = "SplashImageManager"
        private const val SPLASH_IMAGE_URL = "https://artemon4es.github.io/tv-channels-api/files/splash/splash_image.png"
        private const val SPLASH_LOGO_URL = "https://artemon4es.github.io/tv-channels-api/files/splash/logo_app.png"
        private const val BACKGROUND_URL = "https://artemon4es.github.io/tv-channels-api/files/splash/background.png"
        
        private const val PREFS_NAME = "splash_images"
        private const val KEY_SPLASH_HASH = "splash_hash"
        private const val KEY_LOGO_HASH = "logo_hash"
        private const val KEY_BACKGROUND_HASH = "background_hash"
        private const val KEY_LAST_CHECK = "last_check"
        
        private const val CACHE_DIR = "splash_cache"
        private const val CHECK_INTERVAL = 30 * 60 * 1000L // 30 минут
    }
    
    private val client = OkHttpClient()
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    data class SplashConfig(
        val splashImageUrl: String,
        val logoUrl: String,
        val backgroundUrl: String,
        val version: Int,
        val lastUpdated: String
    )
    
    /**
     * Проверяет и загружает обновления заставки
     */
    suspend fun checkAndUpdateSplashImages(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                // Проверяем, нужно ли проверять обновления
                if (!shouldCheckForUpdates()) {
                    Log.d(TAG, "Проверка обновлений заставки не требуется")
                    return@withContext false
                }
                
                Log.d(TAG, "Проверка обновлений заставки...")
                
                var hasUpdates = false
                
                // Проверяем и обновляем каждое изображение
                if (checkAndDownloadImage(SPLASH_LOGO_URL, "logo_app.png", KEY_LOGO_HASH)) {
                    hasUpdates = true
                    Log.d(TAG, "Обновлен логотип")
                }
                
                if (checkAndDownloadImage(BACKGROUND_URL, "background.png", KEY_BACKGROUND_HASH)) {
                    hasUpdates = true
                    Log.d(TAG, "Обновлен фон")
                }
                
                // Обновляем время последней проверки
                prefs.edit().putLong(KEY_LAST_CHECK, System.currentTimeMillis()).apply()
                
                if (hasUpdates) {
                    Log.d(TAG, "Заставка обновлена")
                } else {
                    Log.d(TAG, "Обновлений заставки нет")
                }
                
                hasUpdates
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка проверки обновлений заставки: ${e.message}")
                false
            }
        }
    }
    
    /**
     * Загружает изображение в ImageView с кэшированием
     */
    suspend fun loadImageIntoView(imageView: ImageView, imageType: ImageType) {
        withContext(Dispatchers.IO) {
            try {
                val fileName = when (imageType) {
                    ImageType.LOGO -> "logo_app.png"
                    ImageType.BACKGROUND -> "background.png"
                }
                
                val cachedFile = File(getCacheDir(), fileName)
                
                val bitmap = if (cachedFile.exists()) {
                    // Загружаем из кэша
                    Log.d(TAG, "Загрузка $fileName из кэша")
                    BitmapFactory.decodeFile(cachedFile.absolutePath)
                } else {
                    // Загружаем из ресурсов по умолчанию
                    Log.d(TAG, "Загрузка $fileName из ресурсов")
                    val resourceId = when (imageType) {
                        ImageType.LOGO -> R.drawable.logo_app
                        ImageType.BACKGROUND -> R.drawable.background
                    }
                    BitmapFactory.decodeResource(context.resources, resourceId)
                }
                
                // Устанавливаем изображение в UI потоке
                withContext(Dispatchers.Main) {
                    if (bitmap != null) {
                        imageView.setImageBitmap(bitmap)
                        Log.d(TAG, "$fileName загружен в ImageView")
                    } else {
                        Log.e(TAG, "Не удалось загрузить $fileName")
                    }
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка загрузки изображения: ${e.message}")
                
                // В случае ошибки загружаем изображение по умолчанию
                withContext(Dispatchers.Main) {
                    val resourceId = when (imageType) {
                        ImageType.LOGO -> R.drawable.logo_app
                        ImageType.BACKGROUND -> R.drawable.background
                    }
                    imageView.setImageResource(resourceId)
                }
            }
        }
    }
    
    /**
     * Устанавливает фон для активности
     */
    suspend fun setActivityBackground(activity: android.app.Activity) {
        withContext(Dispatchers.IO) {
            try {
                val cachedFile = File(getCacheDir(), "background.png")
                
                val bitmap = if (cachedFile.exists()) {
                    BitmapFactory.decodeFile(cachedFile.absolutePath)
                } else {
                    null
                }
                
                withContext(Dispatchers.Main) {
                    if (bitmap != null) {
                        val drawable = android.graphics.drawable.BitmapDrawable(activity.resources, bitmap)
                        activity.window.decorView.background = drawable
                        Log.d(TAG, "Фон активности обновлен")
                    }
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка установки фона: ${e.message}")
            }
        }
    }
    
    /**
     * Проверяет и загружает изображение если оно изменилось
     */
    private suspend fun checkAndDownloadImage(url: String, fileName: String, hashKey: String): Boolean {
        return try {
            val request = Request.Builder()
                .url(url)
                .build()
            
            val response = client.newCall(request).execute()
            
            if (response.isSuccessful) {
                val imageBytes = response.body?.bytes()
                if (imageBytes != null) {
                    val newHash = calculateHash(imageBytes)
                    val oldHash = prefs.getString(hashKey, "")
                    
                    if (newHash != oldHash) {
                        // Изображение изменилось, сохраняем новое
                        val cacheFile = File(getCacheDir(), fileName)
                        FileOutputStream(cacheFile).use { it.write(imageBytes) }
                        
                        // Сохраняем новый хеш
                        prefs.edit().putString(hashKey, newHash).apply()
                        
                        Log.d(TAG, "$fileName обновлен (hash: $newHash)")
                        return true
                    } else {
                        Log.d(TAG, "$fileName не изменился")
                    }
                }
            } else {
                Log.w(TAG, "Не удалось загрузить $url: ${response.code}")
            }
            
            response.close()
            false
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка загрузки $url: ${e.message}")
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
     * Получает директорию кэша для изображений
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
     * Очищает кэш изображений
     */
    fun clearCache() {
        try {
            val cacheDir = getCacheDir()
            cacheDir.listFiles()?.forEach { it.delete() }
            prefs.edit().clear().apply()
            Log.d(TAG, "Кэш изображений очищен")
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка очистки кэша: ${e.message}")
        }
    }
    
    enum class ImageType {
        LOGO,
        BACKGROUND
    }
}