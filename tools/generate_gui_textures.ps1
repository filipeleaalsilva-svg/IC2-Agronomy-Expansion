param(
    [string]$DarkPack = "$(Join-Path (Get-Location) 'dist\CropManager-GUI-Dark-0.5.0-alpha8')"
)
$ErrorActionPreference = 'Stop'
$scriptPath = Join-Path $PSScriptRoot 'generate_gui_textures.py'
& py $scriptPath $DarkPack
if ($LASTEXITCODE -ne 0) {
    throw "generate_gui_textures.py failed with exit code $LASTEXITCODE"
}
