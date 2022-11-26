package com.udacity.project4.locationreminders.data

import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result

//Use FakeDataSource that acts as a test double to the LocalDataSource
class FakeDataSource(var strangeList: MutableList<ReminderDTO>) : ReminderDataSource {
    var list: MutableList<ReminderDTO>

    init {
        list = strangeList
//        if (strangeList != null) {
//            if (strangeList.isNotEmpty()) {
//                val reminder = ReminderDTO("title", "description", "location", 5.5, 10.5)
//                list = mutableListOf(reminder)
//            }
//        }
    }

    override suspend fun getReminders(): Result<List<ReminderDTO>> {
        return Result.Success<List<ReminderDTO>>(list)
    }

    override suspend fun saveReminder(reminder: ReminderDTO) {
        list.add(reminder)
    }

    override suspend fun getReminder(id: String): Result<ReminderDTO> {
        list.firstOrNull { it.id == id }?.let { return Result.Success(it) }
        return Result.Error("404")
    }

    override suspend fun deleteAllReminders() {
        list = mutableListOf()
    }



    fun setShouldFail(shouldFail: Boolean) {
        this.shouldFail = shouldFail
    }

//    override suspend fun getReminders(): Result<List<ReminderDTO>> {
//        return try {
//            if (shouldFail) throw Exception("Error getting reminders")
//            Result.Success(reminderList)
//        } catch (e: Exception) {
//            Result.Error(e.message)
//        }
//    }

}