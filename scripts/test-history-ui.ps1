param(
    [string]$AdbPath = 'C:/Users/sjuan/AppData/Local/Android/Sdk/platform-tools/adb.exe',
    [string]$Serial = 'emulator-5556',
    [string[]]$TestMethods,
    [switch]$SkipInstall,
    [string]$ReportDirectory = ''
)
$ErrorActionPreference = 'Stop'
# Deliberadamente separado del AVD personal y de suites anteriores.
if ($Serial -ne 'emulator-5556') { throw 'La suite de historial solo admite el temporal emulator-5556.' }
$historyProjectRoot = Split-Path -Parent $PSScriptRoot
$historyReportDir = if ($ReportDirectory) { $ReportDirectory } else {
    Join-Path $historyProjectRoot ('app/build/reports/history-ui/' + (Get-Date -Format 'yyyyMMdd-HHmmss'))
}
New-Item -ItemType Directory -Force -Path $historyReportDir | Out-Null
$historyState = & $AdbPath -s $Serial get-state
if ($LASTEXITCODE -ne 0 -or $historyState.Trim() -ne 'device') { throw 'El temporal no responde.' }
$historyAvd = & $AdbPath -s $Serial emu avd name
if ($LASTEXITCODE -ne 0 -or ($historyAvd -join "`n") -notmatch '(?m)^RoadMap_Test_API35\s*$') {
    throw 'El dispositivo no es el AVD temporal aprobado de pruebas.'
}
$historyBoot = & $AdbPath -s $Serial shell getprop sys.boot_completed
if ($historyBoot.Trim() -ne '1') { throw 'El temporal todavía no completó el arranque.' }
if (-not $SkipInstall) {
    foreach ($historyApk in @('app/build/outputs/apk/debug/app-debug.apk', 'app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk')) {
        $historyApkPath = Join-Path $historyProjectRoot $historyApk
        if (-not (Test-Path -LiteralPath $historyApkPath)) { throw "Falta compilar $historyApk" }
        & $AdbPath -s $Serial install --no-streaming -r $historyApkPath
        if ($LASTEXITCODE -ne 0) { throw "No se pudo instalar $historyApk" }
    }
}
$historyAllMethods = @(
    'summarySupportsLargeFontLongNamesAndPendingFilter',
    'confirmedEditCancelPreservesStatesAndNames',
    'postponedHistoryConfirmsExistingSelectionWithoutReentry',
    'welcomeAndSelectionPreserveExistingStates',
    'selectionReviewAndConfirmUseMemoryRepository',
    'saveFailureAndRecoveryPreserveDraft',
    'enCursoExplainsBothMissingRequirements',
    'conflictKeepsLocalSelectionUntilExplicitReview',
    'sessionSwitchDropsHistory',
    'confirmedPreparationSkipsWelcomeAndAllowsExplicitReview',
    'absentPreparationCanPostponeAndResumeWithoutChangingStates',
    'h21ShowsBothRequirementStatesWithoutSaving',
    'h23ReturnsCourseToPendingManually',
    'h27ThresholdFortyFiveRequiresExplicitSave',
    'jointCorrectionAddsOmittedRequirementWithoutRemovingApproval',
    'academicConflictRequiresReviewBeforeSecondSave',
    'confirmedReviewSurvivesProfileRoundTripWithDraft',
    'postponedReviewSurvivesProfileRoundTripWithDraft'
)
$historyMethods = if ($TestMethods) { $TestMethods } else { $historyAllMethods }
if (@($historyMethods | Where-Object { $_ -notin $historyAllMethods }).Count -gt 0) { throw 'Caso de historial desconocido.' }
$historyResults = @()
foreach ($historyMethod in $historyMethods) {
    $historyClass = "com.example.roadmap_utepsa.HistorialFlowTest#$historyMethod"
    $historyOutput = & $AdbPath -s $Serial shell am instrument -w -r -e class $historyClass 'com.example.roadmap_utepsa.test/androidx.test.runner.AndroidJUnitRunner' 2>&1
    $historyExitCode = $LASTEXITCODE
    $historyLog = $historyOutput -join "`n"
    Set-Content -LiteralPath (Join-Path $historyReportDir "$historyMethod.txt") -Value $historyLog -Encoding UTF8
    $historySuccess = $historyExitCode -eq 0 -and $historyLog -match 'OK \(1 test\)' -and $historyLog -match 'INSTRUMENTATION_CODE: -1'
    $historyResults += [pscustomobject]@{ test = $historyMethod; success = $historySuccess; adbExitCode = $historyExitCode }
    Write-Output "$historyMethod : $historySuccess"
    if (-not $historySuccess) { Write-Output $historyLog; break }
}
$historyCaptureDir = Join-Path $historyReportDir 'captures'
New-Item -ItemType Directory -Force -Path $historyCaptureDir | Out-Null
& $AdbPath -s $Serial pull '/sdcard/Android/data/com.example.roadmap_utepsa/files/history-ui-captures/.' $historyCaptureDir
$historyCaptureExit = $LASTEXITCODE
$historyPassed = @($historyResults | Where-Object success).Count
$historySummary = [pscustomobject]@{
    serial = $Serial
    environment = 'Android temporal aislado; autenticación y preparación simuladas en memoria; sin Firebase'
    expected = $historyMethods.Count
    passed = $historyPassed
    success = $historyResults.Count -eq $historyMethods.Count -and $historyPassed -eq $historyMethods.Count
    screenshotsCopied = $historyCaptureExit -eq 0
    results = $historyResults
}
$historySummary | ConvertTo-Json -Depth 5 | Set-Content -LiteralPath (Join-Path $historyReportDir 'resultados.json') -Encoding UTF8
if (-not $historySummary.success) { exit 1 }
