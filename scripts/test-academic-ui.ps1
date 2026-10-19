param(
    [string]$AdbPath = 'C:/Users/sjuan/AppData/Local/Android/Sdk/platform-tools/adb.exe',
    [string]$Serial = 'emulator-5554',
    [string[]]$TestMethods,
    [switch]$SkipInstall
)
$ErrorActionPreference = 'Stop'
if ($Serial -notmatch '^emulator-\d+$') { throw 'Este ejecutor se limita a un emulador Android.' }
$projectRoot = Split-Path -Parent $PSScriptRoot
$reportDir = Join-Path $projectRoot 'app/build/reports/academic-steps-567/isolated'
New-Item -ItemType Directory -Force $reportDir | Out-Null
$deviceState = & $AdbPath -s $Serial get-state
if ($LASTEXITCODE -ne 0 -or $deviceState.Trim() -ne 'device') { throw 'El emulador no responde.' }
if (-not $SkipInstall) { foreach ($relative in @('app/build/outputs/apk/debug/app-debug.apk', 'app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk')) {
    & $AdbPath -s $Serial install --no-streaming -r (Join-Path $projectRoot $relative)
    if ($LASTEXITCODE -ne 0) { throw "No se pudo instalar $relative" }
} }
$allMethods = @(
    'rendersApprovedCatalogAndSearchesWithoutAccents',
    'savesReopensAndReturnsToPending',
    'saveFailureKeepsSelectionAndRetries',
    'cancellingDirtyPanelRequiresConfirmationAndDoesNotWrite',
    'profileReturnKeepsSearchAndProtectsDirtyNames',
    'profileCompletionEntersCurriculumOnlyAfterSave',
    'readErrorOffersRetryWithoutShowingFakeProgress',
    'signingOutDropsCurriculumAndShowsLogin'
)
$methods = if ($TestMethods) { $TestMethods } else { $allMethods }
if (@($methods | Where-Object { $_ -notin $allMethods }).Count -gt 0) { throw 'Caso de prueba desconocido.' }
$results = @()
foreach ($method in $methods) {
    $testClass = "com.example.roadmap_utepsa.academic.AcademicFlowTest#$method"
    $output = & $AdbPath -s $Serial shell am instrument -w -r -e class $testClass 'com.example.roadmap_utepsa.test/androidx.test.runner.AndroidJUnitRunner' 2>&1
    $exitCode = $LASTEXITCODE
    $log = $output -join "`n"
    Set-Content -LiteralPath (Join-Path $reportDir "$method.txt") -Value $log -Encoding UTF8
    $success = $exitCode -eq 0 -and $log -match 'OK \(1 test\)' -and $log -match 'INSTRUMENTATION_CODE: -1'
    $results += [pscustomobject]@{ test = $method; success = $success; adbExitCode = $exitCode }
    Write-Output "$method : $success"
    if (-not $success) { Write-Output $log; break }
}
$passed = @($results | Where-Object success).Count
$summary = [pscustomobject]@{
    serial = $Serial
    execution = 'one instrumentation process per test'
    expected = $methods.Count
    passed = $passed
    success = $results.Count -eq $methods.Count -and $passed -eq $methods.Count
    results = $results
}
$summary | ConvertTo-Json -Depth 5 | Set-Content -LiteralPath (Join-Path $reportDir 'resultados.json') -Encoding UTF8
if (-not $summary.success) { exit 1 }
