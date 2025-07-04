#!/usr/bin/env pwsh

# SERVICE TOGGLE FIX TOOL
# Complete solution for service toggle synchronization issues
# This tool fixes the disconnect between web panel and GitHub Pages

Write-Host ""
Write-Host "🔧 SERVICE TOGGLE FIX TOOL" -ForegroundColor Cyan
Write-Host "============================" -ForegroundColor Cyan
Write-Host "Solving web panel ↔ GitHub Pages synchronization issues" -ForegroundColor White
Write-Host ""

# Menu options
function Show-Menu {
    Write-Host "📋 Available Actions:" -ForegroundColor Green
    Write-Host ""
    Write-Host "1️⃣  Diagnose Current Problem" -ForegroundColor Yellow
    Write-Host "2️⃣  Force Enable Service (sync to GitHub)" -ForegroundColor Green  
    Write-Host "3️⃣  Force Disable Service (sync to GitHub)" -ForegroundColor Red
    Write-Host "4️⃣  Verify All Systems Status" -ForegroundColor Cyan
    Write-Host "5️⃣  Fix Web Panel Logic (Advanced)" -ForegroundColor Magenta
    Write-Host "0️⃣  Exit" -ForegroundColor Gray
    Write-Host ""
}

function Test-GitHubToken {
    param([string]$token)
    
    if ([string]::IsNullOrEmpty($token)) {
        return $false
    }
    
    try {
        $response = Invoke-RestMethod -Uri "https://api.github.com/repos/artemon4es/tv-channels-api" -Headers @{
            "Authorization" = "token $token"
            "Accept" = "application/vnd.github.v3+json"
        } -TimeoutSec 10
        
        return $response.permissions.push -eq $true
    } catch {
        return $false
    }
}

function Get-GitHubToken {
    $token = ""
    
    # Try to read from environment
    if ($env:GITHUB_TOKEN) {
        $token = $env:GITHUB_TOKEN
        Write-Host "Found token in environment variable" -ForegroundColor Green
    } else {
        Write-Host "🔑 GitHub Personal Access Token required" -ForegroundColor Yellow
        Write-Host "   Get it from: https://github.com/settings/tokens" -ForegroundColor White
        Write-Host "   Required permissions: repo, workflow" -ForegroundColor White
        $token = Read-Host "Enter GitHub token" -AsSecureString
        $token = [Runtime.InteropServices.Marshal]::PtrToStringAuto([Runtime.InteropServices.Marshal]::SecureStringToBSTR($token))
    }
    
    if (-not (Test-GitHubToken $token)) {
        Write-Host "❌ Invalid or insufficient permissions on GitHub token!" -ForegroundColor Red
        return $null
    }
    
    Write-Host "✅ GitHub token validated successfully" -ForegroundColor Green
    return $token
}

function Invoke-Diagnosis {
    Write-Host "🔍 Running comprehensive diagnosis..." -ForegroundColor Cyan
    & powershell -ExecutionPolicy Bypass -File "debug_service_toggle.ps1"
}

function Set-ServiceStatus {
    param([bool]$enabled, [string]$token)
    
    $statusText = if ($enabled) { "ENABLE" } else { "DISABLE" }
    $statusValue = if ($enabled) { "true" } else { "false" }
    
    Write-Host "🔄 Force $statusText service (sync with GitHub)..." -ForegroundColor Yellow
    
    try {
        & powershell -ExecutionPolicy Bypass -File "force_sync_service.ps1" -GitHubToken $token -ServiceStatus $statusValue
        
        Write-Host ""
        Write-Host "✅ Service status changed successfully!" -ForegroundColor Green
        Write-Host "💡 Wait 5-10 minutes for GitHub Pages to fully update" -ForegroundColor Yellow
        
        # Offer immediate verification
        Write-Host ""
        $verify = Read-Host "Verify status now? (y/n)"
        if ($verify -eq "y" -or $verify -eq "Y") {
            Invoke-Diagnosis
        }
        
    } catch {
        Write-Host "❌ Failed to update service status: $($_.Exception.Message)" -ForegroundColor Red
    }
}

function Invoke-StatusVerification {
    Write-Host "🔍 Verifying all systems..." -ForegroundColor Cyan
    Write-Host ""
    
    # Local status
    if (Test-Path "./api/config.json") {
        $local = Get-Content "./api/config.json" | ConvertFrom-Json
        $localStatus = $local.service_config.service_available
        Write-Host "📁 Local config.json: service_available = $localStatus" -ForegroundColor White
    } else {
        Write-Host "📁 Local config.json: NOT FOUND" -ForegroundColor Red
        return
    }
    
    # GitHub Pages status
    try {
        $githubUrl = "https://artemon4es.github.io/tv-channels-api/api/config.json?t=" + [DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds()
        $github = Invoke-RestMethod -Uri $githubUrl -Headers @{
            "Cache-Control" = "no-cache"
            "Pragma" = "no-cache"
        } -TimeoutSec 10
        
        $githubStatus = $github.service_config.service_available
        Write-Host "🌐 GitHub Pages: service_available = $githubStatus" -ForegroundColor White
        
        # Compare
        if ($localStatus -eq $githubStatus) {
            Write-Host "✅ SYNCHRONIZED: Both systems show same status" -ForegroundColor Green
            Write-Host "💡 If Android app shows different status, restart the app" -ForegroundColor Yellow
        } else {
            Write-Host "⚠️ DESYNCHRONIZED: Systems show different status!" -ForegroundColor Red
            Write-Host "💡 Use option 2 or 3 to force synchronization" -ForegroundColor Yellow
        }
        
    } catch {
        Write-Host "🌐 GitHub Pages: CONNECTION ERROR" -ForegroundColor Red
        Write-Host "   Error: $($_.Exception.Message)" -ForegroundColor White
    }
}

function Fix-WebPanelLogic {
    Write-Host "🔧 Analyzing web panel logic..." -ForegroundColor Magenta
    Write-Host ""
    
    if (-not (Test-Path "index.html")) {
        Write-Host "❌ index.html not found in current directory" -ForegroundColor Red
        return
    }
    
    Write-Host "📋 Checking web panel configuration..." -ForegroundColor Yellow
    
    # Check for common issues in web panel
    $htmlContent = Get-Content "index.html" -Raw
    
    $issues = @()
    
    # Check 1: Verify disableService function exists
    if ($htmlContent -notmatch "function disableService\(\)") {
        $issues += "❌ disableService() function not found"
    } else {
        Write-Host "✅ disableService() function found" -ForegroundColor Green
    }
    
    # Check 2: Verify updateConfigFieldDirect function exists  
    if ($htmlContent -notmatch "function updateConfigFieldDirect\(") {
        $issues += "❌ updateConfigFieldDirect() function not found"
    } else {
        Write-Host "✅ updateConfigFieldDirect() function found" -ForegroundColor Green
    }
    
    # Check 3: Verify GitHub API integration
    if ($htmlContent -notmatch "api\.github\.com") {
        $issues += "❌ GitHub API integration not found"
    } else {
        Write-Host "✅ GitHub API integration found" -ForegroundColor Green
    }
    
    # Check 4: Verify error handling
    if ($htmlContent -notmatch "catch.*error") {
        $issues += "⚠️ Limited error handling detected"
    } else {
        Write-Host "✅ Error handling found" -ForegroundColor Green
    }
    
    Write-Host ""
    
    if ($issues.Count -eq 0) {
        Write-Host "✅ Web panel logic appears correct" -ForegroundColor Green
        Write-Host "💡 Issue likely in GitHub token or network connectivity" -ForegroundColor Yellow
    } else {
        Write-Host "⚠️ Found potential issues:" -ForegroundColor Yellow
        foreach ($issue in $issues) {
            Write-Host "   $issue" -ForegroundColor White
        }
    }
    
    Write-Host ""
    Write-Host "🔧 Recommendations:" -ForegroundColor Cyan
    Write-Host "   1. Ensure GitHub token is set correctly in web panel" -ForegroundColor White
    Write-Host "   2. Check browser console for JavaScript errors" -ForegroundColor White
    Write-Host "   3. Verify network connectivity to GitHub API" -ForegroundColor White
    Write-Host "   4. Try hard refresh (Ctrl+F5) on web panel" -ForegroundColor White
}

# Main execution
do {
    Show-Menu
    $choice = Read-Host "Select option"
    Write-Host ""
    
    switch ($choice) {
        "1" {
            Invoke-Diagnosis
        }
        "2" {
            $token = Get-GitHubToken
            if ($token) {
                Set-ServiceStatus -enabled $true -token $token
            }
        }
        "3" {
            $token = Get-GitHubToken  
            if ($token) {
                Set-ServiceStatus -enabled $false -token $token
            }
        }
        "4" {
            Invoke-StatusVerification
        }
        "5" {
            Fix-WebPanelLogic
        }
        "0" {
            Write-Host "👋 Goodbye!" -ForegroundColor Green
            break
        }
        default {
            Write-Host "❌ Invalid option. Try again." -ForegroundColor Red
        }
    }
    
    if ($choice -ne "0") {
        Write-Host ""
        Write-Host "Press any key to continue..."
        $null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")
        Clear-Host
    }
    
} while ($choice -ne "0") 