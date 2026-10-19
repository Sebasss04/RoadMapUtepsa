param(
    [string]$AdbPath = "$env:LOCALAPPDATA/Android/Sdk/platform-tools/adb.exe",
    [ValidateSet('UI', 'Regressions', 'SDK')][string]$Mode = 'UI',
    [switch]$SkipInstall,
    [string]$OutputDirectory
)
$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'test-class-names.ps1')
$accountRoot = Split-Path -Parent $PSScriptRoot
$accountOut = Join-Path $accountRoot 'app/build/reports/account-auth'
if ($OutputDirectory) { $accountOut = [System.IO.Path]::GetFullPath($OutputDirectory, $accountRoot) }
New-Item -ItemType Directory -Force -Path $accountOut | Out-Null
$accountSerial = 'emulator-5556'
$accountAvd = & $AdbPath -s $accountSerial emu avd name
if ($LASTEXITCODE -ne 0 -or ($accountAvd -join "`n") -notmatch '(?m)^RoadMap_Recommendations_Test_API35\s*$') {
    throw 'Refusing tests on an unknown or personal device.'
}
if ((& $AdbPath -s $accountSerial shell getprop sys.boot_completed).Trim() -ne '1') { throw 'Test AVD is not ready.' }
if (-not $SkipInstall) {
    foreach ($accountApk in @('app/build/outputs/apk/debug/app-debug.apk', 'app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk')) {
        & $AdbPath -s $accountSerial install --no-streaming -r (Join-Path $accountRoot $accountApk)
        if ($LASTEXITCODE -ne 0) { throw "Failed to install $accountApk" }
    }
}
if ($Mode -eq 'SDK') {
    # Auth 25 useEmulator retains a reference to a deleted secondary FirebaseApp
    # across grouped tests. Keep each original case intact in a fresh app process.
    $accountCases = @(
        'AccountSdkEmulatorTest#googleThenPasswordLinksAndWrongGoogleEmailCannotSwitchSession',
        'AccountSdkEmulatorTest#unverifiedCannotReadOrCreateProfileAndVerifiedCan',
        'AccountSdkEmulatorTest#passwordThenGoogleLinksWithoutChangingUidOrProfileAndBothMethodsRecoverIt',
        'AccountSdkEmulatorTest#recoverySendsWebResetCodeAndUnknownEmailHasNeutralOutcome',
        'OfertaSdkEmulatorTest#createReplaceEmptyRecreateAndConcurrentEditors',
        'RecomendacionesSdkEmulatorTest#guardedReadsRecalculateAfterSavedChangesAndNeverModifySnapshots',
        'PreparacionSdkEmulatorTest#postponePersistsServerTimestampAndRepositoryRecreation'
    )
    $accountResults = @()
    $accountAllOutput = [System.Collections.Generic.List[string]]::new()
    foreach ($accountCase in $accountCases) {
        & $AdbPath -s $accountSerial shell am force-stop com.example.roadmap_utepsa
        if ($LASTEXITCODE -ne 0) { throw 'Could not stop synthetic process before SDK case.' }
        $accountArgs = @('-s', $accountSerial, 'shell', 'am', 'instrument', '-w', '-r', '-e', 'class', (Resolve-RoadMapTest $accountCase))
        foreach ($accountOptIn in @('account_sdk_emulator','offer_sdk_emulator','recommendations_sdk_emulator','history_sdk_emulator')) {
            $accountArgs += @('-e', $accountOptIn, 'demo-roadmap-utepsa:8089:9099')
        }
        $accountArgs += 'com.example.roadmap_utepsa.test/androidx.test.runner.AndroidJUnitRunner'
        $accountOutput = & $AdbPath @accountArgs 2>&1
        $accountExit = $LASTEXITCODE
        $accountText = $accountOutput -join "`n"
        $accountPassed = $accountExit -eq 0 -and $accountText -match 'OK \(1 test\)' -and $accountText -notmatch 'FAILURES!!!'
        $accountResults += [pscustomobject]@{ case = $accountCase; passed = $accountPassed }
        $accountText | Set-Content -LiteralPath (Join-Path $accountOut "SDK-$($accountResults.Count).txt") -Encoding UTF8
        $accountAllOutput.Add("SDK CASE: $accountCase")
        $accountAllOutput.Add($accountText)
        Write-Output $accountText
    }
    $accountPassedCount = @($accountResults | Where-Object passed).Count
    $accountSummary = "SDK demo: $accountPassedCount/$($accountCases.Count) passed; each case ran in a separate process."
    $accountAllOutput.Add($accountSummary)
    $accountAllOutput -join "`n" | Set-Content -LiteralPath (Join-Path $accountOut 'SDK.txt') -Encoding UTF8
    $accountResults | ConvertTo-Json | Set-Content -LiteralPath (Join-Path $accountOut 'SDK-results.json') -Encoding UTF8
    Write-Output $accountSummary
    if ($accountPassedCount -ne $accountCases.Count) { throw 'SDK verification failed. No failed case was ignored; inspect SDK-results.json.' }
    return
}
$accountClasses = if ($Mode -eq 'Regressions') {
    'AccountFlowTest,ResumenVisualTest,ResumenIntegrationTest,AcademicFlowTest,HistorialFlowTest,OfertaFlowTest,RecomendacionesFlowTest,LoginFeedbackTest'
} else { 'AccountFlowTest' }
$accountClasses = ($accountClasses.Split(',') | ForEach-Object { (Resolve-RoadMapTest $_) }) -join ','
$accountArgs = @('-s', $accountSerial, 'shell', 'am', 'instrument', '-w', '-r', '-e', 'class', $accountClasses)
$accountArgs += 'com.example.roadmap_utepsa.test/androidx.test.runner.AndroidJUnitRunner'
$accountOutput = & $AdbPath @accountArgs 2>&1
$accountExit = $LASTEXITCODE
$accountText = $accountOutput -join "`n"
$accountText | Set-Content -LiteralPath (Join-Path $accountOut "$Mode.txt") -Encoding UTF8
Write-Output $accountText
if ($accountExit -ne 0 -or $accountText -notmatch 'OK \(\d+ tests?\)' -or $accountText -match 'FAILURES!!!') {
    throw 'Account verification failed. Inspect the saved evidence; do not claim completion.'
}
if ($Mode -ne 'SDK') {
    & $AdbPath -s $accountSerial pull '/sdcard/Android/data/com.example.roadmap_utepsa/files/account-ui-captures' $accountOut
    if ($LASTEXITCODE -ne 0) { throw 'Could not copy synthetic UI captures.' }
}
