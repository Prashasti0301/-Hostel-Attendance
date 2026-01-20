package com.example.hostelattendance.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.GeoPoint

data class Attendance(
    val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val enrollmentNumber: String = "",
    val date: String = "",
    val timestamp: Timestamp = Timestamp.now(),
    val location: GeoPoint? = null,
    val method: String = "",
    val status: String = ""
)