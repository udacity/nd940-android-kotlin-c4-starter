package com.udacity.project4.locationreminders.savereminder.selectreminderlocation

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.location.Address
import android.location.Geocoder
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Granularity
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices.getFusedLocationProviderClient
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.databinding.FragmentSelectLocationBinding
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.utils.GeofencingConstants.GEOFENCE_RADIUS_IN_METERS
import com.udacity.project4.utils.TAG
import com.udacity.project4.utils.permissionGranted
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import com.udacity.project4.utils.setNavigationResult
import com.udacity.project4.utils.showRequestPermissionRationale
import com.udacity.project4.utils.toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.ext.android.inject
import java.io.IOException
import java.util.*


class SelectLocationFragment : BaseFragment(), OnMapReadyCallback {

    //--------------------------------------------------
    // Attributes
    //--------------------------------------------------

    companion object {
        private const val UPDATE_INTERVAL = (10 * 1000).toLong() // 10 secs
        private const val FASTEST_INTERVAL: Long = 2000 // 2 secs
        private const val FINE = Manifest.permission.ACCESS_FINE_LOCATION
        private const val BACK = Manifest.permission.ACCESS_BACKGROUND_LOCATION
        const val ARGUMENTS = "args"
    }

    // Use Koin to get the view model of the SaveReminder
    override val _viewModel: SaveReminderViewModel by inject()
    private var map: GoogleMap? = null
    private var currentMarker: Marker? = null
    private var currentCircleMarker: Circle? = null
    private var currentPOI: PointOfInterest? = null
    private lateinit var poiLatLng : LatLng

    private var alertPleaseAcceptAllowAllTime: AlertDialog? = null
    private var alertShouldEnableGps: AlertDialog? = null

    private lateinit var binding: FragmentSelectLocationBinding
    private lateinit var parent: FragmentActivity

    private var poiLocation = ""

    //--------------------------------------------------
    // GPS Location Attributes
    //--------------------------------------------------

    private var fusedLocationProvider: FusedLocationProviderClient? = null
    private val locationRequest: LocationRequest = LocationRequest.Builder(
        Priority.PRIORITY_BALANCED_POWER_ACCURACY, UPDATE_INTERVAL
    ).apply {
        setMinUpdateDistanceMeters(5F)
        setGranularity(Granularity.GRANULARITY_PERMISSION_LEVEL)
        setWaitForAccurateLocation(true)
        setMinUpdateIntervalMillis(FASTEST_INTERVAL)
    }.build()

    private var locationCallback: LocationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            Log.d(TAG, "SelectLocationFragment.locationCallback.onLocationResult().")
            val locationList = locationResult.locations
            if (locationList.isNotEmpty()) {
                // The last location in the list is the newest.
                val location = locationList.last()
                goToLocation(location.latitude, location.longitude)

                // Inform user to select a POI (Point of Interest)
                parent.toast(R.string.select_poi)
            }
        }
    }

    //--------------------------------------------------
    // Activity Result Callbacks
    //--------------------------------------------------

    /**
     * Source:
     * https://developer.android.com/training/permissions/requesting#allow-system-manage-request-code
     */

    // Register the permissions callback, which handles the user's response to the system
    // permissions dialog. Save the return value, an instance of ActivityResultLauncher. You can use
    // either a val, as shown in this snippet, or a lateinit var in your onAttach() or onCreate()
    // method.
    private val requestFinePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        Log.d(TAG, "SelectLocationFragment.requestFinePermissionLauncher() -> isGranted: $isGranted")
        if (isGranted) {
            // Permission is granted. Continue the action or workflow in your app.
            checkBackgroundLocationPermission()
        } else {
            // Explain to the user that the feature is unavailable because the feature requires a
            // permission that the user has denied. At the same time, respect the user's decision.
            // Don't link to system settings in an effort to convince the user to change their
            // decision.
            permissionDeniedFeedback()
        }
    }

    private val requestBackPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        Log.d(TAG, "SelectLocationFragment.requestBackPermissionLauncher() -> isGranted: $isGranted")
        if (isGranted) {
            lifecycleScope.launch {
                delay(2000)
                checkGpsEnabled()
            }
        } else {
            permissionDeniedFeedback()
        }
    }

    /**
     * Source:
     * https://www.tothenew.com/blog/android-katha-onactivityresult-is-deprecated-now-what/
     */
    private var activityResultLauncher = registerForActivityResult(ActivityResultContracts
        .StartActivityForResult()) {
        Log.d(TAG, "SelectLocationFragment.activityResultLauncher.")
        if (isGpsEnabled()) {
            parent.toast(R.string.gps_enabled)
            requestTracking()
        } else {
            permissionDeniedFeedback()
        }
    }

    //--------------------------------------------------
    // Lifecycle Methods
    //--------------------------------------------------

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val layoutId = R.layout.fragment_select_location
        binding = DataBindingUtil.inflate(inflater, layoutId, container, false)

        binding.viewModel = _viewModel
        binding.lifecycleOwner = this
        init()

        return binding.root
    }

    private fun init() {
        parent = requireActivity()
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, backPressHandler)

        setDisplayHomeAsUpEnabled(true)

        fusedLocationProvider = getFusedLocationProviderClient(parent)
        startMapFeature()
        initMenus()
        checkLocationPermission()

        binding.saveButton.setOnClickListener {
            Log.d(TAG, "+++++++++++++++ SelectLocationFragment.init() -> poiLatLng: $poiLatLng")
            Log.d(TAG, "+++++++++++++++ SelectLocationFragment.init() -> poiLocation: $poiLocation")

            onLocationSelected(
                location = poiLocation,
                latitude = poiLatLng.latitude,
                longitude = poiLatLng.longitude
            )
        }
    }

    override fun onPause() {
        super.onPause()
        Log.d(TAG, "SelectLocationFragment.onPause().")
        if (parent.permissionGranted(FINE)) {
            fusedLocationProvider?.removeLocationUpdates(locationCallback)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "SelectLocationFragment.onDestroy().")
        disableDialogs()
    }

    private fun disableDialogs() {
        alertPleaseAcceptAllowAllTime?.let {
            if (it.isShowing) {
                it.cancel()
            }
        }
        alertShouldEnableGps?.let {
            if (it.isShowing) {
                it.cancel()
            }
        }
    }

    //--------------------------------------------------
    // Menu Methods
    //--------------------------------------------------

    private fun initMenus() {
        val menuHost: MenuHost = requireActivity()
        addMenu(menuHost)
    }

    @Suppress("UNUSED_EXPRESSION")
    private fun addMenu(menuHost: MenuHost) {
        // Add menu items without using the Fragment Menu APIs. Note how we can tie the
        // MenuProvider to the viewLifecycleOwner and an optional Lifecycle.State (here, RESUMED)
        // to indicate when the menu should be visible.
        menuHost.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.map_options, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                when (menuItem.itemId) {
                    R.id.normal_map -> {
                        map?.mapType = GoogleMap.MAP_TYPE_NORMAL
                        true
                    }
                    R.id.hybrid_map -> {
                        map?.mapType = GoogleMap.MAP_TYPE_HYBRID
                        true
                    }
                    R.id.satellite_map -> {
                        map?.mapType = GoogleMap.MAP_TYPE_SATELLITE
                        true
                    }
                    R.id.terrain_map -> {
                        map?.mapType = GoogleMap.MAP_TYPE_TERRAIN
                        true
                    }
                    android.R.id.home -> {
                        requireActivity().onBackPressedDispatcher.onBackPressed()
                        true
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

    //--------------------------------------------------
    // Permission Methods
    //--------------------------------------------------

    private fun checkLocationPermission() {
        Log.d(TAG, "SelectLocationFragment.checkLocationPermission().")
        when {
            parent.permissionGranted(FINE) -> {
                // You can use the API that requires the permission.
                Log.d(TAG, "SelectLocationFragment.checkLocationPermission() -> [1]")
                checkBackgroundLocationPermission()
            }
            parent.showRequestPermissionRationale(FINE) -> {
                // In an educational UI, explain to the user why your app requires this permission
                // for a specific feature to behave as expected, and what features are disabled if
                // it's declined. In this UI, include a "cancel" or "no thanks" button that lets the
                // user continue using your app without granting the permission.
                // --------------------------------------------------
                // Show an explanation to the user *asynchronously* -- don't block this thread
                // waiting for the user's response! After the user sees the explanation, try again
                // to request the permission.
                Log.d(TAG, "SelectLocationFragment.checkLocationPermission() -> [2]")
                val title = this.getString(R.string.location_permission_needed)
                val message = this.getString(R.string.location_permission_explanation)
                AlertDialog.Builder(parent)
                    .setTitle(title)
                    .setMessage(message)
                    .setPositiveButton("OK") { _, _ ->
                        // Prompt the user once explanation has been shown.
                        requestFinePermissionLauncher.launch(FINE)
                    }
                    .create()
                    .show()
            }
            else -> {
                // You can directly ask for the permission.
                // The registered ActivityResultCallback gets the result of this request.
                Log.d(TAG, "SelectLocationFragment.checkLocationPermission() -> [3]")
                requestFinePermissionLauncher.launch(FINE)
            }
        }
    }

    private fun checkBackgroundLocationPermission() {
        Log.d(TAG, "SelectLocationFragment.checkBackgroundLocationPermission().")
        when {
            parent.permissionGranted(BACK) -> {
                // You can use the API that requires the permission.
                lifecycleScope.launch {
                    delay(2000)
                    checkGpsEnabled()
                }
            }
            else -> {
                // You can directly ask for the permission.
                // The registered ActivityResultCallback gets the result of this request.
                requestBackgroundLocationPermission()
            }
        }
    }

    /**
     * This is the only method that will call the "Location permission" settings screen
     * (https://bit.ly/41VLahm) of your app, showing the user 4 possible options:
     * - "Allow all the time"
     * - "Allow only while using the app"
     * - "Ask every time"
     * - "Don't allow"
     * This settings screen will only be called if the app has Manifest.permission.ACCESS_FINE_LOCATION.
     * Once the app have it, then, we need the Manifest.permission.ACCESS_BACKGROUND_LOCATION
     * (in order to track the user GPS's location).
     */
    private fun requestBackgroundLocationPermission() {
        Log.d(TAG, "SelectLocationFragment.requestBackgroundLocationPermission().")
        val builder = AlertDialog.Builder(parent)
        builder.setMessage(R.string.please_accept_allow_all_time)
            .setCancelable(false)
            .setPositiveButton("Yes") { _, _ ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    requestBackPermissionLauncher.launch(BACK)
                } else {
                    requestFinePermissionLauncher.launch(FINE)
                }
            }
            .setNegativeButton("No") { dialog, _ ->
                dialog.cancel()
                permissionDeniedFeedback()
            }
        alertPleaseAcceptAllowAllTime = builder.create()
        alertPleaseAcceptAllowAllTime?.show()
    }

    //--------------------------------------------------
    // Maps Methods
    //--------------------------------------------------

    private fun startMapFeature() {
        Log.d(TAG, "SelectLocationFragment.startMapFeature().")
        val mapFragment = childFragmentManager
            .findFragmentById(R.id.map_fragment) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    /**
     * Manipulates the map once available. This callback is triggered when the map is ready to
     * be used. This is where we can add markers or lines, add listeners or move the camera.
     * In this case, we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    override fun onMapReady(googleMap: GoogleMap) {
        Log.d(TAG, "SelectLocationFragment.onMapReady().")
        map = googleMap

        // Add a marker in Sydney and move the camera
        val sydney = LatLng(-34.0, 151.0)
        map?.let {
            it.addMarker(MarkerOptions().position(sydney).title("Marker in Sydney"))
            it.moveCamera(CameraUpdateFactory.newLatLng(sydney))
//            setMapLongClick(it)
            setMapClick(it)
            // Put a marker to location that the user selected
            setPoiClick(it)
            // Add style to the map
            setMapStyle(it)
        }
    }

    /*
    private fun setMapLongClick(map: GoogleMap) {
        Log.d(TAG, "setMapLongClick().")
        map.setOnMapLongClickListener { latLng ->
            Log.d(TAG, "setMapLongClick() -> map.setOnMapLongClickListener.")
            // A Snippet is Additional text that's displayed below the title.
            val snippet = String.format(
                Locale.getDefault(),
                "Lat: %1$.5f, Long: %2$.5f",
                latLng.latitude,
                latLng.longitude
            )

            map.addMarker(
                MarkerOptions()
                    .position(latLng)
                    .title(getString(R.string.dropped_pin))
                    .snippet(snippet)
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))
            )

            onLocationSelected(
                location = "",
                latitude = latLng.latitude,
                longitude = latLng.longitude
            )
        }
    }
     */

    private fun setMapClick(map: GoogleMap?) {
        Log.d(TAG, "SelectLocationFragment.setMapClick().")
        map?.setOnMapClickListener { latLng ->
            val geocoder = Geocoder(requireContext(), Locale.getDefault())
            fetchGeocode(geocoder, latLng)
        }
    }

    private fun fetchGeocode(geocoder: Geocoder, latLng: LatLng) {
        Log.d(TAG, "SelectLocationFragment.fetchGeocode() -> #1")
        lifecycleScope.launch(Dispatchers.IO) {
//        lifecycleScope.launch(Dispatchers.Main) {
//            withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "SelectLocationFragment.fetchGeocode() -> #2")
                geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1) { addresses ->
                    Log.d(TAG, "SelectLocationFragment.fetchGeocode() -> #3")
                    val poi = PointOfInterest(latLng, "", addresses[0].featureName)
                    updateCurrentPoi(poi)
                }
                Log.d(TAG, "SelectLocationFragment.fetchGeocode() -> #8")
            } catch (e: IOException) {
                Log.e(TAG, "Failed to fetch address list: ${e.message}")
                Log.d(TAG, "SelectLocationFragment.fetchGeocode() -> #9")
            }
            Log.d(TAG, "SelectLocationFragment.fetchGeocode() -> #10")
//            }
//            Log.d(TAG, "SelectLocationFragment.fetchGeocode() -> #11")
        }
//        Log.d(TAG, "SelectLocationFragment.fetchGeocode() -> #12")
    }

    private fun putMarkerOnMap(poi: PointOfInterest) {
        Log.d(TAG, "SelectLocationFragment.putMarkerOnMap() -> #1")
        lifecycleScope.launch(Dispatchers.Main) {
//            withContext(Dispatchers.IO) {
                try {
                    poiLatLng = poi.latLng
                    poiLocation = poi.name

                    Log.d(TAG, "---------- SelectLocationFragment.putMarkerOnMap() -> poiLatLng: $poiLatLng")
                    Log.d(TAG, "---------- SelectLocationFragment.putMarkerOnMap() -> poiLocation: ${poi.name}")
                    Log.d(TAG, "SelectLocationFragment.putMarkerOnMap() -> #2")
                    currentMarker = map?.addMarker(MarkerOptions().position(poi.latLng).title(poi.name))
                    Log.d(TAG, "SelectLocationFragment.putMarkerOnMap() -> #3")
                    if (currentMarker != null) {
                        Log.d(TAG, "SelectLocationFragment.putMarkerOnMap() -> #4")
                    } else {
                        Log.d(TAG, "SelectLocationFragment.putMarkerOnMap() -> #5")
                    }
                    Log.d(TAG, "SelectLocationFragment.putMarkerOnMap() -> #6")
                    currentMarker?.showInfoWindow()
                    Log.d(TAG, "SelectLocationFragment.putMarkerOnMap() -> #7")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to put Marker on map: ${e.message}")
                    Log.d(TAG, "SelectLocationFragment.putMarkerOnMap() -> #8")
                }
                Log.d(TAG, "SelectLocationFragment.putMarkerOnMap() -> #9")
//            }
        }
        Log.d(TAG, "SelectLocationFragment.putMarkerOnMap() -> #10")
    }

    private fun putCircleOnMap(poi: PointOfInterest) {
        Log.d(TAG, "SelectLocationFragment.putCircleOnMap() -> #1")
        lifecycleScope.launch(Dispatchers.Main) {
            try {
                Log.d(TAG, "SelectLocationFragment.putCircleOnMap() -> #2")
                // Add circle range.
                currentCircleMarker = map?.addCircle(CircleOptions()
                    .center(poi.latLng)
                    .radius(GEOFENCE_RADIUS_IN_METERS.toDouble())
                    .fillColor(
                        ContextCompat.getColor(
                            requireContext(),
                            R.color.geofencing_circle_fill_color
                        )
                    )
                    .strokeColor(
                        ContextCompat.getColor(
                            requireContext(),
                            R.color.geofencing_circle_stroke_color
                        )
                    )
                )
                Log.d(TAG, "SelectLocationFragment.putCircleOnMap() -> #3")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to put Circle on map: ${e.message}")
                Log.d(TAG, "SelectLocationFragment.putCircleOnMap() -> #4")
            }
            Log.d(TAG, "SelectLocationFragment.putCircleOnMap() -> #5")
        }
        Log.d(TAG, "SelectLocationFragment.putCircleOnMap() -> #6")
    }

    private fun removeMarkers() {
        Log.d(TAG, "SelectLocationFragment.removeMarkers() -> #1")
        lifecycleScope.launch(Dispatchers.Main) {
            try {
                Log.d(TAG, "SelectLocationFragment.removeMarkers() -> #2")
                currentMarker?.remove()
                currentCircleMarker?.remove()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to remove Marker and Circle Marker on map: ${e.message}")
                Log.d(TAG, "SelectLocationFragment.removeMarkers() -> #3")
            }
        }
        Log.d(TAG, "SelectLocationFragment.removeMarkers() -> #4")
    }

    private fun updateCurrentPoi(poi: PointOfInterest) {
        binding.saveButton.visibility = View.VISIBLE
        removeMarkers()
        currentPOI = poi
        putMarkerOnMap(poi)
        putCircleOnMap(poi)
    }

    /*
    private fun setPoiClick(map: GoogleMap) {
        map.setOnPoiClickListener { poi ->
            /*
            val poiMarker = map.addMarker(
                MarkerOptions()
                    .position(poi.latLng)
                    .title(poi.name)
            )
             */
//            poiMarker?.showInfoWindow()

            latLng = poi.latLng
            poiLocation = poi.name

            val snippet = String.format(
                Locale.getDefault(),
                "Lat: %1$.5f, Long: %2$.5f",
                latLng.latitude, latLng.longitude
            )

            map.addMarker(
                MarkerOptions()
                    .position(latLng)
//                    .title(getString(R.string.dropped_pin))
                    .title(poiLocation)
                    .snippet(snippet)
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))
            )

            binding.saveButton.visibility = View.VISIBLE
        }
    }
     */

    private fun setPoiClick(map: GoogleMap?) {
        Log.d(TAG, "SelectLocationFragment.setPoiClick().")
        map?.setOnPoiClickListener { poi ->
            updateCurrentPoi(poi)
        }
    }

    private fun setMapStyle(map: GoogleMap) {
        Log.d(TAG, "SelectLocationFragment.setMapStyle().")
        try {
            // Customize the styling of the base map using a JSON object defined in a raw res file.
            val success = map.setMapStyle(
                MapStyleOptions.loadRawResourceStyle(
                    requireActivity(),
                    R.raw.map_style
                )
            )
            if (!success) {
                Log.e(TAG, "Style parsing failed.")
            }
        } catch (e: Resources.NotFoundException) {
            Log.e(TAG, "Can't find style. Error: ", e)
        }
    }

    //--------------------------------------------------
    // Location Methods
    //--------------------------------------------------

    /**
     * Source:
     * https://stackoverflow.com/a/25175756/1354788
     */
    private fun checkGpsEnabled() {
        Log.d(TAG, "SelectLocationFragment.checkGpsEnabled().")
        if (!isGpsEnabled()) {
            Log.d(TAG, "SelectLocationFragment.checkGpsEnabled() -> [1]")
            buildAlertMessageNoGps()
        } else {
            Log.d(TAG, "SelectLocationFragment.checkGpsEnabled() -> [2]")
            requestTracking()
        }
    }

    private fun isGpsEnabled(): Boolean {
        Log.d(TAG, "SelectLocationFragment.isGpsEnabled().")
        val manager = parent.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return manager.isProviderEnabled(LocationManager.GPS_PROVIDER)
    }

    private fun buildAlertMessageNoGps() {
        Log.d(TAG, "SelectLocationFragment.buildAlertMessageNoGps().")
        val builder = AlertDialog.Builder(parent)
        builder.setMessage(R.string.should_enable_gps)
            .setCancelable(false)
            .setPositiveButton("Yes") { _, _ ->
                activityResultLauncher.launch(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
            }
            .setNegativeButton("No") { dialog, _ ->
                dialog.cancel()
                permissionDeniedFeedback()
            }
        alertShouldEnableGps = builder.create()
        alertShouldEnableGps?.show()
    }

    private fun goToLocation(lat: Double, lng: Double) {
        Log.d(TAG, "SelectLocationFragment.goToLocation().")
        map?.let {
            val latLng = LatLng(lat, lng)
            // Zoom to the user location after taking his permission.
            val cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, 18F)
            it.moveCamera(cameraUpdate)
            it.mapType = GoogleMap.MAP_TYPE_NORMAL
        }
    }

    private fun permissionDeniedFeedback() {
        Log.d(TAG, "SelectLocationFragment.permissionDeniedFeedback().")
        parent.toast(R.string.allow_all_time_did_not_accepted)
        requireActivity().onBackPressedDispatcher.onBackPressed()
    }

    @SuppressLint("MissingPermission")
    private fun requestTracking() {
        Log.d(TAG, "SelectLocationFragment.requestTracking().")
        fusedLocationProvider?.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )
    }

    /**
     * When the user confirms on the selected location, send back the selected location details
     * to the view model and navigate back to the previous fragment to save the reminder and add
     * the geofence.
     */
    private fun onLocationSelected(location: String, latitude: Double, longitude: Double) {
        Log.d(TAG, "SelectLocationFragment.onLocationSelected() -> " +
            "location: $location, latitude: $latitude, longitude: $longitude")

        parent.toast(R.string.poi_selected)
        lifecycleScope.launch {
            delay(2000)
            val triple = Triple(location, latitude, longitude)
            setNavigationResult(triple, ARGUMENTS)
            findNavController().popBackStack()
        }

        /*
        // Use the navigationCommand live data to navigate between the fragments
        _viewModel.navigationCommand.postValue(
            NavigationCommand.To(
                SelectLocationFragmentDirections.actionSelectLocationFragmentToSaveReminderFragment(
                )
            )
        )
         */
    }
}