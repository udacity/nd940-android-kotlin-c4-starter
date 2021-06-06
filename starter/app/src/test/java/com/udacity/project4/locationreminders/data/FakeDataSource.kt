package com.udacity.project4.locationreminders.data

import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result

//Use FakeDataSource that acts as a test double to the LocalDataSource
class FakeDataSource(var reminders: MutableList<ReminderDTO>? = mutableListOf()) : ReminderDataSource {

    // Create a fake data source to act as a double to the real data source
    private var shouldReturnError = false

    fun setShouldReturnError(value: Boolean) {
        this.shouldReturnError = value
    }

    override suspend fun getReminders(): Result<List<ReminderDTO>> {
        if (shouldReturnError) {
            return Result.Error("Reminders not found", 404)
        } else {
            return Result.Success(ArrayList(reminders))
        }
    }

    override suspend fun saveReminder(reminder: ReminderDTO) {
        reminders?.add(reminder)
    }

    override suspend fun getReminder(id: String): Result<ReminderDTO> {
        return if (shouldReturnError) {
            Result.Error("Error message")
        } else {
            val reminder = reminders?.find { it.id == id }

            if (null != reminder) {
                Result.Success(reminder)
            } else {
                Result.Error("Reminder not found!", 404)
            }
        }
    }

    override suspend fun deleteAllReminders() {
        reminders?.clear()
    }


}