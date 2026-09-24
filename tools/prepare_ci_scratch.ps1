param(
  [double]$MinimumScratchGB = 12
)

$ErrorActionPreference = "Stop"

function Remove-CiDirectory {
  param([Parameter(Mandatory=$true)][string]$Path)

  if (-not (Test-Path -LiteralPath $Path)) { return }

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

  if (-not $env:GITHUB_ENV) { throw "GITHUB_ENV is not available" }
  "$Name=$Value" | Out-File -FilePath $env:GITHUB_ENV -Append -Encoding utf8
  Set-Item -Path "Env:$Name" -Value $Value
}

$targetDrive = Get-CimInstance Win32_LogicalDisk -Filter "DriveType=3" |
  Where-Object { $_.DeviceID -eq "E:" } |
  Select-Object -First 1

if (-not $targetDrive) {
  throw "Required CI drive E: is not available"
}

$freeGB = [int64]$targetDrive.FreeSpace / 1GB
Write-Host ("Pinned CI drive: E:\ free={0:N2} GB size={1:N2} GB" -f $freeGB, ([int64]$targetDrive.Size / 1GB))

if ($freeGB -lt $MinimumScratchGB) {
  throw ("E:\ has only {0:N2} GB free; require >= {1:N2} GB" -f $freeGB, $MinimumScratchGB)
}

$ciRoot = "E:\VeilReader-CI"
$runRoot = Join-Path $ciRoot ("runs\{0}" -f $env:GITHUB_RUN_ID)
$gradleHome = Join-Path $ciRoot "gradle-home"
$tempRoot = Join-Path $ciRoot ("temp\{0}-{1}" -f $env:GITHUB_RUN_ID, $env:GITHUB_JOB)
$avdHome = Join-Path $ciRoot "avd"
$appBuildTarget = Join-Path $runRoot "app-build"
$benchmarkBuildTarget = Join-Path $runRoot "benchmark-build"
$projectGradleTarget = Join-Path $runRoot "project-gradle"

New-Item -ItemType Directory -Force -Path $ciRoot,$gradleHome,$tempRoot,$avdHome | Out-Null

if (Test-Path -LiteralPath $runRoot) {
  Remove-Item -LiteralPath $runRoot -Recurse -Force -ErrorAction Stop
}
New-Item -ItemType Directory -Force -Path $appBuildTarget,$benchmarkBuildTarget,$projectGradleTarget | Out-Null

$staleCutoff = (Get-Date).AddHours(-24)
foreach ($parent in @((Join-Path $ciRoot "runs"), (Join-Path $ciRoot "temp"))) {
  if (Test-Path -LiteralPath $parent) {
    Get-ChildItem -LiteralPath $parent -Force -ErrorAction SilentlyContinue |
      Where-Object { $_.FullName -ne $runRoot -and $_.FullName -ne $tempRoot -and $_.LastWriteTime -lt $staleCutoff } |
      ForEach-Object {
        Write-Host "Removing stale E-drive CI scratch: $($_.FullName)"
        Remove-Item -LiteralPath $_.FullName -Recurse -Force -ErrorAction SilentlyContinue
      }
  }
}

$appBuildLink = Join-Path (Get-Location).Path "app\build"
$benchmarkBuildLink = Join-Path (Get-Location).Path "benchmark\build"
$projectGradleLink = Join-Path (Get-Location).Path ".gradle"

Remove-CiDirectory -Path $appBuildLink
Remove-CiDirectory -Path $benchmarkBuildLink
Remove-CiDirectory -Path $projectGradleLink

New-Item -ItemType Junction -Path $appBuildLink -Target $appBuildTarget | Out-Null
New-Item -ItemType Junction -Path $benchmarkBuildLink -Target $benchmarkBuildTarget | Out-Null
New-Item -ItemType Junction -Path $projectGradleLink -Target $projectGradleTarget | Out-Null

Set-GithubEnv -Name "VEIL_CI_ROOT" -Value $ciRoot
Set-GithubEnv -Name "VEIL_CI_SCRATCH_DRIVE" -Value "E:"
Set-GithubEnv -Name "GRADLE_USER_HOME" -Value $gradleHome
Set-GithubEnv -Name "TEMP" -Value $tempRoot
Set-GithubEnv -Name "TMP" -Value $tempRoot
Set-GithubEnv -Name "ANDROID_AVD_HOME" -Value $avdHome

$after = Get-CimInstance Win32_LogicalDisk -Filter "DriveType=3" |
  Where-Object { $_.DeviceID -eq "E:" } |
  Select-Object -First 1
$freeAfterGB = [int64]$after.FreeSpace / 1GB

Write-Host ("E:\ free after CI setup={0:N2} GB" -f $freeAfterGB)
Write-Host "GRADLE_USER_HOME=$gradleHome"
Write-Host "TEMP/TMP=$tempRoot"
Write-Host "ANDROID_AVD_HOME=$avdHome"
Write-Host "app/build -> $appBuildTarget"
Write-Host "benchmark/build -> $benchmarkBuildTarget"
Write-Host ".gradle -> $projectGradleTarget"

if ($freeAfterGB -lt $MinimumScratchGB) {
  throw ("E:\ fell below required CI headroom: {0:N2} GB free; require >= {1:N2} GB" -f $freeAfterGB, $MinimumScratchGB)
}

Write-Host "E-drive CI relocation: GREEN"
