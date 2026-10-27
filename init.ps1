# init.ps1 - Windows PowerShell init script for Dashwheel

$ErrorActionPreference = "Stop"
$RootDir = $PSScriptRoot
Set-Location $RootDir

$InstallCmd = ".\gradlew dependencies"
$VerifyCmd = ".\gradlew :app:lintGithubDebug :app:testGithubDebugUnitTest :app:assembleGithubDebug"
$StartCmd = ".\gradlew :app:installGithubDebug"

Write-Host "==> Working directory: $RootDir"
Write-Host "==> Syncing dependencies"
Invoke-Expression $InstallCmd
if ($LASTEXITCODE -ne 0) { throw "Install failed" }

Write-Host "==> Running baseline verification"
Invoke-Expression $VerifyCmd
if ($LASTEXITCODE -ne 0) { throw "Verification failed" }

Write-Host "==> Startup command"
Write-Host "    $StartCmd"

if ($env:RUN_START_COMMAND -eq "1") {
    Write-Host "==> Starting the app"
    Invoke-Expression $StartCmd
} else {
    Write-Host "Set `$env:RUN_START_COMMAND=1 if you want init.ps1 to launch the app directly."
}
