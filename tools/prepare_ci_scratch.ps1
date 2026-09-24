param(
  [double]$MinimumScratchGB = 12,
  [double]$MinimumWorkspaceGB = 0.5
)

$ErrorActionPreference = "Stop"

function Remove-CiDirectory {
  param([Parameter(Mandatory=$true)][string]$Path)

  if (-not (Test-Path -LiteralPath $Path)) {
    return
  }

  $item = Get-Item -LiteralPath $Path -Force
  $isReparsePoint = (($item.Attributes -band [System.IO.FileAttributes]::ReparsePoint) -ne 0)

  if ($isReparsePoint) {
    Write-Host "Removing CI junction: $Path"
    [System.IO.Directory]::Delete($Path, $false)
  } else {
    Write-Host "Removing reproducible CI directory: $Path"
    Remove-Item -LiteralPath $Path -Recurse -Force -ErrorAction Stop
  }
}

function Set-GithubEnv {
  param(
    [Parameter(Mandatory=$true)][string]$Name,
    [Parameter(Mandatory=$true)][string]$Value
  )

  if (-not $env:GITHUB_ENV) {
    throw "GITHUB_ENV is not available"
  }

  "$Name=$Value" | Out-File -FilePath $env:GITHUB_ENV -Append -Encoding utf8
  Set-Item -Path "Env:$Name" -Value $Value
}

Write-Host "=== Local fixed-drive inventory ==="
$fixedDrives = @(Get-CimInstance Win32_LogicalDisk -Filter "DriveType=3" |
  Where-Object { $_.DeviceID -and $_.FreeSpace -ne $null } |
  Sort-Object @{ Expression = { [int64]$_.FreeSpace }; Descending = $true })

if (-not $fixedDrives) {
  throw "No local fixed filesystem drive was discovered"
}

foreach ($drive in $fixedDrives) {
  Write-Host ("{0}\ free={1:N2} GB size={2:N2} GB" -f $drive.DeviceID, ([int64]$drive.FreeSpace / 1GB), ([int64]$drive.Size / 1GB))
}

$bestDrive = $fixedDrives | Select-Object -First 1
$bestRoot = "$($bestDrive.DeviceID)\"
$workspaceRoot = [System.IO.Path]::GetPathRoot((Get-Location).Path)
$workspaceDriveId = $workspaceRoot.TrimEnd("\")
$workspaceDrive = $fixedDrives | Where-Object { $_.DeviceID -eq $workspaceDriveId } | Select-Object -First 1

if (-not $workspaceDrive) {
  throw "Could not resolve workspace drive $workspaceRoot"
}

$workspaceFreeGB = [int64]$workspaceDrive.FreeSpace / 1GB
$scratchFreeGB = [int64]$bestDrive.FreeSpace / 1GB

if ($workspaceFreeGB -lt $MinimumWorkspaceGB) {
  throw ("Workspace drive {0} has only {1:N2} GB free; require >= {2:N2} GB for checkout/runner overhead" -f $workspaceRoot, $workspaceFreeGB, $MinimumWorkspaceGB)
}

$ciRoot = Join-Path $bestRoot "VeilReader-CI"
$runRoot = Join-Path $ciRoot ("runs\{0}" -f $env:GITHUB_RUN_ID)
$gradleHome = Join-Path $ciRoot "gradle-home"
$tempRoot = Join-Path $ciRoot ("temp\{0}-{1}" -f $env:GITHUB_RUN_ID, $env:GITHUB_JOB)
$avdHome = Join-Path $ciRoot "avd"
$appBuildTarget = Join-Path $runRoot "app-build"
$benchmarkBuildTarget = Join-Path $runRoot "benchmark-build"

New-Item -ItemType Directory -Force -Path $ciRoot,$gradleHome,$tempRoot,$avdHome | Out-Null

# Clear only this workflow run's previous heavy outputs. This also frees a
# completed preflight job before the release-verification job begins.
if (Test-Path -LiteralPath $runRoot) {
  Remove-Item -LiteralPath $runRoot -Recurse -Force -ErrorAction Stop
}
New-Item -ItemType Directory -Force -Path $appBuildTarget,$benchmarkBuildTarget | Out-Null

# Remove stale CI-owned run/temp directories, never user files or Android SDKs.
$staleCutoff = (Get-Date).AddHours(-24)
foreach ($parent in @((Join-Path $ciRoot "runs"), (Join-Path $ciRoot "temp"))) {
  if (Test-Path -LiteralPath $parent) {
    Get-ChildItem -LiteralPath $parent -Force -ErrorAction SilentlyContinue |
      Where-Object { $_.FullName -ne $runRoot -and $_.FullName -ne $tempRoot -and $_.LastWriteTime -lt $staleCutoff } |
      ForEach-Object {
        Write-Host "Removing stale CI scratch: $($_.FullName)"
        Remove-Item -LiteralPath $_.FullName -Recurse -Force -ErrorAction SilentlyContinue
      }
  }
}

$appBuildLink = Join-Path (Get-Location).Path "app\build"
$benchmarkBuildLink = Join-Path (Get-Location).Path "benchmark\build"
$projectGradle = Join-Path (Get-Location).Path ".gradle"

Remove-CiDirectory -Path $appBuildLink
Remove-CiDirectory -Path $benchmarkBuildLink
Remove-CiDirectory -Path $projectGradle

New-Item -ItemType Junction -Path $appBuildLink -Target $appBuildTarget | Out-Null
New-Item -ItemType Junction -Path $benchmarkBuildLink -Target $benchmarkBuildTarget | Out-Null

Set-GithubEnv -Name "VEIL_CI_ROOT" -Value $ciRoot
Set-GithubEnv -Name "VEIL_CI_SCRATCH_DRIVE" -Value $bestDrive.DeviceID
Set-GithubEnv -Name "GRADLE_USER_HOME" -Value $gradleHome
Set-GithubEnv -Name "TEMP" -Value $tempRoot
Set-GithubEnv -Name "TMP" -Value $tempRoot
Set-GithubEnv -Name "ANDROID_AVD_HOME" -Value $avdHome

# Re-read free space after cleanup/junction setup.
$bestAfter = Get-CimInstance Win32_LogicalDisk -Filter "DriveType=3" |
  Where-Object { $_.DeviceID -eq $bestDrive.DeviceID } |
  Select-Object -First 1
$workspaceAfter = Get-CimInstance Win32_LogicalDisk -Filter "DriveType=3" |
  Where-Object { $_.DeviceID -eq $workspaceDriveId } |
  Select-Object -First 1

$scratchFreeAfterGB = [int64]$bestAfter.FreeSpace / 1GB
$workspaceFreeAfterGB = [int64]$workspaceAfter.FreeSpace / 1GB

Write-Host ("Selected CI scratch drive: {0}\ ({1:N2} GB free)" -f $bestDrive.DeviceID, $scratchFreeAfterGB)
Write-Host ("Workspace drive: {0} ({1:N2} GB free)" -f $workspaceRoot, $workspaceFreeAfterGB)
Write-Host "GRADLE_USER_HOME=$gradleHome"
Write-Host "TEMP/TMP=$tempRoot"
Write-Host "ANDROID_AVD_HOME=$avdHome"
Write-Host "app/build -> $appBuildTarget"
Write-Host "benchmark/build -> $benchmarkBuildTarget"

if ($scratchFreeAfterGB -lt $MinimumScratchGB) {
  throw ("Selected scratch drive {0}\ has only {1:N2} GB free; require >= {2:N2} GB" -f $bestDrive.DeviceID, $scratchFreeAfterGB, $MinimumScratchGB)
}

if ($workspaceFreeAfterGB -lt $MinimumWorkspaceGB) {
  throw ("Workspace drive {0} fell below safe headroom: {1:N2} GB free; require >= {2:N2} GB" -f $workspaceRoot, $workspaceFreeAfterGB, $MinimumWorkspaceGB)
}

Write-Host "CI scratch relocation: GREEN"
