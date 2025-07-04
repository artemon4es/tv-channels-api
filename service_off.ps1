# SERVICE DISABLE SCRIPT
Write-Host "Disabling service..." -ForegroundColor Red

try {
    $configPath = "api/config.json"
    $config = Get-Content $configPath -Raw | ConvertFrom-Json
    
    Write-Host "Current status: $($config.service_config.service_available)" -ForegroundColor Yellow
    
    $config.service_config.service_available = $false
    $config.service_config.message = ""
    
    $config | ConvertTo-Json -Depth 10 | Set-Content $configPath -Encoding UTF8
    
    Write-Host "SERVICE DISABLED!" -ForegroundColor Green
    Write-Host "New status: $($config.service_config.service_available)" -ForegroundColor Green
    
    $verifyConfig = Get-Content $configPath -Raw | ConvertFrom-Json
    Write-Host "Verification: service_available = $($verifyConfig.service_config.service_available)" -ForegroundColor Cyan
    
} catch {
    Write-Host "ERROR: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host "File changed locally. To apply on GitHub use:" -ForegroundColor Yellow
Write-Host "git add api/config.json && git commit -m 'Service disabled' && git push" -ForegroundColor White 