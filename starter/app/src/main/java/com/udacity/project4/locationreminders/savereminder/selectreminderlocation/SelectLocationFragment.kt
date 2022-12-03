package com.udacity.project4.locationreminders.savereminder.selectreminderlocation


import android.Manifest
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Resources
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.*
import android.widget.Button
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import androidx.navigation.fragment.findNavController
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.GoogleMap.OnMyLocationButtonClickListener
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.MarkerOptions
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.databinding.FragmentSelectLocationBinding
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import org.koin.android.ext.android.inject
import java.util.*


class SelectLocationFragment : BaseFragment(), OnMapReadyCallback {

    //Use Koin to get the view model of the SaveReminder
    override val _viewModel: SaveReminderViewModel by inject()
    lateinit var mapBtn: Button
    private lateinit var latLng: LatLng
    private lateinit var binding: FragmentSelectLocationBinding
    private var mMap: GoogleMap? = null
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    var currentLocation: Location? = null
    val LOCATION_ZOOM_DISTANCE = 10.0f
    var droppedName: String = ""
    private val callback = OnMapReadyCallback { googleMap ->
        mMap = googleMap
        mMap!!.setOnMyLocationButtonClickListener(object : OnMyLocationButtonClickListener {
            override fun onMyLocationButtonClick(): Boolean {

                if (activity?.checkPermission(
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        _viewModel.CODE_REQUEST,
                        _viewModel.CODE_REQUEST
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    checkGPSEnable()
                } else {
                    requestPermissions(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        ), _viewModel.CODE_REQUEST
                    )
                }
//                zoomToCurrentLocation()
                return true
            }
        })
        setMapStyle(mMap!!)
        googleMap.setOnMapClickListener {
            latLng = it
            val snippet = String.format(
                Locale.getDefault(),
                "Lat: ${latLng.latitude}, Long: ${latLng.longitude}",
                latLng.latitude,
                latLng.longitude
            )
            mapBtn.visibility = View.VISIBLE
            googleMap.clear()
            googleMap.addMarker(
                MarkerOptions()
                    .position(it)
                    .title(getString(R.string.picked_location))
                    .snippet(snippet)
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))
            )
            droppedName = getString(R.string.picked_location)
        }

        googleMap.setOnPoiClickListener {
            latLng = it.latLng
            val snippet = String.format(
                Locale.getDefault(),
                "Lat: ${latLng.latitude}, Long: ${latLng.longitude}",
                latLng.latitude,
                latLng.longitude
            )
            mapBtn.visibility = View.VISIBLE
            googleMap.clear()
            googleMap.addMarker(
                MarkerOptions()
                    .position(latLng)
                    .title(it.name)
                    .snippet(snippet)
            )
            droppedName = it.name
        }
        if (currentLocation != null) {
            val homeLatLng = LatLng(
                currentLocation!!.latitude,
                currentLocation!!.longitude
            )
            mMap?.animateCamera(
                CameraUpdateFactory.newLatLngZoom(
                    homeLatLng, LOCATION_ZOOM_DISTANCE
                )
            )
            mMap?.addMarker(MarkerOptions().position(homeLatLng))
        } else {
//                Toast.makeText(context, getString(R.string.cannot_get_location), Toast.LENGTH_LONG)
//                    .show()
        }

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_select_location, container, false)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext())
        binding.viewModel = _viewModel
        binding.lifecycleOwner = this
        mapBtn = binding.mapButton

        setHasOptionsMenu(true)
        setDisplayHomeAsUpEnabled(true)

        mapBtn.setOnClickListener {
            onLocationSelected()
        }

        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?
        mapFragment!!.getMapAsync(callback)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (activity?.checkPermission(
                Manifest.permission.ACCESS_COARSE_LOCATION,
                _viewModel.CODE_REQUEST,
                _viewModel.CODE_REQUEST
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            checkGPSEnable()
        } else {
            requestPermissions(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ), _viewModel.CODE_REQUEST
            )
        }
    }

    private fun onLocationSelected() {
        binding.viewModel?.latitude?.postValue(latLng.latitude)
        binding.viewModel?.longitude?.postValue(latLng.longitude)
        _viewModel.droppedName.postValue(droppedName)
        findNavController().popBackStack()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.map_options, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.normal_map -> {
            mMap?.mapType = GoogleMap.MAP_TYPE_NORMAL
//            changeMapStyle(R.raw.normal_map)
            true
        }
        R.id.hybrid_map -> {
            mMap?.mapType = GoogleMap.MAP_TYPE_HYBRID
//            changeMapStyle(R.raw.hybrid_map)
            true
        }
        R.id.satellite_map -> {
            mMap?.mapType = GoogleMap.MAP_TYPE_SATELLITE
//            changeMapStyle(R.raw.satellite_map)
            true
        }
        R.id.terrain_map -> {
            mMap?.mapType = GoogleMap.MAP_TYPE_TERRAIN
//            changeMapStyle(R.raw.terrain_map)
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap  = googleMap
        if (currentLocation != null)
            mMap?.animateCamera(
                CameraUpdateFactory.newLatLngZoom(
                    LatLng(
                        currentLocation!!.latitude,
                        currentLocation!!.longitude
                    ), LOCATION_ZOOM_DISTANCE
                )
            )


    }

    private fun setMapStyle(map: GoogleMap) {
        try {
            // Customize the styling of the base map using a JSON object defined
            // in a raw resource file.
            val success = map.setMapStyle(
                MapStyleOptions.loadRawResourceStyle(
                    requireContext(),
                    R.raw.hybrid_map
                )
            )

            if (!success) {
                Log.e("TAG", "Style parsing failed.")
            }
        } catch (e: Resources.NotFoundException) {
            Log.e("TAG", "Can't find style. Error: ", e)
        }
    }

    private fun zoomToCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            )
            == PackageManager.PERMISSION_GRANTED &&

            ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            val locationRequest = LocationRequest().apply {
                priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            }
            var locationCallback = object : LocationCallback() {
                override fun onLocationResult(p0: LocationResult) {
                    super.onLocationResult(p0)
                    p0.lastLocation?.let { location ->
                        currentLocation = location
                        if (mMap != null) {
                            mMap?.animateCamera(
                                CameraUpdateFactory.newLatLngZoom(
                                    LatLng(
                                        location.latitude,
                                        location.longitude
                                    ), LOCATION_ZOOM_DISTANCE
                                )
                            )
                        }
//                        fusedLocationClient.removeLocationUpdates(this)
                        latLng = LatLng(currentLocation?.latitude!!,currentLocation?.longitude!!)
                        val snippet = String.format(
                            Locale.getDefault(),
                            "Lat: ${latLng.latitude}, Long: ${latLng.longitude}",
                            latLng.latitude,
                            latLng.longitude
                        )
                        mapBtn.visibility = View.VISIBLE
                        mMap?.clear()
                        mMap?.addMarker(
                            MarkerOptions()
                                .position(latLng)
                                .title(getString(R.string.picked_location))
                                .snippet(snippet)
                                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))
                        )
                        droppedName = getString(R.string.picked_location)
                    }
                }
            }
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.myLooper()
            )
            mMap?.isMyLocationEnabled = true
        } else {
            Toast.makeText(
                context,
                getString(R.string.need_permission_to_get_your_location),
                Toast.LENGTH_LONG
            ).show()
            mMap?.isMyLocationEnabled = true
        }

    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == _viewModel.CODE_REQUEST) {
            val manager = context?.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED && manager.isProviderEnabled(
                    LocationManager.GPS_PROVIDER
                ))
            ) {
                checkGPSEnable()
                mMap?.isMyLocationEnabled = true
            } else {
                Toast.makeText(
                    context,
                    getString(R.string.need_permission_to_get_your_location),
                    Toast.LENGTH_LONG
                ).show()
                mMap?.isMyLocationEnabled = true
            }
        }
        checkGPSEnable()
    }

    private fun checkGPSEnable() {
        val manager = context?.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if (!manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            val dialogBuilder = AlertDialog.Builder(context)
            dialogBuilder.setMessage(getString(R.string.enable_gps))
                .setCancelable(false)
                .setPositiveButton(getString(R.string.yes)) { dialog, id
                    ->
                    startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                }
                .setNegativeButton(getString(R.string.no)) { dialog, id ->
                    dialog.cancel()
                }
            val alert = dialogBuilder.create()
            alert.show()
        } else {
            zoomToCurrentLocation()
        }
    }


}
