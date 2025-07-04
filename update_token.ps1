# PowerShell скрипт для обновления GitHub токена
# Для пользователя: artemon4es

param(
    [Parameter(Mandatory=$true)]
    [string]$Token
)

$configFile = "api/config.json"

Write-Host "🔐 Обновление GitHub токена..." -ForegroundColor Blue

# Проверяем существование файла
if (-not (Test-Path $configFile)) {
    Write-Host "❌ Файл $configFile не найден!" -ForegroundColor Red
    Write-Host "Создайте файл api/config.json сначала" -ForegroundColor Yellow
    exit 1
}

# Читаем содержимое файла
$content = Get-Content $configFile -Raw

# Заменяем токен
$newContent = $content -replace "ЗАМЕНИТЕ_НА_ВАШ_GITHUB_TOKEN", $Token

# Записываем обратно
Set-Content $configFile -Value $newContent -Encoding UTF8

Write-Host "✅ Токен успешно обновлен в файле $configFile" -ForegroundColor Green
Write-Host "🔗 Теперь загрузите файл в GitHub репозиторий" -ForegroundColor Cyan

# Показываем обновленную секцию
Write-Host "`nОбновленная секция service_config:" -ForegroundColor Yellow
$config = Get-Content $configFile | ConvertFrom-Json
$serviceConfig = $config.service_config | ConvertTo-Json -Depth 3
Write-Host $serviceConfig 