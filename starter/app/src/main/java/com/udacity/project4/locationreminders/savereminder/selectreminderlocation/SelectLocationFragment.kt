package com.udacity.project4.locationreminders.savereminder.selectreminderlocation


import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.content.res.Resources
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.material.snackbar.Snackbar
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.databinding.FragmentSelectLocationBinding
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import org.koin.android.ext.android.inject
import java.util.*

class SelectLocationFragment : BaseFragment(), OnMapReadyCallback {

    //Use Koin to get the view model of the SaveReminder
    override val _viewModel: SaveReminderViewModel by inject()
    private lateinit var binding: FragmentSelectLocationBinding

    private lateinit var googleMap: GoogleMap

    private var lastKnownLocation: Location? = null
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    private var currentMarker: Marker? = null
    private var currentPOI: PointOfInterest? = null

    private val TAG = SelectLocationFragment::class.java.simpleName

    companion object {
        private val REQUEST_LOCATION_PERMISSION = 1
        private const val DEFAULT_ZOOM = 15
        private val defaultLocation = LatLng(28.377374445116924, -81.57004261842762)
        private const val KEY_LOCATION = "location"
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_select_location, container, false)

        binding.viewModel = _viewModel
        binding.lifecycleOwner = this

        setHasOptionsMenu(true)
        setDisplayHomeAsUpEnabled(true)

        val mapFragment =
            childFragmentManager.findFragmentById(R.id.support_map_fragment) as SupportMapFragment
        mapFragment.getMapAsync(this)

        fusedLocationProviderClient =
            LocationServices.getFusedLocationProviderClient(requireActivity())

        binding.selectLocationSaveButton.setOnClickListener {
            onLocationSelected()
        }


        return binding.root
    }

    private fun onLocationSelected() {
        // When the user confirms on the selected location,
        // send back the selected location details to the view model
        // and navigate back to the previous fragment to save the reminder
        // and add the geofence.
        _viewModel.latitude.postValue(currentMarker?.position?.latitude)
        _viewModel.longitude.postValue(currentMarker?.position?.longitude)
        _viewModel.selectedPOI.postValue(currentPOI)
        _viewModel.reminderSelectedLocationStr.postValue(currentPOI?.name)
        _viewModel.navigationCommand.postValue(NavigationCommand.Back)
    }


    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.map_options, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        // Change the map type based on the user's selection.
        R.id.normal_map -> {
            googleMap.mapType = GoogleMap.MAP_TYPE_NORMAL
            true
        }
        R.id.hybrid_map -> {
            googleMap.mapType = GoogleMap.MAP_TYPE_HYBRID
            true
        }
        R.id.satellite_map -> {
            googleMap.mapType = GoogleMap.MAP_TYPE_SATELLITE
            true
        }
        R.id.terrain_map -> {
            googleMap.mapType = GoogleMap.MAP_TYPE_TERRAIN
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map

        if (null != _viewModel.selectedPOI.value) {
            currentPOI = _viewModel.selectedPOI.value
            currentMarker = googleMap.addMarker(
                MarkerOptions()
                    .position(_viewModel.selectedPOI.value!!.latLng)
                    .title(_viewModel.selectedPOI.value!!.name)
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
            )
        }

        setMapLongClick(googleMap)
        setMapStyle(googleMap)
        setPoiClickListener(googleMap)
        enableMyLocation()
    }

    private fun setMapLongClick(map: GoogleMap) {
        map.setOnMapClickListener { latLng ->
            val snippet = String.format(
                Locale.getDefault(),
                "Lat: %1$.5f, Long $2$.5f",
                latLng.latitude,
                latLng.longitude
            )

            binding.selectLocationSaveButton.setOnClickListener {
                _viewModel.latitude.value = latLng.latitude
                _viewModel.longitude.value = latLng.longitude
                _viewModel.reminderSelectedLocationStr.value = getString(R.string.dropped_pin)
                _viewModel.navigationCommand.value = NavigationCommand.Back
            }

            currentMarker = map.addMarker(
                MarkerOptions()
                    .position(latLng)
                    .title(getString(R.string.dropped_pin))
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
            )
            binding.selectLocationSaveButton.visibility = View.VISIBLE
        }
    }

    private fun setPoiClickListener(map: GoogleMap) {

        map.setOnPoiClickListener { pointOfInterest ->
            val currentLocation = pointOfInterest.latLng
            binding.selectLocationSaveButton.visibility = View.VISIBLE
            if (null != currentMarker) {
                currentMarker?.remove()
            }
            currentPOI = pointOfInterest
            currentMarker = map.addMarker(
                MarkerOptions()
                    .position(currentLocation)
                    .title(pointOfInterest.name)
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
            )
        }
    }

    @SuppressLint("MissingPermission")
    private fun enableMyLocation() {
        if (isPermissionGranted()) {
            googleMap.setMyLocationEnabled(true)
            updateUI()
            getDeviceLocation()
        } else {
            requestPermissions(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                REQUEST_LOCATION_PERMISSION
            )
        }
    }

    private fun updateUI() {
        try {
            if (isPermissionGranted()) {
                googleMap.uiSettings?.isMyLocationButtonEnabled = true
                googleMap.uiSettings?.isMapToolbarEnabled = false
                googleMap.isMyLocationEnabled = true
            } else {
                googleMap.uiSettings?.isMyLocationButtonEnabled = false
                googleMap.uiSettings?.isMapToolbarEnabled = false
                googleMap.isMyLocationEnabled = false
            }
        } catch (e: SecurityException) {
            Log.e(TAG, e.message, e)
        }
    }

    private fun getDeviceLocation() {
        try {
            if (isPermissionGranted()) {
                val lastLocation = fusedLocationProviderClient.lastLocation
                lastLocation.addOnCompleteListener(requireActivity()) { task ->
                    if (task.isSuccessful) {
                        lastKnownLocation = task.result!!
                        googleMap.moveCamera(
                            CameraUpdateFactory.newLatLngZoom(
                                LatLng(
                                    lastKnownLocation!!.latitude,
                                    lastKnownLocation!!.longitude
                                ), DEFAULT_ZOOM.toFloat()
                            )
                        )
                    } else {
                        googleMap.moveCamera(
                            CameraUpdateFactory
                                .newLatLngZoom(defaultLocation, DEFAULT_ZOOM.toFloat())
                        )
                    }
                }
            }
        } catch (e: SecurityException) {
            Log.e(TAG, e.message, e)
        }

    }

    private fun isPermissionGranted(): Boolean {
        return ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun setMapStyle(map: GoogleMap) {
       try {
           // Customize the styling of the base map using a JSON object defined
           // in a raw resource file.
           val success = map.setMapStyle(
               MapStyleOptions.loadRawResourceStyle(
                   requireContext(),
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

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == REQUEST_LOCATION_PERMISSION) {
            if (grantResults.isNotEmpty() && (grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                enableMyLocation()
            } else {
                Snackbar.make(
                    requireView(),
                    getString(R.string.location_required_error),
                    Snackbar.LENGTH_SHORT
                ).show()
            }
        }
    }
}
