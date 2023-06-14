package com.udacity.project4.locationreminders.reminderslist

import android.Manifest
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
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import com.firebase.ui.auth.AuthUI
import com.udacity.project4.MyApp
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.databinding.FragmentRemindersBinding
import com.udacity.project4.utils.TAG
import com.udacity.project4.utils.permissionGranted
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import com.udacity.project4.utils.setTitle
import com.udacity.project4.utils.setup
import com.udacity.project4.utils.toast
import org.koin.androidx.viewmodel.ext.android.viewModel

class ReminderListFragment : BaseFragment() {

    companion object {
        private const val POST = Manifest.permission.POST_NOTIFICATIONS
    }

    // Use Koin to retrieve the ViewModel instance.
    override val _viewModel: RemindersListViewModel by viewModel()
    private lateinit var binding: FragmentRemindersBinding
    private var logoutClicked = false
    private lateinit var parent: FragmentActivity

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        Log.d(TAG, "ReminderListFragment.requestPermissionLauncher -> isGranted: $isGranted")
        (parent.applicationContext as MyApp).hasNotificationPermission = isGranted
        if (isGranted) {
            Log.d(TAG, "ReminderListFragment.requestPermissionLauncher -> Notification permission is granted.")
            parent.toast(R.string.notification_permission_granted)
        } else {
            Log.d(TAG, "ReminderListFragment.requestPermissionLauncher -> Notification permission is NOT granted.")
            parent.toast(R.string.no_notification_permission)
        }
    }

    //--------------------------------------------------
    // Lifecycle Methods
    //--------------------------------------------------

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val layoutId = R.layout.fragment_reminders
        binding = DataBindingUtil.inflate(inflater, layoutId, container, false)
        binding.viewModel = _viewModel
        init()

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(TAG, "ReminderListFragment.onViewCreated().")
        binding.lifecycleOwner = this
        setupRecyclerView()
        binding.addReminderFAB.setOnClickListener {
            navigateToAddReminder()
        }
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "ReminderListFragment.onResume().")

        logoutClicked = false
        // Load the reminders list on the ui
        _viewModel.loadReminders()
    }

    //--------------------------------------------------
    // Main Methods
    //--------------------------------------------------

    private fun init() {
        Log.d(TAG, "ReminderListFragment.init().")
        parent = requireActivity()
        parent.onBackPressedDispatcher.addCallback(viewLifecycleOwner, backPressHandler)

        val menuHost: MenuHost = parent
        addMenu(menuHost)

        setDisplayHomeAsUpEnabled(false)
        setTitle(getString(R.string.app_name))

        binding.refreshLayout.setOnRefreshListener { _viewModel.loadReminders() }

        requestPermissionListener()
    }

    private fun navigateToAddReminder() {
        Log.d(TAG, "ReminderListFragment.navigateToAddReminder().")
        // Use the navigationCommand live data to navigate between the fragments
        _viewModel.navigationCommand.postValue(
            NavigationCommand.To(
                ReminderListFragmentDirections.actionReminderListFragmentToSaveReminderFragment()
            )
        )
    }

    private fun setupRecyclerView() {
        Log.d(TAG, "ReminderListFragment.setupRecyclerView().")
        val adapter = RemindersListAdapter {}
        // Setup the recycler view using the extension function
        binding.reminderssRecyclerView.setup(adapter)
    }

    private fun requestPermissionListener() {
        Log.d(TAG, "ReminderListFragment.requestPermissionListener().")
        when {
            parent.permissionGranted(POST) -> {
                // You can use the API that requires the permission.
                Log.d(TAG, "ReminderListFragment.requestPermissionListener() -> Permission granted!")
            }
            else -> {
                // The registered ActivityResultCallback gets the result of this request.
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    requestPermissionLauncher.launch(POST)
                }
            }
        }
    }

    //--------------------------------------------------
    // Menu Methods
    //--------------------------------------------------

    /**
     * Source:
     * https://java73.medium.com/back-press-handling-with-androidx-navigation-component-60bbc0fd169
     */
    private val backPressHandler = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            if (logoutClicked) {
                AuthUI.getInstance().signOut(requireContext())
            }
            activity?.finishAndRemoveTask()
        }
    }

    private fun addMenu(menuHost: MenuHost) {
        Log.d(TAG, "ReminderListFragment.addMenu().")
        // Add menu items without using the Fragment Menu APIs
        // Note how we can tie the MenuProvider to the viewLifecycleOwner
        // and an optional Lifecycle.State (here, RESUMED) to indicate when
        // the menu should be visible
        menuHost.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.main_menu, menu)
            }
            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                when (menuItem.itemId) {
                    R.id.logout -> {
                        logoutClicked = true
                        parent.onBackPressedDispatcher.onBackPressed()
                    }
                    android.R.id.home -> {
                        parent.onBackPressedDispatcher.onBackPressed()
                    }
                }
                return true
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }
}