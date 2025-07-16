# 🚀 Обновление APK через GitHub Pages

## 📋 Текущая ситуация

✅ **Приложения установлены** на 1000+ устройствах с версией 1.0  
✅ **GitHub Pages API** настроен: `https://artemon4es.github.io/tv-channels-api/`  
✅ **Автоматическая проверка** каждые 5 минут  
✅ **RemoteConfigManager** проверяет обновления  

## 🎯 ПОШАГОВАЯ ИНСТРУКЦИЯ ОБНОВЛЕНИЯ

### ШАГ 1: Подготовка нового APK

**1.1. Внесите изменения в код**
```kotlin
// Например, в MainActivity.kt добавьте:
// 🆕 НОВАЯ ВЕРСИЯ 1.1 - Улучшения интерфейса
findViewById<View>(android.R.id.content).setBackgroundColor(
    android.graphics.Color.parseColor("#2563EB") // Новый синий цвет
)
```

**1.2. Обновите версию в build.gradle**
```gradle
android {
    defaultConfig {
        versionCode 2        // Было: 1
        versionName "1.1"    // Было: "1.0"
    }
}
```

**1.3. Соберите Release APK**
```bash
# В Android Studio:
Build → Generate Signed Bundle / APK → APK → Release
```

**1.4. Найдите готовый APK**
```
Android TV/app/build/outputs/apk/release/app-release.apk
```

---

### ШАГ 2: Загрузка APK в GitHub

**2.1. Переименуйте APK файл**
```bash
# Скопируйте и переименуйте:
cp app-release.apk tv-channels-v1.1.apk
```

**2.2. Загрузите в GitHub репозиторий**
```bash
# Скопируйте файл в:
files/updates/tv-channels-v1.1.apk
```

**Структура должна быть:**
```
tv-channels-api/
├── api/
│   └── config.json
├── files/
│   ├── updates/
│   │   ├── tv-channels-v1.0.apk    # Старая версия
│   │   └── tv-channels-v1.1.apk    # 🆕 НОВАЯ ВЕРСИЯ
│   ├── channels.m3u8
│   └── security_config.xml
└── index.html
```

---

### ШАГ 3: Обновление конфигурации API

**3.1. Отредактируйте api/config.json**

**БЫЛО:**
```json
{
  "app_info": {
    "current_version": "1.0",
    "latest_version": "1.0",
    "version_code": 1,
    "download_url": "",
    "update_required": false,
    "changelog": ""
  }
}
```

**СТАЛО:**
```json
{
  "app_info": {
    "current_version": "1.0",
    "latest_version": "1.1",
    "version_code": 2,
    "download_url": "https://artemon4es.github.io/tv-channels-api/files/updates/tv-channels-v1.1.apk",
    "update_required": false,
    "changelog": "🆕 Новая версия 1.1\n🎨 Улучшен дизайн интерфейса\n⚡ Повышена производительность\n🐛 Исправлены ошибки"
  }
}
```

**3.2. Добавьте дату обновления**
```json
{
  "app_info": {
    "latest_version": "1.1",
    "version_code": 2,
    "download_url": "https://artemon4es.github.io/tv-channels-api/files/updates/tv-channels-v1.1.apk",
    "update_required": false,
    "changelog": "🆕 Новая версия 1.1\n🎨 Улучшен дизайн интерфейса\n⚡ Повышена производительность\n🐛 Исправлены ошибки",
    "release_date": "2025-07-16T10:00:00Z",
    "file_size_mb": 18.5
  }
}
```

---

### ШАГ 4: Загрузка изменений в GitHub

**4.1. Добавьте файлы в Git**
```bash
git add files/updates/tv-channels-v1.1.apk
git add api/config.json
```

**4.2. Создайте коммит**
```bash
git commit -m "🚀 Release v1.1: UI improvements and bug fixes

- Added new APK v1.1 (18.5MB)
- Updated API config with new version
- Enhanced user interface design
- Performance optimizations
- Bug fixes and stability improvements"
```

**4.3. Загрузите на GitHub**
```bash
git push origin main
```

---

### ШАГ 5: Проверка развертывания

**5.1. Проверьте GitHub Pages (через 1-2 минуты)**
```bash
# Откройте в браузере:
https://artemon4es.github.io/tv-channels-api/api/config.json
```

**Должно показать:**
```json
{
  "app_info": {
    "latest_version": "1.1",
    "version_code": 2,
    "download_url": "https://artemon4es.github.io/tv-channels-api/files/updates/tv-channels-v1.1.apk"
  }
}
```

**5.2. Проверьте доступность APK**
```bash
# Откройте в браузере:
https://artemon4es.github.io/tv-channels-api/files/updates/tv-channels-v1.1.apk
```

**Должно начаться скачивание APK файла**

---

## 📱 Что происходит на устройствах

### **Автоматический процесс (каждые 5 минут):**

1. **Приложение** делает запрос к `config.json`
2. **Сравнивает версии:**
   - Установленная: `versionCode = 1`
   - Доступная: `version_code = 2`
3. **Показывает диалог обновления:**
```
🚀 Доступно обновление!

Текущая версия: 1.0
Новая версия: 1.1

📝 Что нового:
🆕 Новая версия 1.1
🎨 Улучшен дизайн интерфейса
⚡ Повышена производительность
🐛 Исправлены ошибки

[✅ Обновить] [⏰ Позже]
```

4. **При нажатии "Обновить":**
   - Скачивается APK с GitHub Pages
   - Устанавливается автоматически
   - Приложение перезапускается

---

## 🎛️ Управление обновлениями

### **Принудительное обновление**
```json
{
  "app_info": {
    "update_required": true
  }
}
```
**Результат:** Приложения не смогут работать без обновления

### **Поэтапное развертывание**
```json
{
  "app_info": {
    "rollout_percentage": 50
  }
}
```
**Результат:** Обновление получат только 50% устройств

### **Откат к предыдущей версии**
```json
{
  "app_info": {
    "latest_version": "1.0",
    "version_code": 1,
    "download_url": "https://artemon4es.github.io/tv-channels-api/files/updates/tv-channels-v1.0.apk"
  }
}
```

---

## 📊 Мониторинг обновлений

### **Проверка статуса API**
```bash
curl https://artemon4es.github.io/tv-channels-api/api/config.json
```

### **Проверка размера APK**
```bash
curl -I https://artemon4es.github.io/tv-channels-api/files/updates/tv-channels-v1.1.apk
# Смотрите Content-Length
```

### **Логи на устройствах**
```bash
adb logcat | grep -E "(RemoteConfigManager|AutoUpdateManager)"
```

**Успешное обновление:**
```
D RemoteConfigManager: Получена конфигурация: {"latest_version":"1.1","version_code":2}
D AutoUpdateManager: Текущая версия: 1.0 (1)
D AutoUpdateManager: Доступная версия: 1.1 (2)
I AutoUpdateManager: Загрузка обновления начата: https://artemon4es.github.io/.../tv-channels-v1.1.apk
D AutoUpdateManager: Установка обновления запущена
```

---

## 🚨 Важные правила

### **Именование APK файлов:**
```
✅ tv-channels-v1.0.apk
✅ tv-channels-v1.1.apk  
✅ tv-channels-v1.2.apk
❌ app-release.apk
❌ update.apk
❌ tv-channels.apk
```

### **Версионирование:**
```
✅ versionCode всегда увеличивается: 1 → 2 → 3
✅ versionName семантическое: 1.0 → 1.1 → 1.2
❌ Никогда не уменьшайте versionCode
```

### **URL структура:**
```
✅ https://username.github.io/tv-channels-api/files/updates/tv-channels-v1.1.apk
❌ https://github.com/username/tv-channels-api/files/updates/tv-channels-v1.1.apk
```

---

## 🎯 Пример полного цикла обновления

### **Релиз версии 1.1:**

**1. Код и сборка:**
```bash
# Изменили код
# versionCode 1 → 2
# versionName "1.0" → "1.1"
./gradlew assembleRelease
```

**2. Загрузка файлов:**
```bash
cp app-release.apk files/updates/tv-channels-v1.1.apk
```

**3. Обновление API:**
```json
{
  "app_info": {
    "latest_version": "1.1",
    "version_code": 2,
    "download_url": "https://artemon4es.github.io/tv-channels-api/files/updates/tv-channels-v1.1.apk"
  }
}
```

**4. Развертывание:**
```bash
git add .
git commit -m "🚀 Release v1.1"
git push origin main
```

**5. Результат через 5 минут:**
- Все устройства получают уведомление об обновлении
- Пользователи могут обновиться одним нажатием
- Новая версия устанавливается автоматически

---

## 💡 Советы по развертыванию

### **Тестирование перед релизом:**
1. Протестируйте APK локально
2. Проверьте размер файла (не более 50 МБ)
3. Убедитесь что versionCode увеличился
4. Проверьте changelog на опечатки

### **Безопасное развертывание:**
1. Сначала загрузите APK
2. Потом обновите config.json
3. Мониторьте логи устройств
4. Будьте готовы к откату

### **Экстренный откат:**
```bash
# Быстро вернуть предыдущую версию
git revert HEAD
git push origin main
```

## 🎉 Результат

После выполнения всех шагов:
- ✅ Новый APK доступен по URL
- ✅ API конфигурация обновлена  
- ✅ Все устройства получат уведомление
- ✅ Обновление происходит автоматически
- ✅ Пользователи видят новые функции

**Время развертывания: 1-2 минуты после git push** 🚀