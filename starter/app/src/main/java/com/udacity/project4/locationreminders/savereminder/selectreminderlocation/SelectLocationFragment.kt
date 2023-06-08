package com.udacity.project4.locationreminders.savereminder.selectreminderlocation

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Resources
import android.location.Geocoder
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.*
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
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
import com.udacity.project4.utils.isGpsEnabled
import com.udacity.project4.utils.permissionDeniedFeedback
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import com.udacity.project4.utils.setNavigationResult
import com.udacity.project4.utils.toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import java.io.IOException
import java.util.*

class SelectLocationFragment : BaseFragment(), OnMapReadyCallback {

    //--------------------------------------------------
    // Attributes
    //--------------------------------------------------

    companion object {
        const val ARGUMENTS = "args"
    }

    // Use Koin to get the view model of the SaveReminder
    override val _viewModel: SaveReminderViewModel by inject()
    private var map: GoogleMap? = null

    private var currentMarker: Marker? = null
    private var currentCircleMarker: Circle? = null
    private var currentPOI: PointOfInterest? = null
    private lateinit var poiLatLng : LatLng
    private var poiLocation = ""

    private var alertShouldEnableGps: AlertDialog? = null

    private lateinit var binding: FragmentSelectLocationBinding
    private lateinit var parent: FragmentActivity

    //--------------------------------------------------
    // Activity Result Callbacks
    //--------------------------------------------------

    /**
     * Source:
     * https://developer.android.com/training/location/permissions#user-choice-affects-permission-grants
     */

    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        Log.d(TAG, "SelectLocationFragment.locationPermissionRequest() -> permissions: $permissions")
        when {
            permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) -> {
                // Precise location access granted.
                Log.d(TAG, "SelectLocationFragment.locationPermissionRequest() -> Precise location access granted.")
                enableMyLocation()
            }
            permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false) -> {
                // Only approximate location access granted.
                Log.d(TAG, "SelectLocationFragment.locationPermissionRequest() -> Only approximate location access granted.")
            } else -> {
                // No location access granted.
                Log.d(TAG, "SelectLocationFragment.locationPermissionRequest() -> No location access granted.")
            }
        }
    }

    /**
     * Source:
     * https://www.tothenew.com/blog/android-katha-onactivityresult-is-deprecated-now-what/
     */
    private var activityResultLauncher = registerForActivityResult(ActivityResultContracts
        .StartActivityForResult()) {
        Log.d(TAG, "SelectLocationFragment.activityResultLauncher.")
        if (parent.isGpsEnabled()) {
            enableMyLocation()
        } else {
            parent.permissionDeniedFeedback()
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

        startMapFeature()
        initMenus()

        binding.saveButton.setOnClickListener {
            onLocationSelected(
                location = poiLocation,
                latitude = poiLatLng.latitude,
                longitude = poiLatLng.longitude
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "SelectLocationFragment.onDestroy().")
        disableDialogs()
    }

    private fun disableDialogs() {
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
            setMapClick(it)
            // Put a marker to location that the user selected.
            setPoiClick(it)
            // Add style to the map.
            setMapStyle(it)
            enableMyLocation()
        }
    }

    private fun setMapClick(map: GoogleMap?) {
        Log.d(TAG, "SelectLocationFragment.setMapClick().")
        map?.setOnMapClickListener { latLng ->
            val geocoder = Geocoder(requireContext(), Locale.getDefault())
            fetchGeocode(geocoder, latLng)
        }
    }

    @Suppress("DEPRECATION")
    private fun fetchGeocode(geocoder: Geocoder, latLng: LatLng) {
        Log.d(TAG, "SelectLocationFragment.fetchGeocode().")
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    Log.d(TAG, "SelectLocationFragment.fetchGeocode() -> Version >= Android 13.")
                    geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1) { addresses ->
                        val poi = PointOfInterest(latLng, "", addresses[0].featureName)
                        updateCurrentPoi(poi)
                    }
                } else {
                    Log.d(TAG, "SelectLocationFragment.fetchGeocode() -> Version < Android 13.")
                    val addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)
                    addresses?.let {
                        val poi = PointOfInterest(latLng, "", it[0].featureName)
                        updateCurrentPoi(poi)
                    }
                }
            } catch (e: IOException) {
                Log.d(TAG, "SelectLocationFragment.fetchGeocode() -> Failed to fetch address list.")
                Log.e(TAG, "Failed to fetch address list: ${e.message}")
            }
        }
    }

    private fun putMarkerOnMap(poi: PointOfInterest) {
        Log.d(TAG, "SelectLocationFragment.putMarkerOnMap().")
        try {
            poiLatLng = poi.latLng
            poiLocation = poi.name
            currentMarker = map?.addMarker(MarkerOptions().position(poi.latLng).title(poi.name))
            currentMarker?.showInfoWindow()
        } catch (e: Exception) {
            Log.d(TAG, "SelectLocationFragment.putMarkerOnMap() -> Failed to put Marker on map.")
            Log.e(TAG, "Failed to put Marker on map: ${e.message}")
        }
    }

    private fun putCircleOnMap(poi: PointOfInterest) {
        Log.d(TAG, "SelectLocationFragment.putCircleOnMap().")
        try {
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
        } catch (e: Exception) {
            Log.d(TAG, "SelectLocationFragment.putCircleOnMap() -> Failed to put Circle on map.")
            Log.e(TAG, "Failed to put Circle on map: ${e.message}")
        }
    }

    private fun removeMarkers() {
        Log.d(TAG, "SelectLocationFragment.removeMarkers().")
        try {
            currentMarker?.remove()
            currentCircleMarker?.remove()
        } catch (e: Exception) {
            Log.d(TAG, "SelectLocationFragment.removeMarkers() -> Failed to remove Marker and Circle Marker on map.")
            Log.e(TAG, "Failed to remove Marker and Circle Marker on map: ${e.message}")
        }
    }

    private fun updateCurrentPoi(poi: PointOfInterest) {
        Log.d(TAG, "SelectLocationFragment.updateCurrentPoi().")
        lifecycleScope.launch(Dispatchers.Main) {
            binding.saveButton.visibility = View.VISIBLE
            removeMarkers()
            currentPOI = poi
            putMarkerOnMap(poi)
            putCircleOnMap(poi)
        }
    }

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

    @SuppressLint("MissingPermission")
    private fun enableMyLocation() {
        Log.d(TAG, "SelectLocationFragment.enableMyLocation().")
        if (isPermissionGranted()) {
            Log.d(TAG, "SelectLocationFragment.enableMyLocation() -> isPermissionGranted? true")
            map?.isMyLocationEnabled = true
            checkGpsEnabled()
        } else {
            Log.d(TAG, "SelectLocationFragment.enableMyLocation() -> isPermissionGranted? false")
            locationPermissionRequest.launch(arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION)
            )
        }
    }
    private fun isPermissionGranted(): Boolean {
        return ContextCompat.checkSelfPermission(
            requireActivity(),
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun checkGpsEnabled() {
        Log.d(TAG, "SelectLocationFragment.checkGpsEnabled().")
        if (!parent.isGpsEnabled()) {
            Log.d(TAG, "SelectLocationFragment.checkGpsEnabled() -> [1]")
            buildAlertMessageNoGps()
        }
    }

    /**
     * Source:
     * https://stackoverflow.com/a/25175756/1354788
     */
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
                parent.permissionDeniedFeedback()
            }
        alertShouldEnableGps = builder.create()
        alertShouldEnableGps?.show()
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
            delay(1000)
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