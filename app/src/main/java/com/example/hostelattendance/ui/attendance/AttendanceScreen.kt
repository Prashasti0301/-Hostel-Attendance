package com.example.hostelattendance.ui.attendance

import android.Manifest
import android.location.Location
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.hostelattendance.data.model.Attendance
import com.example.hostelattendance.ui.theme.*
import com.example.hostelattendance.util.*
import kotlinx.coroutines.launch
import android.widget.Toast
import android.util.Log

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceScreen(
    onLogout: () -> Unit,
    viewModel: AttendanceViewModel = viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope() // ✅ This is correct for Composable

    val locationHelper = remember { LocationHelper(context) }
    val biometricHelper = remember { BiometricHelper(context) }

    val attendanceState by viewModel.attendanceState.collectAsState()

    var showLogoutDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.setLocationHelper(locationHelper)
        viewModel.checkAttendanceStatus()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Attendance System",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = TextPrimary
                        )
                        attendanceState.currentUser?.let { user ->
                            Text(
                                user.name,
                                fontSize = 14.sp,
                                color = TextSecondary
                            )
                        }
                    }
                },
                actions =

                    {
                    IconButton(onClick = { viewModel.refreshData() }) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = TextPrimary
                        )
                    }
                    IconButton(onClick = { showLogoutDialog = true }) {
                        Icon(
                            Icons.Default.Logout,
                            contentDescription = "Logout",
                            tint = TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SurfaceDark
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(BackgroundDark)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Status Card
                item {
                    StatusCard(
                        hasMarkedToday = attendanceState.hasMarkedToday,
                        todayAttendance = attendanceState.todayAttendance,
                        isWithinTimeWindow = attendanceState.isWithinTimeWindow,
                        remainingTime = attendanceState.remainingTime
                    )
                }



                // Mark Attendance Button
                if (!attendanceState.hasMarkedToday) {
                    item {
                        // Permission launcher
                        val locationPermissionLauncher = rememberLauncherForActivityResult(
                            contract = ActivityResultContracts.RequestPermission()
                        ) { isGranted ->
                            Log.d("ATTENDANCE", "📱 Permission result = $isGranted")
                            if (isGranted) {
                                Log.d("ATTENDANCE", "✅ Permission GRANTED")
                                scope.launch {
                                    try {
                                        Log.d("ATTENDANCE", "📍 Getting location...")
                                        val location = locationHelper.getCurrentLocation()

                                        if (location == null) {
                                            Log.e("ATTENDANCE", "❌ Location is NULL!")
                                            Toast.makeText(
                                                context,
                                                "❌ Unable to get location. Turn on GPS!",
                                                Toast.LENGTH_LONG
                                            ).show()
                                            return@launch
                                        }

                                        val distance = locationHelper.calculateDistance(location)

                                        Log.d("ATTENDANCE", "📍 Your location: ${location.latitude}, ${location.longitude}")
                                        Log.d("ATTENDANCE", "🏠 Hostel: ${Constants.HOSTEL_CENTER.latitude}, ${Constants.HOSTEL_CENTER.longitude}")
                                        Log.d("ATTENDANCE", "📏 Distance: ${distance.toInt()} meters")
                                        Log.d("ATTENDANCE", "⭕ Allowed: ${Constants.HOSTEL_RADIUS_METERS.toInt()} meters")

                                        if (locationHelper.isWithinBoundary(location)) {
                                            Log.d("ATTENDANCE", "✅ Within boundary!")

                                            if (biometricHelper.isBiometricAvailable()) {
                                                Log.d("ATTENDANCE", "👆 Showing biometric")
                                                biometricHelper.showBiometricPrompt(
                                                    activity = context as FragmentActivity,
                                                    onSuccess = {
                                                        Log.d("ATTENDANCE", "✅ Biometric success")
                                                        viewModel.markAttendance(AttendanceMethod.BIOMETRIC, location)
                                                    },
                                                    onError = { error ->
                                                        Log.d("ATTENDANCE", "⚠️ Biometric error: $error")
                                                        viewModel.markAttendance(AttendanceMethod.BIOMETRIC, location)
                                                    }
                                                )
                                            } else {
                                                Log.d("ATTENDANCE", "👆 No biometric, marking directly")
                                                viewModel.markAttendance(AttendanceMethod.BIOMETRIC, location)
                                            }
                                        } else {
                                            Log.e("ATTENDANCE", "❌ TOO FAR! Distance = ${distance.toInt()}m")
                                            Toast.makeText(
                                                context,
                                                "❌ You are ${distance.toInt()}m away from hostel\n(Must be within ${Constants.HOSTEL_RADIUS_METERS.toInt()}m)",
                                                Toast.LENGTH_LONG
                                            ).show()
                                        }
                                    } catch (e: Exception) {
                                        Log.e("ATTENDANCE", "❌ Error: ${e.message}", e)
                                        Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                                    }
                                }
                            } else {
                                Log.e("ATTENDANCE", "❌ Permission DENIED")
                                Toast.makeText(
                                    context,
                                    "Location permission required!",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }

                        MarkAttendanceButton(
                            isLoading = attendanceState.isLoading,
                            isWithinTimeWindow = attendanceState.isWithinTimeWindow,
                            onClick = {
                                Log.d("ATTENDANCE", "🔘 ========== BUTTON CLICKED ==========")

                                if (locationHelper.hasLocationPermission()) {
                                    Log.d("ATTENDANCE", "✅ Already has permission")
                                    scope.launch {
                                        try {
                                            Log.d("ATTENDANCE", "📍 Getting location...")
                                            val location = locationHelper.getCurrentLocation()

                                            if (location == null) {
                                                Log.e("ATTENDANCE", "❌ Location is NULL!")
                                                Toast.makeText(
                                                    context,
                                                    "❌ Unable to get location. Turn on GPS!",
                                                    Toast.LENGTH_LONG
                                                ).show()
                                                return@launch
                                            }

                                            val distance = locationHelper.calculateDistance(location)

                                            Log.d("ATTENDANCE", "📍 Your location: ${location.latitude}, ${location.longitude}")
                                            Log.d("ATTENDANCE", "🏠 Hostel: ${Constants.HOSTEL_CENTER.latitude}, ${Constants.HOSTEL_CENTER.longitude}")
                                            Log.d("ATTENDANCE", "📏 Distance: ${distance.toInt()} meters")
                                            Log.d("ATTENDANCE", "⭕ Allowed: ${Constants.HOSTEL_RADIUS_METERS.toInt()} meters")

                                            if (locationHelper.isWithinBoundary(location)) {
                                                Log.d("ATTENDANCE", "✅ Within boundary!")

                                                if (biometricHelper.isBiometricAvailable()) {
                                                    Log.d("ATTENDANCE", "👆 Showing biometric")
                                                    biometricHelper.showBiometricPrompt(
                                                        activity = context as FragmentActivity,
                                                        onSuccess = {
                                                            Log.d("ATTENDANCE", "✅ Biometric success")
                                                            viewModel.markAttendance(AttendanceMethod.BIOMETRIC, location)
                                                        },
                                                        onError = { error ->
                                                            Log.d("ATTENDANCE", "⚠️ Biometric error: $error")
                                                            viewModel.markAttendance(AttendanceMethod.BIOMETRIC, location)
                                                        }
                                                    )
                                                } else {
                                                    Log.d("ATTENDANCE", "👆 No biometric, marking directly")
                                                    viewModel.markAttendance(AttendanceMethod.BIOMETRIC, location)
                                                }
                                            } else {
                                                Log.e("ATTENDANCE", "❌ TOO FAR! Distance = ${distance.toInt()}m")
                                                Toast.makeText(
                                                    context,
                                                    "❌ You are ${distance.toInt()}m away from hostel\n(Must be within ${Constants.HOSTEL_RADIUS_METERS.toInt()}m)",
                                                    Toast.LENGTH_LONG
                                                ).show()
                                            }
                                        } catch (e: Exception) {
                                            Log.e("ATTENDANCE", "❌ Error: ${e.message}", e)
                                            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                                        }
                                    }
                                } else {
                                    Log.d("ATTENDANCE", "❌ No permission, requesting...")
                                    locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                                }
                            }
                        )
                    }
                }

                // Stats Card
                item {
                    StatsCard(stats = attendanceState.stats)
                }

                // Messages
                attendanceState.error?.let { error ->
                    item {
                        ErrorMessage(error) {
                            viewModel.clearMessages()
                        }
                    }
                }

                attendanceState.successMessage?.let { message ->
                    item {
                        SuccessMessage(message) {
                            viewModel.clearMessages()
                        }
                    }
                }

                // History Section
                item {
                    Text(
                        text = "Attendance History",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }

                if (attendanceState.attendanceHistory.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = SurfaceDark
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        Icons.Default.EventBusy,
                                        contentDescription = null,
                                        modifier = Modifier.size(48.dp),
                                        tint = TextSecondary
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        "No attendance records yet",
                                        color = TextSecondary,
                                        fontSize = 16.sp
                                    )
                                }
                            }
                        }
                    }
                } else {
                    items(attendanceState.attendanceHistory) { attendance ->
                        AttendanceHistoryItem(attendance)
                    }
                }
            }
        }
    }

    // Logout Confirmation Dialog
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Logout", color = TextPrimary) },
            text = { Text("Are you sure you want to logout?", color = TextSecondary) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutDialog = false
                        onLogout()
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = ErrorRed
                    )
                ) {
                    Text("Logout")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = SurfaceDark
        )
    }
}

@Composable
fun StatusCard(
    hasMarkedToday: Boolean,
    todayAttendance: Attendance?,
    isWithinTimeWindow: Boolean,
    remainingTime: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (hasMarkedToday) SuccessGreen.copy(alpha = 0.15f)
            else PrimaryBlue.copy(alpha = 0.15f)
        )
    ) {
        Column(
            modifier = Modifier.padding(24.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Text(
                        text = if (hasMarkedToday) "✓ Attendance Marked"
                        else "Attendance Not Marked",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (hasMarkedToday) SuccessGreen else TextPrimary
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = TimeHelper.getCurrentDate(),
                        fontSize = 14.sp,
                        color = TextSecondary
                    )
                }

                Icon(
                    if (hasMarkedToday) Icons.Default.CheckCircle
                    else Icons.Default.AccessTime,
                    contentDescription = null,
                    tint = if (hasMarkedToday) SuccessGreen else PrimaryBlue,
                    modifier = Modifier.size(48.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

           Divider(color = BorderColor.copy(alpha = 0.3f))

            Spacer(modifier = Modifier.height(16.dp))

            if (hasMarkedToday && todayAttendance != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    InfoChip("Time", TimeHelper.getCurrentTime())
                    InfoChip("Status", todayAttendance.status)
                    InfoChip("Method", todayAttendance.method)
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Time Window",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                        Text(
                            text = "${Constants.ATTENDANCE_START_HOUR}:00 - ${Constants.ATTENDANCE_END_HOUR}:00",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = if (isWithinTimeWindow) "Remaining" else "Status",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                        Text(
                            text = remainingTime,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isWithinTimeWindow) SuccessGreen else ErrorRed
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun InfoChip(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = TextSecondary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = SurfaceLight
        ) {
            Text(
                text = value,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }
    }
}

@Composable
fun MarkAttendanceButton(
    isLoading: Boolean,
    isWithinTimeWindow: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp),
        enabled = !isLoading && isWithinTimeWindow,
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = PrimaryBlue,
            disabledContainerColor = SurfaceLight
        )
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(28.dp),
                color = Color.White,
                strokeWidth = 3.dp
            )
        } else {
            Icon(
                Icons.Default.Fingerprint,
                contentDescription = null,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                if (isWithinTimeWindow) "Mark Attendance" else "Time Window Closed",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun StatsCard(stats: Map<String, Int>) {
    if (stats.isEmpty()) return

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = SurfaceDark
        )
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text(
                text = "Statistics",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem("Total", stats["total"] ?: 0, PrimaryBlue)
                StatItem("Present", stats["present"] ?: 0, SuccessGreen)
                StatItem("Late", stats["late"] ?: 0, WarningYellow)
            }
        }
    }
}

@Composable
fun StatItem(label: String, value: Int, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = value.toString(),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = label,
            fontSize = 14.sp,
            color = TextSecondary
        )
    }
}

@Composable
fun AttendanceHistoryItem(attendance: Attendance) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = SurfaceDark
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(
                            when(attendance.status) {
                                "PRESENT" -> SuccessGreen.copy(alpha = 0.15f)
                                "LATE" -> WarningYellow.copy(alpha = 0.15f)
                                else -> ErrorRed.copy(alpha = 0.15f)
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        when(attendance.status) {
                            "PRESENT" -> Icons.Default.CheckCircle
                            "LATE" -> Icons.Default.Schedule
                            else -> Icons.Default.Cancel
                        },
                        contentDescription = null,
                        tint = when(attendance.status) {
                            "PRESENT" -> SuccessGreen
                            "LATE" -> WarningYellow
                            else -> ErrorRed
                        },
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text(
                        text = TimeHelper.formatDate(attendance.timestamp.toDate()),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = TimeHelper.formatTimestamp(attendance.timestamp.toDate()),
                        fontSize = 14.sp,
                        color = TextSecondary
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = when(attendance.status) {
                        "PRESENT" -> SuccessGreen.copy(alpha = 0.15f)
                        "LATE" -> WarningYellow.copy(alpha = 0.15f)
                        else -> ErrorRed.copy(alpha = 0.15f)
                    }
                ) {
                    Text(
                        text = attendance.status,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = when(attendance.status) {
                            "PRESENT" -> SuccessGreen
                            "LATE" -> WarningYellow
                            else -> ErrorRed
                        },
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = attendance.method,
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }
        }
    }
}

@Composable
fun ErrorMessage(message: String, onDismiss: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = ErrorRed.copy(alpha = 0.15f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Error,
                contentDescription = null,
                tint = ErrorRed
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = message,
                color = ErrorRed,
                fontSize = 14.sp,
                modifier = Modifier.weight(1f)
            )
        }
    }
    LaunchedEffect(message) {
        kotlinx.coroutines.delay(5000)
        onDismiss()
    }
}

@Composable
fun SuccessMessage(message: String, onDismiss: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = SuccessGreen.copy(alpha = 0.15f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = null,
                tint = SuccessGreen
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = message,
                color = SuccessGreen,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )
        }
    }
    LaunchedEffect(message) {
        kotlinx.coroutines.delay(5000)
        onDismiss()
    }
}