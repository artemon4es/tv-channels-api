#!/usr/bin/env pwsh

# Force Sync Service Status Script
# Manually synchronizes local config.json with GitHub Pages

param(
    [Parameter(Mandatory=$true)]
    [string]$GitHubToken,
    
    [Parameter(Mandatory=$true)]
    [ValidateSet("true", "false")]
    [string]$ServiceStatus
)

$REPO_OWNER = "artemon4es"
$REPO_NAME = "tv-channels-api"
$FILE_PATH = "api/config.json"
$LOCAL_CONFIG = "./api/config.json"

Write-Host "FORCE SYNC SERVICE STATUS" -ForegroundColor Cyan
Write-Host "=========================" -ForegroundColor Cyan

# Validate input
$statusBool = $ServiceStatus -eq "true"
Write-Host "Target service_available: $statusBool" -ForegroundColor Yellow

# Read local config
if (-not (Test-Path $LOCAL_CONFIG)) {
    Write-Host "ERROR: Local config.json not found!" -ForegroundColor Red
    exit 1
}

$localConfig = Get-Content $LOCAL_CONFIG | ConvertFrom-Json
Write-Host "Current local status: $($localConfig.service_config.service_available)" -ForegroundColor White

# Update local config
$localConfig.service_config.service_available = $statusBool
$localConfig.service_config.message = ""
$localConfig.channels_config.last_updated = (Get-Date -Format "yyyy-MM-ddTHH:mm:ss.fffZ")

# Save updated local config
$updatedJson = $localConfig | ConvertTo-Json -Depth 10
$updatedJson | Set-Content $LOCAL_CONFIG -Encoding UTF8
Write-Host "Local config updated" -ForegroundColor Green

# Prepare for GitHub API
$apiUrl = "https://api.github.com/repos/$REPO_OWNER/$REPO_NAME/contents/$FILE_PATH"
$headers = @{
    "Authorization" = "token $GitHubToken"
    "Accept" = "application/vnd.github.v3+json"
    "Content-Type" = "application/json"
}

try {
    # Get current file SHA
    Write-Host "Getting current file SHA..." -ForegroundColor Yellow
    $response = Invoke-RestMethod -Uri $apiUrl -Headers $headers -Method GET
    $currentSha = $response.sha
    
    Write-Host "Current SHA: $currentSha" -ForegroundColor White
    
    # Encode content to Base64
    $contentBytes = [System.Text.Encoding]::UTF8.GetBytes($updatedJson)
    $contentBase64 = [System.Convert]::ToBase64String($contentBytes)
    
    # Prepare update payload
    $updatePayload = @{
        message = "Force sync: service_available = $statusBool"
        content = $contentBase64
        sha = $currentSha
        branch = "main"
    } | ConvertTo-Json
    
    # Update file via GitHub API
    Write-Host "Updating GitHub repository..." -ForegroundColor Yellow
    $updateResponse = Invoke-RestMethod -Uri $apiUrl -Headers $headers -Method PUT -Body $updatePayload
    
    Write-Host "SUCCESS: File updated in GitHub!" -ForegroundColor Green
    Write-Host "New SHA: $($updateResponse.content.sha)" -ForegroundColor White
    Write-Host "Commit URL: $($updateResponse.commit.html_url)" -ForegroundColor Cyan
    
    # Wait and verify
    Write-Host "`nWaiting 10 seconds for GitHub Pages to update..." -ForegroundColor Yellow
    Start-Sleep -Seconds 10
    
    # Verify GitHub Pages
    $pagesUrl = "https://artemon4es.github.io/tv-channels-api/api/config.json?t=" + [DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds()
    try {
        $pagesResponse = Invoke-RestMethod -Uri $pagesUrl -Headers @{
            "Cache-Control" = "no-cache"
            "Pragma" = "no-cache"
        }
        $pagesStatus = $pagesResponse.service_config.service_available
        
        if ($pagesStatus -eq $statusBool) {
            Write-Host "VERIFICATION SUCCESS: GitHub Pages shows correct status: $pagesStatus" -ForegroundColor Green
        } else {
            Write-Host "VERIFICATION PENDING: GitHub Pages still shows: $pagesStatus (expected: $statusBool)" -ForegroundColor Yellow
            Write-Host "Note: GitHub Pages may take 5-10 minutes to update" -ForegroundColor White
        }
    } catch {
        Write-Host "Could not verify GitHub Pages: $($_.Exception.Message)" -ForegroundColor Yellow
    }
    
} catch {
    Write-Host "ERROR: Failed to update GitHub repository" -ForegroundColor Red
    Write-Host "Error: $($_.Exception.Message)" -ForegroundColor Red
    
    if ($_.Exception.Message -like "*401*") {
        Write-Host "Check your GitHub token permissions" -ForegroundColor Yellow
    } elseif ($_.Exception.Message -like "*404*") {
        Write-Host "Check repository name and file path" -ForegroundColor Yellow
    }
    
    exit 1
}

Write-Host "`nSUMMARY:" -ForegroundColor Cyan
Write-Host "Local config: service_available = $statusBool" -ForegroundColor White  
Write-Host "GitHub repo: Updated successfully" -ForegroundColor Green
Write-Host "GitHub Pages: Will update in 5-10 minutes" -ForegroundColor Yellow
Write-Host "`nTIP: Use 'powershell debug_service_toggle.ps1' to verify sync" -ForegroundColor Cyan 