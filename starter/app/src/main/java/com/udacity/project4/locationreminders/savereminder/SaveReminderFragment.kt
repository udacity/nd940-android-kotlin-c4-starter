package com.udacity.project4.locationreminders.savereminder

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
import androidx.lifecycle.Lifecycle
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.databinding.FragmentSaveReminderBinding
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import com.udacity.project4.locationreminders.savereminder.selectreminderlocation.SelectLocationFragment
import com.udacity.project4.locationreminders.savereminder.selectreminderlocation.SelectLocationFragment.Companion.ARGUMENTS
import com.udacity.project4.utils.getNavigationResult
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import org.koin.android.ext.android.inject

class SaveReminderFragment : BaseFragment() {

    // Get the view model this time as a single to be shared with the another fragment
    override val _viewModel: SaveReminderViewModel by inject()
    private lateinit var binding: FragmentSaveReminderBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        Log.d(SelectLocationFragment.TAG, "SaveReminderFragment.onCreateView().")

        val layoutId = R.layout.fragment_save_reminder
        binding = DataBindingUtil.inflate(inflater, layoutId, container, false)
        setDisplayHomeAsUpEnabled(true)
        binding.viewModel = _viewModel
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, backPressHandler)

        getNavigationResult(ARGUMENTS)?.observe(viewLifecycleOwner) { result ->
            val triple = result as Triple<String, Double, Double>
            _viewModel.reminderSelectedLocationStr.value = triple.component1()
            _viewModel.latitude.value = triple.component2()
            _viewModel.longitude.value = triple.component3()
        }

        val menuHost: MenuHost = requireActivity()
        addMenu(menuHost)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.lifecycleOwner = this

        binding.selectLocation.setOnClickListener {
            // Navigate to another fragment to get the user location
            val directions = SaveReminderFragmentDirections
                .actionSaveReminderFragmentToSelectLocationFragment()
            _viewModel.navigationCommand.value = NavigationCommand.To(directions)
        }

        binding.saveReminder.setOnClickListener {
            val title = _viewModel.reminderTitle.value
            val description = _viewModel.reminderDescription.value
            val location = _viewModel.reminderSelectedLocationStr.value
            val latitude = _viewModel.latitude.value
            val longitude = _viewModel.longitude.value
                // TODO: Use the user entered reminder details to:
                //  [DOING] 1) Add a geofencing request
                //   [DONE] 2) Save the reminder to the local db
                val dataItem = ReminderDataItem(
                    title = title,
                    description = description,
                    location = location,
                    latitude = latitude,
                    longitude = longitude
                )
            _viewModel.validateAndSaveReminder(dataItem)
        }
    }

    private fun addMenu(menuHost: MenuHost) {
        // Add menu items without using the Fragment Menu APIs. Note how we can tie the MenuProvider
        // to the viewLifecycleOwner and an optional Lifecycle.State (here, RESUMED) to indicate
        // when the menu should be visible.
        menuHost.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {}
            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                when (menuItem.itemId) {
                    android.R.id.home -> {
                        requireActivity().onBackPressedDispatcher.onBackPressed()
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

    override fun onDestroy() {
        super.onDestroy()
        // Make sure to clear the view model after destroy, as it's a single view model.
        _viewModel.onClear()
    }
}