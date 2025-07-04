# Руководство по изображениям для Android TV приложения

## Структура папок для изображений

### 📁 drawable-hdpi/ (средний DPI ~ 240)
- **Назначение**: Изображения для устройств среднего разрешения
- **Размеры иконок каналов**: 48x48 px
- **Размеры изображений загрузки**: 320x240 px

### 📁 drawable-xhdpi/ (высокий DPI ~ 320) 
- **Назначение**: Изображения для устройств высокого разрешения (основная папка для Android TV)
- **Размеры иконок каналов**: 64x64 px  
- **Размеры изображений загрузки**: 480x360 px

### 📁 drawable-xxhdpi/ (очень высокий DPI ~ 480)
- **Назначение**: Изображения для устройств очень высокого разрешения  
- **Размеры иконок каналов**: 96x96 px
- **Размеры изображений загрузки**: 640x480 px

## Типы изображений

### 🎯 Иконки каналов
**Именование файлов**: `channel_[название_канала].png`
- `channel_russia1.png` - иконка канала Россия 1
- `channel_perviy.png` - иконка Первого канала  
- `channel_rtr.png` - иконка РТР-Планета
- `channel_ntv.png` - иконка НТВ

**Формат**: PNG с прозрачным фоном
**Соотношение сторон**: 1:1 (квадратные)

### 🌟 Изображения экрана загрузки
**Именование файлов**: `splash_[описание].png`
- `splash_logo.png` - основной логотип приложения
- `splash_background.png` - фоновое изображение
- `splash_loading.png` - анимация загрузки

**Формат**: PNG или JPG
**Соотношение сторон**: 16:9 или 4:3

## Использование в коде

### Загрузка иконок каналов
```kotlin
// В ChannelList.kt или адаптере
fun loadChannelIcon(channelName: String, imageView: ImageView) {
    val iconName = "channel_${channelName.lowercase().replace(" ", "_")}"
    val iconResId = context.resources.getIdentifier(iconName, "drawable", context.packageName)
    
    if (iconResId != 0) {
        imageView.setImageResource(iconResId)
    } else {
        imageView.setImageResource(R.drawable.channel_placeholder)
    }
}
```

### Использование в SplashActivity
```kotlin
// В SplashActivity.kt
val splashLogo = findViewById<ImageView>(R.id.splashLogo)
splashLogo.setImageResource(R.drawable.splash_logo)
```

## Рекомендации

1. **Оптимизация размера**: Используйте сжатие PNG/JPG для уменьшения размера APK
2. **Качество**: Иконки каналов должны быть четкими и узнаваемыми
3. **Единообразие**: Соблюдайте единый стиль для всех иконок
4. **Тестирование**: Проверяйте отображение на разных размерах экрана Android TV

## Автоматическая загрузка иконок

Для автоматической загрузки иконок каналов из интернета можно добавить функционал в `ChannelUpdateManager.kt`. 