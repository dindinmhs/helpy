package com.example.helpy

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthViewModel(private val authManager: AuthManager) : ViewModel() {

    val currentUser: StateFlow<FirebaseUser?> = authManager.currentUser
    val isLoading: StateFlow<Boolean> = authManager.isLoading

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    suspend fun signInWithGoogle(context: Context) {
        viewModelScope.launch {
            val result = authManager.signInWithGoogle()
            result.onFailure { exception ->
                _errorMessage.value = "Sign in failed: ${exception.message}"
            }
        }
    }

    fun signOut() {
        authManager.signOut()
    }

    fun clearError() {
        _errorMessage.value = null
    }
}

class AuthViewModelFactory(private val authManager: AuthManager) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AuthViewModel(authManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}