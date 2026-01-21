package com.example.hostelattendance.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.example.hostelattendance.util.Constants
import com.example.hostelattendance.util.Result
import kotlinx.coroutines.tasks.await

class AuthRepository {
    private val auth: FirebaseAuth = Firebase.auth

    suspend fun register(
        email: String,
        password: String,
        enrollmentNumber: String,
        name: String,

    ): Result<String> {
        return try {
            if (!email.endsWith(Constants.ALLOWED_EMAIL_DOMAIN)) {
                return Result.Error("Please use your ${Constants.ALLOWED_EMAIL_DOMAIN} email")
            }

            if (password.length < 6) {
                return Result.Error("Password must be at least 6 characters")
            }

            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val userId = result.user?.uid ?: return Result.Error("Failed to create user")

            val userRepo = UserRepository()
            userRepo.createUser(userId, enrollmentNumber, email, name)

            Result.Success(userId)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Registration failed")
        }
    }

    suspend fun login(email: String, password: String): Result<String> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            val userId = result.user?.uid ?: return Result.Error("Login failed")
            Result.Success(userId)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Login failed")
        }
    }

    fun logout() {
        auth.signOut()
    }

    fun getCurrentUserId(): String? {
        return auth.currentUser?.uid
    }

    fun isLoggedIn(): Boolean {
        return auth.currentUser != null
    }
}