package com.example.hostelattendance.data.repository

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.example.hostelattendance.data.model.AttendanceLog
import com.example.hostelattendance.util.Constants
import com.example.hostelattendance.util.Result
import kotlinx.coroutines.tasks.await

class LogRepository {
    private val db: FirebaseFirestore = Firebase.firestore

    suspend fun createLog(
        userId: String,
        action: String,
        details: Map<String, Any>,
        date: String
    ): Result<Unit> {
        return try {
            val log = AttendanceLog(
                id = db.collection(Constants.COLLECTION_LOGS).document().id,
                userId = userId,
                action = action,
                timestamp = Timestamp.now(),
                details = details,
                date = date
            )

            db.collection(Constants.COLLECTION_LOGS)
                .document(log.id)
                .set(log)
                .await()

            Result.Success(Unit)
        } catch (e: Exception) {
            // Silent fail for logs to not interrupt main flow
            Result.Error(e.message ?: "Failed to create log")
        }
    }
}