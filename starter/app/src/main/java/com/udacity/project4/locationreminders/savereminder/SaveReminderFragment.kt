package com.udacity.project4.locationreminders.savereminder

import android.Manifest
import android.annotation.TargetApi
import android.app.Activity
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.content.IntentSender.SendIntentException
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import androidx.navigation.fragment.NavHostFragment
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.material.snackbar.Snackbar
import com.udacity.project4.BuildConfig
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.databinding.FragmentSaveReminderBinding
import com.udacity.project4.locationreminders.geofence.GeofenceBroadcastReceiver
import com.udacity.project4.locationreminders.geofence.GeofenceTransitionsJobIntentService.Companion.ACTION_GEOFENCE_EVENT
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import org.koin.android.ext.android.inject


class SaveReminderFragment : BaseFragment() {
    //Get the view model this time as a single to be shared with the another fragment
    override val _viewModel: SaveReminderViewModel by inject()
    private lateinit var binding: FragmentSaveReminderBinding
    lateinit var manager: LocationManager
    private val MY_PERMISSIONS_REQUEST_LOCATION = 145
    private val MY_PERMISSIONS_REQUEST_BACKGROUND_LOCATION = 948
    lateinit var geofencingClient: GeofencingClient
    lateinit var geofenceList: MutableList<Geofence>
    private val runningQOrLater = android.os.Build.VERSION.SDK_INT >=
            android.os.Build.VERSION_CODES.Q
    private val REQUEST_FOREGROUND_AND_BACKGROUND_PERMISSION_RESULT_CODE = 456
    private val REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE = 478
    private val REQUEST_TURN_DEVICE_LOCATION_ON = 451
    private lateinit var tempReminderDataItem: ReminderDataItem
    val BACKGROUND_LOCATION_PERMISSION_INDEX = 75
    val LOCATION_PERMISSION_INDEX = 21
    val REQUESTCODE_TURNON_GPS = 400
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_save_reminder, container, false)

        setDisplayHomeAsUpEnabled(true)

        binding.viewModel = _viewModel
        manager = (requireActivity().getSystemService(Context.LOCATION_SERVICE) as LocationManager)
        geofencingClient = LocationServices.getGeofencingClient(requireActivity())
        geofenceList = arrayListOf()
        return binding.root
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.lifecycleOwner = this
        binding.selectLocation.setOnClickListener {
            //            Navigate to another fragment to get the user location
            _viewModel.navigationCommand.value =
                NavigationCommand.To(SaveReminderFragmentDirections.actionSaveReminderFragmentToSelectLocationFragment())
        }

        binding.saveReminder.setOnClickListener {
            val title = _viewModel.reminderTitle.value
            val description = _viewModel.reminderDescription.value
            val latitude = _viewModel.latitude.value
            val longitude = _viewModel.longitude.value
            val droppedName = _viewModel.droppedName.value
            _viewModel.reminderSelectedLocationStr.postValue("$latitude,$longitude")
            val location = "$latitude,$longitude"
            if (title.isNullOrEmpty()) {
                Toast.makeText(context, getString(R.string.add_title), Toast.LENGTH_LONG).show()
            } else if (description.isNullOrEmpty()) {
                Toast.makeText(
                    context,
                    getString(R.string.please_add_description),
                    Toast.LENGTH_LONG
                ).show()
            } else if (latitude?.isNaN() == true || longitude?.isNaN() == true || latitude == 0.0 || longitude == 0.0 || latitude == null && longitude == null) {
                Toast.makeText(context, getString(R.string.please_add_location), Toast.LENGTH_LONG)
                    .show()
            } else {
                tempReminderDataItem = ReminderDataItem(
                    title = title, description = description,
                    latitude = latitude, longitude = longitude, location = droppedName
                )
                if (checkGpsStatus()) {
                    if (!foregroundAndBackgroundLocationPermissionApproved()) {
                        requestForegroundAndBackgroundLocationPermissions()
                    } else {
                        addGeofencing(tempReminderDataItem)
                    }
                } else {
                    turnOnGPS()
                }
            }
        }
    }

    private fun addGeofencing(tempReminderDataItem: ReminderDataItem) {
        val intent = Intent(requireContext(), GeofenceBroadcastReceiver::class.java)
        intent.action = ACTION_GEOFENCE_EVENT
        // We use FLAG_UPDATE_CURRENT so that we get the same pending intent back when calling
        // addGeofences() and removeGeofences(). // not done ?
        val geofencePendingIntent =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                PendingIntent.getBroadcast(
                    requireContext(),
                    0,
                    intent,
                    PendingIntent.FLAG_MUTABLE
                )
            } else {
                PendingIntent.getBroadcast(
                    requireContext(),
                    0,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT
                )
            }

        val geofenceList = Geofence.Builder()
            // Set the request ID of the geofence. This is a string to identify this
            // geofence.
            .setRequestId(tempReminderDataItem.id)

            .setCircularRegion(
                tempReminderDataItem.latitude!!,
                tempReminderDataItem.longitude!!,
                250F
            )
            .setExpirationDuration(120000)
            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
            .build()

        val geofencingRequest = GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofence(geofenceList)
            .build()

        geofencingClient.addGeofences(geofencingRequest, geofencePendingIntent).run {
            addOnSuccessListener {
                _viewModel.validateAndSaveReminder(tempReminderDataItem)
//                findNavController().popBackStack()
//                val navController = Navigation.findNavController(requireView())
//                navController.popBackStack()
                val navController = NavHostFragment.findNavController(this@SaveReminderFragment)
//                navController.previousBackStackEntry?.savedStateHandle?.set("isDelete", "true/false")
                navController.popBackStack()
            }
            addOnFailureListener {
                Toast.makeText(
                    context,
                    getString(R.string.failed_please_try_again),
                    Toast.LENGTH_LONG
                ).show()
//                val navController = Navigation.findNavController(requireView())
//                navController.popBackStack()
            }
        }
    }


    @TargetApi(29)
    private fun foregroundAndBackgroundLocationPermissionApproved(): Boolean {
        val foregroundLocationApproved = (
                PackageManager.PERMISSION_GRANTED ==
                        ActivityCompat.checkSelfPermission(
                            requireContext(),
                            Manifest.permission.ACCESS_FINE_LOCATION
                        ))
        val backgroundPermissionApproved =
            if (runningQOrLater) {
                PackageManager.PERMISSION_GRANTED ==
                        ActivityCompat.checkSelfPermission(
                            requireContext(), Manifest.permission.ACCESS_BACKGROUND_LOCATION
                        )
            } else {
                true
            }
        return foregroundLocationApproved && backgroundPermissionApproved
    }

    @TargetApi(29)
    private fun requestForegroundAndBackgroundLocationPermissions() {
        if (foregroundAndBackgroundLocationPermissionApproved())
            return
        var permissionsArray = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        val resultCode = when {
            runningQOrLater -> {
                permissionsArray += Manifest.permission.ACCESS_BACKGROUND_LOCATION
                REQUEST_FOREGROUND_AND_BACKGROUND_PERMISSION_RESULT_CODE
            }
            else -> REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE
        }
//        Log.d(TAG, "Request foreground only location permission")
        ActivityCompat.requestPermissions(
            requireActivity(),
            permissionsArray,
            resultCode
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
//        Log.d(TAG, "onRequestPermissionResult")

        if (
            grantResults.isEmpty() ||
            grantResults[LOCATION_PERMISSION_INDEX] == PackageManager.PERMISSION_DENIED ||
            (requestCode == REQUEST_FOREGROUND_AND_BACKGROUND_PERMISSION_RESULT_CODE &&
                    grantResults[BACKGROUND_LOCATION_PERMISSION_INDEX] ==
                    PackageManager.PERMISSION_DENIED)
        ) {
            Snackbar.make(
                binding.root,
                R.string.permission_denied_explanation,
                Snackbar.LENGTH_INDEFINITE
            ).setAction(R.string.settings) {
                startActivity(Intent().apply {
                    action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                    data = Uri.fromParts("package", BuildConfig.APPLICATION_ID, null)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                })
            }.show()
        } else {
            checkDeviceLocationSettingsAndStartGeofence()
        }
    }

    private fun checkDeviceLocationSettingsAndStartGeofence(resolve: Boolean = true) {
        val locationRequest = LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_LOW_POWER
        }
        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)
        val settingsClient = LocationServices.getSettingsClient(requireActivity())
        val locationSettingsResponseTask =
            settingsClient.checkLocationSettings(builder.build())
        locationSettingsResponseTask.addOnFailureListener { exception ->
            if (exception is ResolvableApiException && resolve) {
                try {
                    exception.startResolutionForResult(
                        requireActivity(),
                        REQUEST_TURN_DEVICE_LOCATION_ON
                    )
                } catch (sendEx: IntentSender.SendIntentException) {
//                    LocationServicesog.d(TAG, "Error getting location settings resolution: " + sendEx.message)
                    Toast.makeText(requireContext(), sendEx.localizedMessage, Toast.LENGTH_LONG)
                        .show()
                }
            } else {
                Snackbar.make(
                    binding.root,
                    R.string.location_required_error, Snackbar.LENGTH_INDEFINITE
                ).setAction(android.R.string.ok) {
                    checkDeviceLocationSettingsAndStartGeofence()
                }.show()
            }
        }
        locationSettingsResponseTask.addOnCompleteListener {
            if (it.isSuccessful) {
                addGeofencing(tempReminderDataItem)
            } else {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.failed_please_try_again),
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_TURN_DEVICE_LOCATION_ON) {
            checkDeviceLocationSettingsAndStartGeofence(false)
        } else
            if (requestCode === REQUESTCODE_TURNON_GPS) {
                when (resultCode) {
                    Activity.RESULT_OK -> {}
                    Activity.RESULT_CANCELED -> {}
                    else -> {}
                }
            }
    }

    private fun checkGpsStatus(): Boolean {
        val locationManager =
            requireActivity().getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
    }

    fun goToTurnOnGPS() {
        val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
        startActivity(intent)
    }

    private fun turnOnGPS() {
        val locationRequest = LocationRequest.create()

        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)

        val settingsClient = LocationServices.getSettingsClient(requireActivity())
        val task = settingsClient.checkLocationSettings(builder.build())

        task.addOnSuccessListener(requireActivity(), OnSuccessListener<LocationSettingsResponse?> {
//            Log.d("GPS_main", "OnSuccess")
            // GPS is ON
        })

        task.addOnFailureListener(requireActivity(), OnFailureListener { e ->
//            Log.d("GPS_main", "GPS off")
            Toast.makeText(
                requireContext(),
                getString(R.string.please_enable_gps),
                Toast.LENGTH_LONG
            ).show()

            // GPS off
            if (e is ResolvableApiException) {
                val resolvable = e
                try {
                    resolvable.startResolutionForResult(requireActivity(), REQUESTCODE_TURNON_GPS)
                } catch (e1: SendIntentException) {
                    e1.printStackTrace()
                }
            }
        })
    }

}
