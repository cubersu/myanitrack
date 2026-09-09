param([string]$Apk = 'app/build/outputs/apk/release/app-release-unsigned.apk')
$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.IO.Compression.FileSystem
$path = Join-Path (Split-Path $PSScriptRoot -Parent) $Apk
$zip = [IO.Compression.ZipFile]::OpenRead($path)
$failures = 0
try {
    foreach ($entry in $zip.Entries | Where-Object { $_.FullName.EndsWith('.so') }) {
        $stream = $entry.Open(); $memory = [IO.MemoryStream]::new()
        $stream.CopyTo($memory); $stream.Dispose(); $bytes = $memory.ToArray(); $memory.Dispose()
        $is64 = $bytes[4] -eq 2
        $offset = if ($is64) { [BitConverter]::ToUInt64($bytes,32) } else { [BitConverter]::ToUInt32($bytes,28) }
        $size = [BitConverter]::ToUInt16($bytes, $(if ($is64) {54} else {42}))
        $count = [BitConverter]::ToUInt16($bytes, $(if ($is64) {56} else {44}))
        $valid = $true
        for ($i=0; $i -lt $count; $i++) {
            $position = [int]($offset + $i * $size)
            if ([BitConverter]::ToUInt32($bytes,$position) -eq 1) {
                $align = if ($is64) { [BitConverter]::ToUInt64($bytes,$position+48) } else { [BitConverter]::ToUInt32($bytes,$position+28) }
                if ($align -lt 16384) { $valid = $false }
            }
        }
        if (!$valid) { $failures++ }
        Write-Output "$($entry.FullName): 16KB ELF segment alignment = $valid"
    }
} finally { $zip.Dispose() }
if ($failures -gt 0) { throw "$failures native libraries require review for 16 KB pages" }
