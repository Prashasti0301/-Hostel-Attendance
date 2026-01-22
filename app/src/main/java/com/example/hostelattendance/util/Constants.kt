package com.example.hostelattendance.util

import com.google.firebase.firestore.GeoPoint

object Constants {
    // Hostel Boundary - CHANGE TO YOUR ACTUAL COORDINATES!

    val HOSTEL_CENTER = GeoPoint(24.436924752254967, 77.15831449580436) // JUET Guna approximate
    const val HOSTEL_RADIUS_METERS = 40000.0

    // Attendance Time Window
    const val ATTENDANCE_START_HOUR = 0
    const val ATTENDANCE_START_MINUTE = 0
    const val ATTENDANCE_END_HOUR = 23
    const val ATTENDANCE_END_MINUTE = 59
    const val LATE_THRESHOLD_MINUTES = 15

    // Email Validation
    const val ALLOWED_EMAIL_DOMAIN = "@juetguna.in"

    // Firebase Collections
    const val COLLECTION_USERS = "users"
    const val COLLECTION_ATTENDANCE = "attendance"
    const val COLLECTION_LOGS = "logs"
}