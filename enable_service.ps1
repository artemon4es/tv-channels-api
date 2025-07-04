# ✅ ВКЛЮЧЕНИЕ IPTV СЕРВИСА

Write-Host "✅ ВКЛЮЧЕНИЕ IPTV СЕРВИСА..." -ForegroundColor Green

try {
    # Загружаем текущую конфигурацию
    $config = Get-Content "api/config.json" | ConvertFrom-Json
    
    # Включаем сервис
    $config.service_config.service_available = $true
    $config.service_config.message = ""
    
    # Сохраняем изменения
    $config | ConvertTo-Json -Depth 10 | Set-Content "api/config.json" -Encoding UTF8
    
    # Коммитим в Git
    git add api/config.json
    git commit -m "✅ Сервис включен - $(Get-Date -Format 'dd.MM.yyyy HH:mm')"
    git push
    
    Write-Host "🎉 СЕРВИС ВКЛЮЧЕН!" -ForegroundColor Green
    Write-Host "🌐 Изменения будут активны в течение 1-2 минут" -ForegroundColor Cyan
    Write-Host "📊 Статус: https://artemon4es.github.io/tv-channels-api/" -ForegroundColor White
    
} catch {
    Write-Host "❌ ОШИБКА: $_" -ForegroundColor Red
} 