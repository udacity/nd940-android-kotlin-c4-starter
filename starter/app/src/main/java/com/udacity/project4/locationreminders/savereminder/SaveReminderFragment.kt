package com.udacity.project4.locationreminders.savereminder

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import com.google.android.gms.location.Geofence
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
import com.udacity.project4.utils.GeofencingConstants.GEOFENCE_EXPIRATION_IN_MILLISECONDS
import com.udacity.project4.utils.GeofencingConstants.GEOFENCE_RADIUS_IN_METERS
import com.udacity.project4.utils.LandmarkDataObject
import com.udacity.project4.utils.TAG
import com.udacity.project4.utils.getNavigationResult
import com.udacity.project4.utils.permissionGranted
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import com.udacity.project4.utils.toast
import org.koin.android.ext.android.inject
import java.util.concurrent.TimeUnit

class SaveReminderFragment : BaseFragment() {

    companion object {
        internal const val ACTION_GEOFENCE_EVENT = "SaveReminderFragment.ACTION_GEOFENCE_EVENT"
        const val GEO_FENCE_ID = "id"
        const val GEO_FENCE_LAT = "lat"
        const val GEO_FENCE_LNG = "lng"
    }

    // Get the view model this time as a single to be shared with the another fragment
    override val _viewModel: SaveReminderViewModel by inject()
    private lateinit var binding: FragmentSaveReminderBinding
    private lateinit var geofencingClient: GeofencingClient
    private lateinit var parent: FragmentActivity
    private lateinit var geofenceData: LandmarkDataObject

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
        //  1) Add a geofencing request.
        //  2) Save the reminder to the local db.
        binding.saveReminder.setOnClickListener {
            Log.d(TAG, "SaveReminderFragment.onViewCreated() -> binding.saveReminder.setOnClickListener.")
            val location = _viewModel.reminderSelectedLocationStr.value
            val lat = _viewModel.latitude.value
            val lng = _viewModel.longitude.value
            saveReminderOnDatabase(location, lat, lng)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Make sure to clear the view model after destroy, as it's a single view model.
        _viewModel.onClear()
    }

    //--------------------------------------------------
    // Database Methods
    //--------------------------------------------------

    private fun saveReminderOnDatabase(location: String?, lat: Double?, lng: Double?) {
        Log.d(TAG, "SaveReminderFragment.saveReminderOnDatabase().")
        val title = _viewModel.reminderTitle.value
        val description = _viewModel.reminderDescription.value
        val dataItem = ReminderDataItem(
            title = title,
            description = description,
            location = location,
            latitude = lat,
            longitude = lng
        )
        val validationOk = _viewModel.validateAndSaveReminder(dataItem)
        if (validationOk) {
            if (location != null && lat != null && lng != null) {
                addGeoFence(location, lat, lng)
            } else {
                Log.e(TAG, "Couldn't create a Geofence.")
            }
        }
    }

    //--------------------------------------------------
    // GeoFence Methods
    //--------------------------------------------------

    /**
     * Adds a GeoFence for the current clue if needed, and removes any existing GeoFence. This
     * method should be called after the user has granted the location permission. If there are
     * no more GeoFences, we remove the GeoFence and let the viewModel know that the ending hint
     * is now "active."
     */
    private fun addGeoFence(location: String, lat: Double, lng: Double) {
        Log.d(TAG, "SaveReminderFragment.addGeoFence().")
        // Build the Geofence Object
        val currentGeofenceData = LandmarkDataObject(location, LatLng(lat, lng))
        geofenceData = currentGeofenceData
        val geofence = buildGeoFence(currentGeofenceData)

        // Build the geofence request
        val geofencingRequest = GeofencingRequest.Builder()
            // The INITIAL_TRIGGER_ENTER flag indicates that geofencing service should trigger a
            // GEOFENCE_TRANSITION_ENTER notification when the geofence is added and if the device
            // is already inside that geofence.
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            // Add the GeoFences to be monitored by geofencing service.
            .addGeofence(geofence)
            .build()

        // First, remove any existing GeoFences that use our pending intent
        geofencingClient.removeGeofences(geofencePendingIntent).run {
            // Regardless of success/failure of the removal, add the new geofence
            addOnCompleteListener {
                // Add the new geofence request with the new geofence
                if (parent.permissionGranted(Manifest.permission.ACCESS_FINE_LOCATION)) {
                    addGeoFenceRequest(geofencingRequest, geofence)
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun addGeoFenceRequest(geofencingRequest: GeofencingRequest, geofence: Geofence) {
        Log.d(TAG, "SaveReminderFragment.addGeoFenceRequest().")
        geofencingClient.addGeofences(geofencingRequest, geofencePendingIntent).run {
            addOnSuccessListener {
                // GeoFences added.
                parent.toast(R.string.geofences_added)
                Log.d(TAG, "Adding GeoFence: ${geofence.requestId}")
            }
            addOnFailureListener {
                // Failed to add GeoFences.
                parent.toast(R.string.geofences_not_added)
                if ((it.message != null)) {
                    Log.d(TAG, it.message!!)
                }
            }
        }
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
            // Set the expiration duration of the geofence. This geofence gets automatically removed
            // after this period of time.
            .setExpirationDuration(GEOFENCE_EXPIRATION_IN_MILLISECONDS)
            // Set the transition types of interest. Alerts are only generated for these
            // transitions. We track entry and exit transitions in this sample.
            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
            .build()
    }

    //--------------------------------------------------
    // Menu Methods
    //--------------------------------------------------

    private fun addMenu(menuHost: MenuHost) {
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