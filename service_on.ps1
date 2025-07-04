# SERVICE ENABLE SCRIPT
Write-Host "Enabling service..." -ForegroundColor Green

try {
    $configPath = "api/config.json"
    $config = Get-Content $configPath -Raw | ConvertFrom-Json
    
    Write-Host "Current status: $($config.service_config.service_available)" -ForegroundColor Yellow
    
    $config.service_config.service_available = $true
    $config.service_config.message = ""
    
    $config | ConvertTo-Json -Depth 10 | Set-Content $configPath -Encoding UTF8
    
    Write-Host "SERVICE ENABLED!" -ForegroundColor Green
    Write-Host "New status: $($config.service_config.service_available)" -ForegroundColor Green
    
    $verifyConfig = Get-Content $configPath -Raw | ConvertFrom-Json
    Write-Host "Verification: service_available = $($verifyConfig.service_config.service_available)" -ForegroundColor Cyan
    
} catch {
    Write-Host "ERROR: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host "File changed locally. To apply on GitHub use:" -ForegroundColor Yellow
Write-Host "git add api/config.json && git commit -m 'Service enabled' && git push" -ForegroundColor White 