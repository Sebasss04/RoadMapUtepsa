param(
    [string]$AdbPath = 'C:/Users/sjuan/AppData/Local/Android/Sdk/platform-tools/adb.exe',
    [string]$Serial = 'emulator-5556',
    [switch]$SdkDemo,
    [switch]$SkipInstall
)
$ErrorActionPreference = 'Stop'
$offerRoot = Split-Path -Parent $PSScriptRoot
$offerOut = Join-Path $offerRoot 'app/build/reports/offer'
New-Item -ItemType Directory -Force -Path $offerOut | Out-Null
if ($Serial -ne 'emulator-5556') { throw 'Only the isolated Offer test serial is allowed.' }
$offerAvd = & $AdbPath -s $Serial emu avd name
if ($LASTEXITCODE -ne 0 -or ($offerAvd -join "`n") -notmatch '(?m)^RoadMap_Offer_Test_API35\s*$') {
    throw 'Refusing to install or test on an unrecognized/personal AVD.'
}
$offerBoot = & $AdbPath -s $Serial shell getprop sys.boot_completed
if ($offerBoot.Trim() -ne '1') { throw 'Test AVD has not finished booting.' }
if (-not $SkipInstall) {
    foreach ($offerApk in @('app/build/outputs/apk/debug/app-debug.apk','app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk')) {
        & $AdbPath -s $Serial install --no-streaming -r (Join-Path $offerRoot $offerApk)
        if ($LASTEXITCODE -ne 0) { throw "Install failed: $offerApk" }
    }
}
$offerClass = if ($SdkDemo) { 'com.example.roadmap_utepsa.offer.OfertaSdkEmulatorTest' } else { 'com.example.roadmap_utepsa.offer.OfertaFlowTest' }
$offerArguments = @('-s',$Serial,'shell','am','instrument','-w','-r','-e','class',$offerClass)
if ($SdkDemo) { $offerArguments += @('-e','offer_sdk_emulator','demo-roadmap-utepsa:8089:9099') }
$offerArguments += 'com.example.roadmap_utepsa.test/androidx.test.runner.AndroidJUnitRunner'
$offerOutput = & $AdbPath @offerArguments 2>&1
$offerExit = $LASTEXITCODE
$offerText = $offerOutput -join "`n"
$offerName = if ($SdkDemo) { 'sdk' } else { 'ui' }
$offerText | Set-Content -LiteralPath (Join-Path $offerOut "$offerName.txt") -Encoding UTF8
Write-Output $offerText
if ($offerExit -ne 0 -or $offerText -notmatch 'OK \(\d+ tests?\)' -or $offerText -notmatch 'INSTRUMENTATION_CODE: -1') { throw 'Offer instrumentation failed; inspect the saved output.' }
if (-not $SdkDemo) {
    & $AdbPath -s $Serial pull '/sdcard/Android/data/com.example.roadmap_utepsa/files/offer-ui-captures' $offerOut
    if ($LASTEXITCODE -ne 0) { throw 'Screenshot evidence could not be copied.' }
}
