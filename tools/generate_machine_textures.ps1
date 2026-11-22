$ErrorActionPreference = 'Stop'
$root = Join-Path (Get-Location) 'src\main\resources\assets\cropmanager\textures'
function Render-Svg([string]$name,[string]$svg,[string]$path) {
    $tmp=Join-Path $env:TEMP ($name+'.svg')
    [IO.File]::WriteAllText($tmp,$svg,[Text.Encoding]::UTF8)
    & magick -background none $tmp -define png:color-type=6 -define png:bit-depth=8 $path
    if($LASTEXITCODE -ne 0){throw "Could not render $name"}
}
function Icon([string]$id,[string]$signal) {
    $out=Join-Path $root ('items\'+$id+'.png')
    $glow=if($signal -eq 'blue'){'#55dcff'}elseif($signal -eq 'red'){'#ff5a36'}else{'#83ed68'}
    $core=if($signal -eq 'red'){'#ffcf40'}else{'#d6f4ed'}
    $svg="<svg xmlns='http://www.w3.org/2000/svg' width='16' height='16' viewBox='0 0 16 16'><g shape-rendering='crispEdges'><rect x='2' y='2' width='12' height='11' fill='#252b30'/><rect x='3' y='3' width='10' height='9' fill='#71828b'/><rect x='4' y='4' width='8' height='7' fill='#14191e'/><rect x='5' y='5' width='6' height='5' fill='$glow'/><rect x='6' y='6' width='4' height='3' fill='$core'/><rect x='7' y='7' width='2' height='1' fill='$glow'/><rect x='4' y='13' width='2' height='2' fill='#b5a05c'/><rect x='7' y='13' width='2' height='2' fill='#b5a05c'/><rect x='10' y='13' width='2' height='2' fill='#b5a05c'/><rect x='0' y='5' width='2' height='2' fill='#b5a05c'/><rect x='14' y='5' width='2' height='2' fill='#b5a05c'/><rect x='0' y='9' width='2' height='2' fill='#b5a05c'/><rect x='14' y='9' width='2' height='2' fill='#b5a05c'/></g></svg>"
    Render-Svg $id $svg $out
}
Icon 'wireless_range_upgrade' 'blue'
Icon 'harvest_transfer_upgrade' 'green'
Icon 'wireless_eu_upgrade' 'red'

$blocks=Join-Path $root 'blocks'
$front="<svg xmlns='http://www.w3.org/2000/svg' width='16' height='16' viewBox='0 0 16 16'><g shape-rendering='crispEdges'><rect width='16' height='16' fill='#30363a'/><rect x='1' y='1' width='14' height='14' fill='#697278'/><rect x='2' y='2' width='12' height='12' fill='#343c41'/><rect x='3' y='3' width='10' height='8' fill='#12191d'/><rect x='4' y='4' width='8' height='6' fill='#245b45'/><rect x='5' y='5' width='6' height='4' fill='#337654'/><rect x='6' y='6' width='4' height='2' fill='#57966a'/><rect x='2' y='12' width='12' height='2' fill='#9b8b55'/><rect x='3' y='12' width='2' height='1' fill='#e3c46d'/><rect x='7' y='12' width='2' height='1' fill='#e3c46d'/><rect x='11' y='12' width='2' height='1' fill='#e3c46d'/><rect x='1' y='1' width='2' height='2' fill='#b4bdc1'/><rect x='13' y='1' width='2' height='2' fill='#b4bdc1'/><rect x='1' y='13' width='2' height='2' fill='#20272b'/><rect x='13' y='13' width='2' height='2' fill='#20272b'/><rect x='12' y='4' width='2' height='2' fill='#b94236'/></g></svg>"
$active=$front.Replace("#245b45","#36864d").Replace("#337654","#52b45f").Replace("#57966a","#a2e665").Replace("#b94236","#50e66a")
$side="<svg xmlns='http://www.w3.org/2000/svg' width='16' height='16' viewBox='0 0 16 16'><g shape-rendering='crispEdges'><rect width='16' height='16' fill='#30363a'/><rect x='1' y='1' width='14' height='14' fill='#5c666d'/><rect x='2' y='2' width='12' height='12' fill='#3c454b'/><path d='M3 3h2v10H3z M7 3h2v10H7z M11 3h2v10h-2z' fill='#252c30'/><path d='M4 4h1v8H4z M8 4h1v8H8z M12 4h1v8h-1z' fill='#7e898d'/><rect x='1' y='1' width='2' height='2' fill='#bdc5c7'/><rect x='13' y='13' width='2' height='2' fill='#252b2e'/><rect x='2' y='14' width='12' height='1' fill='#9b8b55'/></g></svg>"
$top="<svg xmlns='http://www.w3.org/2000/svg' width='16' height='16' viewBox='0 0 16 16'><g shape-rendering='crispEdges'><rect width='16' height='16' fill='#30363a'/><rect x='1' y='1' width='14' height='14' fill='#70797d'/><rect x='2' y='2' width='12' height='12' fill='#465056'/><rect x='4' y='4' width='8' height='8' fill='#343d42'/><rect x='5' y='5' width='6' height='6' fill='#283035'/><rect x='6' y='6' width='4' height='4' fill='#69767b'/><rect x='7' y='7' width='2' height='2' fill='#3d484c'/><rect x='2' y='2' width='2' height='2' fill='#c4ccca'/><rect x='12' y='2' width='2' height='2' fill='#c4ccca'/><rect x='2' y='12' width='2' height='2' fill='#242b2f'/><rect x='12' y='12' width='2' height='2' fill='#242b2f'/></g></svg>"
Render-Svg 'biogas-weeder-front' $front (Join-Path $blocks 'biogas_weeder_front.png')
Render-Svg 'biogas-weeder-front-active' $active (Join-Path $blocks 'biogas_weeder_front_active.png')
Render-Svg 'biogas-weeder-side' $side (Join-Path $blocks 'biogas_weeder_side.png')
Render-Svg 'biogas-weeder-top' $top (Join-Path $blocks 'biogas_weeder_top.png')
