#!/usr/bin/env pwsh

# QUICK FIX: Service Toggle Synchronization
# Simple tool to sync local config with GitHub Pages

Write-Host ""
Write-Host "QUICK FIX: Service Toggle Sync" -ForegroundColor Cyan
Write-Host "==============================" -ForegroundColor Cyan
Write-Host ""

# Check current status
if (-not (Test-Path "./api/config.json")) {
    Write-Host "ERROR: config.json not found!" -ForegroundColor Red
    exit 1
}

$local = Get-Content "./api/config.json" | ConvertFrom-Json
$localStatus = $local.service_config.service_available

Write-Host "Current local status: service_available = $localStatus" -ForegroundColor Yellow

# Check GitHub Pages
try {
    $githubUrl = "https://artemon4es.github.io/tv-channels-api/api/config.json?t=" + [DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds()
    $github = Invoke-RestMethod -Uri $githubUrl -Headers @{
        "Cache-Control" = "no-cache"
        "Pragma" = "no-cache"
    } -TimeoutSec 10
    
    $githubStatus = $github.service_config.service_available
    Write-Host "GitHub Pages status: service_available = $githubStatus" -ForegroundColor Yellow
    
    if ($localStatus -eq $githubStatus) {
        Write-Host ""
        Write-Host "SUCCESS: Systems are synchronized!" -ForegroundColor Green
        Write-Host "If Android app shows wrong status, restart the app." -ForegroundColor White
        exit 0
    }
    
} catch {
    Write-Host "GitHub Pages: Connection error" -ForegroundColor Red
    $githubStatus = "unknown"
}

Write-Host ""
Write-Host "PROBLEM: Systems are NOT synchronized!" -ForegroundColor Red
Write-Host "Local: $localStatus | GitHub: $githubStatus" -ForegroundColor White
Write-Host ""

# Quick fix options
Write-Host "Quick Fix Options:" -ForegroundColor Green
Write-Host "1. Set service to ENABLED (sync to GitHub)" -ForegroundColor Green
Write-Host "2. Set service to DISABLED (sync to GitHub)" -ForegroundColor Red
Write-Host "3. Exit" -ForegroundColor Gray
Write-Host ""

$choice = Read-Host "Choose option (1/2/3)"

if ($choice -eq "3") {
    Write-Host "Goodbye!" -ForegroundColor Gray
    exit 0
}

if ($choice -ne "1" -and $choice -ne "2") {
    Write-Host "Invalid choice. Exiting." -ForegroundColor Red
    exit 1
}

# Get GitHub token
Write-Host ""
Write-Host "GitHub Personal Access Token required" -ForegroundColor Yellow
Write-Host "Get it from: https://github.com/settings/tokens" -ForegroundColor White
Write-Host "Required scopes: repo, workflow" -ForegroundColor White
Write-Host ""

$token = Read-Host "Enter GitHub token" -AsSecureString
$token = [Runtime.InteropServices.Marshal]::PtrToStringAuto([Runtime.InteropServices.Marshal]::SecureStringToBSTR($token))

if ([string]::IsNullOrEmpty($token)) {
    Write-Host "No token provided. Exiting." -ForegroundColor Red
    exit 1
}

# Set target status
$targetStatus = ($choice -eq "1")
$statusText = if ($targetStatus) { "ENABLED" } else { "DISABLED" }

Write-Host ""
Write-Host "Setting service to $statusText..." -ForegroundColor Yellow

# Update local config
$local.service_config.service_available = $targetStatus
$local.service_config.message = ""
$local.channels_config.last_updated = (Get-Date -Format "yyyy-MM-ddTHH:mm:ss.fffZ")

# Save local
$updatedJson = $local | ConvertTo-Json -Depth 10
$updatedJson | Set-Content "./api/config.json" -Encoding UTF8
Write-Host "Local config updated" -ForegroundColor Green

# Update GitHub
try {
    $apiUrl = "https://api.github.com/repos/artemon4es/tv-channels-api/contents/api/config.json"
    $headers = @{
        "Authorization" = "token $token"
        "Accept" = "application/vnd.github.v3+json"
        "Content-Type" = "application/json"
    }
    
    # Get current SHA
    $response = Invoke-RestMethod -Uri $apiUrl -Headers $headers -Method GET
    $currentSha = $response.sha
    
    # Encode content
    $contentBytes = [System.Text.Encoding]::UTF8.GetBytes($updatedJson)
    $contentBase64 = [System.Convert]::ToBase64String($contentBytes)
    
    # Update payload
    $updatePayload = @{
        message = "Fix service toggle: service_available = $targetStatus"
        content = $contentBase64
        sha = $currentSha
        branch = "main"
    } | ConvertTo-Json
    
    # Send update
    $updateResponse = Invoke-RestMethod -Uri $apiUrl -Headers $headers -Method PUT -Body $updatePayload
    
    Write-Host "GitHub repository updated successfully!" -ForegroundColor Green
    Write-Host "Commit: $($updateResponse.commit.html_url)" -ForegroundColor Cyan
    
    Write-Host ""
    Write-Host "SUCCESS! Service is now $statusText" -ForegroundColor Green
    Write-Host ""
    Write-Host "Next steps:" -ForegroundColor Cyan
    Write-Host "1. Wait 5-10 minutes for GitHub Pages to update" -ForegroundColor White
    Write-Host "2. Restart Android TV app to clear cache" -ForegroundColor White
    Write-Host "3. Check that app shows correct status" -ForegroundColor White
    
} catch {
    Write-Host ""
    Write-Host "ERROR: Failed to update GitHub" -ForegroundColor Red
    Write-Host "Error: $($_.Exception.Message)" -ForegroundColor Red
    
    if ($_.Exception.Message -like "*401*" -or $_.Exception.Message -like "*403*") {
        Write-Host ""
        Write-Host "Token issue detected. Check:" -ForegroundColor Yellow
        Write-Host "1. Token is valid and not expired" -ForegroundColor White
        Write-Host "2. Token has 'repo' and 'workflow' permissions" -ForegroundColor White
        Write-Host "3. You have write access to the repository" -ForegroundColor White
    }
} 