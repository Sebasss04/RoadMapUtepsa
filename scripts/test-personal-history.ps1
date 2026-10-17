param(
    [switch]$Authorized,
    [string]$Permission = '',
    [string]$ExpectedProject = 'roadmap-utepsa-c9409',
    [string]$Subject = 'TRBA-330',
    [string]$Serial = 'emulator-5554',
    [string]$ExpectedAvdName = '',
    [string]$AdbPath = 'C:/Users/sjuan/AppData/Local/Android/Sdk/platform-tools/adb.exe'
)
$ErrorActionPreference = 'Stop'
# Sin este consentimiento exacto, ni siquiera se consulta el dispositivo personal.
if (-not $Authorized -or $Permission -cne 'TRBA330_PENDING_ENCURSO_RESTORE' -or
    $ExpectedProject -cne 'roadmap-utepsa-c9409' -or $Subject -cne 'TRBA-330') {
    throw 'Ejecución real bloqueada: exige -Authorized y permiso específico TRBA330_PENDING_ENCURSO_RESTORE.'
}
if ($Serial -cne 'emulator-5554' -or [string]::IsNullOrWhiteSpace($ExpectedAvdName)) {
    throw 'Se requiere emulator-5554 y su identidad AVD esperada explícita; no se arrancará ningún AVD.'
}
$personalProjectRoot = Split-Path -Parent $PSScriptRoot
$personalRunId = [guid]::NewGuid().ToString()
$personalReport = Join-Path $personalProjectRoot "app/build/reports/personal-history-real/$personalRunId"
New-Item -ItemType Directory -Path $personalReport | Out-Null
$personalState = & $AdbPath -s $Serial get-state
if ($LASTEXITCODE -ne 0 -or $personalState.Trim() -ne 'device') { throw 'Dispositivo personal no disponible; sin arranque ni cambios.' }
$personalAvdResponse = & $AdbPath -s $Serial emu avd name
if ($LASTEXITCODE -ne 0) { throw 'No se pudo verificar la identidad AVD.' }
$personalAvd = @($personalAvdResponse | ForEach-Object { $_.Trim() } | Where-Object { $_ -and $_ -ne 'OK' })
if ($personalAvd.Count -ne 1 -or $personalAvd[0] -cne $ExpectedAvdName) { throw 'Identidad AVD diferente; sin escrituras.' }

# Requiere APKs instalados previamente y revisados. Nunca reinstala ni borra datos.
$personalPackage = 'com.example.roadmap_utepsa'
$personalRunner = 'com.example.roadmap_utepsa.test/androidx.test.runner.AndroidJUnitRunner'
$personalResults = @()
function Invoke-PersonalStage([string]$Method) {
    $personalClass = "com.example.roadmap_utepsa.PersonalAcademicPersistenceTest#$Method"
    $personalOutput = & $AdbPath -s $Serial shell am instrument -w -r -e class $personalClass -e personal_project $ExpectedProject -e personal_subject $Subject -e personal_permission $Permission -e personal_run $personalRunId $personalRunner 2>&1
    $personalExit = $LASTEXITCODE
    $personalLog = $personalOutput -join "`n"
    # Los informes de la prueba omiten UID y el baseline privado nunca se copia.
    Set-Content -LiteralPath (Join-Path $personalReport "$Method.txt") -Value $personalLog -Encoding UTF8
    $personalOk = $personalExit -eq 0 -and $personalLog -match 'OK \(1 test\)' -and $personalLog -match 'INSTRUMENTATION_CODE: -1'
    Write-Host "$Method : $personalOk"
    return [pscustomobject]@{ stage = $Method; success = $personalOk; adbExitCode = $personalExit }
}
function Restart-PersonalAppOnly {
    & $AdbPath -s $Serial shell am force-stop $personalPackage | Out-Null
    if ($LASTEXITCODE -ne 0) { throw 'No se pudo cerrar solo la app para probar la reapertura.' }
    # El siguiente proceso de instrumentación abre MainActivity mediante su regla.
    # Sin emu kill, reboot, wipe, clear ni gestión de cuentas/AVD.
}
function Test-PersonalAbortWithoutWrite {
    # Solo el estado explícito escrito antes de crear baseline/intentar guardar.
    # Archivo ausente, JSON inválido, otra ejecución o muerte de proceso son ambiguos.
    $personalGuardFile = Join-Path $personalReport 'stage1-guard.json'
    try {
        & $AdbPath -s $Serial pull "/sdcard/Android/data/com.example.roadmap_utepsa/files/personal-history-evidence/$personalRunId/stage1.json" $personalGuardFile 2>&1 | Out-Null
        if ($LASTEXITCODE -ne 0 -or -not (Test-Path -LiteralPath $personalGuardFile -PathType Leaf)) { return $false }
        $personalGuard = Get-Content -LiteralPath $personalGuardFile -Raw | ConvertFrom-Json
        return ($personalGuard.run_id -is [string] -and $personalGuard.run_id -ceq $personalRunId -and
            $personalGuard.stage -is [string] -and $personalGuard.stage -ceq 'stage1' -and
            $personalGuard.project -is [string] -and $personalGuard.project -ceq $ExpectedProject -and
            $personalGuard.subject -is [string] -and $personalGuard.subject -ceq $Subject -and
            $personalGuard.status -is [string] -and
            $personalGuard.status -ceq 'ABORTED_NO_WRITE_AUTH_PROFILE_BASELINE_OR_ORIGINAL_GUARD')
    } catch { return $false }
}

$personalFailed = $false
$personalEmergency = $false
$personalAbortedWithoutWrite = $false
$personalEmergencyAttempted = $false
try {
    foreach ($personalMethod in @('stage1SaveEnCursoFromApp', 'stage2ReopenAndRestoreFromApp', 'stage3ReopenVerifyExactOriginal')) {
        if ($personalResults.Count -gt 0) { Restart-PersonalAppOnly }
        $personalResult = Invoke-PersonalStage $personalMethod
        $personalResults += $personalResult
        if (-not $personalResult.success) { $personalFailed = $true; break }
    }
} catch {
    $personalFailed = $true
    Write-Host 'Ejecución interrumpida; se comprobará la evidencia de esta ejecución antes de decidir la recuperación.'
} finally {
    if ($personalFailed -and $personalMethod -ceq 'stage1SaveEnCursoFromApp') {
        $personalAbortedWithoutWrite = Test-PersonalAbortWithoutWrite
    }
    if ($personalFailed -and -not $personalAbortedWithoutWrite) {
        # También cubre muerte del proceso antes de un finally Kotlin. La prueba
        # de emergencia exige baseline/UID/ejecución propios y compara EN_CURSO
        # dentro de la transacción. Nunca borra APROBADA ni otros esquemas.
        try {
            $personalEmergencyAttempted = $true
            $personalRestoreResult = Invoke-PersonalStage 'emergencyRestoreKnownBaseline'
            $personalResults += $personalRestoreResult
            $personalEmergency = $personalRestoreResult.success
            if ($personalEmergency) {
                Restart-PersonalAppOnly
                $personalVerification = Invoke-PersonalStage 'stage3ReopenVerifyExactOriginal'
                $personalResults += $personalVerification
                $personalEmergency = $personalVerification.success
            }
        } catch { $personalEmergency = $false }
    }
    $personalEvidence = Join-Path $personalReport 'evidence'
    New-Item -ItemType Directory -Force -Path $personalEvidence | Out-Null
    & $AdbPath -s $Serial pull "/sdcard/Android/data/com.example.roadmap_utepsa/files/personal-history-evidence/$personalRunId/." $personalEvidence
    $personalEvidenceCopied = $LASTEXITCODE -eq 0
    $personalSummary = [pscustomobject]@{
        environment = 'Firebase real; solo SDK Android autenticado; UID omitido'
        subject = $Subject
        runId = $personalRunId
        serial = $Serial
        success = -not $personalFailed -and $personalResults.Count -eq 3
        abortedWithoutWrite = $personalAbortedWithoutWrite
        emergencyRestorationAttempted = $personalEmergencyAttempted
        emergencyRestorationVerifiedAfterReopen = $personalEmergency
        restorationNeedsReview = $personalFailed -and -not $personalAbortedWithoutWrite -and -not $personalEmergency
        evidenceCopied = $personalEvidenceCopied
        results = $personalResults
    }
    $personalSummary | ConvertTo-Json -Depth 5 | Set-Content -LiteralPath (Join-Path $personalReport 'resultados.json') -Encoding UTF8
}
if ($personalFailed) { exit 1 }
