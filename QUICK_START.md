# 🚀 БЫСТРЫЙ СТАРТ

## После настройки GitHub (уже выполнено)

### 🔧 **ЕЖЕДНЕВНЫЕ КОМАНДЫ**

```powershell
# Загрузить все изменения на GitHub
./upload_to_github.ps1

# Проверить работу API
./test_api_simple.ps1

# Быстро отключить сервис
./disable_service.ps1

# Быстро включить сервис
./enable_service.ps1
```

---

### 📋 **УПРАВЛЕНИЕ ЧЕРЕЗ ВЕБ-ИНТЕРФЕЙС**

**Ваша панель управления:**
🌐 https://artemon4es.github.io/tv-channels-api/

**Что можно делать:**
- ✅ Включать/отключать сервис
- 📺 Управлять каналами
- 📱 Обновлять APK приложения
- 🔧 Настраивать безопасность

---

### 📝 **АЛГОРИТМ РАБОТЫ**

1. **Внесите изменения** в файлы проекта
2. **Выполните** `./upload_to_github.ps1`
3. **Подождите** 1-2 минуты
4. **Проверьте** изменения в браузере

---

### 🔗 **ПОЛЕЗНЫЕ ССЫЛКИ**

- **GitHub репозиторий**: https://github.com/artemon4es/tv-channels-api
- **Веб-панель**: https://artemon4es.github.io/tv-channels-api/
- **Настройки GitHub Pages**: https://github.com/artemon4es/tv-channels-api/settings/pages

---

### 🎯 **ПРОВЕРКА РАБОТЫ**

Все должно работать:
- [ ] `./upload_to_github.ps1` - загружает файлы
- [ ] `./test_api_simple.ps1` - показывает ✅ тесты
- [ ] Веб-панель открывается в браузере
- [ ] API отвечает с данными

**🎉 ВСЕ ГОТОВО ДЛЯ РАБОТЫ!**

## 🚀 Быстрый старт TV Channels + GitHub Pages API

## 📋 Что вы получите

✅ **Полностью автоматизированная система:**
- Удаленное включение/отключение сервиса
- Автообновление приложения внутри Android TV
- Горячее обновление списка каналов
- Работа с 1000+ устройств **БЕСПЛАТНО**

## ⚡ Быстрое развертывание (5 минут)

### **Шаг 1: Подготовка GitHub**
```bash
# Замените YOUR_USERNAME на ваш GitHub username
export GITHUB_USERNAME=YOUR_USERNAME

# Запустите автоматическое развертывание
./deploy.sh $GITHUB_USERNAME
```

### **Шаг 2: Активация GitHub Pages**
1. Перейдите: `https://github.com/$GITHUB_USERNAME/tv-channels-api/settings/pages`
2. Source: **"Deploy from a branch"**
3. Branch: **"main"**
4. Нажмите **"Save"**

### **Шаг 3: Сборка APK**
```bash
# Автоматическая сборка с настройками API
./build_apk.sh $GITHUB_USERNAME 1.1
```

### **Шаг 4: Проверка**
```bash
# Тестирование API (через 5-10 минут после активации)
./test_api.sh $GITHUB_USERNAME
```

## 🎯 Результат

После выполнения команд у вас будет:

🌐 **API панель управления:**
- `https://your-username.github.io/tv-channels-api`

📱 **Готовое APK:**
- `Android TV/build_output/tv-channels-v1.1-release.apk`

🔧 **Инструменты управления:**
- Удаленное отключение сервиса
- Обновление каналов без перезапуска
- Автообновление приложения

## 🎛️ Управление сервисом

### **Отключить сервис:**
Отредактируйте `api/config.json`:
```json
{
  "service_config": {
    "service_available": false,
    "message": "Техническое обслуживание до 18:00"
  }
}
```

### **Обновить каналы:**
1. Измените `files/channels.m3u8`
2. Увеличьте версию в `api/config.json`:
```json
{
  "channels_config": {
    "version": 6
  }
}
```

### **Загрузить новый APK:**
1. Соберите новую версию: `./build_apk.sh $GITHUB_USERNAME 1.2`
2. Загрузите APK в `files/updates/`
3. Обновите `api/config.json`:
```json
{
  "app_info": {
    "latest_version": "1.2",
    "version_code": 3,
    "download_url": "https://your-username.github.io/tv-channels-api/files/updates/tv-channels-v1.2-release.apk"
  }
}
```

## 📊 Масштабирование на 1000+ устройств

### **Метод 1: Автоустановка через ADB**
```bash
# Создайте скрипт для массовой установки
for device in $(adb devices | grep -v "List" | cut -f1); do
  adb -s $device install tv-channels-v1.1-release.apk
done
```

### **Метод 2: USB флешки**
1. Запишите APK на USB флешки
2. Создайте QR-код с инструкциями
3. Раздайте установщикам

### **Метод 3: Telegram бот**
```python
import telebot
import requests

bot = telebot.TeleBot("YOUR_BOT_TOKEN")

@bot.message_handler(commands=['get_app'])
def send_app(message):
    apk_url = f"https://{GITHUB_USERNAME}.github.io/tv-channels-api/files/updates/tv-channels-v1.1-release.apk"
    bot.send_message(message.chat.id, f"📱 Скачать приложение: {apk_url}")

bot.polling()
```

## 🛠️ Разработка и отладка

### **Локальная отладка:**
```bash
# Логи Android
adb logcat | grep "MainActivity\|RemoteConfigManager"

# Проверка API
curl https://your-username.github.io/tv-channels-api/api/config.json
```

### **Мониторинг:**
- **GitHub Pages статус**: Settings → Pages
- **API панель**: `https://your-username.github.io/tv-channels-api`
- **Коммиты**: История изменений репозитория

## 💰 Экономика

| Количество устройств | Стоимость/месяц | Трафик |
|---------------------|-----------------|---------|
| 1-10 | $0 | Неограниченный |
| 100 | $0 | Неограниченный |
| 1000+ | $0 | Неограниченный |

**Итого: $0/месяц для любого количества устройств! 🎉**

## 🔧 Поддержка

### **Часто задаваемые вопросы:**

**Q: API не отвечает**
A: Проверьте, что GitHub Pages активирован и прошло 5-10 минут после активации

**Q: Приложение не обновляется**
A: Убедитесь, что version_code в config.json больше текущего

**Q: Каналы не обновляются**
A: Увеличьте channels_config.version в config.json

### **Диагностика:**
```bash
# Проверка API
./test_api.sh your-username

# Проверка APK
adb install -r tv-channels-v1.1-debug.apk
```

## 🏆 Преимущества решения

✅ **Бесплатно** - полностью без затрат
✅ **Надежно** - GitHub Pages 99.9% uptime
✅ **Масштабируемо** - неограниченное количество устройств
✅ **Просто** - управление через веб-интерфейс
✅ **Быстро** - развертывание за 5 минут

## 📄 Лицензия

MIT License - используйте свободно для любых целей

---

🎯 **Готово! Теперь у вас есть профессиональная система управления IPTV приложением с полным удаленным контролем!** 