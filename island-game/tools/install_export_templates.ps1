param(
    [string]$ZipFile = 'E:\EyadStudio\Tools\Godot\4.7.2\exports\Godot_v4.7.2-stable_export_templates.tpz'
)
$ErrorActionPreference = 'Stop'
$sha = 'f298490b8d44d934be425a5a65a51bf15f422428b229a06a6e11d9ffea248011'
if (!(Test-Path -LiteralPath $ZipFile)) { throw "Download not completed: $ZipFile" }
if ((Get-Item -LiteralPath $ZipFile).Length -ne 1281349702) {
    throw 'Incomplete official Godot 4.7.2 standard template archive; refusing install.'
}
$actualHash = (Get-FileHash -LiteralPath $ZipFile -Algorithm SHA256).Hash.ToLowerInvariant()
if ($actualHash -ne $sha) { throw 'Godot template SHA-256 mismatch; refusing install.' }
Add-Type -AssemblyName System.IO.Compression
Add-Type -AssemblyName System.IO.Compression.FileSystem
$target = Join-Path $env:APPDATA 'Godot\export_templates\4.7.2.stable'
New-Item -ItemType Directory -Force -Path $target | Out-Null
$zip = [System.IO.Compression.ZipFile]::OpenRead($ZipFile)
try {
    foreach ($name in @('android_debug.apk','android_release.apk')) {
        $matches = @($zip.Entries | Where-Object { $_.Name -eq $name })
        if ($matches.Count -ne 1) { throw "Missing or duplicate Android template $name" }
        $dest = Join-Path $target $name
        [System.IO.Compression.ZipFileExtensions]::ExtractToFile($matches[0], $dest, $true)
        if ((Get-Item -LiteralPath $dest).Length -lt 1000000) {
            throw "Unexpectedly small $name"
        }
        Write-Output ("TEMPLATE_READY " + $dest + " " + (Get-Item $dest).Length)
    }
    [IO.File]::WriteAllText((Join-Path $target 'version.txt'), "4.7.2.stable", [Text.UTF8Encoding]::new($false))
} finally {
    $zip.Dispose()
}
Write-Output ('TEMPLATE_GATE_READY '+$target)
