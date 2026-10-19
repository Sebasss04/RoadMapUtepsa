package com.example.roadmap_utepsa.auth

import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.tasks.await
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/** Admin verification ONLY in the explicit local demo. Never uses a production endpoint. */
internal suspend fun verifyDemoUser(auth: FirebaseAuth, host: String) {
    check(auth.app.options.projectId == "demo-roadmap-utepsa")
    require(host == "10.0.2.2" || host == "127.0.0.1")
    val uid = checkNotNull(auth.currentUser).uid
    withContext(Dispatchers.IO) {
        val connection = URL("http://$host:9099/identitytoolkit.googleapis.com/v1/accounts:update?key=local-emulator-only-key")
            .openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "POST"; connection.doOutput = true
            connection.connectTimeout = 10_000; connection.readTimeout = 10_000
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("Authorization", "Bearer owner")
            connection.outputStream.use { it.write(JSONObject().put("localId", uid).put("emailVerified", true).toString().toByteArray()) }
            check(connection.responseCode == 200)
        } finally { connection.disconnect() }
    }
    auth.currentUser!!.reload().await(); auth.currentUser!!.getIdToken(true).await()
    check(auth.currentUser!!.isEmailVerified)
}
