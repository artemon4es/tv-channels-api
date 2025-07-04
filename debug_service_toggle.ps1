#!/usr/bin/env pwsh

# Diagnostic script for service toggle issue debugging
# Author: Android TV IPTV Diagnostic System
# Checks all stages of service_available change process

Write-Host "DEBUG: SERVICE TOGGLE PROBLEM DIAGNOSIS" -ForegroundColor Cyan
Write-Host "=======================================" -ForegroundColor Cyan

$timestamp = Get-Date -Format "yyyy-MM-dd HH:mm:ss"
Write-Host "Start time: $timestamp" -ForegroundColor Yellow

# URL endpoints
$GITHUB_PAGES_URL = "https://artemon4es.github.io/tv-channels-api/api/config.json"
$LOCAL_CONFIG = "./api/config.json"

Write-Host "`nSTAGE 1: Check local config.json" -ForegroundColor Green

if (Test-Path $LOCAL_CONFIG) {
    $localConfig = Get-Content $LOCAL_CONFIG | ConvertFrom-Json
    $localStatus = $localConfig.service_config.service_available
    Write-Host "Local file found" -ForegroundColor Green
    Write-Host "Local status: service_available = $localStatus" -ForegroundColor White
} else {
    Write-Host "Local config.json not found!" -ForegroundColor Red
    exit 1
}

Write-Host "`nSTAGE 2: Check GitHub Pages (no cache)" -ForegroundColor Green

try {
    # Add timestamp to bypass cache
    $timestamp_param = [DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds()
    $github_url_nocache = "$GITHUB_PAGES_URL?t=$timestamp_param"
    
    $response = Invoke-RestMethod -Uri $github_url_nocache -Headers @{
        "Cache-Control" = "no-cache, no-store, must-revalidate"
        "Pragma" = "no-cache"
        "Expires" = "0"
    } -TimeoutSec 10
    
    $githubStatus = $response.service_config.service_available
    Write-Host "GitHub Pages accessible" -ForegroundColor Green
    Write-Host "GitHub Pages status: service_available = $githubStatus" -ForegroundColor White
    
} catch {
    Write-Host "GitHub Pages connection error: $($_.Exception.Message)" -ForegroundColor Red
    $githubStatus = $null
}

Write-Host "`nSTAGE 3: Compare statuses" -ForegroundColor Green

if ($null -ne $githubStatus) {
    if ($localStatus -eq $githubStatus) {
        Write-Host "Statuses MATCH: $localStatus = $githubStatus" -ForegroundColor Green
        Write-Host "Problem is NOT in file synchronization" -ForegroundColor Yellow
    } else {
        Write-Host "Statuses DO NOT MATCH!" -ForegroundColor Red
        Write-Host "   Local: $localStatus" -ForegroundColor White
        Write-Host "   GitHub Pages: $githubStatus" -ForegroundColor White
        Write-Host "PROBLEM FOUND: GitHub Pages not updated" -ForegroundColor Yellow
    }
} else {
    Write-Host "Cannot compare - GitHub Pages unavailable" -ForegroundColor Red
}

Write-Host "`nSTAGE 4: Cache testing" -ForegroundColor Green

# Test multiple URL variants to detect caching
$urls = @(
    $GITHUB_PAGES_URL,
    ($GITHUB_PAGES_URL + "?nocache=" + (Get-Random)),
    ($GITHUB_PAGES_URL + "?v=" + (Get-Date -Format "HHmmss"))
)

foreach ($url in $urls) {
    try {
        $response = Invoke-RestMethod -Uri $url -TimeoutSec 5
        $status = $response.service_config.service_available
        Write-Host "$url : $status"
    } catch {
        Write-Host "$url : ERROR" -ForegroundColor Red
    }
}

Write-Host "`nSTAGE 5: Android app simulation" -ForegroundColor Green

try {
    # Simulate Android app request
    $androidHeaders = @{
        "User-Agent" = "okhttp/4.9.0"
        "Cache-Control" = "no-cache"
        "Pragma" = "no-cache"
        "Expires" = "0"
    }
    
    $androidResponse = Invoke-RestMethod -Uri $GITHUB_PAGES_URL -Headers $androidHeaders -TimeoutSec 10
    $androidStatus = $androidResponse.service_config.service_available
    
    Write-Host "Android simulation successful" -ForegroundColor Green
    Write-Host "Android sees status: service_available = $androidStatus" -ForegroundColor White
    
    if ($androidStatus -ne $localStatus) {
        Write-Host "Android sees DIFFERENT status!" -ForegroundColor Red
        Write-Host "Possible client-side caching issue" -ForegroundColor Yellow
    }
    
} catch {
    Write-Host "Android simulation error: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host "`nSTAGE 6: Fix recommendations" -ForegroundColor Green

if ($null -ne $githubStatus -and $localStatus -ne $githubStatus) {
    Write-Host "PROBLEM: GitHub Pages not synchronized" -ForegroundColor Yellow
    Write-Host "Solutions:" -ForegroundColor White
    Write-Host "   1. Wait 5-10 minutes (normal GitHub Pages delay)" -ForegroundColor White
    Write-Host "   2. Check GitHub Actions in repository" -ForegroundColor White
    Write-Host "   3. Make empty commit for forced deploy" -ForegroundColor White
    
} elseif ($null -eq $githubStatus) {
    Write-Host "PROBLEM: GitHub Pages unavailable" -ForegroundColor Yellow
    Write-Host "Solutions:" -ForegroundColor White
    Write-Host "   1. Check internet connection" -ForegroundColor White
    Write-Host "   2. Check GitHub Pages status" -ForegroundColor White
    Write-Host "   3. Check repository settings" -ForegroundColor White
    
} else {
    Write-Host "FILES ARE SYNCHRONIZED" -ForegroundColor Green
    Write-Host "Possible problem causes:" -ForegroundColor White
    Write-Host "   1. Web panel caching (Ctrl+F5)" -ForegroundColor White
    Write-Host "   2. Android app caching" -ForegroundColor White
    Write-Host "   3. Web panel logic issue" -ForegroundColor White
    Write-Host "   4. Android app logic issue" -ForegroundColor White
}

Write-Host "`nSTAGE 7: File last modification time" -ForegroundColor Green

try {
    if ($null -ne $githubStatus) {
        $lastUpdated = $response.channels_config.last_updated
        Write-Host "Last config update: $lastUpdated" -ForegroundColor White
        
        $updateTime = [DateTime]::Parse($lastUpdated)
        $timeDiff = (Get-Date) - $updateTime
        
        if ($timeDiff.TotalMinutes -lt 10) {
            $minutes = [math]::Round($timeDiff.TotalMinutes, 1)
            Write-Host "File was updated recently ($minutes min ago)" -ForegroundColor Green
        } else {
            $hours = [math]::Round($timeDiff.TotalHours, 1)
            Write-Host "File not updated for long time ($hours h ago)" -ForegroundColor Yellow
        }
    }
} catch {
    Write-Host "Could not determine update time" -ForegroundColor Yellow
}

$endTimestamp = Get-Date -Format "yyyy-MM-dd HH:mm:ss"
Write-Host "`nDIAGNOSIS COMPLETED: $endTimestamp" -ForegroundColor Cyan
Write-Host "=======================================" -ForegroundColor Cyan 