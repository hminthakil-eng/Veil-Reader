param(
    [ValidateSet("status", "unit-tests", "full-ci")]
    [string]$Command = "status"
)

$ErrorActionPreference = "Stop"

Write-Output "Eyad Studio Company OS - Veil Reader mobile command"
Write-Output "command=$Command"
Write-Output "repo=$env:GITHUB_REPOSITORY"
Write-Output "ref=$env:GITHUB_REF_NAME"
Write-Output "sha=$env:GITHUB_SHA"

function Invoke-PolicyChecks {
    $bash = Join-Path $env:ProgramFiles "Git\bin\bash.exe"
    if (-not (Test-Path $bash)) {
        throw "Git Bash not found at $bash"
    }
    & $bash "tools/test-policy.sh"
    if ($LASTEXITCODE -ne 0) { throw "Reading policy checks failed." }

    python tools/test_performance_budget.py
    if ($LASTEXITCODE -ne 0) { throw "Performance budget parser checks failed." }
    python tools/test_performance_delta.py
    if ($LASTEXITCODE -ne 0) { throw "Performance delta parser checks failed." }
    python tools/test_performance_dashboard.py
    if ($LASTEXITCODE -ne 0) { throw "Performance dashboard checks failed." }
}

switch ($Command) {
    "status" {
        java -version
        gradle --version
        android sdk list
    }
    "unit-tests" {
        Invoke-PolicyChecks
        gradle :app:testDebugUnitTest --stacktrace
        if ($LASTEXITCODE -ne 0) { throw "Unit tests failed." }
    }
    "full-ci" {
        Invoke-PolicyChecks
        gradle :app:testDebugUnitTest --stacktrace
        if ($LASTEXITCODE -ne 0) { throw "Unit tests failed." }
        git diff --exit-code -- app/schemas/
        if ($LASTEXITCODE -ne 0) { throw "Room schema drift detected." }
        gradle :app:lintDebug --stacktrace
        if ($LASTEXITCODE -ne 0) { throw "Android lint failed." }
        gradle :app:assembleDebug --stacktrace
        if ($LASTEXITCODE -ne 0) { throw "Debug APK build failed." }
        gradle :benchmark:assembleBenchmarkBenchmark --stacktrace
        if ($LASTEXITCODE -ne 0) { throw "Performance harness compile failed." }
    }
}
