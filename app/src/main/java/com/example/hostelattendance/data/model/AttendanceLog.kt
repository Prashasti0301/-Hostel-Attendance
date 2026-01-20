package com.example.hostelattendance.data.model

import com.google.firebase.Timestamp

data class AttendanceLog(
    val id: String = "",
    val userId: String = "",
    val action: String = "",
    val timestamp: Timestamp = Timestamp.now(),
    val details: Map<String, Any> = emptyMap(),
    val date: String = ""
)