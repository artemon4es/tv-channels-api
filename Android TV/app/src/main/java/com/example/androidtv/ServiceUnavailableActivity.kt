package com.example.androidtv

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class ServiceUnavailableActivity : AppCompatActivity() {
    private lateinit var messageTextView: TextView
    private lateinit var detailsTextView: TextView
    private lateinit var refreshButton: Button
    private lateinit var exitButton: Button
    private lateinit var remoteConfigManager: RemoteConfigManager
    
    companion object {
        private const val TAG = "ServiceUnavailableActivity"
        private const val EXTRA_MESSAGE = "service_message"
        
        fun start(context: Context, message: String) {
            val intent = Intent(context, ServiceUnavailableActivity::class.java)
            intent.putExtra(EXTRA_MESSAGE, message)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            context.startActivity(intent)
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_service_unavailable)
        
        // Инициализация компонентов
        messageTextView = findViewById(R.id.messageTextView)
        detailsTextView = findViewById(R.id.detailsTextView)
        refreshButton = findViewById(R.id.refreshButton)
        exitButton = findViewById(R.id.exitButton)
        
        remoteConfigManager = RemoteConfigManager(this)
        
        // Показываем стандартное сообщение
        val adminMessage = intent.getStringExtra(EXTRA_MESSAGE)
        messageTextView.text = adminMessage ?: "Сервис временно недоступен.\nОбратитесь к администратору."
        
        // Настройка кнопок
        refreshButton.setOnClickListener {
            refreshButton.isEnabled = false
            refreshButton.text = "Проверка..."
            checkServiceStatus()
        }
        
        exitButton.setOnClickListener {
            finishAffinity() // Закрываем все активности приложения
        }
        
        // Устанавливаем фокус на кнопку обновления
        refreshButton.requestFocus()
        
        // Настройка обработки кнопки назад (современный подход)
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Отключаем кнопку назад - пользователь не может обойти блокировку
                // Вместо этого показываем диалог выхода
                exitButton.performClick()
            }
        })
    }
    
    private fun checkServiceStatus() {
        lifecycleScope.launch {
            try {
                Log.d(TAG, "Проверка статуса сервиса...")
                
                val remoteConfig = remoteConfigManager.checkRemoteConfig()
                
                if (remoteConfig != null) {
                    if (remoteConfig.serviceAvailable && !remoteConfig.maintenanceMode) {
                        // Сервис снова доступен - возвращаемся к главному экрану
                        Log.d(TAG, "Сервис снова доступен")
                        val intent = Intent(this@ServiceUnavailableActivity, MainActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                        finish()
                    } else {
                        // Сервис все еще недоступен - показываем стандартное сообщение
                        val message = if (remoteConfig.maintenanceMode) {
                            "Проводится техническое обслуживание. Попробуйте позже."
                        } else {
                            "Сервис временно недоступен.\nОбратитесь к администратору."
                        }
                        
                        messageTextView.text = message
                        Log.d(TAG, "Сервис все еще недоступен")
                    }
                } else {
                    // Ошибка подключения
                    Log.w(TAG, "Не удалось проверить статус сервиса")
                    detailsTextView.text = "Не удалось проверить статус сервиса.\nПроверьте подключение к интернету."
                    detailsTextView.visibility = android.view.View.VISIBLE
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка проверки статуса сервиса", e)
                detailsTextView.text = "Ошибка проверки: ${e.message}"
                detailsTextView.visibility = android.view.View.VISIBLE
            } finally {
                // Возвращаем состояние кнопки
                refreshButton.isEnabled = true
                refreshButton.text = "Обновить"
            }
        }
    }

} 