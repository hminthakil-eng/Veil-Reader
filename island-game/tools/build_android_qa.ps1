param(
    [string]$Godot = 'E:\EyadStudio\Tools\Godot\4.7.2\Godot_v4.7.2-stable_win64_console.exe',
    [string]$AndroidSdk = 'C:\Users\Microsoft\AppData\Local\Android\Sdk',
    [string]$JavaSdk = 'C:\Users\Microsoft\AppData\Local\EyadDevTools\jdk-17'
)
$ErrorActionPreference = 'Stop'
$project = Split-Path -Parent $PSScriptRoot
$package = 'org.eyadstudio.islandbookshop.qa'
$template = Join-Path $env:APPDATA 'Godot\export_templates\4.7.2.stable\android_debug.apk'
foreach ($required in @($Godot, $template, "$AndroidSdk\platform-tools\adb.exe",
        "$JavaSdk\bin\java.exe", "$project\export_presets.cfg")) {
    if (!(Test-Path -LiteralPath $required)) { throw "Android export prerequisite missing: $required" }
}
$env:JAVA_HOME = $JavaSdk
$env:ANDROID_HOME = $AndroidSdk
$env:ANDROID_SDK_ROOT = $AndroidSdk
$env:PATH = "$JavaSdk\bin;$AndroidSdk\platform-tools;$env:PATH"
$target = Join-Path $project 'build\IslandBookshop-QA.apk'
New-Item -ItemType Directory -Force -Path (Split-Path $target -Parent) | Out-Null
Write-Output ('EXPORT_DEBUG_START '+$project)
& $Godot --headless --path $project --export-debug 'Android Debug QA' $target
if ($LASTEXITCODE -ne 0) { throw "Godot Android export exit $LASTEXITCODE" }
if (!(Test-Path -LiteralPath $target)) { throw 'Godot exited without an APK output' }
if ((Get-Item -LiteralPath $target).Length -lt 1000000) { throw 'APK implausibly small' }
$buildTool = Join-Path $AndroidSdk 'build-tools\35.0.1\aapt.exe'
if (!(Test-Path -LiteralPath $buildTool)) { throw 'AAPT from Android Build Tools 35.0.1 is required for manifest verification' }
$badging = (& $buildTool dump badging $target 2>&1) -join "`n"
$perms = (& $buildTool dump permissions $target 2>&1) -join "`n"
if ($LASTEXITCODE -ne 0) { throw 'Manifest permissions audit failed' }
if ($badging -notmatch [regex]::Escape($package)) { throw 'Unexpected package name in APK' }
$forbidden = @('android.permission.INTERNET','android.permission.READ_EXTERNAL_STORAGE',
    'android.permission.WRITE_EXTERNAL_STORAGE','android.permission.CAMERA',
    'android.permission.RECORD_AUDIO','android.permission.ACCESS_FINE_LOCATION')
foreach ($permission in $forbidden) {
    if ($perms.Contains($permission)) { throw "Unexpected permission in Android manifest: $permission" }
}
Write-Output ('APK_QA_READY '+$target)
Write-Output ('APK_BYTES '+(Get-Item $target).Length)
Write-Output ('APK_SHA256 '+(Get-FileHash -LiteralPath $target -Algorithm SHA256).Hash)
Write-Output 'ANDROID_MANIFEST_PERMISSION_AUDIT PASS'
