package com.example.utils

import com.example.viewmodel.*

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.example.MainActivity
import com.example.viewmodel.GameViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

object GoogleAuthHelper {
    const val SERVER_CLIENT_ID = "988366584024-umu1kpfvi06mpl6nphm3u6kqcsa9c091.apps.googleusercontent.com"
    const val RC_SIGN_IN = 9001

    private var pendingViewModel: GameViewModel? = null
    private var pendingCallback: ((Boolean, String?) -> Unit)? = null

    fun findActivity(context: Context): Activity? {
        var current: Context? = context
        while (current is ContextWrapper) {
            if (current is Activity) return current
            current = current.baseContext
        }
        if (current is Activity) return current
        return MainActivity.currentActivity
    }

    fun handleActivityResult(requestCode: Int, resultCode: Int, data: Intent?): Boolean {
        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(com.google.android.gms.common.api.ApiException::class.java)
                val idToken = account?.idToken
                val email = account?.email
                val displayName = account?.displayName ?: "Tüccar"
                
                if (!email.isNullOrBlank()) {
                    CoroutineScope(Dispatchers.IO).launch {
                        if (account.account != null) {
                            try {
                                val act = MainActivity.currentActivity ?: findActivity(account.account.let { MainActivity.currentActivity ?: return@launch })
                                if (act != null) {
                                    val token = com.google.android.gms.auth.GoogleAuthUtil.getToken(
                                        act,
                                        account.account!!,
                                        "oauth2:https://www.googleapis.com/auth/drive.appdata"
                                    )
                                    com.example.data.GoogleDriveSaveManager.setAccessToken(token)
                                }
                            } catch (e: Exception) {
                                Log.w("GoogleAuthHelper", "Google Drive AppData token fetch optional fallback: ${e.message}")
                            }
                        }
                        withContext(Dispatchers.Main) {
                            pendingViewModel?.signInWithGoogleAccount(email, displayName, idToken) { success, msg ->
                                pendingCallback?.invoke(success, msg)
                                pendingViewModel = null
                                pendingCallback = null
                            }
                        }
                    }
                } else {
                    pendingCallback?.invoke(false, "Google hesabından e-posta bilgisi alınamadı.")
                    pendingViewModel = null
                    pendingCallback = null
                }
            } catch (e: com.google.android.gms.common.api.ApiException) {
                Log.e("GoogleAuthHelper", "GoogleSignIn ApiException statusCode: ${e.statusCode}", e)
                val detailedMsg = when (e.statusCode) {
                    10 -> "Google Cloud Hata Kodu 10 (DEVELOPER_ERROR): Web Client ID veya SHA-1 uyuşmazlığı. Google Cloud'daki Client ID ve SHA-1'in güncellenmesi 5-10 dk sürebilir."
                    12500 -> "Google Hata Kodu 12500: Cihazdaki Google Play Hizmetleri veya OAuth yapılandırması doğrulanamadı."
                    7 -> "Google Hata Kodu 7: Ağ bağlantısı hatası."
                    else -> "Google Giriş Hatası (Kod: ${e.statusCode}): ${e.localizedMessage}"
                }
                pendingCallback?.invoke(false, detailedMsg)
                pendingViewModel = null
                pendingCallback = null
            } catch (e: Exception) {
                Log.e("GoogleAuthHelper", "GoogleSignIn general exception", e)
                pendingCallback?.invoke(false, "Giriş başarısız: ${e.localizedMessage ?: e.message}")
                pendingViewModel = null
                pendingCallback = null
            }
            return true
        }
        return false
    }

    fun checkAndRestoreGoogleSession(context: Context, viewModel: GameViewModel) {
        try {
            val account = GoogleSignIn.getLastSignedInAccount(context)
            val email = account?.email
            if (!email.isNullOrBlank()) {
                val displayName = account.displayName ?: "Tüccar"
                val idToken = account.idToken
                Log.i("GoogleAuthHelper", "Restoring previous Google session for $email")
                CoroutineScope(Dispatchers.IO).launch {
                    if (account.account != null) {
                        try {
                            val token = com.google.android.gms.auth.GoogleAuthUtil.getToken(
                                context,
                                account.account!!,
                                "oauth2:https://www.googleapis.com/auth/drive.appdata"
                            )
                            com.example.data.GoogleDriveSaveManager.setAccessToken(token)
                        } catch (_: Exception) {}
                    }
                    withContext(Dispatchers.Main) {
                        viewModel.signInWithGoogleAccount(email, displayName, idToken)
                    }
                }
            }
        } catch (e: Exception) {
            Log.w("GoogleAuthHelper", "Could not restore last signed-in Google account", e)
        }
    }

    private fun launchClassicGoogleSignIn(
        activity: Activity,
        viewModel: GameViewModel,
        onComplete: (Boolean, String?) -> Unit
    ) {
        try {
            pendingViewModel = viewModel
            pendingCallback = onComplete

            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestProfile()
                .requestIdToken(SERVER_CLIENT_ID)
                .requestScopes(com.google.android.gms.common.api.Scope("https://www.googleapis.com/auth/drive.appdata"))
                .build()

            val googleSignInClient = GoogleSignIn.getClient(activity, gso)
            var hasLaunched = false
            fun doLaunch() {
                if (!hasLaunched) {
                    hasLaunched = true
                    try {
                        val signInIntent = googleSignInClient.signInIntent
                        activity.startActivityForResult(signInIntent, RC_SIGN_IN)
                    } catch (e: Exception) {
                        Log.e("GoogleAuthHelper", "startActivityForResult failed", e)
                        onComplete(false, "Google giriş ekranı başlatılamadı: ${e.localizedMessage}")
                    }
                }
            }

            try {
                googleSignInClient.signOut().addOnCompleteListener {
                    doLaunch()
                }
                // Safety timer for PC / emulators: if signOut listener doesn't trigger within 600ms, launch anyway!
                activity.window.decorView.postDelayed({
                    doLaunch()
                }, 600L)
            } catch (_: Exception) {
                doLaunch()
            }
        } catch (e: Exception) {
            Log.e("GoogleAuthHelper", "Failed to launch classic GoogleSignIn with ID Token, falling back to standard profile", e)
            try {
                val fallbackGso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestEmail()
                    .requestProfile()
                    .build()
                val fallbackClient = GoogleSignIn.getClient(activity, fallbackGso)
                val fallbackIntent = fallbackClient.signInIntent
                activity.startActivityForResult(fallbackIntent, RC_SIGN_IN)
            } catch (fallbackError: Exception) {
                Log.e("GoogleAuthHelper", "Fallback Google Sign-In also failed", fallbackError)
                onComplete(false, "Google hesap seçici açılamadı: ${e.localizedMessage}")
            }
        }
    }

    fun launchGoogleSignIn(
        context: Context,
        scope: CoroutineScope,
        viewModel: GameViewModel,
        onStart: () -> Unit = {},
        onComplete: (Boolean, String?) -> Unit
    ) {
        val activity = findActivity(context)
        if (activity == null) {
            Log.e("GoogleAuthHelper", "No Activity found to launch Google Sign-In")
            onComplete(false, "Activity context bulunamadı. Lütfen uygulamayı yeniden başlatın.")
            return
        }

        onStart()

        // Directly launch classic Google Sign-In for 100% device compatibility
        launchClassicGoogleSignIn(activity, viewModel, onComplete)
    }
}

