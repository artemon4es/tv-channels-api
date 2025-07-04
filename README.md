# 📺 TV Channels - Android TV IPTV Приложение

Приложение для просмотра IPTV каналов на Android TV с системой удаленного управления через GitHub Pages.

## 🚀 Возможности

- ✅ **Удаленное управление** - включение/отключение сервиса через GitHub Pages
- ✅ **Автообновление приложения** - загрузка и установка обновлений внутри приложения  
- ✅ **Горячее обновление каналов** - изменение списка каналов без перезапуска
- ✅ **Офлайн режим** - работа с кэшированными данными при недоступности сервера
- ✅ **Удаленная конфигурация безопасности** - обновление network security config

## 🏗️ Архитектура

```
[Android TV App] ←→ [GitHub Pages API] ←→ [GitHub Repository]
```

## 📋 Инструкция по развертыванию

### **Шаг 1: Создание GitHub Pages API**

1. **Создайте новый репозиторий на GitHub:**
   ```
   Имя: tv-channels-api
   Публичный: ✅ Да
   ```

2. **Клонируйте репозиторий:**
   ```bash
   git clone https://github.com/ваш-username/tv-channels-api.git
   cd tv-channels-api
   ```

3. **Создайте структуру файлов:**
   ```bash
   mkdir -p api files/updates
   ```

4. **Скопируйте файлы из этого проекта:**
   - `api/config.json` → `tv-channels-api/api/config.json`
   - `files/channels.m3u8` → `tv-channels-api/files/channels.m3u8`
   - `files/security_config.xml` → `tv-channels-api/files/security_config.xml`
   - `index.html` → `tv-channels-api/index.html`

5. **Отредактируйте `api/config.json`:**
   ```json
   {
     "app_info": {
       "download_url": "https://ваш-username.github.io/tv-channels-api/files/updates/app-v1.1.apk"
     }
   }
   ```
   
6. **Загрузите файлы:**
   ```bash
   git add .
   git commit -m "Initial API setup"
   git push origin main
   ```

7. **Активируйте GitHub Pages:**
   - Зайдите в Settings репозитория
   - Найдите секцию "Pages"
   - Source: "Deploy from a branch"  
   - Branch: "main"
   - Нажмите "Save"

### **Шаг 2: Настройка Android приложения**

1. **Откройте файл:** `Android TV/app/src/main/java/com/example/androidtv/RemoteConfigManager.kt`

2. **Замените URL на ваш:**
   ```kotlin
   private const val BASE_URL = "https://ваш-username.github.io/tv-channels-api"
   ```

3. **Соберите APK:**
   ```bash
   cd "Android TV"
   ./gradlew assembleRelease
   ```

4. **Загрузите APK в GitHub Pages:**
   ```bash
   cp app/build/outputs/apk/release/app-release.apk ../tv-channels-api/files/updates/app-v1.1.apk
   cd ../tv-channels-api
   git add files/updates/app-v1.1.apk
   git commit -m "Add initial APK"
   git push origin main
   ```

### **Шаг 3: Проверка работоспособности**

1. **Проверьте API:**
   - Откройте: `https://ваш-username.github.io/tv-channels-api`
   - Должна открыться страница с статусом API

2. **Проверьте endpoints:**
   - Config: `https://ваш-username.github.io/tv-channels-api/api/config.json`
   - Channels: `https://ваш-username.github.io/tv-channels-api/files/channels.m3u8`

3. **Установите приложение на Android TV**

## 🎛️ Управление через API

### **Отключение сервиса:**
Отредактируйте `api/config.json`:
```json
{
  "service_config": {
    "service_available": false,
    "message": "Техническое обслуживание до 18:00"
  }
}
```

### **Обновление каналов:**
1. Отредактируйте `files/channels.m3u8`
2. Увеличьте версию в `api/config.json`:
   ```json
   {
     "channels_config": {
       "version": 6
     }
   }
   ```

### **Загрузка нового APK:**
1. Загрузите новый APK в `files/updates/`
2. Обновите `api/config.json`:
   ```json
   {
     "app_info": {
       "latest_version": "1.2",
       "version_code": 3,
       "download_url": "https://ваш-username.github.io/tv-channels-api/files/updates/app-v1.2.apk",
       "changelog": "🔧 Исправления\n✨ Новые функции"
     }
   }
   ```

## 📊 Мониторинг

- **API статус:** `https://ваш-username.github.io/tv-channels-api`
- **Логи Android:** `adb logcat | grep "TV Channels"`

## 🔧 Расширенные настройки

### **Принудительное обновление:**
```json
{
  "app_info": {
    "update_required": true
  }
}
```

### **Режим обслуживания:**
```json
{
  "service_config": {
    "maintenance_mode": true
  }
}
```

## 🎯 Распространение на 1000+ ТВ

### **Метод 1: USB флешки**
1. Создайте APK с предустановленным API URL
2. Запишите на USB флешки  
3. Раздайте установщикам

### **Метод 2: QR-коды**
1. Создайте QR-код с ссылкой на APK
2. Распечатайте инструкции
3. Пользователи сканируют и устанавливают

### **Метод 3: Telegram Bot**
```python
# Пример простого бота для рассылки APK
import telebot

bot = telebot.TeleBot("YOUR_BOT_TOKEN")

@bot.message_handler(commands=['get_app'])
def send_app(message):
    bot.send_document(message.chat.id, 
        'https://ваш-username.github.io/tv-channels-api/files/updates/app-v1.1.apk')
```

## 💰 Стоимость

- **GitHub Pages:** $0/месяц
- **Трафик:** Неограниченный  
- **Хранилище:** 1 ГБ бесплатно

Для 1000+ устройств: **$0/месяц** 🎉

## 🛠️ Разработка

### **Структура проекта:**
```
Android TV/
├── app/src/main/java/com/example/androidtv/
│   ├── MainActivity.kt                 # Главная активность
│   ├── RemoteConfigManager.kt          # Работа с API
│   ├── AutoUpdateManager.kt            # Автообновление
│   ├── Channel.kt                      # Модель канала
│   └── ChannelList.kt                  # Список каналов
api/
├── config.json                         # Основная конфигурация
files/
├── channels.m3u8                       # Список каналов
├── security_config.xml                 # Конфигурация безопасности
└── updates/                            # APK файлы
```

### **Сборка APK:**
```bash
# Debug
./gradlew assembleDebug

# Release
./gradlew assembleRelease

# Подписанный APK
./gradlew assembleRelease -PsignConfig=release
```

## 📞 Поддержка

При возникновении проблем:
1. Проверьте логи: `adb logcat | grep "MainActivity\|RemoteConfigManager"`
2. Проверьте доступность API: `curl https://ваш-username.github.io/tv-channels-api/api/config.json`
3. Убедитесь, что GitHub Pages активированы

## 📄 Лицензия

MIT License - используйте свободно для коммерческих и некоммерческих проектов. 