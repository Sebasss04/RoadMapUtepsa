# Shared exact mapping; package id and instrumentation runner stay unchanged.
function Resolve-RoadMapTest([string]$Name) {
    $parts = $Name.Split('#', 2)
    $packages = @{
        'AcademicFlowTest' = 'com.example.roadmap_utepsa.academic'
        'AccountFlowTest' = 'com.example.roadmap_utepsa.account'
        'AccountSdkEmulatorTest' = 'com.example.roadmap_utepsa.account'
        'DemoVerifiedUser' = 'com.example.roadmap_utepsa.auth'
        'ExampleInstrumentedTest' = 'com.example.roadmap_utepsa.academic'
        'HistorialFlowTest' = 'com.example.roadmap_utepsa.history'
        'LoginFeedbackTest' = 'com.example.roadmap_utepsa.auth'
        'NotificationFlowTest' = 'com.example.roadmap_utepsa.notification'
        'NotificationNavigationTest' = 'com.example.roadmap_utepsa.notification'
        'NotificationPermissionDeniedTest' = 'com.example.roadmap_utepsa.notification'
        'OfertaFixtures' = 'com.example.roadmap_utepsa.offer'
        'OfertaFlowTest' = 'com.example.roadmap_utepsa.offer'
        'OfertaSdkEmulatorTest' = 'com.example.roadmap_utepsa.offer'
        'PersonalAcademicPersistenceTest' = 'com.example.roadmap_utepsa.academic'
        'PreparacionSdkEmulatorTest' = 'com.example.roadmap_utepsa.history'
        'ProductionOfferReadOnlyTest' = 'com.example.roadmap_utepsa.offer'
        'ProductionPreparationReadOnlyTest' = 'com.example.roadmap_utepsa.history'
        'QuantityResourcesTest' = 'com.example.roadmap_utepsa.shared'
        'RecomendacionesFlowTest' = 'com.example.roadmap_utepsa.recommendation'
        'RecomendacionesSdkEmulatorTest' = 'com.example.roadmap_utepsa.recommendation'
        'ReminderPreferenceUiTest' = 'com.example.roadmap_utepsa.notification'
        'ReminderProcessPersistenceTest' = 'com.example.roadmap_utepsa.notification'
        'ResumenIntegrationTest' = 'com.example.roadmap_utepsa.academic'
        'ResumenVisualTest' = 'com.example.roadmap_utepsa.academic'
        'SaeMigrationReadOnlyAuditTest' = 'com.example.roadmap_utepsa.academic'
    }
    if (-not $packages.ContainsKey($parts[0])) { throw "Unknown test class: $Name" }
    $result = $packages[$parts[0]] + '.' + $parts[0]
    if ($parts.Length -eq 2) { $result += '#' + $parts[1] }
    return $result
}
