param(
    [string]$CleanroomRoot = "$env:USERPROFILE\.cleanroom\relauncher",
    [string]$Java25Exe = "$env:APPDATA\PrismLauncher\java\azul_zulu_jre25.0.3\bin\java.exe",
    [string]$PrismModsDir = "$env:APPDATA\PrismLauncher\instances\CropManagerTest\minecraft\mods"
)
$ErrorActionPreference = 'Stop'
$project = Split-Path $PSScriptRoot -Parent
Set-Location $project

Write-Host "=== [1/5] Validando e gerando texturas de GUI ===" -ForegroundColor Cyan
python tools\generate_gui_textures.py
if ($LASTEXITCODE -ne 0) { throw "Falha ao gerar/validar texturas GUI" }

Write-Host "=== [2/5] Executando Testes Unitarios, AssetIntegrity, Build e reobfRuntimeTests ===" -ForegroundColor Cyan
& .\gradlew.bat test build reobfRuntimeTests --no-daemon
if ($LASTEXITCODE -ne 0) { throw "Falha no Gradle test/build/reobfRuntimeTests" }

$xmlFiles = Get-ChildItem -Path "$project\build\test-results\test" -Filter *.xml
$totalTests = 0; $totalFailures = 0; $totalErrors = 0; $totalSkipped = 0
foreach ($f in $xmlFiles) {
    [xml]$x = Get-Content $f.FullName
    $totalTests += [int]$x.testsuite.tests
    $totalFailures += [int]$x.testsuite.failures
    $totalErrors += [int]$x.testsuite.errors
    $totalSkipped += [int]$x.testsuite.skipped
}
Write-Host ("Unitarios: {0} suites, {1} testes (failures={2}, errors={3}, skipped={4})" -f $xmlFiles.Count, $totalTests, $totalFailures, $totalErrors, $totalSkipped) -ForegroundColor Green
if ($totalFailures -gt 0 -or $totalErrors -gt 0) { throw "Testes unitarios falharam" }

# Montar classpath do Cleanroom 0.6.11-alpha
$cleanroomJar = Join-Path $CleanroomRoot "libraries\com\cleanroommc\cleanroom\0.6.11-alpha\cleanroom-0.6.11-alpha.jar"
$serverJar = Join-Path "$env:USERPROFILE\.gradle" "caches\retro_futura_gradle\mc-vanilla\1.12.2\server.jar"
if (-not (Test-Path $serverJar)) { $serverJar = Join-Path "$project\.gradle-home" "caches\retro_futura_gradle\mc-vanilla\1.12.2\server.jar" }
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
}
$jars.Insert(0, (Resolve-Path -LiteralPath $cleanroomJar).ProviderPath)
$jars.Add((Resolve-Path -LiteralPath $serverJar).ProviderPath)
$classpath = '"' + (($jars | Select-Object -Unique) -join ';').Replace('\', '/') + '"'

$verifyDir = Join-Path $project "build\cleanroom-instance-verify"
$modsDir = Join-Path $verifyDir "mods"
New-Item -ItemType Directory -Force -Path $modsDir | Out-Null
Remove-Item (Join-Path $modsDir "*.jar") -Force -ErrorAction SilentlyContinue
if (Test-Path $PrismModsDir) {
    Get-ChildItem -Path $PrismModsDir -Filter *.jar | Where-Object { $_.Name -notlike "*-cleanroom-relauncher*" -and $_.Name -notlike "CropManager-*" } | ForEach-Object {
        Copy-Item $_.FullName $modsDir -Force
    }
} else {
    Copy-Item "$project\libs\industrialcraft-2-2.8.222-ex112.jar" $modsDir -Force
}
Copy-Item "$project\build\libs\CropManager-0.5.0-alpha8.jar" $modsDir -Force
Set-Content -LiteralPath (Join-Path $verifyDir "eula.txt") -Value "eula=true" -Encoding ascii
Set-Content -LiteralPath (Join-Path $verifyDir "server.properties") -Encoding ascii -Value @(
    'level-name=cropmanager-test-world', 'level-type=FLAT', 'generate-structures=false',
    'server-ip=127.0.0.1', 'server-port=25577', 'online-mode=false', 'view-distance=2',
    'spawn-protection=0', 'max-tick-time=180000', 'allow-nether=false'
)

Write-Host "=== [3/5] Cleanroom Smoke Test (sem addon de testes) ===" -ForegroundColor Cyan
$cleanArgs = @('--enable-native-access=ALL-UNNAMED', '-Xmx2G', '-Dfile.encoding=UTF-8', '-Dfml.queryResult=confirm', '-Dterminal.jline=false', '-cp', $classpath, 'net.minecraftforge.fml.relauncher.ServerLaunchWrapper', 'nogui')
[IO.File]::WriteAllLines((Join-Path $verifyDir "java-clean.args"), [string[]]$cleanArgs, [Text.UTF8Encoding]::new($false))
"" | Out-File "$verifyDir\clean.out.log" -Encoding utf8
"" | Out-File "$verifyDir\clean.err.log" -Encoding utf8
Push-Location $verifyDir
try {
    $proc = Start-Process -FilePath $Java25Exe -ArgumentList '@java-clean.args' -RedirectStandardOutput "$verifyDir\clean.out.log" -RedirectStandardError "$verifyDir\clean.err.log" -PassThru -NoNewWindow
    $done = $false
    $deadline = (Get-Date).AddSeconds(60)
    while (-not $proc.HasExited -and (Get-Date) -lt $deadline) {
        if (Select-String -Path "$verifyDir\clean.out.log" -Pattern "Done \(" -Quiet) { $done = $true; Start-Sleep -Seconds 1; break }
        Start-Sleep -Seconds 1
    }
    if (-not $proc.HasExited) { $proc.Kill() }
    if (-not $done) { throw "Smoke test Cleanroom nao atingiu Done" }
    Write-Host "Cleanroom Smoke Test: PASSOU" -ForegroundColor Green
} finally { Pop-Location }

Write-Host "=== [4/5] Cleanroom Runtime Integration Tests (RuntimeChecks) ===" -ForegroundColor Cyan
Copy-Item "$project\build\libs\CropManager-0.5.0-alpha8-runtime-tests.jar" $modsDir -Force
$rtArgs = @('-Dcropmanager.testWorld=true') + $cleanArgs
[IO.File]::WriteAllLines((Join-Path $verifyDir "java-rt.args"), [string[]]$rtArgs, [Text.UTF8Encoding]::new($false))
Push-Location $verifyDir
try {
    if (Test-Path "rt.console.log") { Remove-Item "rt.console.log" }
    & $Java25Exe '@java-rt.args' 2>&1 | Tee-Object -FilePath 'rt.console.log'
    if ($LASTEXITCODE -ne 0 -or -not (Select-String -LiteralPath 'rt.console.log' -Pattern 'CROP_MANAGER_RUNTIME_TESTS_PASSED' -Quiet)) {
        throw "RuntimeChecks no Cleanroom falhou; verifique build\cleanroom-instance-verify\rt.console.log"
    }
    $passCount = (Select-String -LiteralPath 'rt.console.log' -Pattern 'RUNTIME PASS:').Count
    Write-Host "Cleanroom RuntimeChecks: PASSOU ($passCount verificacoes)" -ForegroundColor Green
} finally {
    Remove-Item (Join-Path $modsDir "CropManager-0.5.0-alpha8-runtime-tests.jar") -Force -ErrorAction SilentlyContinue
    Pop-Location
}

Write-Host "=== [5/5] Sincronizando JAR validado na instancia PrismLauncher ===" -ForegroundColor Cyan
if (Test-Path $PrismModsDir) {
    Copy-Item "$project\build\libs\CropManager-0.5.0-alpha8.jar" (Join-Path $PrismModsDir "CropManager-0.5.0-alpha8.jar") -Force
    Write-Host "Instancia atualizada: $PrismModsDir" -ForegroundColor Green
}
Write-Host "ROTINA COMPLETA FINALIZADA COM SUCESSO!" -ForegroundColor Green
