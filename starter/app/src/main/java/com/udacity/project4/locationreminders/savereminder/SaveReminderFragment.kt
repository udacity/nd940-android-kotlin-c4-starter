package com.udacity.project4.locationreminders.savereminder

import android.Manifest
import android.Manifest.permission.ACCESS_BACKGROUND_LOCATION
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import androidx.navigation.fragment.findNavController
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.databinding.FragmentSaveReminderBinding
import com.udacity.project4.locationreminders.geofence.GeofenceBroadcastReceiver
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
            _viewModel.reminderSelectedLocationStr.postValue("$latitude,$longitude")
            val location = "$latitude,$longitude"
            if (title.isNullOrEmpty()) {
                Toast.makeText(context, getString(R.string.add_title), Toast.LENGTH_LONG).show()
            }
            if (description.isNullOrEmpty()) {
                Toast.makeText(
                    context,
                    getString(R.string.please_add_description),
                    Toast.LENGTH_LONG
                ).show()
            } else if (latitude?.isNaN() == true || longitude?.isNaN() == true || latitude == 0.0 || longitude == 0.0) {
                Toast.makeText(context, getString(R.string.please_add_location), Toast.LENGTH_LONG)
                    .show()
            } else {
                val tempReminderDataItem = ReminderDataItem(
                    title = title, description = description,
                    latitude = latitude, longitude = longitude, location = location
                )
                if (isBackGroundPermissionGranted()) {
                    addGeofencing(tempReminderDataItem)
                } else
                    requestBackgroundLocationPermission()
            }
        }
    }

    private fun addGeofencing(tempReminderDataItem: ReminderDataItem) {
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        val intent = Intent(requireContext(), GeofenceBroadcastReceiver::class.java)
        // We use FLAG_UPDATE_CURRENT so that we get the same pending intent back when calling
        // addGeofences() and removeGeofences().
        val geofencePendingIntent =
            PendingIntent.getBroadcast(requireContext(), 0, intent, PendingIntent.FLAG_IMMUTABLE)
        geofenceList = arrayListOf(
            Geofence.Builder()
                // Set the request ID of the geofence. This is a string to identify this
                // geofence.
                .setRequestId(tempReminderDataItem.title.toString())

                .setCircularRegion(
                    tempReminderDataItem.latitude!!,
                    tempReminderDataItem.longitude!!,
                    5F
                )
                .setExpirationDuration(543)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_EXIT)

                .build()
        )

        geofencingClient.addGeofences(getGeofencingRequest(), geofencePendingIntent).run {
            addOnSuccessListener {
                _viewModel.validateAndSaveReminder(tempReminderDataItem)
                findNavController().popBackStack()
            }
            addOnFailureListener {
                Toast.makeText(
                    context,
                    getString(R.string.failed_please_try_again),
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun getGeofencingRequest(): GeofencingRequest {
        return GeofencingRequest.Builder().apply {
            setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            addGeofences(geofenceList)
        }.build()
    }

    private fun requestBackgroundLocationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(
                    ACCESS_BACKGROUND_LOCATION
                ),
                MY_PERMISSIONS_REQUEST_BACKGROUND_LOCATION
            )
        } else {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                MY_PERMISSIONS_REQUEST_LOCATION
            )
        }
    }

    private fun isBackGroundPermissionGranted(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return ActivityCompat.checkSelfPermission(
                requireContext(),
                ACCESS_BACKGROUND_LOCATION
            ) > -1
        } else {
            return ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) > -1
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        //make sure to clear the view model after destroy, as it's a single view model.
        _viewModel.onClear()
    }
}
