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

    // Use values from Constants for consistency
    private val HOSTEL_LATITUDE = Constants.HOSTEL_CENTER.latitude
    private val HOSTEL_LONGITUDE = Constants.HOSTEL_CENTER.longitude
    private val HOSTEL_RADIUS_METERS = Constants.HOSTEL_RADIUS_METERS

    fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    @Throws(SecurityException::class)
    suspend fun getCurrentLocation(): Location? {
        if (!hasLocationPermission()) {
            println("DEBUG LocationHelper: No permission")
            throw SecurityException("Location permission not granted")
        }

        return try {
            println("DEBUG LocationHelper: Requesting location...")
            val cancellationTokenSource = CancellationTokenSource()

            // Suppress the warning since we've already checked permission
            @Suppress("MissingPermission")
            val location = fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                cancellationTokenSource.token
            ).await()

            println("DEBUG LocationHelper: Got location = $location")
            println("DEBUG LocationHelper: Latitude = ${location?.latitude}, Longitude = ${location?.longitude}")
            location
        } catch (e: Exception) {
            println("DEBUG LocationHelper: Error = ${e.message}")
            e.printStackTrace()
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
        println("DEBUG LocationHelper: Current: (${currentLocation.latitude}, ${currentLocation.longitude})")
        println("DEBUG LocationHelper: Hostel: ($HOSTEL_LATITUDE, $HOSTEL_LONGITUDE)")
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

    // Helper method to get formatted distance message
    fun getDistanceMessage(userLocation: Location): String {
        val distance = calculateDistance(userLocation)
        return if (distance < 1000) {
            "${distance.toInt()} meters from hostel"
        } else {
            "${"%.2f".format(distance / 1000)} km from hostel"
        }
    }
}