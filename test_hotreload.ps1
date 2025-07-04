# 🔄 ТЕСТ HOTRELOAD ФУНКЦИОНАЛЬНОСТИ
# Проверка быстроты отклика системы при изменении статуса сервиса

param(
    [string]$Token = "",
    [int]$MaxWaitTime = 30
)

Write-Host "🔄 Тестирование HotReload системы..." -ForegroundColor Cyan
Write-Host "=" * 50

$baseUrl = "https://artemon4es.github.io/tv-channels-api"
$apiUrl = "$baseUrl/api/config.json"
$repoUrl = "https://api.github.com/repos/artemon4es/tv-channels-api"

# Проверка токена
if ([string]::IsNullOrEmpty($Token)) {
    Write-Host "❌ Не указан GitHub токен!" -ForegroundColor Red
    Write-Host "💡 Использование: .\test_hotreload.ps1 -Token 'YOUR_TOKEN'" -ForegroundColor Yellow
    exit 1
}

# Функция для получения текущего статуса сервиса
function Get-ServiceStatus {
    try {
        $response = Invoke-RestMethod -Uri "$apiUrl?t=$(Get-Date -UFormat %s)" -TimeoutSec 10
        return $response.service_config.service_available
    }
    catch {
        Write-Host "❌ Ошибка получения статуса: $($_.Exception.Message)" -ForegroundColor Red
        return $null
    }
}

# Функция для обновления статуса сервиса
function Set-ServiceStatus {
    param([bool]$Status, [string]$Message = "")
    
    try {
        # Получаем текущий config.json
        $getResponse = Invoke-RestMethod -Uri "$repoUrl/contents/api/config.json" -Headers @{
            "Authorization" = "token $Token"
            "Accept" = "application/vnd.github.v3+json"
        }
        
        # Декодируем содержимое
        $currentConfig = [System.Text.Encoding]::UTF8.GetString([System.Convert]::FromBase64String($getResponse.content)) | ConvertFrom-Json
        
        # Обновляем статус
        $currentConfig.service_config.service_available = $Status
        $currentConfig.service_config.message = $Message
        
        # Кодируем обновленное содержимое
        $updatedContent = $currentConfig | ConvertTo-Json -Depth 10
        $encodedContent = [System.Convert]::ToBase64String([System.Text.Encoding]::UTF8.GetBytes($updatedContent))
        
        # Обновляем файл
        $updateBody = @{
            message = "🔄 Тест HotReload: сервис $(if($Status){'включен'}else{'отключен'})"
            content = $encodedContent
            sha = $getResponse.sha
            branch = "main"
        } | ConvertTo-Json -Depth 10
        
        Invoke-RestMethod -Uri "$repoUrl/contents/api/config.json" -Method Put -Headers @{
            "Authorization" = "token $Token"
            "Accept" = "application/vnd.github.v3+json"
            "Content-Type" = "application/json"
        } -Body $updateBody | Out-Null
        
        return $true
    }
    catch {
        Write-Host "❌ Ошибка обновления статуса: $($_.Exception.Message)" -ForegroundColor Red
        return $false
    }
}

# Функция для ожидания изменения статуса
function Wait-StatusChange {
    param([bool]$ExpectedStatus, [int]$MaxWait = 30)
    
    Write-Host "⏳ Ожидание изменения статуса на $ExpectedStatus..." -ForegroundColor Yellow
    
    $startTime = Get-Date
    $timeout = $startTime.AddSeconds($MaxWait)
    
    while ((Get-Date) -lt $timeout) {
        $currentStatus = Get-ServiceStatus
        
        if ($currentStatus -eq $ExpectedStatus) {
            $elapsed = [math]::Round(((Get-Date) - $startTime).TotalSeconds, 2)
            Write-Host "✅ Статус изменен за $elapsed секунд" -ForegroundColor Green
            return $true
        }
        
        Start-Sleep -Seconds 2
        Write-Host "." -NoNewline -ForegroundColor Gray
    }
    
    Write-Host ""
    Write-Host "❌ Статус не изменился за $MaxWait секунд" -ForegroundColor Red
    return $false
}

# Начинаем тест
Write-Host "🔍 Получение текущего статуса сервиса..." -ForegroundColor Cyan
$initialStatus = Get-ServiceStatus

if ($initialStatus -eq $null) {
    Write-Host "❌ Не удалось получить текущий статус сервиса!" -ForegroundColor Red
    exit 1
}

Write-Host "📊 Текущий статус сервиса: $(if($initialStatus){'Включен'}else{'Отключен'})" -ForegroundColor $(if($initialStatus){'Green'}else{'Red'})
Write-Host ""

# Тест 1: Отключение сервиса
Write-Host "🔄 ТЕСТ 1: Отключение сервиса" -ForegroundColor Cyan
Write-Host "-" * 30

if ($initialStatus) {
    Write-Host "📤 Отключаем сервис..." -ForegroundColor Yellow
    
    $success = Set-ServiceStatus -Status $false -Message "Тест HotReload - сервис отключен"
    
    if ($success) {
        $result1 = Wait-StatusChange -ExpectedStatus $false -MaxWait $MaxWaitTime
        
        if ($result1) {
            Write-Host "✅ Тест 1 пройден!" -ForegroundColor Green
        } else {
            Write-Host "❌ Тест 1 не пройден!" -ForegroundColor Red
        }
    } else {
        Write-Host "❌ Не удалось отключить сервис" -ForegroundColor Red
    }
} else {
    Write-Host "ℹ️  Сервис уже отключен" -ForegroundColor Blue
}

Write-Host ""

# Тест 2: Включение сервиса
Write-Host "🔄 ТЕСТ 2: Включение сервиса" -ForegroundColor Cyan
Write-Host "-" * 30

Write-Host "📤 Включаем сервис..." -ForegroundColor Yellow

$success = Set-ServiceStatus -Status $true -Message ""

if ($success) {
    $result2 = Wait-StatusChange -ExpectedStatus $true -MaxWait $MaxWaitTime
    
    if ($result2) {
        Write-Host "✅ Тест 2 пройден!" -ForegroundColor Green
    } else {
        Write-Host "❌ Тест 2 не пройден!" -ForegroundColor Red
    }
} else {
    Write-Host "❌ Не удалось включить сервис" -ForegroundColor Red
}

Write-Host ""

# Итоговый результат
Write-Host "=" * 50
Write-Host "🎯 ИТОГОВЫЙ РЕЗУЛЬТАТ:" -ForegroundColor Cyan

if ($result1 -and $result2) {
    Write-Host "✅ Все тесты пройдены! HotReload работает корректно." -ForegroundColor Green
} elseif ($result1 -or $result2) {
    Write-Host "⚠️  Частично пройдены. Есть проблемы с HotReload." -ForegroundColor Yellow
} else {
    Write-Host "❌ Тесты не пройдены. HotReload не работает." -ForegroundColor Red
}

Write-Host ""
Write-Host "💡 Рекомендации:" -ForegroundColor Yellow
Write-Host "• Проверьте админ-панель: $baseUrl" -ForegroundColor Gray
Write-Host "• Убедитесь, что GitHub Pages активирован" -ForegroundColor Gray
Write-Host "• Проверьте токен администратора" -ForegroundColor Gray
Write-Host "• Если тесты не проходят, подождите 5-10 минут" -ForegroundColor Gray 