package com.udacity.project4

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import android.util.Log
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Granularity
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.udacity.project4.utils.permissionGranted
import com.udacity.project4.utils.requestTrackingPermissions
import com.udacity.project4.utils.showRequestPermissionRationale
import com.udacity.project4.utils.toast

/**
 * This Activity shows how to track the user Location
 *
 * Sources:
 * https://stackoverflow.com/questions/60384554/access-background-location-not-working-on-lower-than-q-29-android-versions
 * https://stackoverflow.com/questions/40142331/how-to-request-location-permission-at-runtime/40142454#40142454
 * https://github.com/zoontek/react-native-permissions/issues/620
 */
class TrackingActivity : AppCompatActivity() {

    companion object {
        private const val MY_PERMISSIONS_REQUEST_LOCATION = 99
        private const val MY_PERMISSIONS_REQUEST_BACKGROUND_LOCATION = 66
        private const val UPDATE_INTERVAL = (10 * 1000).toLong() // 10 secs
        private const val FASTEST_INTERVAL: Long = 2000 // 2 secs
    }

    private var fusedLocationProvider: FusedLocationProviderClient? = null
    private val locationRequest: LocationRequest = LocationRequest.Builder(
        Priority.PRIORITY_BALANCED_POWER_ACCURACY, UPDATE_INTERVAL
    ).apply {
        setMinUpdateDistanceMeters(5F)
        setGranularity(Granularity.GRANULARITY_PERMISSION_LEVEL)
        setWaitForAccurateLocation(true)
        setMinUpdateIntervalMillis(FASTEST_INTERVAL)
    }.build()

    /*
    private val locationRequest: LocationRequest = LocationRequest.create().apply {
        interval = 30
        fastestInterval = 10
        priority = LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY
        maxWaitTime = 60
    }
     */

    private var locationCallback: LocationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            val locationList = locationResult.locations
            if (locationList.isNotEmpty()) {
                // The last location in the list is the newest.
                val location = locationList.last()
                toast("Got Location: $location")
            }
        }
    }

    //--------------------------------------------------
    // Lifecycle Methods
    //--------------------------------------------------

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tracking)

        fusedLocationProvider = LocationServices.getFusedLocationProviderClient(this)
        checkLocationPermission()
    }

    @SuppressLint("MissingPermission")
    override fun onResume() {
        super.onResume()
        if (permissionGranted(Manifest.permission.ACCESS_FINE_LOCATION)) {
            fusedLocationProvider?.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
        }
    }

    override fun onPause() {
        super.onPause()
        if (permissionGranted(Manifest.permission.ACCESS_FINE_LOCATION)) {
            fusedLocationProvider?.removeLocationUpdates(locationCallback)
        }
    }

    //--------------------------------------------------
    // Permission Methods
    //--------------------------------------------------

    private fun checkLocationPermission() {
        if (!permissionGranted(Manifest.permission.ACCESS_FINE_LOCATION)) {
            // Should we show an explanation?
            if (showRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)) {
                // Show an explanation to the user *asynchronously* -- don't block this thread
                // waiting for the user's response! After the user sees the explanation, try again
                // to request the permission.
                val title = this.getString(R.string.location_permission_needed)
                val message = this.getString(R.string.location_permission_explanation)
                AlertDialog.Builder(parent)
                    .setTitle(title)
                    .setMessage(message)
                    .setPositiveButton("OK") { _, _ ->
                        // Prompt the user once explanation has been shown.
                        requestLocationPermission()
                    }
                    .create()
                    .show()
            } else {
                // No explanation needed, we can request the permission.
                requestLocationPermission()
            }
        } else {
            checkBackgroundLocationPermission()
        }
    }

    private fun checkBackgroundLocationPermission() {
        if (!permissionGranted(Manifest.permission.ACCESS_BACKGROUND_LOCATION)) {
            requestBackgroundLocationPermission()
        }
    }

    private fun requestLocationPermission() {
        requestTrackingPermissions(
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            MY_PERMISSIONS_REQUEST_LOCATION
        )
    }

    private fun requestBackgroundLocationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            requestTrackingPermissions(
                arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION),
                MY_PERMISSIONS_REQUEST_BACKGROUND_LOCATION
            )
        } else {
            requestTrackingPermissions(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                MY_PERMISSIONS_REQUEST_LOCATION
            )
        }
    }

    @SuppressLint("MissingSuperCall", "MissingPermission")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        Log.d("aaa", "bbb")
        when (requestCode) {
            MY_PERMISSIONS_REQUEST_LOCATION -> {
                onRequestLocationPermissionsResult(grantResults)
                return
            }
            MY_PERMISSIONS_REQUEST_BACKGROUND_LOCATION -> {
                onRequestBackgroundLocationPermissionsResult(grantResults)
                return
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun onRequestLocationPermissionsResult(grantResults: IntArray) {
        // If request is cancelled, the result arrays are empty.
        if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            // Permission was granted, yay! Do the location-related task you need to do.
            if (permissionGranted(Manifest.permission.ACCESS_FINE_LOCATION)) {
                fusedLocationProvider?.requestLocationUpdates(
                    locationRequest,
                    locationCallback,
                    Looper.getMainLooper()
                )

                // Now check background location.
                checkBackgroundLocationPermission()
            }

        } else {
            // Permission denied, boo! Disable the functionality that depends on this permission.
            toast("Permission denied")

            // Check if we are in a state where the user has denied the permission and
            // selected Don't ask again.
            if (!showRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)) {
                startActivity(
                    Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.fromParts("package", this.packageName, null),
                    ),
                )
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun onRequestBackgroundLocationPermissionsResult(grantResults: IntArray) {
        // If request is cancelled, the result arrays are empty.
        if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            // Permission was granted, yay! Do the location-related task you need to do.
            if (permissionGranted(Manifest.permission.ACCESS_FINE_LOCATION)) {
                fusedLocationProvider?.requestLocationUpdates(
                    locationRequest,
                    locationCallback,
                    Looper.getMainLooper()
                )
                toast("Granted Background Location Permission")
            }
        } else {
            // Permission denied, boo! Disable the functionality that depends on this permission.
            toast("Permission denied")
        }
    }
}