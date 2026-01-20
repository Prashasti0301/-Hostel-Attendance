package com.example.hostelattendance.ui.attendance

import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hostelattendance.data.model.Attendance
import com.example.hostelattendance.data.model.User
import com.example.hostelattendance.data.repository.AttendanceRepository
import com.example.hostelattendance.data.repository.AuthRepository
import com.example.hostelattendance.data.repository.LogRepository
import com.example.hostelattendance.data.repository.UserRepository
import com.example.hostelattendance.domain.usecase.MarkAttendanceUseCase
import com.example.hostelattendance.util.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class AttendanceState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null,
    val hasMarkedToday: Boolean = false,
    val todayAttendance: Attendance? = null,
    val attendanceHistory: List<Attendance> = emptyList(),
    val isWithinTimeWindow: Boolean = false,
    val remainingTime: String = "",
    val currentUser: User? = null,
    val stats: Map<String, Int> = emptyMap()
)

class AttendanceViewModel(
    private val authRepository: AuthRepository = AuthRepository(),
    private val attendanceRepository: AttendanceRepository = AttendanceRepository(),
    private val userRepository: UserRepository = UserRepository(),
    private val logRepository: LogRepository = LogRepository(),
    private var locationHelper: LocationHelper? = null
) : ViewModel() {

    private val _attendanceState = MutableStateFlow(AttendanceState())
    val attendanceState: StateFlow<AttendanceState> = _attendanceState

    init {
        loadUserData()
    }

    fun setLocationHelper(helper: LocationHelper) {
        locationHelper = helper
    }

    private fun loadUserData() {
        viewModelScope.launch {
            val userId = authRepository.getCurrentUserId() ?: return@launch

            when (val result = userRepository.getUser(userId)) {
                is Result.Success -> {
                    _attendanceState.value = _attendanceState.value.copy(
                        currentUser = result.data
                    )
                    checkAttendanceStatus()
                    loadAttendanceHistory()
                    loadStats()
                }
                is Result.Error -> {
                    _attendanceState.value = _attendanceState.value.copy(
                        error = result.message
                    )
                }
                else -> {}
            }
        }
    }

    fun checkAttendanceStatus() {
        viewModelScope.launch {
            val userId = authRepository.getCurrentUserId() ?: return@launch
            val date = TimeHelper.getCurrentDate()

            when (val result = attendanceRepository.hasMarkedAttendanceToday(userId, date)) {
                is Result.Success -> {
                    val hasMarked = result.data

                    // Get today's attendance details if marked
                    val todayAttendance = if (hasMarked) {
                        when (val attendanceResult = attendanceRepository.getTodayAttendance(userId, date)) {
                            is Result.Success -> attendanceResult.data
                            else -> null
                        }
                    } else null

                    _attendanceState.value = _attendanceState.value.copy(
                        hasMarkedToday = hasMarked,
                        todayAttendance = todayAttendance,
                        isWithinTimeWindow = TimeHelper.isWithinAttendanceWindow(),
                        remainingTime = TimeHelper.getRemainingTimeText()
                    )
                }
                is Result.Error -> {
                    _attendanceState.value = _attendanceState.value.copy(
                        error = result.message
                    )
                }
                else -> {}
            }
        }
    }

    fun markAttendance(method: AttendanceMethod, location: Location) {
        viewModelScope.launch {
            val userId = authRepository.getCurrentUserId() ?: return@launch
            val helper = locationHelper ?: return@launch

            _attendanceState.value = _attendanceState.value.copy(isLoading = true)

            val useCase = MarkAttendanceUseCase(
                attendanceRepository,
                logRepository,
                userRepository,
                helper
            )

            when (val result = useCase.execute(userId, method, location)) {
                is Result.Success -> {
                    _attendanceState.value = _attendanceState.value.copy(
                        isLoading = false,
                        successMessage = result.data,
                        hasMarkedToday = true
                    )
                    checkAttendanceStatus()
                    loadAttendanceHistory()
                    loadStats()
                }
                is Result.Error -> {
                    _attendanceState.value = _attendanceState.value.copy(
                        isLoading = false,
                        error = result.message
                    )
                }
                else -> {
                    _attendanceState.value = _attendanceState.value.copy(isLoading = false)
                }
            }
        }
    }

    fun loadAttendanceHistory() {
        viewModelScope.launch {
            val userId = authRepository.getCurrentUserId() ?: return@launch

            when (val result = attendanceRepository.getAttendanceHistory(userId, 30)) {
                is Result.Success -> {
                    _attendanceState.value = _attendanceState.value.copy(
                        attendanceHistory = result.data
                    )
                }
                is Result.Error -> {
                    _attendanceState.value = _attendanceState.value.copy(
                        error = result.message
                    )
                }
                else -> {}
            }
        }
    }

    private fun loadStats() {
        viewModelScope.launch {
            val userId = authRepository.getCurrentUserId() ?: return@launch

            when (val result = attendanceRepository.getAttendanceStats(userId)) {
                is Result.Success -> {
                    _attendanceState.value = _attendanceState.value.copy(
                        stats = result.data
                    )
                }
                else -> {}
            }
        }
    }

    fun clearMessages() {
        _attendanceState.value = _attendanceState.value.copy(
            error = null,
            successMessage = null
        )
    }

    fun refreshData() {
        checkAttendanceStatus()
        loadAttendanceHistory()
        loadStats()
    }
}