param(
    [string]$Repo = "hminthakil-eng/Veil-Reader",
    [string]$RunnerDir = "$env:USERPROFILE\veil-runner"
)

$ErrorActionPreference = "Stop"

function Ensure-Gh {
    if (Get-Command gh -ErrorAction SilentlyContinue) { return }
    if (Get-Command winget -ErrorAction SilentlyContinue) {
        winget install --id GitHub.cli -e --accept-package-agreements --accept-source-agreements
    } else {
        throw "GitHub CLI (gh) is required. Install it from https://cli.github.com/ and rerun this script."
    }
}

Ensure-Gh
try { gh auth status | Out-Null } catch { gh auth login }

New-Item -ItemType Directory -Force -Path $RunnerDir | Out-Null
Set-Location $RunnerDir

$tag = gh api repos/actions/runner/releases/latest --jq ".tag_name"
$version = $tag.TrimStart("v")
$zip = "actions-runner-win-x64-$version.zip"
$url = "https://github.com/actions/runner/releases/download/$tag/$zip"
if (-not (Test-Path $zip)) { Invoke-WebRequest -Uri $url -OutFile $zip }
if (-not (Test-Path ".\config.cmd")) { Expand-Archive -Path $zip -DestinationPath . -Force }

$token = gh api -X POST "repos/$Repo/actions/runners/registration-token" --jq ".token"
& .\config.cmd --unattended --replace --url "https://github.com/$Repo" --token $token --name "veil-reader-windows" --labels "veil-reader" --work "_work"

Write-Host ""
Write-Host "Veil Runner configured."
Write-Host "Start it now with: .\run.cmd"
Write-Host "For persistent startup, run: .\svc install ; .\svc start"
