# 🔐 Безопасная архитектура для TV Channels API

## ⚠️ КРИТИЧЕСКАЯ ОШИБКА - ИСПРАВЛЕНА

**НИКОГДА НЕ РАЗМЕЩАЙТЕ СЕКРЕТНЫЕ ТОКЕНЫ В ПУБЛИЧНЫХ РЕПОЗИТОРИЯХ!**

## 🏗️ Правильная архитектура безопасности

### **Вариант 1: Двухрепозиторная архитектура (Рекомендуется)**

#### **Репозиторий 1: Публичный (tv-channels-api)**
- 📁 `index.html` - веб-интерфейс
- 📁 `files/channels.m3u8` - список каналов
- 📁 `files/security_config.xml` - конфигурация безопасности
- 📁 `api/config.json` - **БЕЗ ТОКЕНА**

#### **Репозиторий 2: Приватный (tv-channels-admin)**
- 📁 `config/admin.json` - конфигурация с токеном
- 📁 `scripts/update.js` - скрипты управления
- 📁 `auth/tokens.json` - токены доступа

### **Вариант 2: GitHub Secrets + Actions**

#### **Настройка GitHub Secrets:**
1. **Репозиторий:** Settings → Secrets and variables → Actions
2. **Секреты:**
   - `ADMIN_TOKEN` - токен для администрирования
   - `API_KEY` - ключ API для приложения
   - `WEBHOOK_SECRET` - секрет для веб-хуков

#### **GitHub Actions для автоматизации:**
```yaml
name: Update Configuration
on:
  workflow_dispatch:
    inputs:
      service_available:
        description: 'Service Available'
        required: true
        default: 'true'
        type: boolean

jobs:
  update-config:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v2
      - name: Update Config
        run: |
          echo "Service: ${{ github.event.inputs.service_available }}"
          # Обновляем конфигурацию без токена
        env:
          ADMIN_TOKEN: ${{ secrets.ADMIN_TOKEN }}
```

### **Вариант 3: Переменные окружения в приложении**

#### **Android приложение:**
```kotlin
class RemoteConfigManager(private val context: Context) {
    
    // Токен устанавливается через безопасные каналы
    fun setAccessToken(token: String) {
        val prefs = context.getSharedPreferences("secure_config", Context.MODE_PRIVATE)
        prefs.edit().putString("access_token", token).apply()
    }
    
    // Проверка авторизации без публичного хранения
    private suspend fun verifyAuth(): Boolean {
        val token = getStoredToken()
        // Проверяем токен с GitHub API
        return checkTokenWithGitHub(token)
    }
}
```

## 🛡️ Безопасная публичная конфигурация

### **api/config.json (БЕЗ ТОКЕНА):**
```json
{
  "app_info": {
    "current_version": "1.0",
    "latest_version": "1.1",
    "version_code": 2,
    "download_url": "https://artemon4es.github.io/tv-channels-api/files/updates/app-v1.1.apk",
    "update_required": false,
    "changelog": "Обновления системы"
  },
  "service_config": {
    "service_available": true,
    "message": "",
    "maintenance_mode": false,
    "auth_required": true,
    "auth_endpoint": "https://api.github.com/user"
  },
  "channels_config": {
    "version": 5,
    "last_updated": "2025-07-04T12:00:00Z",
    "url": "https://artemon4es.github.io/tv-channels-api/files/channels.m3u8",
    "security_config_url": "https://artemon4es.github.io/tv-channels-api/files/security_config.xml"
  }
}
```

## 🔧 Методы авторизации

### **1. Авторизация через приложение**
- Пользователь вводит токен в приложении
- Токен сохраняется в защищенном хранилище
- Приложение проверяет токен с GitHub API

### **2. Авторизация через QR-код**
- Генерируется временный QR-код с токеном
- Пользователь сканирует QR-код в приложении
- Токен автоматически сохраняется

### **3. Авторизация через веб-интерфейс**
- Создается безопасный веб-интерфейс для управления
- Администратор авторизуется через GitHub OAuth
- Изменения применяются через GitHub API

## 📱 Реализация в Android приложении

### **Безопасное хранение токена:**
```kotlin
// Установка токена (один раз)
remoteConfigManager.setAccessToken("ghp_your_token_here")

// Проверка авторизации
if (remoteConfigManager.verifyAuth()) {
    // Выполнить административные действия
}
```

### **Управление через интерфейс:**
```kotlin
// В MainActivity добавить метод для ввода токена
private fun showTokenInputDialog() {
    val builder = AlertDialog.Builder(this)
    val input = EditText(this)
    input.inputType = InputType.TYPE_TEXT_VARIATION_PASSWORD
    
    builder.setTitle("Введите токен администратора")
    builder.setView(input)
    
    builder.setPositiveButton("OK") { _, _ ->
        val token = input.text.toString()
        remoteConfigManager.setAccessToken(token)
    }
    
    builder.show()
}
```

## 🎯 Рекомендуемое решение

### **Для производственного использования:**
1. **Создайте приватный репозиторий** для административной конфигурации
2. **Используйте GitHub Secrets** для хранения токенов
3. **Настройте GitHub Actions** для автоматизации
4. **Реализуйте безопасное хранение** токенов в приложении

### **Для разработки:**
1. Используйте переменные окружения
2. Создайте отдельный файл `.env` (добавьте в .gitignore)
3. Тестируйте с временными токенами

## 🚨 Что делать СЕЙЧАС:

1. **НЕМЕДЛЕННО удалить токен** из публичных файлов
2. **Обновить конфигурацию** без токена
3. **Реализовать безопасное хранение** в приложении
4. **Создать приватный репозиторий** для административных задач

**Безопасность превыше всего!** 🔒 