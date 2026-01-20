package com.example.hostelattendance

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.hostelattendance.data.repository.AuthRepository
import com.example.hostelattendance.ui.attendance.AttendanceScreen
import com.example.hostelattendance.ui.auth.LoginScreen
import com.example.hostelattendance.ui.auth.RegisterScreen
import com.example.hostelattendance.ui.navigation.Screen
import com.example.hostelattendance.ui.theme.HostelAttendanceTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            HostelAttendanceTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AttendanceApp()
                }
            }
        }
    }
}

@Composable
fun AttendanceApp() {
    val navController = rememberNavController()
    val authRepository = AuthRepository()

    val startDestination = if (authRepository.isLoggedIn()) {
        Screen.Attendance.route
    } else {
        Screen.Login.route
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Login.route) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Screen.Attendance.route) {
                        popUpTo(Screen.Login.route) {
                            inclusive = true
                        }
                    }
                },
                onNavigateToRegister = {
                    navController.navigate(Screen.Register.route)
                }
            )
        }

        composable(Screen.Register.route) {
            RegisterScreen(
                onRegisterSuccess = {
                    navController.navigate(Screen.Attendance.route) {
                        popUpTo(Screen.Register.route) {
                            inclusive = true
                        }
                    }
                },
                onNavigateToLogin = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.Attendance.route) {
            AttendanceScreen(
                onLogout = {
                    authRepository.logout()
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) {
                            inclusive = true
                        }
                    }
                }
            )
        }
    }
}