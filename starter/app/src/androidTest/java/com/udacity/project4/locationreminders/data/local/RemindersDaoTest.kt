package com.udacity.project4.locationreminders.data.local

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;
import com.udacity.project4.locationreminders.data.dto.ReminderDTO

import org.junit.Before;
import org.junit.Rule;
import org.junit.runner.RunWith;

import kotlinx.coroutines.ExperimentalCoroutinesApi;
import kotlinx.coroutines.test.runBlockingTest
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.notNullValue
import org.hamcrest.MatcherAssert.assertThat
import org.junit.After
import org.junit.Test

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
//Unit test the DAO
@SmallTest
class RemindersDaoTest {

    private lateinit var remindersDatabase: RemindersDatabase

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    @Before
    fun initDb() {
        remindersDatabase = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            RemindersDatabase::class.java
        ).build()
    }

    @After
    fun closeDatabase() = remindersDatabase.close()

    private fun getReminder(): ReminderDTO {
        return ReminderDTO(
            title = "test title",
            description = "test description",
            location = "location",
            latitude = 28.377374445116924,
            longitude = -81.57004261842762
        )
    }

    @Test
    fun insertReminderAndGetById() = runBlockingTest {
        val reminder = getReminder()
        remindersDatabase.reminderDao().saveReminder(reminder)

        val fetchedReminder = remindersDatabase.reminderDao().getReminderById(reminder.id)

        assertThat<ReminderDTO>(fetchedReminder as ReminderDTO, notNullValue())
        assertThat(fetchedReminder.id, `is`(reminder.id))
        assertThat(fetchedReminder.title, `is`(reminder.title))
        assertThat(fetchedReminder.description, `is`(reminder.description))
        assertThat(fetchedReminder.location, `is`(reminder.location))
        assertThat(fetchedReminder.latitude, `is`(reminder.latitude))
        assertThat(fetchedReminder.longitude, `is`(reminder.longitude))
    }
}