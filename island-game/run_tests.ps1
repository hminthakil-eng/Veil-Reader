param(
    [string]$GodotExe = 'E:\EyadStudio\Tools\Godot\4.7.2\Godot_v4.7.2-stable_win64_console.exe'
)
$ErrorActionPreference = 'Stop'
if (!(Test-Path -LiteralPath $GodotExe)) { throw "Godot missing: $GodotExe" }
$project = Split-Path -Parent $MyInvocation.MyCommand.Path

function Invoke-GodotGate {
    param([string]$Title, [string[]]$ArgsList, [string]$ExpectedMarker = '')
    Write-Output ("=== " + $Title + " ===")
    $out = [System.IO.Path]::GetTempFileName()
    $err = [System.IO.Path]::GetTempFileName()
    try {
        $p = Start-Process -FilePath $GodotExe -ArgumentList $ArgsList -NoNewWindow -Wait -PassThru -RedirectStandardOutput $out -RedirectStandardError $err
        $standard = [System.IO.File]::ReadAllText($out)
        $errors = [System.IO.File]::ReadAllText($err)
        Write-Output $standard
        if ($errors) { Write-Output $errors }
        $combined = $standard + [Environment]::NewLine + $errors
        if ($p.ExitCode -ne 0) {
            throw "Godot gate '$Title' exited with $($p.ExitCode)."
        }
        if ($combined -match '(?i)SCRIPT ERROR:|ERROR: FAIL|Parse Error:|Parser Error:') {
            throw "Godot gate '$Title' printed an engine/script failure, despite its exit code."
        }
        if ($ExpectedMarker -and !$combined.Contains($ExpectedMarker)) {
            throw "Godot gate '$Title' did not reach expected test summary: $ExpectedMarker"
        }
    } finally {
        Remove-Item -LiteralPath $out,$err -Force -ErrorAction SilentlyContinue
    }
}

Invoke-GodotGate 'Godot version' @('--headless', '--version') '4.7.2.stable.official'
Invoke-GodotGate 'Clean source import' @('--headless', '--editor', '--path', $project, '--import', '--quit')
Invoke-GodotGate 'Legacy fictional book + clock tests' @('--headless', '--path', $project, '--script', 'res://tests/run_headless_tests.gd') 'ISLAND_TEST_RESULTS passes=19 failures=0'
Invoke-GodotGate 'Legacy main scene startup' @('--headless', '--path', $project, '--quit-after', '3')
Invoke-GodotGate 'Town economy financial invariants' @('--headless', '--path', $project, '--script', 'res://tests/run_economy_tests.gd') 'ISLAND_ECONOMY_TEST_RESULTS passes=30 failures=0'
Invoke-GodotGate 'Existing town economy preview startup' @('--headless', '--path', $project, '--scene', 'res://scenes/economy_proof.tscn', '--quit-after', '3')
Invoke-GodotGate 'New single-authority world commerce session' @('--headless', '--path', $project, '--script', 'res://tests/run_unified_commerce_tests.gd') 'WORLD_COMMERCE_TEST_RESULTS passes=25 failures=0'
Write-Output 'ISLAND_GODOT_GATE PASS - 19 legacy shop + 30 legacy economy + 25 new world commerce = 74 passing assertions. Headless only, no device QA.'
