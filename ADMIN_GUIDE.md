# 🔧 РУКОВОДСТВО АДМИНИСТРАТОРА IPTV СЕРВИСА

## 📋 БЫСТРЫЕ КОМАНДЫ

### 1. ✅ ВКЛЮЧИТЬ СЕРВИС
```powershell
# Открыть PowerShell в папке проекта
cd "C:\Users\ghostLeto\Desktop\Android TV"

# Редактировать api/config.json
# Изменить: "service_available": true, "message": ""

# Закоммитить изменения
git add api/config.json
git commit -m "✅ Сервис включен"
git push
```

### 2. ❌ ОТКЛЮЧИТЬ СЕРВИС С СООБЩЕНИЕМ
```powershell
# Редактировать api/config.json
# Изменить: 
#   "service_available": false
#   "message": "Сервис временно не доступен.\nОбратитесь к администратору."

git add api/config.json
git commit -m "❌ Сервис отключен"
git push
```

### 3. 📺 ОБНОВИТЬ СПИСОК КАНАЛОВ

**Способ 1 - Загрузка нового файла:**
```powershell
# Скопировать новый файл каналов
Copy-Item "путь\к\новому\файлу.m3u8" "files\channels.m3u8"

# Обновить версию в api/config.json
# Изменить: "version": НОВОЕ_ЧИСЛО (например: 1625140800)
# Изменить: "last_updated": "2025-01-20T12:00:00Z"

git add files/channels.m3u8 api/config.json
git commit -m "📺 Обновлен список каналов"
git push
```

**Способ 2 - Редактирование через GitHub:**
1. Перейти: https://github.com/artemon4es/tv-channels-api
2. Открыть файл `files/channels.m3u8`
3. Нажать "Edit" (карандаш)
4. Внести изменения
5. Commit changes

### 4. 📱 ОБНОВИТЬ APK ПРИЛОЖЕНИЯ

```powershell
# 1. Скопировать новый APK в папку обновлений
New-Item -ItemType Directory -Path "files/updates" -Force
Copy-Item "путь\к\новому\app.apk" "files\updates\app-v1.2.apk"

# 2. Обновить конфигурацию в api/config.json
# Изменить:
#   "latest_version": "1.2"
#   "version_code": 12
#   "download_url": "https://artemon4es.github.io/tv-channels-api/files/updates/app-v1.2.apk"
#   "update_required": true (для принудительного обновления)
#   "changelog": "🔧 Исправлены ошибки\n✨ Новые функции"

git add files/updates/app-v1.2.apk api/config.json
git commit -m "📱 Новая версия APK v1.2"
git push
```

### 5. 🔒 ДОБАВИТЬ НОВЫЕ ДОМЕНЫ

```powershell
# Редактировать files/security_config.xml
# Добавить новые домены в секцию <domain-config>

git add files/security_config.xml
git commit -m "🔒 Добавлены новые домены"
git push
```

---

## 🚀 АВТОМАТИЗАЦИЯ ЧЕРЕЗ СКРИПТЫ

### Скрипт быстрого отключения сервиса:
```powershell
# disable_service.ps1
$config = Get-Content "api/config.json" | ConvertFrom-Json
$config.service_config.service_available = $false
$config.service_config.message = "Сервис временно не доступен.\nОбратитесь к администратору."
$config | ConvertTo-Json -Depth 10 | Set-Content "api/config.json" -Encoding UTF8

git add api/config.json
git commit -m "❌ Сервис отключен экстренно"
git push

Write-Host "✅ Сервис отключен!" -ForegroundColor Red
```

### Скрипт быстрого включения:
```powershell
# enable_service.ps1
$config = Get-Content "api/config.json" | ConvertFrom-Json
$config.service_config.service_available = $true
$config.service_config.message = ""
$config | ConvertTo-Json -Depth 10 | Set-Content "api/config.json" -Encoding UTF8

git add api/config.json
git commit -m "✅ Сервис включен"
git push

Write-Host "✅ Сервис включен!" -ForegroundColor Green
```

---

## 📊 МОНИТОРИНГ

### Проверка статуса:
1. **Веб-интерфейс**: https://artemon4es.github.io/tv-channels-api/
2. **Прямой API**: https://artemon4es.github.io/tv-channels-api/api/config.json

### Лог изменений через Git:
```powershell
git log --oneline -10  # Последние 10 коммитов
```

---

## ⚡ ЭКСТРЕННЫЕ ДЕЙСТВИЯ

### 🚨 ЭКСТРЕННОЕ ОТКЛЮЧЕНИЕ (1 минута):
```powershell
# Быстрое отключение через GitHub веб-интерфейс:
# 1. https://github.com/artemon4es/tv-channels-api/edit/main/api/config.json
# 2. Изменить "service_available": false
# 3. Commit changes
```

### 🔧 ЭКСТРЕННОЕ ВКЛЮЧЕНИЕ:
```powershell
# Аналогично, но "service_available": true
```

---

## 📱 КАК ПОЛЬЗОВАТЕЛИ ПОЛУЧАЮТ ОБНОВЛЕНИЯ

### Обновление каналов:
- ✅ Автоматически каждые 30 минут
- ✅ При запуске приложения
- ✅ По изменению `channels_config.version`

### Обновление APK:
- ✅ Проверка при запуске приложения
- ✅ Уведомление пользователю о новой версии
- ✅ Принудительное обновление если `update_required: true`
- ✅ Автоматическая загрузка и установка

### Отключение сервиса:
- ✅ Сообщение на весь экран при запуске
- ✅ Блокировка доступа к каналам
- ✅ Показ сообщения из `service_config.message` 