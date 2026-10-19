param(
    [string]$AdbPath = "$env:LOCALAPPDATA/Android/Sdk/platform-tools/adb.exe",
    [ValidateSet('UI', 'PermissionDenied', 'Persistence')][string]$Mode = 'UI',
    [switch]$SkipInstall,
    [string]$OutputDirectory
)
$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'test-class-names.ps1')
$reminderRoot = Split-Path -Parent $PSScriptRoot
$reminderOut = Join-Path $reminderRoot 'app/build/reports/quality-reminders'
if ($OutputDirectory) { $reminderOut = [System.IO.Path]::GetFullPath($OutputDirectory, $reminderRoot) }
New-Item -ItemType Directory -Force -Path $reminderOut | Out-Null
$reminderSerial = 'emulator-5556'
$reminderAvd = & $AdbPath -s $reminderSerial emu avd name
if ($LASTEXITCODE -ne 0 -or ($reminderAvd -join "`n") -notmatch '(?m)^RoadMap_Recommendations_Test_API35\s*$') {
    throw 'Refusing notification tests outside the isolated synthetic AVD.'
}
if ((& $AdbPath -s $reminderSerial shell getprop sys.boot_completed).Trim() -ne '1') { throw 'Synthetic AVD is not ready.' }
& $AdbPath -s $reminderSerial shell input keyevent KEYCODE_WAKEUP
& $AdbPath -s $reminderSerial shell wm dismiss-keyguard
& $AdbPath -s $reminderSerial shell svc power stayon true
if (-not $SkipInstall) {
    foreach ($reminderApk in @('app/build/outputs/apk/debug/app-debug.apk', 'app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk')) {
        & $AdbPath -s $reminderSerial install --no-streaming -r (Join-Path $reminderRoot $reminderApk)
        if ($LASTEXITCODE -ne 0) { throw "Failed to install $reminderApk" }
    }
}
function Invoke-ReminderCase([string]$Name, [string]$Classes, [string[]]$ExtraArguments = @()) {
    $reminderClasses = ($Classes.Split(',') | ForEach-Object { (Resolve-RoadMapTest $_) }) -join ','
    $reminderArgs = @('-s', $reminderSerial, 'shell', 'am', 'instrument', '-w', '-r', '-e', 'class', $reminderClasses) + $ExtraArguments +
        'com.example.roadmap_utepsa.test/androidx.test.runner.AndroidJUnitRunner'
    $reminderOutput = & $AdbPath @reminderArgs 2>&1
    $reminderExit = $LASTEXITCODE
    $reminderText = $reminderOutput -join "`n"
    $reminderText | Set-Content -LiteralPath (Join-Path $reminderOut "$Name.txt") -Encoding UTF8
    Write-Output $reminderText
    if ($reminderExit -ne 0 -or $reminderText -notmatch 'OK \(\d+ tests?\)' -or $reminderText -match 'FAILURES!!!') {
        throw "Notification verification $Name failed; inspect the saved evidence."
    }
}
if ($Mode -eq 'UI') {
    & $AdbPath -s $reminderSerial shell pm grant com.example.roadmap_utepsa android.permission.POST_NOTIFICATIONS
    if ($LASTEXITCODE -ne 0) { throw 'Could not grant synthetic permission.' }
    Invoke-ReminderCase 'UI' 'NotificationFlowTest,NotificationNavigationTest,ReminderPreferenceUiTest,QuantityResourcesTest'
    & $AdbPath -s $reminderSerial pull '/sdcard/Android/data/com.example.roadmap_utepsa/files/quality-reminder-captures' $reminderOut
    if ($LASTEXITCODE -ne 0) { throw 'Could not copy synthetic captures.' }
} elseif ($Mode -eq 'PermissionDenied') {
    try {
        & $AdbPath -s $reminderSerial shell pm revoke com.example.roadmap_utepsa android.permission.POST_NOTIFICATIONS
        if ($LASTEXITCODE -ne 0) { throw 'Could not revoke synthetic permission.' }
        Invoke-ReminderCase 'PermissionDenied' 'NotificationPermissionDeniedTest'
    } finally {
        & $AdbPath -s $reminderSerial shell pm grant com.example.roadmap_utepsa android.permission.POST_NOTIFICATIONS
    }
} else {
    Invoke-ReminderCase 'Persistence-seed' 'ReminderProcessPersistenceTest' @('-e', 'reminder_persistence_phase', 'seed')
    & $AdbPath -s $reminderSerial shell am force-stop com.example.roadmap_utepsa
    if ($LASTEXITCODE -ne 0) { throw 'Could not stop the synthetic process.' }
    Invoke-ReminderCase 'Persistence-check' 'ReminderProcessPersistenceTest' @('-e', 'reminder_persistence_phase', 'check')
}
