package com.udacity.project4.locationreminders.savereminder

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.Geofence.NEVER_EXPIRE
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.LatLng
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.databinding.FragmentSaveReminderBinding
import com.udacity.project4.locationreminders.geofence.GeofenceBroadcastReceiver
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import com.udacity.project4.locationreminders.savereminder.selectreminderlocation.SelectLocationFragment.Companion.ARGUMENTS
import com.udacity.project4.utils.GeofencingConstants.GEOFENCE_RADIUS_IN_METERS
import com.udacity.project4.utils.LandmarkDataObject
import com.udacity.project4.utils.TAG
import com.udacity.project4.utils.errorMessage
import com.udacity.project4.utils.getNavigationResult
import com.udacity.project4.utils.isGpsEnabled
import com.udacity.project4.utils.permissionDeniedFeedback
import com.udacity.project4.utils.permissionGranted
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import com.udacity.project4.utils.toast
import org.koin.android.ext.android.inject

class SaveReminderFragment : BaseFragment() {

    //--------------------------------------------------
    // Attributes
    //--------------------------------------------------

    companion object {
        internal const val ACTION_GEOFENCE_EVENT = "SaveReminderFragment.ACTION_GEOFENCE_EVENT"
        const val GEO_FENCE_ID = "id"
        const val GEO_FENCE_LAT = "lat"
        const val GEO_FENCE_LNG = "lng"
    }

    // Get the view model this time as a single to be shared with the another fragment
    override val _viewModel: SaveReminderViewModel by inject()

    private lateinit var binding: FragmentSaveReminderBinding
    private lateinit var parent: FragmentActivity
    private var alertShouldEnableGps: AlertDialog? = null

    private lateinit var geofencingClient: GeofencingClient
    private lateinit var geofenceData: LandmarkDataObject

    private var currentGeoFenceLocation = ""
    private var currentGeoFenceLatitude = 0.0
    private var currentGeoFenceLongitude = 0.0
    private lateinit var dataItem: ReminderDataItem

    // A PendingIntent for the Broadcast Receiver that handles geofence transitions.
    private val geofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(requireContext(), GeofenceBroadcastReceiver::class.java)
        intent.action = ACTION_GEOFENCE_EVENT
        intent.putExtra(GEO_FENCE_ID, geofenceData.id)
        intent.putExtra(GEO_FENCE_LAT, geofenceData.latLong.latitude)
        intent.putExtra(GEO_FENCE_LNG, geofenceData.latLong.longitude)
        // Use FLAG_UPDATE_CURRENT so that you get the same pending intent back when calling
        // addGeoFences() and removeGeoFences().
        var intentFlagTypeUpdateCurrent = PendingIntent.FLAG_UPDATE_CURRENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Source: https://stackoverflow.com/a/74174664/1354788
            intentFlagTypeUpdateCurrent = PendingIntent.FLAG_MUTABLE
        }
        PendingIntent.getBroadcast(requireContext(), 0, intent, intentFlagTypeUpdateCurrent)
    }

    //--------------------------------------------------
    // Activity Result Callbacks
    //--------------------------------------------------

    //
    private val finePermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isEnabled ->
        Log.d(TAG, "SaveReminderFragment.finePermissionRequest -> isEnabled: $isEnabled")
        if (isEnabled) {
            checkGpsEnabled()
        }
    }
     //

    /*
    private val finePermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        Log.d(TAG, "SaveReminderFragment.locationPermissionRequest -> permissions: $permissions")
        val finePermission = permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false)
        val coarsePermission = permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false)
        if (finePermission && coarsePermission) {
            Log.d(TAG, "SaveReminderFragment.finePermissionRequest -> Permissions granted.")
            checkGpsEnabled()
        } else {
            // No location access granted.
            Log.d(TAG, "SaveReminderFragment.finePermissionRequest -> No location access granted.")
        }
    }
    */

    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        Log.d(TAG, "SaveReminderFragment.locationPermissionRequest -> permissions: $permissions")
        val finePermission = permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false)
        val backgroundPermission = hasBackgroundPermission()
        if (finePermission && backgroundPermission) {
            Log.d(TAG, "SaveReminderFragment.locationPermissionRequest -> Permissions granted.")
            checkGpsEnabled()
        } else {
            // No location access granted.
            Log.d(TAG, "SaveReminderFragment.locationPermissionRequest -> No location access granted.")
        }
    }

    /**
     * Source:
     * https://www.tothenew.com/blog/android-katha-onactivityresult-is-deprecated-now-what/
     */
    private var activityResultLauncher = registerForActivityResult(ActivityResultContracts
        .StartActivityForResult()) {
        Log.d(TAG, "SaveReminderFragment.activityResultLauncher.")
        if (parent.isGpsEnabled()) {
            Log.d(TAG, "SaveReminderFragment.activityResultLauncher -> GPS enabled.")
            addGeoFence()
        } else {
            Log.d(TAG, "SaveReminderFragment.activityResultLauncher -> GPS NOT enabled.")
            parent.permissionDeniedFeedback()
        }
    }

    //--------------------------------------------------
    // Lifecycle Methods
    //--------------------------------------------------

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        Log.d(TAG, "SaveReminderFragment.onCreateView().")

        val layoutId = R.layout.fragment_save_reminder
        binding = DataBindingUtil.inflate(inflater, layoutId, container, false)
        binding.viewModel = _viewModel
        init()

        return binding.root
    }

    private fun init() {
        Log.d(TAG, "SaveReminderFragment.init().")
        setDisplayHomeAsUpEnabled(true)
        parent = requireActivity()
        parent.onBackPressedDispatcher.addCallback(viewLifecycleOwner, backPressHandler)

        getNavigationResult(ARGUMENTS)?.observe(viewLifecycleOwner) { result ->
            val triple = result as Triple<String, Double, Double>
            _viewModel.reminderSelectedLocationStr.value = triple.component1()
            _viewModel.latitude.value = triple.component2()
            _viewModel.longitude.value = triple.component3()
        }

        val menuHost: MenuHost = parent
        addMenu(menuHost)

        geofencingClient = LocationServices.getGeofencingClient(parent)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(TAG, "SaveReminderFragment.onViewCreated().")

        binding.lifecycleOwner = this
        binding.selectLocation.setOnClickListener {
            Log.d(TAG, "SaveReminderFragment.onViewCreated() -> binding.selectLocation.setOnClickListener")
            // Navigate to another fragment to get the user location
            val directions = SaveReminderFragmentDirections
                .actionSaveReminderFragmentToSelectLocationFragment()
            _viewModel.navigationCommand.value = NavigationCommand.To(directions)
        }

        // Use the user entered reminder details to:
        //  1) Check GeoFence permissions.
        //  2) Add a geofencing request.
        //  3) Save the reminder to the local db.
        binding.saveReminder.setOnClickListener {
            Log.d(TAG, "SaveReminderFragment.onViewCreated() -> binding.saveReminder.setOnClickListener.")
            val location = _viewModel.reminderSelectedLocationStr.value
            val lat = _viewModel.latitude.value
            val lng = _viewModel.longitude.value
            if (location != null) {
                currentGeoFenceLocation = location
            }
            if (lat != null) {
                currentGeoFenceLatitude = lat
            }
            if (lng != null) {
                currentGeoFenceLongitude = lng
            }
            if (_viewModel.testing.value == true) {
                _viewModel.validateAndSaveReminder(dataItem)
            } else {
                saveReminderOnDatabase()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "SaveReminderFragment.onDestroy().")
        // Make sure to clear the view model after destroy, as it's a single view model.
        _viewModel.onClear()
        disableDialogs()
    }

    private fun disableDialogs() {
        Log.d(TAG, "SaveReminderFragment.disableDialogs().")
        alertShouldEnableGps?.let {
            if (it.isShowing) {
                it.cancel()
            }
        }
    }

    //--------------------------------------------------
    // Database Methods
    //--------------------------------------------------

    private fun saveReminderOnDatabase() {
        Log.d(TAG, "SaveReminderFragment.saveReminderOnDatabase().")
        val title = _viewModel.reminderTitle.value
        val description = _viewModel.reminderDescription.value
        dataItem = ReminderDataItem(
            title = title,
            description = description,
            location = currentGeoFenceLocation,
            latitude = currentGeoFenceLatitude,
            longitude = currentGeoFenceLongitude
        )
        val paramsValid = _viewModel.validateEnteredData(dataItem)
//        val paramsValid = titleValid &&
//            currentGeoFenceLocation.isNotEmpty() &&
//            currentGeoFenceLatitude != 0.0 &&
//            currentGeoFenceLongitude != 0.0
        if (paramsValid) {
            checkGeoFencePermissions()
        } else {
            Log.d(TAG, "SaveReminderFragment.saveReminderOnDatabase() -> Couldn't create a Geofence.")
        }
    }

    //--------------------------------------------------
    // GeoFence Methods
    //--------------------------------------------------

    /**
     * When we want to add a Geofence, the flow should be as follows:
     *
     * 1.First check if both the required permissions (foreground and background) have been granted.
     * If there is any ungranted permission, request it properly.
     *
     * 2.If all the required permissions have been granted, then we should proceed to check if the
     * device location is on. If the device location is not on, show the location settings dialog
     * and ask the user to enable it.
     *
     * 3. We should automatically attempt to add a Geofence ONLY IF we are certain that the required
     * permissions have been granted and the device location is on.
     */
    private fun checkGeoFencePermissions() {
        Log.d(TAG, "SaveReminderFragment.checkGeoFencePermissions().")
        val backgroundPermission = hasBackgroundPermission()
        val finePermission = parent.permissionGranted(Manifest.permission.ACCESS_FINE_LOCATION)
        // Source: https://developer.android.com/training/location/geofencing#RequestGeofences
        val geofencePermissions = backgroundPermission && finePermission
        if (geofencePermissions) {
            Log.d(TAG, "SaveReminderFragment.checkGeoFencePermissions() -> GeoFence permissions enabled.")
            checkGpsEnabled()
        } else {
            Log.d(TAG, "SaveReminderFragment.checkGeoFencePermissions() -> GeoFence permissions NOT enabled.")
            parent.toast(R.string.location_permission_needed)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                Log.d(TAG, "SaveReminderFragment.checkGeoFencePermissions() -> " +
                    "Asking for 'Manifest.permission.ACCESS_BACKGROUND_LOCATION' and " +
                    "'Manifest.permission.ACCESS_FINE_LOCATION' permissions.")
                locationPermissionRequest.launch(arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                )
            } else {
                Log.d(TAG, "SaveReminderFragment.checkGeoFencePermissions() -> " +
                        "Asking for 'Manifest.permission.ACCESS_FINE_LOCATION' permission.")
                finePermissionRequest.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }
    }

    private fun hasBackgroundPermission(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            parent.permissionGranted(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        } else {
            true
        }

    private fun checkGpsEnabled() {
        Log.d(TAG, "SaveReminderFragment.checkGpsEnabled().")
        if (!parent.isGpsEnabled()) {
            Log.d(TAG, "SaveReminderFragment.checkGpsEnabled() -> GPS is NOT enabled.")
            buildAlertMessageNoGps()
        } else {
            Log.d(TAG, "SaveReminderFragment.checkGpsEnabled() -> GPS is enabled.")
            addGeoFence()
        }
    }

    /**
     * Source:
     * https://stackoverflow.com/a/25175756/1354788
     */
    private fun buildAlertMessageNoGps() {
        Log.d(TAG, "SaveReminderFragment.buildAlertMessageNoGps().")
        val builder = AlertDialog.Builder(parent)
        builder.setMessage(R.string.should_enable_gps)
            .setCancelable(false)
            .setPositiveButton("Yes") { _, _ ->
                activityResultLauncher.launch(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
            }
            .setNegativeButton("No") { dialog, _ ->
                dialog.cancel()
                parent.permissionDeniedFeedback()
            }
        alertShouldEnableGps = builder.create()
        alertShouldEnableGps?.show()
    }

    private fun addGeoFence() {
        Log.d(TAG, "SaveReminderFragment.addGeoFence().")
        // Build the Geofence Object.
        val currentGeofenceData = LandmarkDataObject(currentGeoFenceLocation,
            LatLng(currentGeoFenceLatitude, currentGeoFenceLongitude))
        geofenceData = currentGeofenceData
        val geofence = buildGeoFence(currentGeofenceData)

        // Build the geofence request.
        val geofencingRequest = GeofencingRequest.Builder()
            // The INITIAL_TRIGGER_ENTER flag indicates that geofencing service should trigger a
            // GEOFENCE_TRANSITION_ENTER notification when the geofence is added and if the device
            // is already inside that geofence.
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            // Add the GeoFences to be monitored by geofencing service.
            .addGeofence(geofence)
            .build()

        addGeoFenceRequest(geofencingRequest, geofence)
    }

    private fun buildGeoFence(currentGeofenceData: LandmarkDataObject) : Geofence {
        Log.d(TAG, "SaveReminderFragment.buildGeoFence().")
        return Geofence.Builder()
            // Set the request ID, string to identify the geofence.
            .setRequestId(currentGeofenceData.id)
            // Set the circular region of this geofence.
            .setCircularRegion(
                currentGeofenceData.latLong.latitude,
                currentGeofenceData.latLong.longitude,
                GEOFENCE_RADIUS_IN_METERS
            )
            // Set the expiration duration of the geofence.
            .setExpirationDuration(NEVER_EXPIRE)
            // Set the transition types of interest. Alerts are only generated for these
            // transitions. We track entry and exit transitions in this sample.
            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
            .build()
    }

    @SuppressLint("MissingPermission")
    private fun addGeoFenceRequest(geofencingRequest: GeofencingRequest, geofence: Geofence) {
        Log.d(TAG, "SaveReminderFragment.addGeoFenceRequest().")
        geofencingClient.addGeofences(geofencingRequest, geofencePendingIntent).run {
            addOnSuccessListener {
                // GeoFences added.
                parent.toast(R.string.geofences_added)
                Log.d(TAG, "SaveReminderFragment.addGeoFenceRequest() -> SUCCESS!! Adding GeoFence: ${geofence.requestId}")
                _viewModel.validateAndSaveReminder(dataItem)
            }
            addOnFailureListener {
                // Failed to add GeoFences.
                parent.toast(R.string.geofences_not_added)
                val message = it.message
                if (message != null) {
                    val errorMessage = errorMessage(parent, message.toInt())
                    Log.d(TAG, "SaveReminderFragment.addGeoFenceRequest() -> ERROR!! Error message: $errorMessage")
                }
            }
        }
    }

    //--------------------------------------------------
    // Menu Methods
    //--------------------------------------------------

    private fun addMenu(menuHost: MenuHost) {
        Log.d(TAG, "SaveReminderFragment.addMenu().")
        // Add menu items without using the Fragment Menu APIs. Note how we can tie the MenuProvider
        // to the viewLifecycleOwner and an optional Lifecycle.State (here, RESUMED) to indicate
        // when the menu should be visible.
        menuHost.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {}
            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                when (menuItem.itemId) {
                    android.R.id.home -> {
                        parent.onBackPressedDispatcher.onBackPressed()
                    }
                }
                return true
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    private val backPressHandler = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            _viewModel.navigationCommand.postValue(
                NavigationCommand.Back
            )
        }
    }
}