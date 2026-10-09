param(
    [string]$GameDirectory = 'E:\Minecraft\cyhj0\.minecraft',
    [string]$InstanceName = 'HBUCM_Client'
)

$ErrorActionPreference = 'Stop'
$projectRoot = [System.IO.Path]::GetFullPath($PSScriptRoot)
$libraryRoot = Join-Path $GameDirectory 'libraries'
$buildRoot = Join-Path $projectRoot 'build'
$classesRoot = [System.IO.Path]::GetFullPath((Join-Path $buildRoot 'classes'))
$outputRoot = Join-Path $buildRoot 'libs'

$jars = New-Object 'System.Collections.Generic.List[string]'
$requiredJars = @(
    'net\neoforged\neoforge\21.1.249\neoforge-21.1.249-client.jar',
    'net\neoforged\neoforge\21.1.249\neoforge-21.1.249-universal.jar',
    'net\minecraft\client\1.21.1-20240808.144430\client-1.21.1-20240808.144430-srg.jar'
)
foreach ($relativePath in $requiredJars) {
    $jarPath = Join-Path $libraryRoot $relativePath
    if (-not (Test-Path -LiteralPath $jarPath)) { throw "Missing dependency: $jarPath" }
    $jars.Add($jarPath)
}
$manifests = @(
    (Join-Path $GameDirectory "versions\$InstanceName\$InstanceName.json"),
    (Join-Path $GameDirectory 'versions\1.21.1\1.21.1.json')
)
foreach ($manifestPath in $manifests) {
    $manifest = Get-Content -LiteralPath $manifestPath -Raw -Encoding UTF8 | ConvertFrom-Json
    foreach ($library in $manifest.libraries) {
        $relativePath = $library.downloads.artifact.path
        if (-not $relativePath -or $relativePath -match 'natives') { continue }
        $jarPath = Join-Path $libraryRoot $relativePath
        if (Test-Path -LiteralPath $jarPath) { $jars.Add($jarPath) }
    }
}
$classpath = ($jars | Select-Object -Unique) -join ';'

# Only clean the resolved classes directory inside this project.
$expectedPrefix = $projectRoot.TrimEnd('\') + '\'
if (-not $classesRoot.StartsWith($expectedPrefix, [StringComparison]::OrdinalIgnoreCase)) {
    throw "Build output is outside the project: $classesRoot"
}
if (Test-Path -LiteralPath $classesRoot) {
    $classesEntry = Get-Item -LiteralPath $classesRoot
    if ($classesEntry.Attributes -band [System.IO.FileAttributes]::ReparsePoint) {
        throw "Refusing to clean a linked directory: $classesRoot"
    }
    Remove-Item -LiteralPath $classesRoot -Recurse -Force
}
New-Item -ItemType Directory -Force -Path $classesRoot,$outputRoot | Out-Null

$sources = @(Get-ChildItem -LiteralPath (Join-Path $projectRoot 'src\main\java') -Recurse -Filter '*.java')
$arguments = New-Object 'System.Collections.Generic.List[string]'
$arguments.Add('-cp')
$arguments.Add('"' + $classpath.Replace('\', '/') + '"')
foreach ($source in $sources) {
    $arguments.Add('"' + $source.FullName.Replace('\', '/') + '"')
}
$argumentsPath = Join-Path $buildRoot 'javac-args.txt'
[System.IO.File]::WriteAllLines($argumentsPath, $arguments, (New-Object System.Text.UTF8Encoding($false)))
[System.IO.File]::WriteAllText((Join-Path $buildRoot 'classpath.txt'), $classpath,
    (New-Object System.Text.UTF8Encoding($false)))

& javac --release 21 -encoding UTF-8 -proc:none -d $classesRoot "@$argumentsPath"
if ($LASTEXITCODE -ne 0) { throw "javac failed: $LASTEXITCODE" }

$artifact = Join-Path $outputRoot 'bencao-0.1.0.jar'
& jar --create --file $artifact -C $classesRoot . -C (Join-Path $projectRoot 'src\main\resources') . -C $projectRoot LICENSE
if ($LASTEXITCODE -ne 0) { throw "jar failed: $LASTEXITCODE" }
Write-Output "Built $artifact ($($sources.Count) Java sources)"
