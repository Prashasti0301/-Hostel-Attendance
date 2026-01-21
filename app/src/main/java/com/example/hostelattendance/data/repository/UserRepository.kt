package com.example.hostelattendance.data.repository

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.example.hostelattendance.data.model.User
import com.example.hostelattendance.util.Constants
import com.example.hostelattendance.util.Result
import kotlinx.coroutines.tasks.await

class UserRepository {
    private val db: FirebaseFirestore = Firebase.firestore

    suspend fun createUser(
        userId: String,
        enrollmentNumber: String,
        email: String,
        name: String,

    ): Result<Unit> {
        return try {
            val user = User(
                id = userId,
                enrollmentNumber = enrollmentNumber,
                email = email,
                name = name,
                registeredAt = Timestamp.now(),
                biometricEnabled = false,

            )

            db.collection(Constants.COLLECTION_USERS)
                .document(userId)
                .set(user)
                .await()

            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to create user")
        }
    }

    suspend fun getUser(userId: String): Result<User> {
        return try {
            val doc = db.collection(Constants.COLLECTION_USERS)
                .document(userId)
                .get()
                .await()

            val user = doc.toObject(User::class.java)
                ?: return Result.Error("User not found")

            Result.Success(user)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to get user")
        }
    }
}