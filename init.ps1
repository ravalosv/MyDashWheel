# init.ps1 - Windows PowerShell init script for Dashwheel

$ErrorActionPreference = "Stop"
$RootDir = $PSScriptRoot
Set-Location $RootDir

$StartCmd = ".\gradlew.bat :app:installGithubDebug"

Write-Host "==> Working directory: $RootDir"
Write-Host "==> Local repository state (no Gradle checks)"
git rev-parse --abbrev-ref HEAD
if ($LASTEXITCODE -ne 0) { throw "Branch check failed" }
git --no-pager status --short
if ($LASTEXITCODE -ne 0) { throw "Repository status failed" }
if (-not (Test-Path -LiteralPath ".\gradlew.bat" -PathType Leaf)) {
    throw "Gradle wrapper missing"
}
Write-Host "Validation policy: docs/harness/quality-gates.md"

Write-Host "==> Startup command"
Write-Host "    $StartCmd"

if ($env:RUN_START_COMMAND -eq "1") {
    Write-Host "==> Starting the app"
    Invoke-Expression $StartCmd
    if ($LASTEXITCODE -ne 0) { throw "App installation failed" }
} else {
    Write-Host "Set `$env:RUN_START_COMMAND=1 if you want init.ps1 to launch the app directly."
}
