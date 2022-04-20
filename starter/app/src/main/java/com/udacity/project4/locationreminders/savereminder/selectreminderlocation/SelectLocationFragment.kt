package com.udacity.project4.locationreminders.savereminder.selectreminderlocation


import android.Manifest
import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Resources
import android.location.Criteria
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.renderscript.ScriptIntrinsicYuvToRGB
import android.util.Log
import android.view.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.MutableLiveData
import androidx.navigation.fragment.findNavController
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.material.snackbar.Snackbar
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.databinding.FragmentSelectLocationBinding
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import org.koin.android.ext.android.inject
import java.util.*

class SelectLocationFragment : BaseFragment(){

    //Use Koin to get the view model of the SaveReminder
    override val _viewModel: SaveReminderViewModel by inject()
    private lateinit var binding: FragmentSelectLocationBinding
    private lateinit var map: GoogleMap
    private var poi: MutableLiveData<PointOfInterest?> = MutableLiveData(null)
    private val REQUEST_LOCATION_PERMISSION = 1
    private val TAG = "SELECTFRAGMENTMAP"
    private var userLocation:Location?=null
    private lateinit var contxt:Context


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_select_location, container, false)

        binding.viewModel = _viewModel
        binding.lifecycleOwner = this
        poi.value = null

        poi.observe(viewLifecycleOwner, androidx.lifecycle.Observer {
            binding.saveLocation.isEnabled = it != null
        })

        setHasOptionsMenu(true)
        setDisplayHomeAsUpEnabled(true)

val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync{
            googleMap ->
            map = googleMap

            val locationRequest = LocationRequest()
            locationRequest.interval = 60000
            locationRequest.fastestInterval = 5000
            locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY

            val longitude = -73.935242
            val latitude = 40.73061

            val latLng = LatLng(longitude, latitude)
            val zoom = 15f

            map.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoom))
            enableMyLocation()
            val androidOverlay = GroundOverlayOptions()
                .image(BitmapDescriptorFactory.fromResource(R.drawable.android))
                .position(latLng, 15f)
            map.addGroundOverlay(androidOverlay)


            setMapStyle(map)
            setMapLongClickListener(map)
            setOnPoiSelected(map)

            Log.i(TAG, "map ready")
        }
        binding.saveLocation.setOnClickListener {
            onLocationSelected()
        }


        return binding.root
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        contxt = context
    }

    @SuppressLint("MissingPermission")
    private fun enableMyLocation() {
        if (isPermissionGranted()) {
            map.isMyLocationEnabled = true
        }
        else {
            ActivityCompat.requestPermissions(
                this.requireActivity(),
                arrayOf<String>(Manifest.permission.ACCESS_FINE_LOCATION),
                REQUEST_LOCATION_PERMISSION
            )
        }
        getUserLocation()
        if (userLocation != null){
            val latLng = LatLng(userLocation!!.latitude, userLocation!!.longitude)
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
        }
    }

    @SuppressLint("MissingPermission")
    private fun getUserLocation(){
        val locationManager = this.activity?.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        userLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
        if (userLocation == null){
            val criteria = Criteria()
            criteria.accuracy = Criteria.ACCURACY_COARSE
            userLocation = locationManager.getBestProvider(criteria, true)
                ?.let { locationManager.getLastKnownLocation(it) }
        }
        Log.i(TAG, "this is user location: $userLocation")
    }

    private fun onLocationSelected() {

        if (poi.value == null ){
            Snackbar.make(this.requireView(), "Please select a poi before continuing !",Snackbar.LENGTH_LONG).show()
        }else {
            _viewModel.latitude.value = poi.value?.latLng?.latitude
            _viewModel.longitude.value = poi.value?.latLng?.longitude
            _viewModel.selectedPOI.value = poi.value
            findNavController().popBackStack(R.id.saveReminderFragment, false)
        }



    }


    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.map_options, menu)
    }


    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.normal_map -> {
            map.mapType = GoogleMap.MAP_TYPE_NORMAL
            true
        }
        R.id.hybrid_map -> {
            map.mapType = GoogleMap.MAP_TYPE_HYBRID
            true
        }
        R.id.satellite_map -> {
            map.mapType = GoogleMap.MAP_TYPE_SATELLITE
            true
        }
        R.id.terrain_map -> {
            map.mapType = GoogleMap.MAP_TYPE_TERRAIN
            true
        }
        else -> super.onOptionsItemSelected(item)
    }



    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        // Check if location permissions are granted and if so enable the
        // location data layer.
        if (requestCode == REQUEST_LOCATION_PERMISSION) {
            if (grantResults.isNotEmpty() && (grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                enableMyLocation()
            }
        }
    }


    private fun setMapLongClickListener(map:GoogleMap){


        map.setOnMapLongClickListener {position->

            val snipet = String.format(
                Locale.getDefault(),
                "Lat: %1$.5f, Long: %2$.5f",
                position.latitude, position.longitude
            )

            map.addMarker(
                MarkerOptions()
                    .position(position)
                    .title(getString(R.string.dropped_pin))
                    .snippet(snipet)
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))
            )
            this.poi.value = PointOfInterest(LatLng(position.latitude, position.longitude), getString(R.string.dropped_pin), "point of interest")
        }


    }


    private fun setOnPoiSelected(map: GoogleMap){
        map.setOnPoiClickListener{poi->
            val poiMarker = map.addMarker(
                MarkerOptions()
                    .position(poi.latLng)
                    .title(poi.name)
            )
            poiMarker?.showInfoWindow()
            this.poi.value = poi
        }
    }




    private fun isPermissionGranted() : Boolean {
        return ContextCompat.checkSelfPermission(
            contxt,
            Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }


    private fun setMapStyle(map: GoogleMap){
        try {
            val success = map.setMapStyle(MapStyleOptions.loadRawResourceStyle(this.requireContext(), R.raw.map_style))
            if (!success) {
                Log.e(ContentValues.TAG, "Style parsing failed.")
            }else{
                Log.i(TAG, "parse style successfully")
            }
        }catch (e: Resources.NotFoundException){
            Log.e(ContentValues.TAG, "Can't find style. Error: ", e)
        }
    }




}
