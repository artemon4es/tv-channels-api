# 🚨 ЭКСТРЕННОЕ ОТКЛЮЧЕНИЕ IPTV СЕРВИСА
param([string]$Message = "Сервис временно не доступен.`nОбратитесь к администратору.")

Write-Host "🚨 ОТКЛЮЧЕНИЕ IPTV СЕРВИСА..." -ForegroundColor Red

try {
    # Загружаем текущую конфигурацию
    $config = Get-Content "api/config.json" | ConvertFrom-Json
    
    # Отключаем сервис
    $config.service_config.service_available = $false
    $config.service_config.message = $Message
    
    # Сохраняем изменения
    $config | ConvertTo-Json -Depth 10 | Set-Content "api/config.json" -Encoding UTF8
    
    # Коммитим в Git
    git add api/config.json
    git commit -m "🚨 ЭКСТРЕННОЕ ОТКЛЮЧЕНИЕ - $(Get-Date -Format 'dd.MM.yyyy HH:mm')"
    git push
    
    Write-Host "✅ СЕРВИС ОТКЛЮЧЕН!" -ForegroundColor Green
    Write-Host "📝 Сообщение: $Message" -ForegroundColor Yellow
    Write-Host "🌐 Изменения будут активны в течение 1-2 минут" -ForegroundColor Cyan
    
} catch {
    Write-Host "❌ ОШИБКА: $_" -ForegroundColor Red
} 