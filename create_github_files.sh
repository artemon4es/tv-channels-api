#!/bin/bash

# Скрипт создания всех файлов для GitHub Pages
# Для пользователя: artemon4es
# Дата обновления: 4 июля 2025

echo "🚀 Создание файлов для GitHub Pages..."

# Создаем структуру папок
mkdir -p api
mkdir -p files/updates

# Создаем api/config.json
cat > api/config.json << 'EOF'
{
  "app_info": {
    "current_version": "1.0",
    "latest_version": "1.1", 
    "version_code": 2,
    "download_url": "https://artemon4es.github.io/tv-channels-api/files/updates/app-v1.1.apk",
    "update_required": false,
    "changelog": "🔧 Добавлена система автообновления\n✨ Улучшена стабильность воспроизведения\n📱 Оптимизация для Android TV"
  },
  "service_config": {
    "service_available": true,
    "message": "",
    "maintenance_mode": false,
    "access_token": "ЗАМЕНИТЕ_НА_ВАШ_GITHUB_TOKEN"
  },
  "channels_config": {
    "version": 5,
    "last_updated": "2025-07-04T12:00:00Z",
    "url": "https://artemon4es.github.io/tv-channels-api/files/channels.m3u8",
    "security_config_url": "https://artemon4es.github.io/tv-channels-api/files/security_config.xml"
  },
  "api_info": {
    "version": "1.0",
    "last_updated": "2025-07-04T12:00:00Z",
    "endpoints": {
      "config": "/api/config.json",
      "channels": "/files/channels.m3u8",
      "security": "/files/security_config.xml"
    }
  }
}
EOF

# Создаем files/channels.m3u8 (краткая версия)
cat > files/channels.m3u8 << 'EOF'
#EXTM3U
#EXTINF:-1 tvg-id="pervy" tvg-logo="https://iptvx.one/picons/perviy-kanal.png" group-title="Эфирные",Первый канал HD
https://live-mirror-01.ott.tricolor.tv/live/live/1TV_hd/hls_enc/1TV_hd.m3u8?drmreq=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJhcHBfdHlwZSI6ImFwdHBfb3R0IiwiY2xhc3MiOiJCUk9XU0VSIiwiZHJlaWQiOiI1NDIxOTAwNzk1ODc0MiIsImV4cCI6MTc1MTg3Mjg4NSwiaHdpZCI6IjM0YmQxMDNmZGFiNDliZTFiMWY2NjljNWU0ZjE3YzA1YTBjNzFkMTk3ZDAwY2U2OWM1MDA1YWI1ZjlhOGM3N2IiLCJsYXN0X3JlcV9kYXRlIjoxNzUxMjY4MDg1LCJzZXNzaW9uX2lkIjoiOTNmMDRlMzU0MjdhMDQ3M2EzYmUwYzRiZjhjYjg5MDciLCJ0b2tlbl90eXBlIjoiQ29udGVudEFjY2Vzc1Rva2VuIiwidHlwZSI6Ik1PWklMTEEifQ.JZaPSRErMA94UlobjPgSVBdyB_5y-Kl9t5otFSPBrFo

#EXTINF:-1 tvg-id="rossia1" tvg-logo="https://iptvx.one/picons/rossia-1.png" group-title="Эфирные",Россия 1 HD
https://live-mirror-01.ott.tricolor.tv/live/live/Rossia_1_0_hd/hls_enc/Rossia_1_0_hd.m3u8?drmreq=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJhcHBfdHlwZSI6ImFwdHBfb3R0IiwiY2xhc3MiOiJCUk9XU0VSIiwiZHJlaWQiOiI1NDIxOTAwNzk1ODc0MiIsImV4cCI6MTc1MTg3Mjg4NSwiaHdpZCI6IjM0YmQxMDNmZGFiNDliZTFiMWY2NjljNWU0ZjE3YzA1YTBjNzFkMTk3ZDAwY2U2OWM1MDA1YWI1ZjlhOGM3N2IiLCJsYXN0X3JlcV9kYXRlIjoxNzUxMjY4MDg1LCJzZXNzaW9uX2lkIjoiOTNmMDRlMzU0MjdhMDQ3M2EzYmUwYzRiZjhjYjg5MDciLCJ0b2tlbl90eXBlIjoiQ29udGVudEFjY2Vzc1Rva2VuIiwidHlwZSI6Ik1PWklMTEEifQ.JZaPSRErMA94UlobjPgSVBdyB_5y-Kl9t5otFSPBrFo

#EXTINF:-1 tvg-logo="https://iptvx.one/picons/rossia-24.png" tvg-id="rossia-24" group-title="Новости", Россия-24
https://live-stream-13.ott.tricolor.tv/live/live/rossia_24/hls_enc/rossia_24.m3u8?drmreq=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJhcHBfdHlwZSI6ImFwdHBfb3R0IiwiY2xhc3MiOiJCUk9XU0VSIiwiZHJlaWQiOiI1NDIxOTAwNzk1ODc0MiIsImV4cCI6MTc1MTg3Mjg4NSwiaHdpZCI6IjM0YmQxMDNmZGFiNDliZTFiMWY2NjljNWU0ZjE3YzA1YTBjNzFkMTk3ZDAwY2U2OWM1MDA1YWI1ZjlhOGM3N2IiLCJsYXN0X3JlcV9kYXRlIjoxNzUxMjY4MDg1LCJzZXNzaW9uX2lkIjoiOTNmMDRlMzU0MjdhMDQ3M2EzYmUwYzRiZjhjYjg5MDciLCJ0b2tlbl90eXBlIjoiQ29udGVudEFjY2Vzc1Rva2VuIiwidHlwZSI6Ik1PWklMTEEifQ.JZaPSRErMA94UlobjPgSVBdyB_5y-Kl9t5otFSPBrFo

#EXTINF:-1 tvg-id="ntv" group-title="Эфирные",НТВ HD
https://live-stream-22.ott.tricolor.tv/live/live/NTV_0_hd/hls_enc/NTV_0_hd.m3u8?drmreq=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJhcHBfdHlwZSI6ImFwdHBfb3R0IiwiY2xhc3MiOiJCUk9XU0VSIiwiZHJlaWQiOiI1NDIxOTAwNzk1ODc0MiIsImV4cCI6MTc1MTg3Mjg4NSwiaHdpZCI6IjM0YmQxMDNmZGFiNDliZTFiMWY2NjljNWU0ZjE3YzA1YTBjNzFkMTk3ZDAwY2U2OWM1MDA1YWI1ZjlhOGM3N2IiLCJsYXN0X3JlcV9kYXRlIjoxNzUxMjY4MDg1LCJzZXNzaW9uX2lkIjoiOTNmMDRlMzU0MjdhMDQ3M2EzYmUwYzRiZjhjYjg5MDciLCJ0b2tlbl90eXBlIjoiQ29udGVudEFjY2Vzc1Rva2VuIiwidHlwZSI6Ik1PWklMTEEifQ.JZaPSRErMA94UlobjPgSVBdyB_5y-Kl9t5otFSPBrFo
EOF

# Создаем files/security_config.xml
cat > files/security_config.xml << 'EOF'
<?xml version="1.0" encoding="utf-8"?>
<network-security-config>
    <domain-config cleartextTrafficPermitted="true">
        <domain includeSubdomains="true">tricolor.tv</domain>
        <domain includeSubdomains="true">ott.tricolor.tv</domain>
        <domain includeSubdomains="true">cdnvideo.ru</domain>
        <domain includeSubdomains="true">smotrim.ru</domain>
        <domain includeSubdomains="true">epg.iptvx.one</domain>
        <domain includeSubdomains="true">iptvx.one</domain>
        <domain includeSubdomains="true">televizor-24-tochka.ru</domain>
        <domain includeSubdomains="true">getsiptv.ru</domain>
        <domain includeSubdomains="true">mirtv.cdnvideo.ru</domain>
        <domain includeSubdomains="true">player.smotrim.ru</domain>
        <domain includeSubdomains="true">profi-rus.narod.ru</domain>
        <domain includeSubdomains="true">dmi3y-tv.ru</domain>
        <domain includeSubdomains="true">tvhls.pskovline.tv</domain>
        <domain includeSubdomains="true">spas.mediacdn.ru</domain>
        <!-- GitHub Pages API -->
        <domain includeSubdomains="true">github.io</domain>
        <domain includeSubdomains="true">githubusercontent.com</domain>
    </domain-config>
</network-security-config>
EOF

# Создаем .gitignore
cat > .gitignore << 'EOF'
# Файлы Android Studio
*.iml
.gradle
/local.properties
/.idea/caches
/.idea/libraries
/.idea/modules.xml
/.idea/workspace.xml
/.idea/navEditor.xml
/.idea/assetWizardSettings.xml
.DS_Store
/build
/captures
.externalNativeBuild
.cxx
local.properties

# APK файлы (кроме папки updates)
*.apk
!files/updates/*.apk

# Временные файлы
*.tmp
*.temp
*.log
*.backup

# Ключи подписи (безопасность)
*.jks
*.keystore

# Windows
Thumbs.db
ehthumbs.db
Desktop.ini

# Локальные настройки
.env
config.local.*

# Build output
Android\ TV/app/build/
Android\ TV/build/
Android\ TV/.gradle/
Android\ TV/build_output/
EOF

echo "✅ Все файлы созданы успешно!"
echo ""
echo "📋 Созданные файлы:"
echo "   📁 api/config.json"
echo "   📁 files/channels.m3u8"
echo "   📁 files/security_config.xml"
echo "   📁 files/updates/ (пустая папка)"
echo "   📁 .gitignore"
echo ""
echo "🔐 ВАЖНО: Замените 'ЗАМЕНИТЕ_НА_ВАШ_GITHUB_TOKEN' на ваш реальный токен!"
echo "🔗 Создайте токен: https://github.com/settings/tokens"
echo ""
echo "🚀 Следующие шаги:"
echo "1. Создайте GitHub Personal Access Token"
echo "2. Загрузите файлы и замените токен на реальный"
echo "3. Активируйте GitHub Pages в настройках"
echo "4. Подождите 10 минут и протестируйте API"
echo ""
echo "🔗 Ваш API будет доступен по адресу:"
echo "   https://artemon4es.github.io/tv-channels-api" 