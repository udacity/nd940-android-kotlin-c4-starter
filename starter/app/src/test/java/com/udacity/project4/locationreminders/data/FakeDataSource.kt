package com.udacity.project4.locationreminders.data

import androidx.lifecycle.MutableLiveData
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result

//Use FakeDataSource that acts as a test double to the LocalDataSource
class FakeDataSource : ReminderDataSource {

    val remindersServiceData: LinkedHashMap<String, ReminderDTO> = LinkedHashMap()

    override suspend fun getReminders(): Result<List<ReminderDTO>> {
        if (remindersServiceData.isNotEmpty())
           return Result.Success(remindersServiceData.values.toList())
        return Result.Error("no reminder found")
    }

    override suspend fun saveReminder(reminder: ReminderDTO) {
        remindersServiceData[reminder.id] = reminder
    }

    override suspend fun getReminder(id: String): Result<ReminderDTO> {
        for ( reminder in remindersServiceData.values)
        {
            if (reminder.id == id)
                return Result.Success(reminder)
        }
        return Result.Error("No reminder with the given id")
    }

    override suspend fun deleteAllReminders() {
        remindersServiceData.values.clear()
    }


}