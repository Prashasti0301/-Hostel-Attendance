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
import com.example.hostelattendance.util.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceScreen(
    onLogout: () -> Unit,
    viewModel: AttendanceViewModel = viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val locationHelper = remember { LocationHelper(context) }
    val biometricHelper = remember { BiometricHelper(context) }

    val attendanceState by viewModel.attendanceState.collectAsState()

    var currentLocation by remember { mutableStateOf<Location?>(null) }
    var showMethodDialog by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.setLocationHelper(locationHelper)
        viewModel.checkAttendanceStatus()
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            scope.launch {
                currentLocation = locationHelper.getCurrentLocation()
                if (currentLocation != null) {
                    showMethodDialog = true
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Attendance System",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                        attendanceState.currentUser?.let { user ->
                            Text(
                                user.name,
                                fontSize = 14.sp,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refreshData() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                    IconButton(onClick = { showLogoutDialog = true }) {
                        Icon(Icons.Default.Logout, contentDescription = "Logout")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF3949AB),
                    titleContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFFF5F5F5))
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
                if (!attendanceState.hasMarkedToday && attendanceState.isWithinTimeWindow) {
                    item {
                        MarkAttendanceButton(
                            isLoading = attendanceState.isLoading,
                            onClick = {
                                if (locationHelper.hasLocationPermission()) {
                                    scope.launch {
                                        currentLocation = locationHelper.getCurrentLocation()
                                        if (currentLocation != null) {
                                            showMethodDialog = true
                                        }
                                    }
                                } else {
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
                        color = Color(0xFF1A237E),
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }

                if (attendanceState.attendanceHistory.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = Color.White
                            )
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "No attendance records yet",
                                    color = Color.Gray,
                                    fontSize = 16.sp
                                )
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

    // Biometric Dialog
    if (showMethodDialog && currentLocation != null) {
        BiometricAuthDialog(
            onDismiss = { showMethodDialog = false },
            onAuthenticate = {
                if (biometricHelper.isBiometricAvailable()) {
                    biometricHelper.showBiometricPrompt(
                        activity = context as FragmentActivity,
                        onSuccess = {
                            currentLocation?.let { loc ->
                                viewModel.markAttendance(AttendanceMethod.BIOMETRIC, loc)
                            }
                            showMethodDialog = false
                        },
                        onError = { _ ->
                            showMethodDialog = false
                        }
                    )
                } else {
                    showMethodDialog = false
                }
            }
        )
    }

    // Logout Confirmation Dialog
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Logout") },
            text = { Text("Are you sure you want to logout?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutDialog = false
                        onLogout()
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = Color(0xFFC62828)
                    )
                ) {
                    Text("Logout")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancel")
                }
            }
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
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (hasMarkedToday) Color(0xFF4CAF50) else Color(0xFF3949AB)
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
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
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = TimeHelper.getCurrentDate(),
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }

                Icon(
                    if (hasMarkedToday) Icons.Default.CheckCircle
                    else Icons.Default.AccessTime,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(48.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Divider(color = Color.White.copy(alpha = 0.3f))

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
                            color = Color.White.copy(alpha = 0.8f)
                        )
                        Text(
                            text = "${Constants.ATTENDANCE_START_HOUR}:00 - ${Constants.ATTENDANCE_END_HOUR}:00",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = if (isWithinTimeWindow) "Remaining" else "Status",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                        Text(
                            text = remainingTime,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
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
            color = Color.White.copy(alpha = 0.8f)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color.White.copy(alpha = 0.2f)
        ) {
            Text(
                text = value,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }
    }
}

@Composable
fun MarkAttendanceButton(
    isLoading: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp),
        enabled = !isLoading,
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFF4CAF50)
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
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
                "Mark Attendance",
                fontSize = 18.sp,
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
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Statistics",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1A237E)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem("Total", stats["total"] ?: 0, Color(0xFF3949AB))
                StatItem("Present", stats["present"] ?: 0, Color(0xFF4CAF50))
                StatItem("Late", stats["late"] ?: 0, Color(0xFFFF9800))
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
                .background(color.copy(alpha = 0.1f)),
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
            color = Color.Gray
        )
    }
}

@Composable
fun AttendanceHistoryItem(attendance: Attendance) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
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
                                "PRESENT" -> Color(0xFF4CAF50).copy(alpha = 0.1f)
                                "LATE" -> Color(0xFFFF9800).copy(alpha = 0.1f)
                                else -> Color(0xFFF44336).copy(alpha = 0.1f)
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
                            "PRESENT" -> Color(0xFF4CAF50)
                            "LATE" -> Color(0xFFFF9800)
                            else -> Color(0xFFF44336)
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
                        color = Color(0xFF1A237E)
                    )
                    Text(
                        text = TimeHelper.formatTimestamp(attendance.timestamp.toDate()),
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = when(attendance.status) {
                        "PRESENT" -> Color(0xFF4CAF50).copy(alpha = 0.1f)
                        "LATE" -> Color(0xFFFF9800).copy(alpha = 0.1f)
                        else -> Color(0xFFF44336).copy(alpha = 0.1f)
                    }
                ) {
                    Text(
                        text = attendance.status,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = when(attendance.status) {
                            "PRESENT" -> Color(0xFF4CAF50)
                            "LATE" -> Color(0xFFFF9800)
                            else -> Color(0xFFF44336)
                        },
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = attendance.method,
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
        }
    }
}

@Composable
fun BiometricAuthDialog(
    onDismiss: () -> Unit,
    onAuthenticate: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                Icons.Default.Fingerprint,
                contentDescription = null,
                tint = Color(0xFF3949AB),
                modifier = Modifier.size(48.dp)
            )
        },
        title = {
            Text(
                "Verify Identity",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text("Use biometric authentication to mark your attendance")
        },
        confirmButton = {
            Button(
                onClick = onAuthenticate,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF3949AB)
                )
            ) {
                Text("Authenticate")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun ErrorMessage(message: String, onDismiss: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFFEBEE)
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
                tint = Color(0xFFC62828)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = message,
                color = Color(0xFFC62828),
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
            containerColor = Color(0xFFE8F5E9)
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
                tint = Color(0xFF2E7D32)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = message,
                color = Color(0xFF2E7D32),
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