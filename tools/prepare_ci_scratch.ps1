param([double]$MinimumScratchGB = 12)

$ErrorActionPreference = "Stop"

function Assert-EPath {
  param([string]$Name, [string]$Path)
  if (-not $Path -or [IO.Path]::GetPathRoot($Path) -ne "E:\") {
    throw "$Name must be on E: before any Android work: $Path"
  }
  $ancestor = $Path
  while ($ancestor) {
    if (Test-Path -LiteralPath $ancestor) {
      $item = Get-Item -LiteralPath $ancestor -Force
      if ($item.Attributes -band [IO.FileAttributes]::ReparsePoint) {
        throw "Inspect and migrate reparse point before building: $ancestor -> $($item.Target)"
      }
    }
    $ancestor = Split-Path -Path $ancestor -Parent
  }
}

function Set-GithubEnv {
  param([string]$Name, [string]$Value)
  if (-not $env:GITHUB_ENV) { throw "GITHUB_ENV is not available" }
  "$Name=$Value" | Out-File -FilePath $env:GITHUB_ENV -Append -Encoding utf8
  Set-Item -Path "Env:$Name" -Value $Value
}

Get-CimInstance Win32_LogicalDisk -Filter "DriveType=3" |
  Select-Object DeviceID,FreeSpace,Size | Format-Table -AutoSize
$drive = Get-CimInstance Win32_LogicalDisk -Filter "DeviceID='E:'"
if (-not $drive -or ([int64]$drive.FreeSpace / 1GB) -lt $MinimumScratchGB) {
  throw "E: must be available with at least $MinimumScratchGB GB free"
}

Assert-EPath "workspace" $env:GITHUB_WORKSPACE
Assert-EPath "runner temp" $env:RUNNER_TEMP
Assert-EPath "tool cache" $env:RUNNER_TOOL_CACHE
$ciRoot = "E:\VeilReader-CI"
$tempRoot = Join-Path $ciRoot ("temp\{0}-{1}-{2}" -f $env:GITHUB_RUN_ID,$env:GITHUB_RUN_ATTEMPT,$env:GITHUB_JOB)
$paths = [ordered]@{
  GRADLE_USER_HOME = (Join-Path $ciRoot "gradle-home")
  TEMP = $tempRoot
  TMP = $tempRoot
  ANDROID_USER_HOME = (Join-Path $ciRoot "android-user")
  ANDROID_AVD_HOME = (Join-Path $ciRoot "avd")
}
foreach ($entry in $paths.GetEnumerator()) {
  Assert-EPath $entry.Key $entry.Value
  New-Item -ItemType Directory -Force -Path $entry.Value | Out-Null
  Set-GithubEnv $entry.Key $entry.Value
}
Set-GithubEnv "VEIL_CI_ROOT" $ciRoot
Set-GithubEnv "VEIL_CI_SCRATCH_DRIVE" "E:"

# Builds stay inside the E: checkout. Preserve previous outputs/caches; never
# delete a run directory shared with the preceding preflight job.
$inventory = @(
  @{Name="workspace"; Path=$env:GITHUB_WORKSPACE; Class="required source"},
  @{Name="runner temp"; Path=$env:RUNNER_TEMP; Class="CI temp; inventory only"},
  @{Name="tool cache"; Path=$env:RUNNER_TOOL_CACHE; Class="rebuildable cache"},
  @{Name="Gradle cache"; Path=$env:GRADLE_USER_HOME; Class="rebuildable cache"},
  @{Name="AVD"; Path=$env:ANDROID_AVD_HOME; Class="unknown; preserve"},
  @{Name="project Gradle"; Path=(Join-Path $env:GITHUB_WORKSPACE ".gradle"); Class="rebuildable cache"},
  @{Name="root build"; Path=(Join-Path $env:GITHUB_WORKSPACE "build"); Class="rebuildable"},
  @{Name="app build"; Path=(Join-Path $env:GITHUB_WORKSPACE "app\build"); Class="rebuildable; preserve evidence"},
  @{Name="benchmark build"; Path=(Join-Path $env:GITHUB_WORKSPACE "benchmark\build"); Class="rebuildable; preserve evidence"}
)
foreach ($entry in $inventory) {
  Assert-EPath $entry.Name $entry.Path
  $bytes = 0
  if (Test-Path -LiteralPath $entry.Path) {
    $bytes = (Get-ChildItem -LiteralPath $entry.Path -File -Recurse -Force -ErrorAction Stop |
      Measure-Object -Property Length -Sum).Sum
  }
  Write-Host ("{0}: path={1} bytes={2} classification={3}" -f $entry.Name,$entry.Path,$bytes,$entry.Class)
}
Write-Host "E-drive storage preflight passed; no files deleted or relocated."
