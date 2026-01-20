package com.example.hostelattendance.util

sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val message: String) : Result<Nothing>()
    object Loading : Result<Nothing>()
}

enum class AttendanceStatus {
    PRESENT, LATE, ABSENT
}

enum class AttendanceMethod {
    BIOMETRIC, FACE
}