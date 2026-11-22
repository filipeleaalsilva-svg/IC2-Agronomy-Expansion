param(
    [switch]$RunWorldTests,
    [switch]$WithAccelerator,
    [string]$CleanroomRoot = "$env:USERPROFILE\.cleanroom\relauncher",
    [string]$JavaHome = "$env:USERPROFILE\.cleanroom\java\azul_zulu-25\zulu25.28.85-ca-jdk25.0.0-win_x64",
    [string]$GradleUserHome = $env:GRADLE_USER_HOME
)
$ErrorActionPreference = 'Stop'
$project = Split-Path $PSScriptRoot -Parent
$testRoot = Join-Path $project 'build\cleanroom-smoke'
$CleanroomRoot = (Resolve-Path -LiteralPath $CleanroomRoot).ProviderPath
$javaExe = Join-Path $JavaHome 'bin\java.exe'
$cleanroomJar = Join-Path $CleanroomRoot 'libraries\com\cleanroommc\cleanroom\0.6.11-alpha\cleanroom-0.6.11-alpha.jar'
if (-not (Test-Path -LiteralPath $javaExe -PathType Leaf)) { throw "Java executable not found: $javaExe" }
if (-not (Test-Path -LiteralPath $cleanroomJar -PathType Leaf)) { throw "Cleanroom 0.6.11-alpha JAR not found: $cleanroomJar" }
$javaExe = (Resolve-Path -LiteralPath $javaExe).ProviderPath
$cleanroomJar = (Resolve-Path -LiteralPath $cleanroomJar).ProviderPath
$serverRelative = 'caches\retro_futura_gradle\mc-vanilla\1.12.2\server.jar'
if ($GradleUserHome) {
    $serverJar = Join-Path $GradleUserHome $serverRelative
} else {
    # Direct gradlew uses the user cache; tools/build.ps1 defaults to a project cache.
    $serverJar = Join-Path "$env:USERPROFILE\.gradle" $serverRelative
    if (-not (Test-Path -LiteralPath $serverJar -PathType Leaf)) {
        $serverJar = Join-Path "$project\.gradle-home" $serverRelative
    }
}
if (-not (Test-Path -LiteralPath $serverJar -PathType Leaf)) {
    throw "Minecraft server JAR not found: $serverJar. Run the Gradle build first, or pass -GradleUserHome."
}
$serverJar = (Resolve-Path -LiteralPath $serverJar).ProviderPath
$modsRoot = [IO.Path]::GetFullPath((Join-Path $testRoot 'mods'))
New-Item -ItemType Directory -Force -Path $modsRoot | Out-Null
# Only remove known generated addon artifacts in this disposable mods directory.
# Keep IC2, other mods, unknown files, and all subdirectories.
foreach ($oldJar in Get-ChildItem -LiteralPath $modsRoot -File -Filter 'CropManager-*.jar') {
    if ($oldJar.Name -match '^CropManager-\d+\.\d+\.\d+(?:-[A-Za-z]+\d+)?(?:-(?:runtime-tests(?:-dev)?|dev|sources))?\.jar$') {
        if ($oldJar.DirectoryName -ne $modsRoot) { throw "Unexpected addon cleanup path: $($oldJar.FullName)" }
        Remove-Item -LiteralPath $oldJar.FullName
    }
}
Copy-Item -LiteralPath "$project\libs\industrialcraft-2-2.8.222-ex112.jar" -Destination "$testRoot\mods"
Copy-Item -LiteralPath "$project\build\libs\CropManager-0.5.0-alpha2.jar" -Destination "$testRoot\mods"
if (-not $RunWorldTests -and (Test-Path -LiteralPath "$testRoot\eula.txt") -and
    (Select-String -LiteralPath "$testRoot\eula.txt" -Pattern '^eula=true$' -Quiet)) {
    throw 'This test directory already accepts the EULA; use -RunWorldTests for an automatically stopped test run.'
}
if ($RunWorldTests) {
    if (-not (Test-Path -LiteralPath "$testRoot\eula.txt") -or
        -not (Select-String -LiteralPath "$testRoot\eula.txt" -Pattern '^eula=true$' -Quiet)) {
        throw 'Accept the Minecraft EULA by setting eula=true in build/cleanroom-smoke/eula.txt first.'
    }
    Copy-Item -LiteralPath "$project\build\libs\CropManager-0.5.0-alpha2-runtime-tests.jar" -Destination "$testRoot\mods"
    Set-Content -LiteralPath "$testRoot\server.properties" -Encoding ascii -Value @(
        'level-name=cropmanager-test-world', 'level-type=FLAT', 'generate-structures=false',
        'server-ip=127.0.0.1', 'server-port=25576', 'online-mode=false', 'view-distance=2',
        'spawn-protection=0', 'max-tick-time=180000', 'allow-nether=false'
    )
}
if ($WithAccelerator) {
    Copy-Item -LiteralPath "$env:USERPROFILE\Biblioteca\CropAccelerator-0.1.0-alpha1.jar" -Destination "$testRoot\mods"
} else {
    $accelerator = Join-Path $testRoot 'mods\CropAccelerator-0.1.0-alpha1.jar'
    if (Test-Path -LiteralPath $accelerator) { Remove-Item -LiteralPath $accelerator }
}
$metadata = Get-Content -LiteralPath "$CleanroomRoot\0.6.11-alpha\net.minecraftforge.json" -Raw | ConvertFrom-Json
$jars = [Collections.Generic.List[string]]::new()
foreach ($library in $metadata.libraries) {
    $relative = $library.downloads.artifact.path
    if (-not $relative) {
        $parts = $library.name.Split(':')
        $relative = $parts[0].Replace('.', '/') + '/' + $parts[1] + '/' + $parts[2] + '/' + $parts[1] + '-' + $parts[2] + '.jar'
    }
    $file = Join-Path "$CleanroomRoot\libraries" $relative
    if (Test-Path -LiteralPath $file) { $jars.Add((Resolve-Path -LiteralPath $file).ProviderPath) }
    else { Write-Warning "Library unavailable locally: $relative" }
}
$jars.Insert(0, $cleanroomJar)
$jars.Add($serverJar)
$classpath = ($jars | Select-Object -Unique) -join ';'
$arguments = @(
    '--enable-native-access=ALL-UNNAMED', '-Xmx2G', '-Dfile.encoding=UTF-8',
    '-Dfml.queryResult=confirm', '-Dterminal.jline=false',
    '-cp', ('"' + $classpath.Replace('\', '/') + '"'),
    'net.minecraftforge.fml.relauncher.ServerLaunchWrapper', 'nogui'
)
if ($RunWorldTests) { $arguments = @('-Dcropmanager.testWorld=true') + $arguments }
[IO.File]::WriteAllLines("$testRoot\java.args", [string[]]$arguments, [Text.UTF8Encoding]::new($false))
# No EULA is accepted by this script. Without an existing eula.txt, the server
# stops at the vanilla EULA gate, after mod discovery (not full mod loading).
Push-Location $testRoot
try {
    # Remove a previous pass marker before invoking Java, including launch failures.
    if (Test-Path -LiteralPath 'console.log') { Remove-Item -LiteralPath 'console.log' }
    & $javaExe '@java.args' 2>&1 | Tee-Object -FilePath 'console.log'
    if ($LASTEXITCODE -ne 0) { throw "Minecraft exited with code $LASTEXITCODE; inspect build/cleanroom-smoke/console.log." }
    if ($RunWorldTests -and -not (Select-String -LiteralPath 'console.log' -Pattern 'CROP_MANAGER_RUNTIME_TESTS_PASSED' -Quiet)) {
        throw 'Runtime tests did not pass; inspect build/cleanroom-smoke/console.log.'
    }
}
finally { Pop-Location }
