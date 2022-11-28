package com.udacity.project4.locationreminders.data

import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem

//Use FakeDataSource that acts as a test double to the LocalDataSource
class FakeDataSource(var list: MutableList<ReminderDTO>?,var isReturnErrors:Boolean) : ReminderDataSource {


    init {
//        if (strangeList != null) {
//            if (strangeList.isNotEmpty()) {
//                val reminder = ReminderDTO("title", "description", "location", 5.5, 10.5)
//                list = mutableListOf(reminder)
//            }
//        }
    }

    override suspend fun getReminders(): Result<List<ReminderDTO>> {
        if (!isReturnErrors)
            return Result.Success<List<ReminderDTO>>(list!!)
        else {
            return Result.Error("Error")
        }
    }

    override suspend fun saveReminder(reminder: ReminderDTO) {
        if(!isReturnErrors)
        list!!.add(reminder)
    }

    override suspend fun getReminder(id: String): Result<ReminderDTO> {
        if (!isReturnErrors) {
            list!!.firstOrNull { it.id == id }?.let { return Result.Success(it) }
            return Result.Error("404")
        } else
            return Result.Error("Error")
    }

    override suspend fun deleteAllReminders() {
        if(!isReturnErrors)
        list = mutableListOf()
    }


    fun setIsReturnError(isReturnError: Boolean) {
        this.isReturnErrors = isReturnError
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