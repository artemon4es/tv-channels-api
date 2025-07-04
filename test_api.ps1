# PowerShell скрипт тестирования GitHub Pages API
# Для пользователя: artemon4es

# Цвета для вывода
$RED = "Red"
$GREEN = "Green"
$YELLOW = "Yellow"
$BLUE = "Blue"

# Базовый URL для GitHub Pages
$BASE_URL = "https://artemon4es.github.io/tv-channels-api"

Write-Host "🧪 Тестирование GitHub Pages API" -ForegroundColor $BLUE
Write-Host "📍 Базовый URL: $BASE_URL" -ForegroundColor $BLUE
Write-Host "=================================="

# Функция для проверки URL
function Test-ApiUrl {
    param(
        [string]$Url,
        [string]$Name,
        [string]$ExpectedContent = ""
    )
    
    Write-Host "Проверка: $Name" -ForegroundColor $YELLOW
    Write-Host "🔗 URL: $Url"
    
    try {
        # Проверка доступности
        $response = Invoke-WebRequest -Uri $Url -Method GET -UseBasicParsing
        
        if ($response.StatusCode -eq 200) {
            Write-Host "✅ Статус: 200 OK" -ForegroundColor $GREEN
            
            # Проверка содержимого
            if ($ExpectedContent -ne "") {
                if ($response.Content -like "*$ExpectedContent*") {
                    Write-Host "✅ Содержимое корректное" -ForegroundColor $GREEN
                } else {
                    Write-Host "❌ Содержимое не найдено: $ExpectedContent" -ForegroundColor $RED
                    return $false
                }
            }
        } else {
            Write-Host "❌ Статус: $($response.StatusCode)" -ForegroundColor $RED
            return $false
        }
    }
    catch {
        Write-Host "❌ Ошибка: $($_.Exception.Message)" -ForegroundColor $RED
        return $false
    }
    
    Write-Host ""
    return $true
}

# Счетчик успешных тестов
$successCount = 0
$totalTests = 4

# Тест 1: Главная страница
if (Test-ApiUrl -Url $BASE_URL -Name "Главная страница" -ExpectedContent "TV Channels API") {
    $successCount++
}

# Тест 2: Конфигурация
if (Test-ApiUrl -Url "$BASE_URL/api/config.json" -Name "Конфигурация" -ExpectedContent "service_config") {
    $successCount++
}

# Тест 3: Список каналов
if (Test-ApiUrl -Url "$BASE_URL/files/channels.m3u8" -Name "Список каналов" -ExpectedContent "#EXTM3U") {
    $successCount++
}

# Тест 4: Конфигурация безопасности
if (Test-ApiUrl -Url "$BASE_URL/files/security_config.xml" -Name "Конфигурация безопасности" -ExpectedContent "network-security-config") {
    $successCount++
}

Write-Host "=================================="
Write-Host "📊 Результаты тестирования:" -ForegroundColor $BLUE
Write-Host "✅ Успешно: $successCount/$totalTests"

if ($successCount -eq $totalTests) {
    Write-Host "🎉 Все тесты пройдены успешно!" -ForegroundColor $GREEN
    Write-Host "📱 API готов для использования в приложении" -ForegroundColor $GREEN
} else {
    Write-Host "❌ Некоторые тесты не пройдены" -ForegroundColor $RED
    Write-Host "💡 Убедитесь, что GitHub Pages активирован" -ForegroundColor $YELLOW
    Write-Host "💡 Подождите 10 минут после активации" -ForegroundColor $YELLOW
}

Read-Host "Нажмите Enter для выхода" 