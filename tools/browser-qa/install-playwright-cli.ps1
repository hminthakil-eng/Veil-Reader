# Install Playwright CLI only when explicitly run in an authorized Windows workspace.
# Repository source: https://github.com/microsoft/playwright-cli
# This does not install an Android runtime dependency, and no browser launches automatically.
$ErrorActionPreference = "Stop"
$Version = "0.1.22"
if (-not (Get-Command node -ErrorAction SilentlyContinue)) {
    throw "Node.js 18+ is required. Install Node.js before running this script."
}
if (-not (Get-Command npm -ErrorAction SilentlyContinue)) {
    throw "npm is required. Install Node.js/npm before running this script."
}
$CurrentNode = (& node --version).Trim()
$Major = [int]($CurrentNode.TrimStart("v").Split(".")[0])
if ($Major -lt 18) { throw "Playwright CLI needs Node.js 18+. Found $CurrentNode" }
Write-Host "Installing approved Playwright CLI $Version for the current developer environment"
& npm install --global "@playwright/cli@$Version"
if ($LASTEXITCODE -ne 0) { throw "npm installation failed" }
& playwright-cli --help
if ($LASTEXITCODE -ne 0) { throw "Playwright CLI startup check failed" }
Write-Host "Playwright CLI is installed and --help succeeded. Browser binary/session tests remain separate."
