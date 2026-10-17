# Eyad Studio local-only MCP and Telegram setup.
# Must run on the user's Windows PC. No Telegram credentials are read or sent.
[CmdletBinding()]
param([string]$Root = 'E:\AI\Tools\EyadAgentBridge')
$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

if ($env:COMPUTERNAME -ne 'DESKTOP-GSV91KV') {
    throw 'Safety gate: this installer is for DESKTOP-GSV91KV only. Nothing installed.'
}
if (-not (Test-Path 'E:\')) {
    throw 'Safety gate: drive E: is unavailable.'
}
New-Item -ItemType Directory -Force -Path $Root | Out-Null
$log = Join-Path $Root 'installation-status.json'
$report = [ordered]@{ host=$env:COMPUTERNAME; root=$Root; started=(Get-Date -Format o); windowsMcp='pending'; desktopCommander='pending'; telethon='pending'; errors=@() }
function Write-Status {
    $report | ConvertTo-Json -Depth 4 | Set-Content -Path $log -Encoding UTF8
}
function Check-Exit([string]$name) {
    if ($LASTEXITCODE -ne 0) { throw "$name exited with code $LASTEXITCODE" }
}
Write-Status

$uv = Get-Command uv.exe -ErrorAction SilentlyContinue
if (-not $uv) {
    $u = Join-Path $env:APPDATA 'uv\bin\uv.exe'
    if (Test-Path $u) { $uv = Get-Item $u }
}
if (-not $uv) {
    foreach ($u in @("$env:USERPROFILE\.local\bin\uv.exe", 'E:\Tools\uv\uv.exe')) {
        if (Test-Path $u) { $uv = Get-Item $u; break }
    }
}
$python = Get-Command python.exe -ErrorAction SilentlyContinue
$npm = Get-Command npm.cmd -ErrorAction SilentlyContinue
$node = Get-Command node.exe -ErrorAction SilentlyContinue

# 1. Windows-MCP: isolated, pinned, stdio-only. Uses its required Python 3.13.
try {
    if (-not $uv) { throw 'uv.exe not found; install uv before Windows-MCP setup.' }
    $pyDir = Join-Path $Root 'python-runtimes'
    $env:UV_PYTHON_INSTALL_DIR = $pyDir
    & $uvCommand python install 3.13
    Check-Exit 'uv python install 3.13'
    $venv = Join-Path $Root 'windows-mcp\.venv'
    & $uvCommand venv --python 3.13 $venv
    Check-Exit 'uv venv'
    $exe = Join-Path $venv 'Scripts\python.exe'
    & $uvCommand pip install --python $exe 'windows-mcp==0.7.0'
    Check-Exit 'windows-mcp install'
    & $exe -c "import importlib.metadata; print(importlib.metadata.version('windows-mcp'))"
    Check-Exit 'Windows-MCP version check'
    $report.windowsMcp='installed'
} catch {
    $report.windowsMcp='failed'
    $report.errors += "windows-mcp: $($_.Exception.Message)"
}
Write-Status

# 2. Local Desktop Commander MCP: no remote service, separate npm folder.
try {
    if (-not $node -or -not $npm) { throw 'Node.js 22.12+ and npm are required.' }
    $nodeVersion = (& $node.Source -p "process.versions.node").Trim()
    $v = [version](($nodeVersion -split '-')[0])
    if ($v -lt [version]'22.12.0') { throw "Node.js >=22.12.0 required, found $nodeVersion" }
    $prefix = Join-Path $Root 'desktop-commander-mcp'
    New-Item -ItemType Directory -Force -Path $prefix | Out-Null
    & $npm.Source install --prefix $prefix --no-audit --no-fund '@wonderwhy-er/desktop-commander@latest'
    Check-Exit 'Desktop Commander npm install'
    $entry = Join-Path $prefix 'node_modules\@wonderwhy-er\desktop-commander\dist\index.js'
    if (-not (Test-Path $entry)) { throw "Missing entry point: $entry" }
    & $npm.Source list --prefix $prefix '@wonderwhy-er/desktop-commander' --depth=0
    Check-Exit 'Desktop Commander version check'
    $report.desktopCommander='installed'
} catch {
    $report.desktopCommander='failed'
    $report.errors += "desktop-commander: $($_.Exception.Message)"
}
Write-Status

# 3. Telegram Telethon: library only. Login requires explicit user authentication later.
try {
    if (-not $python) { throw 'Python is required for Telethon setup.' }
    $venv = Join-Path $Root 'telethon\.venv'
    & $python.Source -m venv $venv
    Check-Exit 'Telethon venv'
    $exe = Join-Path $venv 'Scripts\python.exe'
    & $exe -m pip install --disable-pip-version-check 'telethon>=1.45,<2'
    Check-Exit 'Telethon pip install'
    & $exe -c "import telethon; print('Telethon version:', telethon.__version__)"
    Check-Exit 'Telethon import test'
    $report.telethon='installed'
} catch {
    $report.telethon='failed'
    $report.errors += "telethon: $($_.Exception.Message)"
}
Write-Status

# Write integration examples only. Do NOT auto-modify credentials or AI app settings.
$wmcp = Join-Path $Root 'windows-mcp\.venv\Scripts\windows-mcp.exe'
$localMcp = Join-Path $Root 'desktop-commander-mcp\node_modules\@wonderwhy-er\desktop-commander\dist\index.js'
$config = [ordered]@{
  mcpServers = [ordered]@{
     'windows-mcp' = [ordered]@{ command=$wmcp; args=@('serve') }
     'desktop-commander-local' = [ordered]@{ command=if($node){$node.Source}else{'node'}; args=@($localMcp) }
  }
}
$config | ConvertTo-Json -Depth 6 | Set-Content -Path (Join-Path $Root 'mcp-servers.example.json') -Encoding UTF8

$telethonScript = @'
"""Upload an existing file to your own Telegram Saved Messages.
Use ONLY after privately authenticating with your own api_id/api_hash.
Never commit credentials or .session files to a repository.
"""
from pathlib import Path
import os, sys, asyncio
from telethon import TelegramClient

async def main():
    if len(sys.argv) != 2:
        raise SystemExit("Usage: python send_saved_messages.py path_to_video.mp4")
    video = Path(sys.argv[1]).expanduser().resolve()
    if not video.is_file():
        raise SystemExit("File not found: " + str(video))
    api_id = os.environ.get("TELEGRAM_API_ID")
    api_hash = os.environ.get("TELEGRAM_API_HASH")
    if not api_id or not api_hash:
        raise SystemExit("Set TELEGRAM_API_ID and TELEGRAM_API_HASH locally after creating your Telegram developer API application at my.telegram.org.")
    local = Path(os.environ.get("LOCALAPPDATA", str(Path.home()))) / "EyadStudio" / "Telegram"
    local.mkdir(parents=True, exist_ok=True)
    async with TelegramClient(str(local / "user-session"), int(api_id), api_hash) as client:
        me = await client.get_me()
        if not me:
            raise SystemExit("Telegram login was not completed.")
        message = await client.send_file("me", str(video), caption="EYAD STUDIO • documentary sample", supports_streaming=True)
        print("SAVED_MESSAGES_SENT message_id=", message.id)

if __name__ == "__main__":
    asyncio.run(main())
'@
$telethonScript | Set-Content -Path (Join-Path $Root 'send_saved_messages.py') -Encoding UTF8

$readme = @'
EYAD STUDIO LOCAL AGENT BRIDGE
Installed software is local-only (stdio MCP), not an online gateway.
- Windows-MCP: windows-mcp\.venv\Scripts\windows-mcp.exe serve
- Desktop Commander MCP: node desktop-commander-mcp\node_modules\@wonderwhy-er\desktop-commander\dist\index.js
- Telethon Python: telethon\.venv\Scripts\python.exe

mcp-servers.example.json is an example for a compatible desktop MCP host.
You must explicitly register these commands in the relevant user account for Gemini/Codex.
Windows GUI automation requires an interactive, unlocked desktop; Windows services cannot assume access.
Telegram does not reuse Telegram Desktop's sign-in. Developer API ID/hash + a one-time login are required.
Never paste your Telegram API hash, verification codes, or session file into chat or GitHub.

To send a file later (run from your own Windows session):
  set TELEGRAM_API_ID and TELEGRAM_API_HASH as local user environment variables
  & 'E:\AI\Tools\EyadAgentBridge\telethon\.venv\Scripts\python.exe' 'E:\AI\Tools\EyadAgentBridge\send_saved_messages.py' 'E:\AI\OpenMontage\projects\iran-midterms-2026-documentary\output_v2\EYAD_STUDIO_IRAN_US_ELECTIONS_DOCUMENTARY_V2_720P.mp4'

See installation-status.json for verified install state.
'@
$readme | Set-Content -Path (Join-Path $Root 'README.txt') -Encoding UTF8
$report.finished=(Get-Date -Format o)
Write-Status
Write-Output "EYAD_SETUP_REPORT_BEGIN"
Get-Content -Path $log
Write-Output "EYAD_SETUP_REPORT_END"
if ($report.errors.Count -gt 0) { throw 'Some installations failed. See installation-status.json.' }
Write-Output "EYAD_LOCAL_TOOLS_INSTALLED"
