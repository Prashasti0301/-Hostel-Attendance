package com.example.hostelattendance.domain.usecase

import android.location.Location
import com.example.hostelattendance.data.model.User
import com.example.hostelattendance.data.repository.AttendanceRepository
import com.example.hostelattendance.data.repository.LogRepository
import com.example.hostelattendance.data.repository.UserRepository
import com.example.hostelattendance.util.*

class MarkAttendanceUseCase(
    private val attendanceRepository: AttendanceRepository,
    private val logRepository: LogRepository,
    private val userRepository: UserRepository,
    private val locationHelper: LocationHelper
) {
    suspend fun execute(
        userId: String,
        method: AttendanceMethod,
        location: Location
    ): Result<String> {
        // Get user info
        val userResult = userRepository.getUser(userId)
        val user = when (userResult) {
            is Result.Success -> userResult.data
            is Result.Error -> return Result.Error("Failed to get user info")
            else -> return Result.Error("Unknown error")
        }

        // Check if already marked
        val date = TimeHelper.getCurrentDate()
        when (val result = attendanceRepository.hasMarkedAttendanceToday(userId, date)) {
            is Result.Success -> {
                if (result.data) {
                    return Result.Error("You have already marked attendance today")
                }
            }
            is Result.Error -> return result
            else -> {}
        }

        // Validate time window
        if (!TimeHelper.isWithinAttendanceWindow()) {
            logRepository.createLog(
                userId = userId,
                action = "ATTENDANCE_FAILED",
                details = mapOf("reason" to "Outside time window"),
                date = date
            )
            return Result.Error("Attendance window is closed (${Constants.ATTENDANCE_START_HOUR}:00 - ${Constants.ATTENDANCE_END_HOUR}:00)")
        }

        // Validate location
        if (!locationHelper.isWithinBoundary(location)) {
            val distance = locationHelper.getDistanceFromHostel(location)
            logRepository.createLog(
                userId = userId,
                action = "ATTENDANCE_FAILED",
                details = mapOf(
                    "reason" to "Outside hostel boundary",
                    "distance" to distance
                ),
                date = date
            )
            return Result.Error("You are outside the hostel premises (${distance.toInt()}m away)")
        }

        // Get status and mark attendance
        val status = TimeHelper.getAttendanceStatus()
        val geoPoint = locationHelper.locationToGeoPoint(location)

        return when (val result = attendanceRepository.markAttendance(
            userId = userId,
            userName = user.name,
            enrollmentNumber = user.enrollmentNumber,
            date = date,
            location = geoPoint,
            method = method.name,
            status = status.name
        )) {
            is Result.Success -> {
                logRepository.createLog(
                    userId = userId,
                    action = "ATTENDANCE_MARKED",
                    details = mapOf(
                        "method" to method.name,
                        "status" to status.name
                    ),
                    date = date
                )
                Result.Success("Attendance marked successfully as ${status.name}")
            }
            is Result.Error -> {
                logRepository.createLog(
                    userId = userId,
                    action = "ATTENDANCE_FAILED",
                    details = mapOf("reason" to result.message),
                    date = date
                )
                result
            }
            else -> Result.Error("Unknown error")
        }
    }
}