package com.example.hostelattendance.ui.navigation

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Register : Screen("register")
    object Attendance : Screen("attendance")
    object Admin : Screen("admin")
}


