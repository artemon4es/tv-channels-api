package com.example.androidtv

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityOptionsCompat
import androidx.core.app.ActivityCompat
import kotlinx.coroutines.*

class SplashActivity : AppCompatActivity() {

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private lateinit var splashImageManager: SplashImageManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        
        // Инициализируем менеджер изображений заставки
        splashImageManager = SplashImageManager(this)

        // Загружаем динамические изображения
        loadDynamicImages()

        // Проверяем подписку перед запуском приложения
        checkSubscriptionStatus()
    }
    
    /**
     * Загружает динамические изображения заставки
     */
    private fun loadDynamicImages() {
        scope.launch {
            try {
                // Проверяем и загружаем обновления изображений
                val hasUpdates = splashImageManager.checkAndUpdateSplashImages()
                if (hasUpdates) {
                    android.util.Log.d("SplashActivity", "Изображения заставки обновлены")
                }
                
                // Загружаем изображения в UI
                val logo = findViewById<ImageView>(R.id.splashLogo)
                val text = findViewById<TextView>(R.id.splashText)
                
                // Загружаем логотип
                splashImageManager.loadImageIntoView(logo, SplashImageManager.ImageType.LOGO)
                
                // Устанавливаем фон
                splashImageManager.setActivityBackground(this@SplashActivity)
                
                // Применяем анимации после загрузки изображений
                val fadeIn = AnimationUtils.loadAnimation(this@SplashActivity, R.anim.fade_in)
                logo.startAnimation(fadeIn)
                text.startAnimation(fadeIn)
                
            } catch (e: Exception) {
                android.util.Log.e("SplashActivity", "Ошибка загрузки изображений: ${e.message}")
                
                // В случае ошибки применяем анимации к стандартным изображениям
                val fadeIn = AnimationUtils.loadAnimation(this@SplashActivity, R.anim.fade_in)
                val logo = findViewById<ImageView>(R.id.splashLogo)
                val text = findViewById<TextView>(R.id.splashText)
                
                logo.startAnimation(fadeIn)
                text.startAnimation(fadeIn)
            }
        }
    }

    private fun checkSubscriptionStatus() {
        scope.launch {
            val subscriptionManager = SubscriptionManager(this@SplashActivity)
            
            // Используем реальную проверку через GitHub API
            val result = try {
                subscriptionManager.checkSubscriptionStatus() // Реальная проверка через GitHub API
            } catch (e: Exception) {
                SubscriptionResult.Error("Ошибка проверки: ${e.message}")
            }
            
            when (result) {
                is SubscriptionResult.Active -> {
                    // Подписка активна - запускаем приложение
                    launchMainActivity()
                }
                is SubscriptionResult.Blocked -> {
                    // Подписка заблокирована - показываем сообщение
                    showSubscriptionError(result.reason)
                }
                is SubscriptionResult.Error -> {
                    // Ошибка проверки - даем доступ (offline режим)
                    launchMainActivity()
                }
            }
        }
    }

    private fun launchMainActivity() {
        Handler(Looper.getMainLooper()).postDelayed({
            val intent = Intent(this, MainActivity::class.java)
            val options = ActivityOptionsCompat.makeCustomAnimation(this, R.anim.fade_in, R.anim.fade_out)
            ActivityCompat.startActivity(this, intent, options.toBundle())
            finish()
        }, 2000)
    }

    private fun showSubscriptionError(reason: String = "Произведите оплату для продления услуг трансляции каналов.") {
        AlertDialog.Builder(this)
            .setTitle("Доступ ограничен")
            .setMessage("$reason\n\nОбратитесь к администратору для активации подписки.")
            .setPositiveButton("Закрыть") { _, _ -> finish() }
            .setCancelable(false)
            .show()
    }

    private fun showConnectionError(errorMessage: String = "Не удалось проверить статус подписки. Проверьте подключение к интернету.") {
        AlertDialog.Builder(this)
            .setTitle("Ошибка подключения")
            .setMessage(errorMessage)
            .setPositiveButton("Повторить") { _, _ -> checkSubscriptionStatus() }
            .setNegativeButton("Закрыть") { _, _ -> finish() }
            .setCancelable(false)
            .show()
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }
} 