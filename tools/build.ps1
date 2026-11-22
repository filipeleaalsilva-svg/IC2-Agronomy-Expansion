param([string]$Ic2Jar, [string]$JavaHome)
$ErrorActionPreference = 'Stop'
$project = Split-Path $PSScriptRoot -Parent
if ($JavaHome) { $env:JAVA_HOME = $JavaHome }
if (-not $env:JAVA_HOME -or -not (Test-Path "$env:JAVA_HOME\bin\java.exe")) {
    throw 'Set JAVA_HOME to a JDK supported by Gradle 9.7 (tested with JDK 25), or pass -JavaHome.'
}
$env:JAVA_HOME = (Resolve-Path -LiteralPath $env:JAVA_HOME).ProviderPath
if ($Ic2Jar) {
    New-Item -ItemType Directory -Force -Path "$project\libs" | Out-Null
    $sourceJar = (Resolve-Path -LiteralPath $Ic2Jar).ProviderPath
    $destinationJar = Join-Path $project 'libs\industrialcraft-2-2.8.222-ex112.jar'
    if (-not [string]::Equals($sourceJar, $destinationJar, [StringComparison]::OrdinalIgnoreCase)) {
        Copy-Item -LiteralPath $sourceJar -Destination $destinationJar
    }
}
if (-not (Test-Path "$project\libs\industrialcraft-2-2.8.222-ex112.jar")) {
    throw 'Supply the audited IC2 JAR using -Ic2Jar. It is not redistributed in the source archive.'
}
if (-not $env:GRADLE_USER_HOME) { $env:GRADLE_USER_HOME = Join-Path $project '.gradle-home' }
$env:GRADLE_USER_HOME = [IO.Path]::GetFullPath($env:GRADLE_USER_HOME)
Push-Location $project
try {
    & .\gradlew.bat clean build --console=plain
    if ($LASTEXITCODE -ne 0) { throw "Gradle failed: $LASTEXITCODE" }
} finally { Pop-Location }
