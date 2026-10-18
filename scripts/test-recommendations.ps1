param(
    [string]$AdbPath = "$env:LOCALAPPDATA/Android/Sdk/platform-tools/adb.exe",
    [string]$Serial = 'emulator-5556',
    [switch]$SdkDemo,
    [switch]$SkipInstall
)
$ErrorActionPreference = 'Stop'
$recRoot = Split-Path -Parent $PSScriptRoot
$recOut = Join-Path $recRoot 'app/build/reports/recommendations'
New-Item -ItemType Directory -Force -Path $recOut | Out-Null
if ($Serial -ne 'emulator-5556') { throw 'Use the isolated recommendations test serial.' }
$recAvd = & $AdbPath -s $Serial emu avd name
if ($LASTEXITCODE -ne 0 -or ($recAvd -join "`n") -notmatch '(?m)^RoadMap_Recommendations_Test_API35\s*$') {
    throw 'Refusing instrumentation on an unknown/personal device.'
}
$recBoot = & $AdbPath -s $Serial shell getprop sys.boot_completed
if ($recBoot.Trim() -ne '1') { throw 'The isolated test AVD is not ready.' }
if (-not $SkipInstall) {
    foreach ($recApk in @('app/build/outputs/apk/debug/app-debug.apk', 'app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk')) {
        & $AdbPath -s $Serial install --no-streaming -r (Join-Path $recRoot $recApk)
        if ($LASTEXITCODE -ne 0) { throw "Install failed: $recApk" }
    }
}
$recClass = if ($SdkDemo) { 'com.example.roadmap_utepsa.RecomendacionesSdkEmulatorTest' }
    else { 'com.example.roadmap_utepsa.RecomendacionesFlowTest,com.example.roadmap_utepsa.OfertaFlowTest' }
$recArgs = @('-s', $Serial, 'shell', 'am', 'instrument', '-w', '-r', '-e', 'class', $recClass)
if ($SdkDemo) { $recArgs += @('-e', 'recommendations_sdk_emulator', 'demo-roadmap-utepsa:8089:9099') }
$recArgs += 'com.example.roadmap_utepsa.test/androidx.test.runner.AndroidJUnitRunner'
$recOutput = & $AdbPath @recArgs 2>&1
$recExit = $LASTEXITCODE
$recText = $recOutput -join "`n"
$recName = if ($SdkDemo) { 'sdk' } else { 'ui' }
$recText | Set-Content -LiteralPath (Join-Path $recOut "$recName.txt") -Encoding UTF8
Write-Output $recText
if ($recExit -ne 0 -or $recText -notmatch 'OK \(\d+ tests?\)' -or $recText -notmatch 'INSTRUMENTATION_CODE: -1') {
    throw 'Recommendations instrumentation failed; inspect the saved output.'
}
if (-not $SdkDemo) {
    & $AdbPath -s $Serial pull '/sdcard/Android/data/com.example.roadmap_utepsa/files/recommendations-ui-captures' $recOut
    if ($LASTEXITCODE -ne 0) { throw 'Could not copy visual evidence.' }
}
