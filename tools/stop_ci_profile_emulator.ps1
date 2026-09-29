$ErrorActionPreference = "Stop"
if (-not $env:VEIL_PROFILE_AVD -or -not $env:VEIL_PROFILE_SERIAL) { exit 0 }
$expected = "VeilReleaseProfile_$($env:GITHUB_RUN_ID)_$($env:GITHUB_RUN_ATTEMPT)"
if ($env:VEIL_PROFILE_AVD -ne $expected -or $env:VEIL_PROFILE_SERIAL -ne "emulator-5580") {
  throw "Refusing cleanup of an emulator not owned by this job"
}
if ($env:ANDROID_AVD_HOME -ne "E:\VeilReader-CI\avd") { throw "Unexpected AVD storage" }
$adb = Join-Path $env:ANDROID_SDK_ROOT "platform-tools\adb.exe"
$avdDir = Join-Path $env:ANDROID_AVD_HOME "$expected.avd"
$ini = Join-Path $env:ANDROID_AVD_HOME "$expected.ini"
$devices = @(& $adb devices)
if ($LASTEXITCODE -ne 0) { throw "Cannot inspect ADB before cleanup" }
if ($devices -match '^emulator-5580\s') {
  $name = @(& $adb -s $env:VEIL_PROFILE_SERIAL emu avd name)
  if ($LASTEXITCODE -ne 0 -or $name -notcontains $expected) { throw "Port belongs to another AVD; preserve it" }
  & $adb -s $env:VEIL_PROFILE_SERIAL emu kill
  if ($LASTEXITCODE -ne 0) { throw "Cannot stop owned profile emulator" }
  $deadline = (Get-Date).AddSeconds(30)
  do {
    Start-Sleep -Seconds 1
    $devices = @(& $adb devices)
    if ($LASTEXITCODE -ne 0) { throw "Cannot verify emulator shutdown" }
    if (-not ($devices -match '^emulator-5580\s')) { break }
  } while ((Get-Date) -lt $deadline)
  if ($devices -match '^emulator-5580\s') { throw "Emulator did not stop; preserve its files" }
}
foreach ($path in @($avdDir, $ini)) {
  if (Test-Path -LiteralPath $path) {
    $item = Get-Item -LiteralPath $path -Force
    if ($item.Attributes -band [IO.FileAttributes]::ReparsePoint) { throw "Refuse cleanup through reparse point: $path" }
    $bytes = if ($item.PSIsContainer) {
      (Get-ChildItem -LiteralPath $path -File -Recurse | Measure-Object Length -Sum).Sum
    } else { $item.Length }
    Write-Host "Cleanup inventory: path=$path bytes=$bytes drive=E: classification=disposable AVD created by this run"
    Remove-Item -LiteralPath $path -Recurse -Force
  }
}
