package com.example.hostelattendance.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.firebase.firestore.GeoPoint
import kotlinx.coroutines.tasks.await

class LocationHelper(private val context: Context) {
    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    // JUET Guna Hostel coordinates
    private val HOSTEL_LATITUDE = 24.436924752254967
    private val HOSTEL_LONGITUDE = 77.15831449580436
    private val HOSTEL_RADIUS_METERS = 100.0 // 100 meter radius

    fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    suspend fun getCurrentLocation(): Location? {
        if (!hasLocationPermission()) {
            println("DEBUG LocationHelper: No permission")
            return null
        }

        return try {
            println("DEBUG LocationHelper: Requesting location...")
            val cancellationTokenSource = CancellationTokenSource()
            val location = fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                cancellationTokenSource.token
            ).await()
            println("DEBUG LocationHelper: Got location = $location")
            location
        } catch (e: Exception) {
            println("DEBUG LocationHelper: Error = ${e.message}")
            null
        }
    }

    // Calculate distance from current location to hostel (returns meters)
    fun calculateDistance(currentLocation: Location): Float {
        val hostelLocation = Location("hostel").apply {
            latitude = HOSTEL_LATITUDE
            longitude = HOSTEL_LONGITUDE
        }
        val distance = currentLocation.distanceTo(hostelLocation)
        println("DEBUG LocationHelper: Distance calculated = $distance meters")
        return distance
    }

    // Check if user is within hostel boundary
    fun isWithinBoundary(userLocation: Location): Boolean {
        val distance = calculateDistance(userLocation)
        val isWithin = distance <= HOSTEL_RADIUS_METERS
        println("DEBUG LocationHelper: Within boundary = $isWithin (distance: $distance, max: $HOSTEL_RADIUS_METERS)")
        return isWithin
    }

    fun locationToGeoPoint(location: Location): GeoPoint {
        return GeoPoint(location.latitude, location.longitude)
    }

    fun getDistanceFromHostel(userLocation: Location): Double {
        return calculateDistance(userLocation).toDouble()
    }
}