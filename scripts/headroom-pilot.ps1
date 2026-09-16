param(
    [ValidateSet('Check','Install','Run','Stats','Rollback')]
    [string]$Mode = 'Check'
)

$ErrorActionPreference = 'Stop'
$repoRoot = Resolve-Path (Join-Path $PSScriptRoot '..')
Set-Location $repoRoot

function Has-Command([string]$Name) {
    return $null -ne (Get-Command $Name -ErrorAction SilentlyContinue)
}

function Require-Headroom {
    if (-not (Has-Command 'headroom')) {
        throw 'Headroom CLI is not installed or not on PATH. Run this script with -Mode Install first.'
    }
}

Write-Host "Eyad Studio / Veil Reader Headroom pilot — mode: $Mode"
Write-Host "Repo: $repoRoot"

switch ($Mode) {
    'Check' {
        Write-Host "git:      $((git --version) 2>$null)"
        if (Has-Command 'uv') { Write-Host "uv:       $((uv --version) 2>$null)" } else { Write-Host 'uv:       not found' }
        if (Has-Command 'python') { Write-Host "python:   $((python --version) 2>&1)" } elseif (Has-Command 'py') { Write-Host "python:   $((py --version) 2>&1)" } else { Write-Host 'python:   not found' }
        if (Has-Command 'headroom') {
            Write-Host "headroom: $((headroom --version) 2>&1)"
            headroom doctor
        } else {
            Write-Host 'headroom: not installed'
        }
        Write-Host 'No files were changed by this check.'
    }

    'Install' {
        if (Has-Command 'headroom') {
            Write-Host 'Headroom is already installed. Running health check.'
            headroom doctor
            break
        }

        if (Has-Command 'uv') {
            Write-Host 'Installing Headroom in an isolated uv tool environment...'
            uv tool install --python 3.13 'headroom-ai[all]'
        }
        elseif (Has-Command 'py') {
            Write-Host 'uv is unavailable. Using Python 3.13 pip fallback...'
            py -3.13 -m pip install --user --upgrade 'headroom-ai[all]'
        }
        elseif (Has-Command 'python') {
            Write-Host 'uv/py launcher unavailable. Using current Python pip as a fallback...'
            python -m pip install --user --upgrade 'headroom-ai[all]'
        }
        else {
            throw 'Neither uv nor Python is available. Install Python/uv before continuing.'
        }

        if (-not (Has-Command 'headroom')) {
            Write-Warning 'Installation completed but the headroom executable is not visible in this shell PATH. Open a new PowerShell window and rerun -Mode Check.'
            break
        }

        headroom doctor
        Write-Host 'Headroom installed. No Veil Reader application files were modified.'
    }

    'Run' {
        Require-Headroom
        Write-Host 'Launching Codex through Headroom with optional code-memory integration disabled for this pilot.'
        Write-Host 'Exit Codex normally when the fixed evaluation task is complete.'
        headroom wrap codex --code-memory none
    }

    'Stats' {
        Require-Headroom
        Write-Host 'Headroom savings:'
        headroom savings
        Write-Host 'Headroom performance:'
        headroom perf
    }

    'Rollback' {
        Require-Headroom
        Write-Host 'Removing persistent Headroom wrapping from Codex...'
        headroom unwrap codex
        Write-Host 'Rollback command completed. Run Codex normally to confirm restoration.'
    }
}
