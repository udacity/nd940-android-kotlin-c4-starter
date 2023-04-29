package com.udacity.project4.locationreminders.reminderslist

import android.app.Application
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.udacity.project4.base.BaseViewModel
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result
import com.udacity.project4.utils.TAG
import kotlinx.coroutines.launch

class RemindersListViewModel(
    app: Application,
    private val dataSource: ReminderDataSource
) : BaseViewModel(app) {
    // List that holds the reminder data to be displayed on the UI.
    val remindersList = MutableLiveData<List<ReminderDataItem>>()

    /**
     * Get all the reminders from the DataSource and add them to the remindersList to be shown on the UI,
     * or show error if any
     */
    fun loadReminders() {
        Log.d(TAG, "RemindersListViewModel.loadReminders().")
        showLoading.value = true
        viewModelScope.launch {
            // Interacting with the dataSource has to be through a coroutine.
            val result = dataSource.getReminders()
            showLoading.postValue(false)
            when (result) {
                is Result.Success<*> -> {
                    Log.d(TAG, "RemindersListViewModel.loadReminders() -> Result.Success")
                    val dataList = ArrayList<ReminderDataItem>()
                    dataList.addAll((result.data as List<ReminderDTO>).map { reminder ->
                        // Map the reminder data from the DB to the be ready to be displayed on the UI.
                        Log.d(TAG, "RemindersListViewModel.loadReminders() -> reminder: $reminder")

                        ReminderDataItem(
                            reminder.title,
                            reminder.description,
                            reminder.location,
                            reminder.latitude,
                            reminder.longitude,
                            reminder.id
                        )
                    })
                    remindersList.value = dataList
                }
                is Result.Error -> {
                    Log.d(TAG, "RemindersListViewModel.loadReminders() -> Result.Error")
                    showSnackBar.value = result.message
                }
            }

            // Check if no data has to be shown.
            invalidateShowNoData()
        }
    }

    /**
     * Inform the user that there's not any data if the remindersList is empty.
     */
    private fun invalidateShowNoData() {
        Log.d(TAG, "RemindersListViewModel.invalidateShowNoData().")
        showNoData.value = remindersList.value == null || remindersList.value!!.isEmpty()
    }
}