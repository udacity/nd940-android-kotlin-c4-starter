package com.udacity.project4.locationreminders.data.local

import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result

class FakeDataSource(var reminders: MutableList<ReminderDTO>? = mutableListOf()):ReminderDataSource {
    override suspend fun getReminders(): Result<List<ReminderDTO>> {
        if (reminders != null)
            return Result.Success(reminders!!)
        return Result.Error("no reminder found")
    }

    override suspend fun saveReminder(reminder: ReminderDTO) {
        reminders?.add(reminder)
    }

    override suspend fun getReminder(id: String): Result<ReminderDTO> {
        if (reminders?.firstOrNull { it.id == id }!= null)
            return Result.Success((reminders?.firstOrNull { it.id == id })!!)
        return Result.Error("No reminder of that id found")
    }

    override suspend fun deleteAllReminders() {
        reminders?.clear()
    }

}