# 🧪 ТЕСТ API GITHUB PAGES
# Простая проверка работы API

Write-Host "🔍 Проверка API GitHub Pages..." -ForegroundColor Cyan

$baseUrl = "https://artemon4es.github.io/tv-channels-api"

# Тестируемые endpoints
$endpoints = @(
    @{url = "$baseUrl/"; name = "Главная страница (Admin Panel)"},
    @{url = "$baseUrl/api/config.json"; name = "Конфигурация API"},
    @{url = "$baseUrl/files/channels.m3u8"; name = "Список каналов"},
    @{url = "$baseUrl/files/security_config.xml"; name = "Безопасность"}
)

Write-Host ""
Write-Host "📊 Результаты тестирования:" -ForegroundColor Yellow
Write-Host "=" * 50

foreach ($endpoint in $endpoints) {
    try {
        Write-Host "📡 $($endpoint.name)..." -NoNewline
        
        $response = Invoke-WebRequest -Uri $endpoint.url -UseBasicParsing -TimeoutSec 10
        
        if ($response.StatusCode -eq 200) {
            Write-Host " ✅ OK" -ForegroundColor Green
            Write-Host "   📊 Размер: $($response.Content.Length) байт" -ForegroundColor Gray
        } else {
            Write-Host " ❌ Ошибка: $($response.StatusCode)" -ForegroundColor Red
        }
    }
    catch {
        Write-Host " ❌ Недоступно" -ForegroundColor Red
        Write-Host "   💡 Причина: $($_.Exception.Message)" -ForegroundColor Gray
    }
    Write-Host ""
}

Write-Host "=" * 50
Write-Host ""

# Проверка GitHub Pages статуса
Write-Host "🌐 Проверка GitHub Pages..." -ForegroundColor Cyan
try {
    $response = Invoke-WebRequest -Uri $baseUrl -UseBasicParsing -TimeoutSec 15
    if ($response.StatusCode -eq 200) {
        Write-Host "✅ GitHub Pages работает!" -ForegroundColor Green
        Write-Host "🔗 Ваш сайт: $baseUrl" -ForegroundColor Cyan
    }
}
catch {
    Write-Host "❌ GitHub Pages недоступен" -ForegroundColor Red
    Write-Host "💡 Возможные причины:" -ForegroundColor Yellow
    Write-Host "   • GitHub Pages еще не активирован" -ForegroundColor Gray
    Write-Host "   • Репозиторий не публичный" -ForegroundColor Gray
    Write-Host "   • Файлы не загружены в ветку main" -ForegroundColor Gray
    Write-Host "   • Нужно подождать 5-10 минут после активации" -ForegroundColor Gray
}

Write-Host ""
Write-Host "🎯 Готово! Проверьте результаты выше." -ForegroundColor Green 