# ===============================================
# 🚀 ЗАГРУЗКА ФАЙЛОВ НА GITHUB PAGES
# Обновление всех файлов проекта на GitHub
# ===============================================

Write-Host "🚀 Загрузка обновленных файлов на GitHub Pages..." -ForegroundColor Cyan
Write-Host "=" * 50 -ForegroundColor Cyan

# Проверяем наличие Git
if (!(Get-Command git -ErrorAction SilentlyContinue)) {
    Write-Host "❌ Git не найден! Установите Git и попробуйте снова." -ForegroundColor Red
    Write-Host "💡 Скачайте Git: https://git-scm.com/download/win" -ForegroundColor Yellow
    exit 1
}

# Проверяем что Git инициализирован
if (!(Test-Path ".git")) {
    Write-Host "❌ Git не инициализирован в этой папке!" -ForegroundColor Red
    Write-Host "💡 Сначала выполните настройку из SETUP_GITHUB_WINDOWS.md" -ForegroundColor Yellow
    exit 1
}

# Проверяем что remote настроен
$remoteOrigin = git remote get-url origin 2>$null
if (!$remoteOrigin) {
    Write-Host "❌ Remote origin не настроен!" -ForegroundColor Red
    Write-Host "💡 Выполните: git remote add origin https://github.com/artemon4es/tv-channels-api.git" -ForegroundColor Yellow
    exit 1
}

# Проверяем наличие файлов
$requiredFiles = @(
    "index.html",
    "api/config.json", 
    "files/channels.m3u8",
    "files/security_config.xml"
)

foreach ($file in $requiredFiles) {
    if (!(Test-Path $file)) {
        Write-Host "❌ Файл не найден: $file" -ForegroundColor Red
        exit 1
    }
}

Write-Host "✅ Все файлы найдены" -ForegroundColor Green

# Создаем папку updates если не существует
if (!(Test-Path "files/updates")) {
    New-Item -ItemType Directory -Path "files/updates" -Force
    Write-Host "✅ Создана папка files/updates" -ForegroundColor Green
}

# Добавляем все файлы
Write-Host "📝 Добавление файлов..." -ForegroundColor Yellow
git add .

# Проверяем изменения
$changes = git status --porcelain
if (!$changes) {
    Write-Host "📄 Нет изменений для коммита" -ForegroundColor Yellow
    exit 0
}

Write-Host "📋 Изменения:" -ForegroundColor Yellow
git status --short

# Коммитим изменения
$timestamp = Get-Date -Format "dd.MM.yyyy HH:mm"
$commitMessage = "🔧 Обновление Control Panel - $timestamp"

Write-Host "💾 Коммит: $commitMessage" -ForegroundColor Yellow
git commit -m $commitMessage

# Отправляем на GitHub
Write-Host "🌐 Отправка на GitHub..." -ForegroundColor Yellow
git push origin main

if ($LASTEXITCODE -eq 0) {
    Write-Host "" -ForegroundColor Green
    Write-Host "🎉 УСПЕШНО ЗАГРУЖЕНО!" -ForegroundColor Green
    Write-Host "🌐 Ваш сайт: https://artemon4es.github.io/tv-channels-api/" -ForegroundColor Cyan
    Write-Host "📊 Админ-панель: https://artemon4es.github.io/tv-channels-api/index.html" -ForegroundColor Cyan
    Write-Host "⏱️ Изменения будут активны через 1-2 минуты" -ForegroundColor Yellow
    Write-Host "" -ForegroundColor Green
    
    # Показываем что было обновлено
    Write-Host "📋 Обновленные файлы:" -ForegroundColor Cyan
    $changes -split "`n" | ForEach-Object {
        if ($_.Trim()) {
            $status = $_.Substring(0,2)
            $file = $_.Substring(3)
            $icon = switch ($status.Trim()) {
                "M" { "📝" }
                "A" { "➕" }
                "D" { "❌" }
                default { "🔄" }
            }
            Write-Host "   $icon $file" -ForegroundColor White
        }
    }
    
    Write-Host ""
    Write-Host "🔧 Что дальше:" -ForegroundColor Yellow
    Write-Host "   • Подождите 1-2 минуты для обновления" -ForegroundColor Gray
    Write-Host "   • Проверьте сайт в браузере" -ForegroundColor Gray
    Write-Host "   • Запустите ./test_api_simple.ps1 для проверки" -ForegroundColor Gray
    
} else {
    Write-Host "❌ Ошибка при отправке на GitHub!" -ForegroundColor Red
    Write-Host ""
    Write-Host "💡 Возможные причины и решения:" -ForegroundColor Yellow
    Write-Host "   • Проблемы с аутентификацией → Проверьте Personal Access Token" -ForegroundColor Gray
    Write-Host "   • Нет подключения к интернету → Проверьте соединение" -ForegroundColor Gray
    Write-Host "   • Неправильный токен → Создайте новый токен на GitHub" -ForegroundColor Gray
    Write-Host "   • Проблемы с Git → Выполните: git config --global --list" -ForegroundColor Gray
    Write-Host ""
    Write-Host "🔧 См. руководство: SETUP_GITHUB_WINDOWS.md" -ForegroundColor Cyan
}
