# 📱 APK Updates Directory

Эта папка предназначена для хранения APK файлов обновлений приложения Android TV.

## 📋 Структура файлов:
```
files/updates/
├── app-v1.0.apk    # Текущая версия
├── app-v1.1.apk    # Следующая версия 
├── app-v1.2.apk    # И так далее...
└── README.md       # Этот файл
```

## 🔧 Как загрузить новую версию:

1. **Через админ-панель:**
   - Откройте https://artemon4es.github.io/tv-channels-api/
   - Авторизуйтесь с GitHub токеном
   - Используйте секцию "📱 Управление APK"

2. **Вручную через GitHub:**
   - Перейдите в эту папку на GitHub
   - Нажмите "Add file" → "Upload files"
   - Загрузите APK с именем `app-v{версия}.apk`
   - Обновите `api/config.json` с новой версией

## ⚠️ Важно:
- Имя файла должно строго соответствовать: `app-v{версия}.apk`
- Обязательно обновите `config.json` после загрузки
- Проверьте что ссылка в `download_url` работает

## 🔗 Формат ссылки:
```
https://artemon4es.github.io/tv-channels-api/files/updates/app-v{версия}.apk
``` 