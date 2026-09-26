package com.example.viewmodel

import com.example.data.SupabaseManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AuthManager {

    private val _isOnlineRegistered = MutableStateFlow(false)
    val isOnlineRegistered: StateFlow<Boolean> = _isOnlineRegistered.asStateFlow()
    
    private val _onlineEmail = MutableStateFlow("")
    val onlineEmail: StateFlow<String> = _onlineEmail.asStateFlow()

    private var currentUserId: String = "local_trader"

    fun setAuthenticated(uid: String, email: String) {
        currentUserId = uid
        _isOnlineRegistered.value = true
        _onlineEmail.value = email
    }

    suspend fun registerWithEmail(email: String, password: String): Pair<Boolean, String?> {
        val res = SupabaseManager.signUpWithEmail(email, password)
        if (res.first) {
            setAuthenticated(res.second ?: email.replace(".", "_"), email)
        }
        return res
    }

    suspend fun loginAnonymously(): String {
        val guestUid = "guest_" + java.util.UUID.randomUUID().toString().take(8)
        setAuthenticated(guestUid, "misafir_tuccar")
        return guestUid
    }

    suspend fun loginWithEmail(email: String, password: String): Pair<Boolean, String?> {
        val res = SupabaseManager.signInWithEmail(email, password)
        if (res.first) {
            setAuthenticated(res.second ?: email.replace(".", "_"), email)
        }
        return res
    }

    fun logout() {
        _isOnlineRegistered.value = false
        _onlineEmail.value = ""
        currentUserId = "local_trader"
    }

    fun getCurrentUserId(): String {
        return currentUserId
    }
}

