package com.example.roadmap_utepsa.services

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.example.roadmap_utepsa.R
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.tasks.await

class GoogleAuthHelper(private val context: Context) {
    private val auth = FirebaseAuth.getInstance()

    private val credentialManager = CredentialManager.create(context)

    suspend fun signInWithGoogle(): Result<String> {

        return try {

            // 1. Configuramos Google
            val googleOption = GetSignInWithGoogleOption.Builder(
                context.getString(
                    R.string.default_web_client_id
                )
            )
                .build()

            // 2. Creamos la solicitud
            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleOption)
                .build()

            // 3. Mostramos el selector de cuentas
            val result = credentialManager.getCredential(
                context,
                request
            )

            // 4. Obtenemos la credencial
            val credential = result.credential

            // 5. Verificamos que sea Google
            if (
                credential is CustomCredential &&
                credential.type ==
                GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {

                val googleIdTokenCredential =
                    GoogleIdTokenCredential
                        .createFrom(credential.data)

                val idToken =
                    googleIdTokenCredential.idToken

                // 6. Convertimos el token de Google
                //    en una credencial de Firebase
                val firebaseCredential =
                    GoogleAuthProvider.getCredential(
                        idToken,
                        null
                    )

                // 7. Iniciamos sesión en Firebase
                val authResult =
                    auth.signInWithCredential(firebaseCredential).await()

                val user = authResult.user

                Result.success(
                    user?.displayName
                        ?: "Usuario autenticado"
                )

            } else {

                Result.failure(
                    Exception(
                        "La credencial no corresponde a Google"
                    )
                )
            }

        } catch (e: GoogleIdTokenParsingException) {

            Result.failure(e)

        } catch (e: Exception) {

            Result.failure(e)
        }
    }
}
