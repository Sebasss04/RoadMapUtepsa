package com.example.roadmap_utepsa.shared

import com.example.roadmap_utepsa.R

import android.content.res.Configuration
import android.graphics.drawable.AdaptiveIconDrawable
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import java.util.Locale

class QuantityResourcesTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
        .createConfigurationContext(Configuration().apply { setLocale(Locale.forLanguageTag("es")) })
    @Test fun zeroOneAndSeveralUseSpanishGrammar() {
        val res = context.resources
        for (n in listOf(0, 1, 3)) {
            val subject = if (n == 1) "materia aprobada" else "materias aprobadas"
            assertEquals("Requiere $n $subject", res.getQuantityString(R.plurals.academic_minimum, n, n))
            assertEquals("${if (n == 1) "Falta" else "Faltan"} $n $subject.",
                res.getQuantityString(R.plurals.history_flow_missing_approved, n, n))
            assertEquals("$n ${if (n == 1) "materia" else "materias"} · 9 semestres curriculares",
                res.getQuantityString(R.plurals.academic_subtitle, n, n))
        }
    }
    @Test fun independentQuantitiesAndProgressKeepTheirOwnAgreement() {
        val res = context.resources
        val detail = res.getString(R.string.history_threshold_detail,
            res.getQuantityString(R.plurals.history_have, 1, 1),
            res.getQuantityString(R.plurals.history_missing_threshold, 3, 3))
        assertEquals("Tienes 1 aprobada distinta de esta materia. Faltan 3 para cumplir el umbral.", detail)
        assertEquals("0 de 1 cambio confirmado", res.getQuantityString(R.plurals.history_flow_progress, 1, 0, 1))
        assertEquals("1 de 3 cambios confirmados", res.getQuantityString(R.plurals.history_flow_progress, 3, 1, 3))
    }
    @Test fun bothLauncherIconsRemainAdaptive() {
        assertTrue(context.getDrawable(R.mipmap.ic_launcher) is AdaptiveIconDrawable)
        assertTrue(context.getDrawable(R.mipmap.ic_launcher_round) is AdaptiveIconDrawable)
    }

    @Test fun accessFeedbackHidesProviderDetailsAndKeepsNeutralRecovery() {
        val ids = listOf(R.string.access_email_exists, R.string.access_method_in_use,
            R.string.access_weak_password, R.string.access_unavailable, R.string.access_failed,
            R.string.profile_saved_detail, R.string.auth_recovery_neutral)
        val technical = Regex("Firebase|Firestore|SDK|UID|token|credencial|transacci", RegexOption.IGNORE_CASE)
        ids.forEach { assertFalse(context.getString(it), technical.containsMatchIn(context.getString(it))) }
        assertTrue(context.getString(R.string.auth_recovery_neutral).startsWith("Si ese correo admite recuperación"))
        assertFalse(context.getString(R.string.access_email_exists).contains("otra cuenta"))
        assertTrue(context.getString(R.string.access_email_exists).contains("Actualiza tus métodos"))
    }

    @Test fun uncertainSaveCopyKeepsReconciliationAndNeverConfirmsSuccess() {
        assertTrue(context.getString(R.string.academic_save_pending).contains("No lo envíes de nuevo"))
        assertTrue(context.getString(R.string.offer_uncertain).contains("comprobaremos el resultado"))
        assertTrue(context.getString(R.string.profile_pending_detail).contains("no podemos confirmar"))
        assertEquals("Tu perfil se guardó correctamente.", context.getString(R.string.profile_saved_detail))
    }
}
