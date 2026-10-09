param([string]$ServerRoot = 'E:\Minecraft\cyhj0_server\ServerFiles-3.0')

$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$testRoot = Join-Path $projectRoot 'build\smoke'
$classesRoot = Join-Path $projectRoot 'build\test-classes'
$resourceRoot = Join-Path $projectRoot 'build\test-resources'
$modRoot = Join-Path $testRoot 'mods'
$classpath = (Get-Content -LiteralPath (Join-Path $projectRoot 'build\classpath.txt') -Raw) +
    ';' + (Join-Path $projectRoot 'build\libs\bencao-0.1.0.jar')

# Reuse this user's existing accepted server EULA; never set acceptance here.
$eula = Join-Path $ServerRoot 'eula.txt'
if (-not (Test-Path -LiteralPath $eula) -or
    -not (Select-String -LiteralPath $eula -Pattern '^eula=true\s*$' -Quiet)) {
    throw 'The existing server must already have an accepted eula.txt.'
}
New-Item -ItemType Directory -Force -Path $testRoot,$classesRoot,$modRoot,(Join-Path $resourceRoot 'META-INF') | Out-Null
Copy-Item -LiteralPath $eula -Destination (Join-Path $testRoot 'eula.txt')
Copy-Item -LiteralPath (Join-Path $projectRoot 'build\libs\bencao-0.1.0.jar') -Destination $modRoot

$utf8 = New-Object System.Text.UTF8Encoding($false)
$metadata = @'
modLoader="javafml"
loaderVersion="[4,)"
license="MIT"
[[mods]]
modId="bencao_smoke_tests"
version="1.0.0"
displayName="Bencao Smoke Tests"
[[dependencies.bencao_smoke_tests]]
modId="bencao"
type="required"
versionRange="[0.1.0]"
ordering="AFTER"
side="BOTH"
'@
[System.IO.File]::WriteAllText((Join-Path $resourceRoot 'META-INF\neoforge.mods.toml'), $metadata, $utf8)
$testSource = Join-Path $projectRoot 'src\test\java\com\bencao\validation\BencaoSmokeTests.java'
& javac --release 21 -encoding UTF-8 -proc:none -cp $classpath -d $classesRoot $testSource
if ($LASTEXITCODE -ne 0) { throw "Test compilation failed: $LASTEXITCODE" }
& jar --create --file (Join-Path $modRoot 'bencao-smoke-tests.jar') -C $classesRoot . -C $resourceRoot .
if ($LASTEXITCODE -ne 0) { throw "Test packaging failed: $LASTEXITCODE" }

$launchTemplate = Join-Path $ServerRoot 'libraries\net\neoforged\neoforge\21.1.249\win_args.txt'
$launchArgs = Get-Content -LiteralPath $launchTemplate -Raw -Encoding UTF8
$libraryRoot = (Join-Path $ServerRoot 'libraries').Replace('\', '/')
$launchArgs = $launchArgs.Replace('libraries/', $libraryRoot + '/').Replace('-DlibraryDirectory=libraries', '-DlibraryDirectory=' + $libraryRoot)
[System.IO.File]::WriteAllText((Join-Path $testRoot 'java-args.txt'), $launchArgs, $utf8)
$properties = @'
server-ip=127.0.0.1
server-port=0
online-mode=false
enable-query=false
enable-rcon=false
level-name=validation-world
level-type=minecraft:flat
generator-settings={"biome":"minecraft:plains","layers":[{"block":"minecraft:bedrock","height":1},{"block":"minecraft:dirt","height":2},{"block":"minecraft:grass_block","height":1}],"lakes":false,"features":false}
generate-structures=false
spawn-protection=0
view-distance=2
simulation-distance=2
max-tick-time=60000
'@
[System.IO.File]::WriteAllText((Join-Path $testRoot 'server.properties'), $properties, $utf8)
Push-Location $testRoot
try {
    & java -Xms256m -Xmx1G '-Dterminal.jline=false' '-Dterminal.ansi=false' '@java-args.txt' nogui
    if ($LASTEXITCODE -ne 0) { throw "Smoke server exited with code $LASTEXITCODE" }
    $log = Get-Content -LiteralPath 'logs\latest.log' -Raw -Encoding UTF8
    if ($log -notmatch 'BENCAO_SMOKE_PASS' -or $log -match 'BENCAO_SMOKE_FAIL') {
        throw 'Smoke tests did not pass; inspect build/smoke/logs/latest.log.'
    }
} finally {
    Pop-Location
}
