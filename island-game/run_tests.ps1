param(
    [string]$GodotExe = 'E:\EyadStudio\Tools\Godot\4.7.2\Godot_v4.7.2-stable_win64_console.exe'
)
$ErrorActionPreference='Stop'
if (!(Test-Path -LiteralPath $GodotExe)) {throw "Godot missing: $GodotExe"}
$project = Split-Path -Parent $MyInvocation.MyCommand.Path
Write-Output '=== Godot version ==='
& $GodotExe --headless --version
if ($LASTEXITCODE -ne 0) {exit $LASTEXITCODE}
Write-Output '=== Clean import / compile ==='
& $GodotExe --headless --editor --path $project --import --quit
if ($LASTEXITCODE -ne 0) {exit $LASTEXITCODE}
Write-Output '=== Headless gameplay tests ==='
& $GodotExe --headless --path $project --script 'res://tests/run_headless_tests.gd'
if ($LASTEXITCODE -ne 0) {exit $LASTEXITCODE}
Write-Output '=== Scene startup ==='
& $GodotExe --headless --path $project --quit-after 3
if ($LASTEXITCODE -ne 0) {exit $LASTEXITCODE}
Write-Output 'ISLAND_GODOT_GATE PASS (headless only; not a device test)'
