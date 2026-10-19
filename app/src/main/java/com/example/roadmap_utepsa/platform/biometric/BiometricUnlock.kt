package com.example.roadmap_utepsa.platform.biometric
import com.example.roadmap_utepsa.R
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.example.roadmap_utepsa.presentation.session.SessionLock

class BiometricUnlock(private val activity: FragmentActivity, private val sessionLock: SessionLock) {
        private val prompt = BiometricPrompt(activity, ContextCompat.getMainExecutor(activity), object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) { sessionLock.authenticated() }
            override fun onAuthenticationFailed() { sessionLock.rejected(R.string.biometric_not_recognized) }
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                sessionLock.failed(when (errorCode) {
                    BiometricPrompt.ERROR_CANCELED, BiometricPrompt.ERROR_USER_CANCELED,
                    BiometricPrompt.ERROR_NEGATIVE_BUTTON -> R.string.biometric_cancelled
                    else -> R.string.biometric_unavailable
                })
            }
        })
    fun authenticate(enable: Boolean) {
        if (!sessionLock.beginPrompt(enable)) return
        val available = BiometricManager.from(activity).canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)
        if (available != BiometricManager.BIOMETRIC_SUCCESS) {
            sessionLock.failed(when (available) {
                BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> R.string.biometric_not_enrolled
                BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> R.string.biometric_no_hardware
                BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> R.string.biometric_sensor_unavailable
                BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED -> R.string.biometric_security_update
                BiometricManager.BIOMETRIC_ERROR_UNSUPPORTED -> R.string.biometric_unsupported
                else -> R.string.biometric_unavailable
            })
            return
        }
        prompt.authenticate(BiometricPrompt.PromptInfo.Builder()
            .setTitle(if (enable) activity.getString(R.string.biometric_enable_title) else activity.getString(R.string.biometric_unlock_title))
            .setSubtitle(activity.getString(R.string.biometric_confirm_subtitle))
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
            .setNegativeButtonText(activity.getString(R.string.biometric_cancel)).build())
    }

}
