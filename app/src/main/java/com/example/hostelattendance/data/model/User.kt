package com.example.hostelattendance.data.model

import com.google.firebase.Timestamp

data class User(
    val id: String = "",
    val enrollmentNumber: String = "",
    val email: String = "",
    val name: String = "",
    val registeredAt: Timestamp = Timestamp.now(),
    val biometricEnabled: Boolean = false
)