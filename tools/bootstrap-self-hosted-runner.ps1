param(
    [string]$Repo = "hminthakil-eng/Veil-Reader",
    [string]$RunnerDir = "$env:USERPROFILE\Documents\EyadStudio\ActionsRunner\VeilReader",
    [string]$RunnerName = "veil-reader-$env:COMPUTERNAME"
)

$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest

function Require-Command {
    param([string]$Name)
    $command = Get-Command $Name -ErrorAction SilentlyContinue
    if (-not $command) {
        throw "$Name is required but is not installed or not on PATH."
    }
    return $command.Source
}

$Gh = Require-Command "gh"

New-Item -ItemType Directory -Force -Path $RunnerDir | Out-Null
Set-Location $RunnerDir

if (Test-Path ".runner") {
    Write-Host "Veil Runner is already configured in $RunnerDir."
    Write-Host "Start it with: .\run.cmd"
    exit 0
}

$previousErrorAction = $ErrorActionPreference
$ErrorActionPreference = "Continue"
$null = & $Gh auth status -h github.com 2>&1
$authExit = $LASTEXITCODE
$ErrorActionPreference = $previousErrorAction
if ($authExit -ne 0) {
    throw "GitHub CLI authorization is required. Run 'gh auth login -h github.com -p https' once, approve access, then rerun this script."
}

if (-not (Test-Path ".\config.cmd")) {
    $releaseJson = & $Gh api "repos/actions/runner/releases/latest"
    if ($LASTEXITCODE -ne 0) { throw "Unable to read the latest GitHub Actions runner release." }

    $release = $releaseJson | ConvertFrom-Json
    $assetName = "actions-runner-win-x64-$($release.tag_name.TrimStart('v')).zip"
    $asset = $release.assets | Where-Object { $_.name -eq $assetName } | Select-Object -First 1
    if (-not $asset) { throw "Runner asset not found in latest release: $assetName" }

    $zipPath = Join-Path $RunnerDir $assetName
    if (-not (Test-Path $zipPath)) {
        Invoke-WebRequest -Uri $asset.browser_download_url -OutFile $zipPath
    }

    if ($asset.digest -and $asset.digest -match "^sha256:(.+)$") {
        $expected = $Matches[1].ToLowerInvariant()
        $actual = (Get-FileHash -Algorithm SHA256 $zipPath).Hash.ToLowerInvariant()
        if ($actual -ne $expected) {
            throw "Runner package SHA-256 mismatch."
        }
        Write-Host "Runner package SHA-256 verified."
    } else {
        Write-Warning "GitHub release metadata did not provide an asset digest; package hash could not be independently verified here."
    }

    Expand-Archive -Path $zipPath -DestinationPath $RunnerDir -Force
}

if (-not (Test-Path ".\config.cmd")) {
    throw "Runner package is incomplete: config.cmd is missing."
}

$token = $null
try {
    $token = (& $Gh api -X POST "repos/$Repo/actions/runners/registration-token" --jq ".token").Trim()
    if ($LASTEXITCODE -ne 0 -or -not $token) {
        throw "Unable to obtain a repository runner registration token. Repository administration permission may be required."
    }

    $configArgs = @(
        "--unattended",
        "--url", "https://github.com/$Repo",
        "--token", $token,
        "--name", $RunnerName,
        "--labels", "veil-reader",
        "--work", "_work"
    )
    & .\config.cmd @configArgs
    if ($LASTEXITCODE -ne 0) {
        throw "GitHub Actions runner configuration failed."
    }
}
finally {
    $token = $null
    Remove-Variable token -ErrorAction SilentlyContinue
}

if (-not (Test-Path ".runner")) {
    throw "Runner configuration returned without creating .runner."
}

Write-Host ""
Write-Host "Veil Runner configured."
Write-Host "Directory: $RunnerDir"
Write-Host "Required workflow labels: self-hosted, Windows, X64, veil-reader"
Write-Host "Start it with: .\run.cmd"
