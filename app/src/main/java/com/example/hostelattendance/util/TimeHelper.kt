package com.example.hostelattendance.util

import java.text.SimpleDateFormat
import java.util.*

object TimeHelper {
    fun isWithinAttendanceWindow(): Boolean {
        val calendar = Calendar.getInstance()
        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
        val currentMinute = calendar.get(Calendar.MINUTE)

        val startMinutes = Constants.ATTENDANCE_START_HOUR * 60 + Constants.ATTENDANCE_START_MINUTE
        val endMinutes = Constants.ATTENDANCE_END_HOUR * 60 + Constants.ATTENDANCE_END_MINUTE
        val currentMinutes = currentHour * 60 + currentMinute

        return currentMinutes in startMinutes..endMinutes
    }

    fun getAttendanceStatus(): AttendanceStatus {
        val calendar = Calendar.getInstance()
        val currentMinutes = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)
        val startMinutes = Constants.ATTENDANCE_START_HOUR * 60 + Constants.ATTENDANCE_START_MINUTE
        val lateThreshold = startMinutes + Constants.LATE_THRESHOLD_MINUTES

        return when {
            currentMinutes <= lateThreshold -> AttendanceStatus.PRESENT
            currentMinutes <= (Constants.ATTENDANCE_END_HOUR * 60 + Constants.ATTENDANCE_END_MINUTE) -> AttendanceStatus.LATE
            else -> AttendanceStatus.ABSENT
        }
    }

    fun getCurrentDate(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(Date())
    }

    fun getCurrentTime(): String {
        val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
        return sdf.format(Date())
    }

    fun getRemainingTimeText(): String {
        val calendar = Calendar.getInstance()
        val endCalendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, Constants.ATTENDANCE_END_HOUR)
            set(Calendar.MINUTE, Constants.ATTENDANCE_END_MINUTE)
            set(Calendar.SECOND, 0)
        }

        val diff = endCalendar.timeInMillis - calendar.timeInMillis
        if (diff <= 0) return "Time window closed"

        val minutes = (diff / 1000 / 60) % 60
        val hours = (diff / 1000 / 60 / 60)

        return "${hours}h ${minutes}m remaining"
    }

    fun formatTimestamp(date: Date): String {
        val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
        return sdf.format(date)
    }

    fun formatDate(date: Date): String {
        val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        return sdf.format(date)
    }
}