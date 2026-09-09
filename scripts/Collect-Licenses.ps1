param([string]$GradleCache = "$env:USERPROFILE/.gradle/caches/modules-2/files-2.1")
$ErrorActionPreference = 'Stop'
$workspace = Split-Path $PSScriptRoot -Parent
$inventory = Join-Path $workspace 'app/build/reports/release-dependencies.tsv'
$notice = [Collections.Generic.List[string]]::new()
$missing = [Collections.Generic.List[string]]::new()
$notice.Add('MyAniTrack - third-party dependencies / third-party notices')
$notice.Add('Generated from the resolved release runtime dependency graph. Project modules and test-only libraries are excluded.')
$notice.Add('')
function Read-PomLicense([string]$group, [string]$artifact, [string]$version, [int]$depth = 0) {
    if ($depth -gt 5) { return @() }
    $directory = Join-Path $GradleCache "$group/$artifact/$version"
    $pom = Get-ChildItem -LiteralPath $directory -Recurse -Filter '*.pom' -ErrorAction SilentlyContinue | Select-Object -First 1
    if (!$pom) {
        if ($group.StartsWith('androidx.') -or $group -eq 'com.google.guava' -or $group -eq 'org.jspecify') {
            return @('Apache-2.0 - https://www.apache.org/licenses/LICENSE-2.0.txt (upstream project license)')
        }
        return @()
    }
    [xml]$xml = [IO.File]::ReadAllText($pom.FullName)
    $licenses = @($xml.SelectNodes("//*[local-name()='licenses']/*[local-name()='license']") | ForEach-Object { "$($_.name) - $($_.url)" })
    if ($licenses.Count -gt 0) { return $licenses }
    $parent = $xml.SelectSingleNode("/*[local-name()='project']/*[local-name()='parent']")
    if ($parent) { return Read-PomLicense $parent.groupId $parent.artifactId $parent.version ($depth + 1) }
    return @()
}
foreach ($line in [IO.File]::ReadAllLines($inventory)) {
    $parts = $line.Split("`t")
    $coordinate = $parts -join ':'
    $licenses = @(Read-PomLicense $parts[0] $parts[1] $parts[2])
    $notice.Add($coordinate)
    if ($licenses.Count -eq 0) { $missing.Add($coordinate); $notice.Add('License metadata requires review.') }
    else { $licenses | ForEach-Object { $notice.Add($_) } }
    $notice.Add('')
}
$target = Join-Path $workspace 'core/ui/src/main/assets/legal/licenses.txt'
Add-Type -AssemblyName System.IO.Compression.FileSystem
$seenNotices = [Collections.Generic.HashSet[string]]::new()
foreach ($line in [IO.File]::ReadAllLines($inventory)) {
    $parts = $line.Split("`t")
    $directory = Join-Path $GradleCache ($parts -join '/')
    Get-ChildItem -LiteralPath $directory -Recurse -File -ErrorAction SilentlyContinue | Where-Object { $_.Extension -in @('.jar', '.aar') } | ForEach-Object {
        $archive = [IO.Compression.ZipFile]::OpenRead($_.FullName)
        try {
            foreach ($entry in $archive.Entries) {
                if ($entry.FullName -match '(?i)(^|/)(LICENSE|NOTICE|COPYING)([._-].*)?$' -and $entry.Length -gt 0 -and $entry.Length -lt 200000) {
                    $reader = [IO.StreamReader]::new($entry.Open())
                    try { $body = $reader.ReadToEnd() } finally { $reader.Dispose() }
                    if ($seenNotices.Add($body)) { $notice.Add("`nEmbedded notice: $($parts -join ':') / $($entry.FullName)`n$body") }
                }
            }
        } finally { $archive.Dispose() }
    }
}
$licenseDirectory = Join-Path $workspace 'distribution/licenses'
if (Test-Path $licenseDirectory) {
    Get-ChildItem -LiteralPath $licenseDirectory -File -Filter '*.txt' | Sort-Object Name | ForEach-Object {
        $notice.Add("`n========== $($_.BaseName) ==========`n")
        $notice.Add([IO.File]::ReadAllText($_.FullName))
    }
}
[IO.File]::WriteAllLines($target, $notice, [Text.UTF8Encoding]::new($false))
[IO.File]::WriteAllLines((Join-Path $workspace 'build/unresolved-licenses.txt'), $missing)
Write-Output "Dependency count: $(([IO.File]::ReadAllLines($inventory)).Count); unresolved license metadata: $($missing.Count)"
