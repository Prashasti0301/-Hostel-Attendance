package com.example.hostelattendance.ui.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class AttendanceRecord(
    val id: String,
    val userName: String,
    val userEmail: String,
    val date: String,
    val time: String,
    val status: String,
    val method: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScreen(
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val db = FirebaseFirestore.getInstance()
    var attendanceRecords by remember { mutableStateOf<List<AttendanceRecord>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedDate by remember { mutableStateOf("Today") }

    LaunchedEffect(selectedDate) {
        scope.launch {
            isLoading = true
            try {
                // Fetch all attendance records
                val snapshot = db.collection("attendance")
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .limit(100)
                    .get()
                    .await()

                val records = snapshot.documents.mapNotNull { doc ->
                    try {
                        val userId = doc.getString("userId") ?: return@mapNotNull null

                        // Fetch user details
                        val userDoc = db.collection("users").document(userId).get().await()

                        AttendanceRecord(
                            id = doc.id,
                            userName = userDoc.getString("name") ?: "Unknown",
                            userEmail = userDoc.getString("email") ?: "Unknown",
                            date = doc.getTimestamp("timestamp")?.toDate()?.toString() ?: "",
                            time = doc.getTimestamp("timestamp")?.toDate()?.toString() ?: "",
                            status = doc.getString("status") ?: "Unknown",
                            method = doc.getString("method") ?: "Unknown"
                        )
                    } catch (e: Exception) {
                        null
                    }
                }

                attendanceRecords = records
            } catch (e: Exception) {
                // Handle error
            }
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Admin Panel - All Attendance") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.padding(16.dp))
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        Text("Total Records: ${attendanceRecords.size}")
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    items(attendanceRecords) { record ->
                        Card(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Name: ${record.userName}")
                                Text("Email: ${record.userEmail}")
                                Text("Status: ${record.status}")
                                Text("Time: ${record.time}")
                                Text("Method: ${record.method}")
                            }
                        }
                    }
                }
            }
        }
    }
}