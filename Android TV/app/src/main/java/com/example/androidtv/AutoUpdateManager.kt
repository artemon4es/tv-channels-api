package com.example.androidtv

import android.app.AlertDialog
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File

class AutoUpdateManager(private val context: Context) {
    
    companion object {
        private const val TAG = "AutoUpdateManager"
        private const val APK_FILENAME = "tv-channels-update.apk"
    }
    
    private var downloadId: Long = -1
    private var downloadReceiver: BroadcastReceiver? = null
    
    data class UpdateInfo(
        val currentVersion: String,
        val latestVersion: String,
        val versionCode: Int,
        val downloadUrl: String,
        val updateRequired: Boolean,
        val changelog: String
    )
    
    /**
     * Проверяет наличие обновлений и показывает диалог если необходимо
     */
    fun checkForUpdates(remoteConfig: RemoteConfigManager.RemoteConfig, showNoUpdatesDialog: Boolean = false) {
        val currentVersion = getCurrentAppVersion()
        val currentVersionCode = getCurrentAppVersionCode()
        
        val updateInfo = UpdateInfo(
            currentVersion = currentVersion,
            latestVersion = remoteConfig.appLatestVersion,
            versionCode = remoteConfig.appVersionCode,
            downloadUrl = remoteConfig.downloadUrl,
            updateRequired = remoteConfig.updateRequired,
            changelog = remoteConfig.changelog
        )
        
        Log.d(TAG, "Текущая версия: $currentVersion ($currentVersionCode)")
        Log.d(TAG, "Доступная версия: ${updateInfo.latestVersion} (${updateInfo.versionCode})")
        
        when {
            updateInfo.versionCode > currentVersionCode && updateInfo.downloadUrl.isNotEmpty() -> {
                showUpdateDialog(updateInfo)
            }
            showNoUpdatesDialog -> {
                showNoUpdatesDialog()
            }
        }
    }
    
    /**
     * Получает текущую версию приложения
     */
    private fun getCurrentAppVersion(): String {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName ?: "1.0"
        } catch (e: PackageManager.NameNotFoundException) {
            Log.e(TAG, "Не удалось получить версию приложения: ${e.message}")
            "1.0"
        }
    }
    
    /**
     * Получает текущий код версии приложения
     */
    private fun getCurrentAppVersionCode(): Int {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.longVersionCode.toInt()
            } else {
                @Suppress("DEPRECATION")
                packageInfo.versionCode
            }
        } catch (e: PackageManager.NameNotFoundException) {
            Log.e(TAG, "Не удалось получить код версии приложения: ${e.message}")
            1
        }
    }
    
    /**
     * Показывает диалог обновления
     */
    private fun showUpdateDialog(updateInfo: UpdateInfo) {
        val title = if (updateInfo.updateRequired) {
            "⚠️ Требуется обновление"
        } else {
            "🚀 Доступно обновление"
        }
        
        val message = """
            Новая версия: ${updateInfo.latestVersion}
            Текущая: ${updateInfo.currentVersion}
            
            📝 Изменения:
            ${updateInfo.changelog}
            
            Обновить автоматически?
        """.trimIndent()
        
        try {
            AlertDialog.Builder(context)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("✅ Обновить") { _, _ ->
                    downloadAndInstallUpdate(updateInfo.downloadUrl)
                }
                .setNegativeButton(if (updateInfo.updateRequired) "❌ Закрыть приложение" else "⏰ Позже") { _, _ ->
                    if (updateInfo.updateRequired) {
                        (context as? MainActivity)?.finish()
                    }
                }
                .setCancelable(!updateInfo.updateRequired)
                .show()
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка показа диалога обновления: ${e.message}")
        }
    }
    
    /**
     * Показывает диалог "нет обновлений"
     */
    private fun showNoUpdatesDialog() {
        try {
            AlertDialog.Builder(context)
                .setTitle("✅ Актуальная версия")
                .setMessage("У вас установлена последняя версия приложения")
                .setPositiveButton("OK", null)
                .show()
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка показа диалога: ${e.message}")
        }
    }
    
    /**
     * Загружает и устанавливает обновление
     */
    private fun downloadAndInstallUpdate(downloadUrl: String) {
        // Проверяем разрешение на установку
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!context.packageManager.canRequestPackageInstalls()) {
                requestInstallPermission()
                return
            }
        }
        
        showDownloadDialog()
        startDownload(downloadUrl)
    }
    
    /**
     * Запрашивает разрешение на установку
     */
    private fun requestInstallPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES)
                    .setData(Uri.parse("package:${context.packageName}"))
                (context as MainActivity).startActivity(intent)
                
                Toast.makeText(context, 
                    "Разрешите установку приложений из неизвестных источников для автообновления", 
                    Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка запроса разрешения на установку: ${e.message}")
            }
        }
    }
    
    /**
     * Показывает диалог загрузки
     */
    private fun showDownloadDialog() {
        try {
            AlertDialog.Builder(context)
                .setTitle("📥 Загрузка обновления")
                .setMessage("Идет загрузка новой версии приложения...\nПо завершении будет предложена автоматическая установка.")
                .setPositiveButton("OK", null)
                .show()
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка показа диалога загрузки: ${e.message}")
        }
    }
    
    /**
     * Начинает загрузку APK файла
     */
    private fun startDownload(downloadUrl: String) {
        try {
            val request = DownloadManager.Request(Uri.parse(downloadUrl))
                .setTitle("Обновление TV Channels")
                .setDescription("Загрузка новой версии...")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, APK_FILENAME)
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(true)
            
            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            downloadId = downloadManager.enqueue(request)
            
            // Регистрируем получатель для отслеживания завершения загрузки
            downloadReceiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    val id = intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                    if (id == downloadId) {
                        context?.unregisterReceiver(this)
                        installUpdate()
                    }
                }
            }
            
            context.registerReceiver(downloadReceiver, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
            
            Log.d(TAG, "Загрузка обновления начата: $downloadUrl")
            
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка запуска загрузки: ${e.message}")
            Toast.makeText(context, "Ошибка загрузки: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
    
    /**
     * Устанавливает загруженное обновление
     */
    private fun installUpdate() {
        try {
            val apkFile = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), APK_FILENAME)
            
            if (apkFile.exists()) {
                val apkUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        apkFile
                    )
                } else {
                    Uri.fromFile(apkFile)
                }
                
                val installIntent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(apkUri, "application/vnd.android.package-archive")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
                }
                
                context.startActivity(installIntent)
                
                Toast.makeText(context, 
                    "Нажмите 'Установить' в открывшемся окне для завершения обновления", 
                    Toast.LENGTH_LONG).show()
                
                Log.d(TAG, "Установка обновления запущена")
                    
            } else {
                Log.e(TAG, "APK файл не найден: ${apkFile.absolutePath}")
                Toast.makeText(context, "Ошибка: файл обновления не найден", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка установки: ${e.message}")
            Toast.makeText(context, "Ошибка установки: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
    
    /**
     * Очищает ресурсы
     */
    fun cleanup() {
        downloadReceiver?.let { receiver ->
            try {
                context.unregisterReceiver(receiver)
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка отмены регистрации получателя: ${e.message}")
            }
        }
    }
} 