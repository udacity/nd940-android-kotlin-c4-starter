package com.udacity.project4.locationreminders.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
//Medium Test to test the repository
@MediumTest
class RemindersLocalRepositoryTest {


    private lateinit var localRepository : RemindersLocalRepository
    private lateinit var reminderDao: RemindersDao
    private lateinit var database: RemindersDatabase

    @Before
    fun init() {

        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            RemindersDatabase::class.java
        ).allowMainThreadQueries().build()
        reminderDao = database.reminderDao()
        localRepository = RemindersLocalRepository(reminderDao)

    }

    @After
    fun cleanUp()
    {
        database.close()
    }

    @Test
    fun saveReminders_getRemindersInLocalRepository() = runBlocking {
        // GIVEN - list of reminders
        val reminders = listOf(
            ReminderDTO("TITLE1", "DESCRIPTION1", "LOCATION1", 1.1, 0.0),
            ReminderDTO("TITLE2", "DESCRIPTION2", "LOCATION2", 1.1, 0.0)
        )

        // WHEN - Save in local repository
        reminders.forEach{
            localRepository.saveReminder(it)
        }

        // THEN - local repository return our list
        val loaded = localRepository.getReminders() as Result.Success
        assertThat(loaded.data, `is`(reminders))
    }


    @Test
    fun saveReminder_getReminderById() = runBlocking {
        // GIVEN - list of reminders
        val reminder = ReminderDTO("TITLE1", "DESCRIPTION1", "LOCATION1", 1.1, 0.0, "id1")

        // WHEN - Save in local repository

        localRepository.saveReminder(reminder)

        // THEN - local repository return our list
        val loaded = localRepository.getReminder("id1") as Result.Success
        assertThat(loaded.data, `is`(reminder))
    }


    @Test
    fun noReminders_getReminderByIdAndGetReminders_returnError() = runBlocking {
        // GIVEN - fresh localRepository
        localRepository.deleteAllReminders()

        // WHEN - Save in local repository
        val reminders = localRepository.getReminders() as Result.Success
        val reminder = localRepository.getReminder("id") as Result.Error

        // THEN - local repository return our list
        assertThat(reminders.data, `is`(listOf()))
        assertThat(reminder.message, `is`("Reminder not found!"))
    }


}