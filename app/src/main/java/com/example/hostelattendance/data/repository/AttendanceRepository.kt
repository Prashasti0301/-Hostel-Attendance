package com.example.hostelattendance.data.repository

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.example.hostelattendance.data.model.Attendance
import com.example.hostelattendance.util.Constants
import com.example.hostelattendance.util.Result
import kotlinx.coroutines.tasks.await

class AttendanceRepository {
    private val db: FirebaseFirestore = Firebase.firestore

    suspend fun markAttendance(
        userId: String,
        userName: String,
        enrollmentNumber: String,
        date: String,
        location: GeoPoint,
        method: String,
        status: String
    ): Result<Unit> {
        return try {
            val docId = "${date}_${userId}"

            val attendance = Attendance(
                id = docId,
                userId = userId,
                userName = userName,
                enrollmentNumber = enrollmentNumber,
                date = date,
                timestamp = Timestamp.now(),
                location = location,
                method = method,
                status = status
            )

            db.collection(Constants.COLLECTION_ATTENDANCE)
                .document(docId)
                .set(attendance)
                .await()

            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to mark attendance")
        }
    }

    suspend fun hasMarkedAttendanceToday(userId: String, date: String): Result<Boolean> {
        return try {
            val docId = "${date}_${userId}"
            val doc = db.collection(Constants.COLLECTION_ATTENDANCE)
                .document(docId)
                .get()
                .await()

            Result.Success(doc.exists())
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to check attendance")
        }
    }

    suspend fun getTodayAttendance(userId: String, date: String): Result<Attendance?> {
        return try {
            val docId = "${date}_${userId}"
            val doc = db.collection(Constants.COLLECTION_ATTENDANCE)
                .document(docId)
                .get()
                .await()

            val attendance = doc.toObject(Attendance::class.java)
            Result.Success(attendance)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to get today's attendance")
        }
    }

    suspend fun getAttendanceHistory(userId: String, limit: Int = 30): Result<List<Attendance>> {
        return try {
            val snapshot = db.collection(Constants.COLLECTION_ATTENDANCE)
                .whereEqualTo("userId", userId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(limit.toLong())
                .get()
                .await()

            val attendanceList = snapshot.documents.mapNotNull {
                it.toObject(Attendance::class.java)
            }

            Result.Success(attendanceList)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to get attendance history")
        }
    }

    suspend fun getAttendanceStats(userId: String): Result<Map<String, Int>> {
        return try {
            val snapshot = db.collection(Constants.COLLECTION_ATTENDANCE)
                .whereEqualTo("userId", userId)
                .get()
                .await()

            val attendanceList = snapshot.documents.mapNotNull {
                it.toObject(Attendance::class.java)
            }

            val stats = mapOf(
                "total" to attendanceList.size,
                "present" to attendanceList.count { it.status == "PRESENT" },
                "late" to attendanceList.count { it.status == "LATE" },
                "absent" to attendanceList.count { it.status == "ABSENT" }
            )

            Result.Success(stats)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to get stats")
        }
    }
}