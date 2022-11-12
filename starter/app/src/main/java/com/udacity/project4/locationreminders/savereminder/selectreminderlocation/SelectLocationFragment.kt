package com.udacity.project4.locationreminders.savereminder.selectreminderlocation


import android.content.res.Resources
import android.os.Bundle
import android.view.*
import android.widget.Button
import androidx.databinding.DataBindingUtil
import androidx.navigation.fragment.findNavController
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.MarkerOptions
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.databinding.FragmentSelectLocationBinding
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import org.koin.android.ext.android.inject

class SelectLocationFragment : BaseFragment(), OnMapReadyCallback {

    //Use Koin to get the view model of the SaveReminder
    override val _viewModel: SaveReminderViewModel by inject()
    lateinit var mapBtn: Button
    lateinit var latLng: LatLng
    private lateinit var binding: FragmentSelectLocationBinding
    private lateinit var mMap: GoogleMap
    private val callback = OnMapReadyCallback { googleMap ->
        googleMap.setOnMapClickListener {
            latLng = it
            mapBtn.visibility = View.VISIBLE
            googleMap.clear()
            googleMap.addMarker(
                MarkerOptions()
                    .position(it)
                    .title(getString(R.string.picked_location))
            )
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_select_location, container, false)

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


//        TODO: add the map setup implementation
//        TODO: zoom to the user location after taking his permission
//        TODO: add style to the map
//        TODO: put a marker to location that the user selected

        return binding.root
    }

    private fun onLocationSelected() {
        binding.viewModel?.latitude?.postValue(latLng.latitude)
        binding.viewModel?.longitude?.postValue(latLng.longitude)
        findNavController().popBackStack()
    }


    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.map_options, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.normal_map -> {
            changeMapStyle(R.raw.normal_map)
            true
        }
        R.id.hybrid_map -> {
            changeMapStyle(R.raw.hybrid_map)
            true
        }
        R.id.satellite_map -> {
            changeMapStyle(R.raw.satellite_map)
            true
        }
        R.id.terrain_map -> {
            changeMapStyle(R.raw.terrain_map)
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
    }

    private fun changeMapStyle(mapStyle: Int) {
        try {
            mMap.setMapStyle(
                MapStyleOptions.loadRawResourceStyle(
                    context, mapStyle
                )
            )
        } catch (e: Resources.NotFoundException) {
            //maybe show toast for user
        }
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }
}
