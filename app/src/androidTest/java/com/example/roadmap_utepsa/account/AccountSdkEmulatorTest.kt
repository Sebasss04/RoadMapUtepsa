package com.example.roadmap_utepsa.account

import com.example.roadmap_utepsa.auth.verifyDemoUser
import android.util.Base64
import androidx.test.platform.app.InstrumentationRegistry
import com.example.roadmap_utepsa.data.auth.FirebaseAuthRepository
import com.example.roadmap_utepsa.data.academic.FirestoreAcademicRepository
import com.example.roadmap_utepsa.data.profile.FirestoreProfileRepository
import com.example.roadmap_utepsa.domain.auth.DifferentGoogleEmail
import com.example.roadmap_utepsa.domain.academic.EstadoAcademico
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.*
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await
import org.json.JSONObject
import org.junit.*
import org.junit.Assert.*
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

/** Real Android Firebase SDK, exclusively secondary app and local demo services. */
class AccountSdkEmulatorTest {
    private lateinit var app: FirebaseApp
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var repo: FirebaseAuthRepository
    private lateinit var email: String
    @Before fun connectDemoOnly() {
        check(InstrumentationRegistry.getArguments().getString("account_sdk_emulator") == "demo-roadmap-utepsa:8089:9099")
        app = FirebaseApp.initializeApp(InstrumentationRegistry.getInstrumentation().targetContext,
            FirebaseOptions.Builder().setProjectId("demo-roadmap-utepsa")
                .setApplicationId("1:1234567890:android:1234567890abcdef").setApiKey("local-emulator-only-key").build(),
            "account-sdk-${UUID.randomUUID()}")
        auth = FirebaseAuth.getInstance(app); auth.useEmulator("10.0.2.2", 9099)
        db = FirebaseFirestore.getInstance(app)
        db.firestoreSettings = FirebaseFirestoreSettings.Builder().setLocalCacheSettings(MemoryCacheSettings.newBuilder().build()).build()
        db.useEmulator("10.0.2.2", 8089); repo = FirebaseAuthRepository(auth)
        email = "account-${UUID.randomUUID()}@example.test"
    }
    private fun google(email: String, subject: String = email): String {
        fun b64(value: String) = Base64.encodeToString(value.toByteArray(), Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
        return b64("{\"alg\":\"none\",\"typ\":\"JWT\"}") + "." + b64(JSONObject()
            .put("iss", "https://accounts.google.com").put("aud", "demo-client")
            .put("sub", subject).put("email", email).put("email_verified", true)
            .put("iat", System.currentTimeMillis()/1000).put("exp", System.currentTimeMillis()/1000+3600).toString()) + "."
    }
    private suspend fun oobCodes(): org.json.JSONArray = withContext(Dispatchers.IO) {
        val c = URL("http://10.0.2.2:9099/emulator/v1/projects/demo-roadmap-utepsa/oobCodes").openConnection() as HttpURLConnection
        try {
            c.setRequestProperty("Authorization", "Bearer owner")
            c.connectTimeout = 10_000; c.readTimeout = 10_000
            c.inputStream.bufferedReader().use { JSONObject(it.readText()).getJSONArray("oobCodes") }
        } finally { c.disconnect() }
    }
    private suspend fun applyCode(method: String, body: JSONObject) = withContext(Dispatchers.IO) {
        val c = URL("http://10.0.2.2:9099/identitytoolkit.googleapis.com/v1/accounts:$method?key=local-emulator-only-key")
            .openConnection() as HttpURLConnection
        try {
            c.requestMethod = "POST"; c.doOutput = true; c.setRequestProperty("Content-Type", "application/json")
            c.connectTimeout = 10_000; c.readTimeout = 10_000
            c.outputStream.use { it.write(body.toString().toByteArray()) }
            check(c.responseCode == 200)
        } finally { c.disconnect() }
    }
    @Test fun unverifiedCannotReadOrCreateProfileAndVerifiedCan() = runBlocking {
        withTimeout(60_000) {
            repo.register(email, "Demo-only-password-2026")
            val uid = repo.currentUid!!
            assertTrue(repo.currentIdentity!!.requiresVerification)
            val profile = FirestoreProfileRepository(db, auth)
            try { profile.load(uid); fail("Unverified profile read must fail") } catch (_: FirebaseFirestoreException) {}
            try { profile.save(uid, "Demo", "Cuenta"); fail("Unverified profile write must fail") } catch (_: FirebaseFirestoreException) {}
            repo.sendVerification(); assertEquals(uid, repo.currentUid)
            val codes = oobCodes()
            val code = (0 until codes.length()).map { codes.getJSONObject(it) }
                .last { it.optString("email") == email && it.optString("requestType") == "VERIFY_EMAIL" }.getString("oobCode")
            applyCode("update", JSONObject().put("oobCode", code)); repo.refreshIdentity()
            assertFalse(repo.currentIdentity!!.requiresVerification)
            profile.save(uid, "Demo", "Verificado"); assertEquals("Demo", profile.load(uid).names)
        }
    }
    @Test fun passwordThenGoogleLinksWithoutChangingUidOrProfileAndBothMethodsRecoverIt() = runBlocking {
        withTimeout(90_000) {
            repo.register(email, "Demo-only-password-2026"); verifyDemoUser(auth, "10.0.2.2")
            val uid = repo.currentUid!!; val profile = FirestoreProfileRepository(db, auth)
            profile.save(uid, "Perfil", "Conservado")
            val academic = FirestoreAcademicRepository(profile, db, auth)
            val data = academic.load(uid)
            val subject = data.materias.single { it.codigo == "BMS-300" }.id
            val history = academic.saveState(uid, subject, EstadoAcademico.APROBADA, data.estados)
            repo.reauthenticatePassword("Demo-only-password-2026")
            try { repo.linkGoogle(google("other@example.test")); fail("Wrong email must be rejected") } catch (_: DifferentGoogleEmail) {}
            assertEquals(uid, repo.currentUid)
            assertFalse(repo.refreshIdentity()!!.providers.contains("google.com"))
            repo.reauthenticatePassword("Demo-only-password-2026"); repo.linkGoogle(google(email))
            assertEquals(uid, repo.currentUid); assertTrue(repo.refreshIdentity()!!.providers.containsAll(setOf("password", "google.com")))
            repo.signOut(); repo.signInWithGoogle(google(email)); assertEquals(uid, repo.currentUid)
            assertEquals("Conservado", profile.load(uid).surnames)
            assertEquals(history, academic.load(uid).estados)
            repo.signOut(); repo.signInWithEmail(email, "Demo-only-password-2026"); assertEquals(uid, repo.currentUid)
            assertEquals("Conservado", profile.load(uid).surnames)
            assertEquals(history, academic.load(uid).estados)
        }
    }
    @Test fun googleThenPasswordLinksAndWrongGoogleEmailCannotSwitchSession() = runBlocking {
        withTimeout(90_000) {
            var stage = "Google sign-in"
            try {
            repo.signInWithGoogle(google(email)); val uid = repo.currentUid!!
            stage = "profile"
            val profile = FirestoreProfileRepository(db, auth); profile.save(uid, "Google", "Conservado")
            stage = "Google reauthentication"
            repo.reauthenticateGoogle(google(email))
            stage = "link password"
            repo.linkPassword("Demo-only-password-2026")
            assertEquals(uid, repo.currentUid)
            stage = "password reauthentication"
            repo.reauthenticatePassword("Demo-only-password-2026")
            stage = "wrong Google email rejection"
            try { repo.linkGoogle(google("other@example.test")); fail("Wrong email must be rejected") } catch (_: DifferentGoogleEmail) {}
            assertEquals(uid, repo.currentUid)
            stage = "password sign-in"
            repo.signOut(); repo.signInWithEmail(email, "Demo-only-password-2026"); assertEquals(uid, repo.currentUid)
            assertEquals("Conservado", profile.load(uid).surnames)
            } catch (e: Exception) { throw AssertionError("Demo stage: $stage (${e.javaClass.simpleName})", e) }
        }
    }
    @Test fun recoverySendsWebResetCodeAndUnknownEmailHasNeutralOutcome() = runBlocking {
        withTimeout(60_000) {
            repo.register(email, "Demo-only-password-2026")
            repo.sendPasswordReset(email); repo.sendPasswordReset("missing-${UUID.randomUUID()}@example.test")
            val uid = repo.currentUid; val codes = oobCodes()
            val reset = (0 until codes.length()).map { codes.getJSONObject(it) }.last {
                it.optString("email") == email && it.optString("requestType") == "PASSWORD_RESET" }
            applyCode("resetPassword", JSONObject().put("oobCode", reset.getString("oobCode")).put("newPassword", "Changed-demo-password-2026"))
            repo.signOut(); repo.signInWithEmail(email, "Changed-demo-password-2026"); assertEquals(uid, repo.currentUid)
        }
    }
    @After fun closeSecondaryOnly() = runBlocking {
        if (::auth.isInitialized) auth.signOut()
        if (::db.isInitialized) db.terminate().await()
        if (::app.isInitialized) app.delete()
    }
}
