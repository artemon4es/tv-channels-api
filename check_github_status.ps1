# CHECK GITHUB PAGES STATUS
Write-Host "Checking GitHub Pages API..." -ForegroundColor Cyan

try {
    $response = Invoke-WebRequest -Uri "https://artemon4es.github.io/tv-channels-api/api/config.json" -UseBasicParsing
    $config = $response.Content | ConvertFrom-Json
    
    Write-Host "GitHub Pages service_available:" $config.service_config.service_available -ForegroundColor Yellow
    
    # Compare with local
    $local = Get-Content "api/config.json" -Raw | ConvertFrom-Json
    Write-Host "Local file service_available:" $local.service_config.service_available -ForegroundColor Green
    
    if ($config.service_config.service_available -eq $local.service_config.service_available) {
        Write-Host "STATUS: SYNCHRONIZED" -ForegroundColor Green
    } else {
        Write-Host "STATUS: NOT SYNCHRONIZED!" -ForegroundColor Red
        Write-Host "Local changes not uploaded to GitHub!" -ForegroundColor Red
    }
    
} catch {
    Write-Host "ERROR:" $_.Exception.Message -ForegroundColor Red
} 