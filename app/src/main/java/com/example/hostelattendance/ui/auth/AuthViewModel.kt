package com.example.hostelattendance.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hostelattendance.data.repository.AuthRepository
import com.example.hostelattendance.util.Result
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class AuthState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSuccess: Boolean = false
)

class AuthViewModel(
    private val authRepository: AuthRepository = AuthRepository()
) : ViewModel() {

    private val _authState = MutableStateFlow(AuthState())
    val authState: StateFlow<AuthState> = _authState

    fun register(
        email: String,
        password: String,
        confirmPassword: String,
        enrollmentNumber: String,
        name: String,

    ) {
        viewModelScope.launch {
            // Validation
            if (name.isBlank()) {
                _authState.value = AuthState(error = "Please enter your name")
                return@launch
            }
            if (enrollmentNumber.isBlank()) {
                _authState.value = AuthState(error = "Please enter enrollment number")
                return@launch
            }
            if (email.isBlank()) {
                _authState.value = AuthState(error = "Please enter email")
                return@launch
            }
            if (password.isBlank()) {
                _authState.value = AuthState(error = "Please enter password")
                return@launch
            }
            if (password != confirmPassword) {
                _authState.value = AuthState(error = "Passwords do not match")
                return@launch
            }

            _authState.value = AuthState(isLoading = true)

            when (val result = authRepository.register(email, password, enrollmentNumber, name)) {
                is Result.Success -> {
                    _authState.value = AuthState(isSuccess = true)
                }
                is Result.Error -> {
                    _authState.value = AuthState(error = result.message)
                }
                else -> {}
            }
        }
    }

    fun login(email: String, password: String) {
        viewModelScope.launch {
            if (email.isBlank()) {
                _authState.value = AuthState(error = "Please enter email")
                return@launch
            }
            if (password.isBlank()) {
                _authState.value = AuthState(error = "Please enter password")
                return@launch
            }

            _authState.value = AuthState(isLoading = true)

            when (val result = authRepository.login(email, password)) {
                is Result.Success -> {
                    _authState.value = AuthState(isSuccess = true)
                }
                is Result.Error -> {
                    _authState.value = AuthState(error = result.message)
                }
                else -> {}
            }
        }
    }

    fun clearError() {
        _authState.value = _authState.value.copy(error = null)
    }

    fun resetState() {
        _authState.value = AuthState()
    }
}