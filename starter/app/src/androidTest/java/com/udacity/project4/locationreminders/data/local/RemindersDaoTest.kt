package com.udacity.project4.locationreminders.data.local

import androidx.room.Database
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result
import junit.framework.Assert.assertNull
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.*

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
//Unit test the DAO
@SmallTest
class RemindersDaoTest {

    private lateinit var database:RemindersDatabase
    private lateinit var reminder:ReminderDTO

    @Before
    fun initDb() = runBlocking {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            RemindersDatabase::class.java
        ).build()
        reminder = ReminderDTO("TITLE", "DESCRIPTION", "LOCATION", 0.0, 0.0)
        database.reminderDao().saveReminder(reminder)
    }


    @After
    fun closeDb() = database.close()


    @Test
    fun insertData_getById() = runBlocking {
        // GIVEN - Database

        // WHEN - Save a reminder

        // THEN - Get reminder return the saved reminder and get reminder with a random id return null
        val result = database.reminderDao().getReminderById(reminder.id)
        val falseResult = database.reminderDao().getReminderById(UUID.randomUUID().toString())
        assertThat(result, `is`(reminder))
        assertNull(falseResult)
    }

    @Test
    fun insertData_getAll() = runBlocking {
        // GIVEN - Database

        // WHEN - Save a reminder

        // THEN - Get reminders gives a list of one element
        val result = database.reminderDao().getReminders()
        assertThat(result.size, `is`(1))
        assertThat(result.first(), `is`(reminder))
    }

    @Test
    fun deleteReminders() = runBlocking {
        // GIVEN - Database

        // WHEN - Delete data
        database.reminderDao().deleteAllReminders()

        // THEN - Get reminders gives an empty list
        val result = database.reminderDao().getReminders()
        assertThat(result.isEmpty(), `is`(true))
    }
}