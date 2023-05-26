package com.udacity.project4.locationreminders.data

import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result

/**
 * Use FakeDataSource that acts as a test double to the LocalDataSource
 */
class FakeDataSource(private var reminderList: MutableList<ReminderDTO>? = mutableListOf())
    : ReminderDataSource {

    private var shouldReturnError = false

    /**
     * Create a fake data source to act as a double to the real data source.
     */
    fun returnError(value: Boolean){
        shouldReturnError = value
    }

    override suspend fun getReminders(): Result<List<ReminderDTO>> {
        // Confirm the correct behavior when the reminders list for some reason can't be loaded
        return if (shouldReturnError){
            Result.Error("No Reminders found")
        } else {
            Result.Success(ArrayList(reminderList))
        }
    }

    override suspend fun saveReminder(reminder: ReminderDTO) {
        reminderList?.add(reminder)
    }


    override suspend fun getReminder(id: String): Result<ReminderDTO> {
        // Confirm the correct behavior when the reminders list for some reason can't be loaded.
        if (shouldReturnError){
            return Result.Error("No Reminders found")
        }
        // Return the reminder with the id
        val reminder = reminderList?.find {
            it.id == id
        }
        return if (reminder!=null){
            Result.Success(reminder)
        } else{
            Result.Error("No Reminders found")
        }
    }

    override suspend fun deleteAllReminders() {
        // Delete all the reminders
        reminderList?.clear()
    }
}