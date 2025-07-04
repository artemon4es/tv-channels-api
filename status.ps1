# SERVICE STATUS CHECK
Write-Host "Checking service status..." -ForegroundColor Blue

try {
    $configPath = "api/config.json"
    
    if (Test-Path $configPath) {
        $config = Get-Content $configPath -Raw | ConvertFrom-Json
        
        Write-Host "=" * 50 -ForegroundColor Gray
        Write-Host "CURRENT SERVICE STATUS" -ForegroundColor White
        Write-Host "=" * 50 -ForegroundColor Gray
        
        $status = $config.service_config.service_available
        $statusText = if ($status) { "ENABLED" } else { "DISABLED" }
        $statusColor = if ($status) { "Green" } else { "Red" }
        
        Write-Host "Status: $statusText" -ForegroundColor $statusColor
        Write-Host "Message: '$($config.service_config.message)'" -ForegroundColor Yellow
        Write-Host "Maintenance: $($config.service_config.maintenance_mode)" -ForegroundColor Cyan
        Write-Host "Channels version: $($config.channels_config.version)" -ForegroundColor Magenta
        Write-Host "APK version: $($config.app_info.current_version) -> $($config.app_info.latest_version)" -ForegroundColor Magenta
        
        Write-Host "=" * 50 -ForegroundColor Gray
        
        if (Test-Path "files/channels.m3u8") {
            $channelsSize = (Get-Item "files/channels.m3u8").Length
            Write-Host "Channels file: $channelsSize bytes" -ForegroundColor Green
        } else {
            Write-Host "Channels file NOT FOUND!" -ForegroundColor Red
        }
        
        Write-Host "Last updated: $($config.channels_config.last_updated)" -ForegroundColor Gray
        
    } else {
        Write-Host "Config file not found!" -ForegroundColor Red
    }
    
} catch {
    Write-Host "ERROR: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host ""
Write-Host "To change status use:" -ForegroundColor Yellow
Write-Host "   .\service_off.ps1  - disable service" -ForegroundColor White
Write-Host "   .\service_on.ps1   - enable service" -ForegroundColor White 