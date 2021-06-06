package com.udacity.project4.locationreminders.data.local

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.hamcrest.CoreMatchers
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.instanceOf
import org.hamcrest.MatcherAssert.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
//Medium Test to test the repository
@MediumTest
class RemindersLocalRepositoryTest {

    private lateinit var localRepository: RemindersLocalRepository
    private lateinit var remindersDatabase: RemindersDatabase

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    @Before
    fun initLocalDatasource() {
        remindersDatabase = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            RemindersDatabase::class.java
        )
            .allowMainThreadQueries()
            .build()

        localRepository = RemindersLocalRepository(
            remindersDatabase.reminderDao(),
            Dispatchers.Main
        )
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
    fun saveReminderAndGetById() = runBlocking {
        val reminder = getReminder()
        localRepository.saveReminder(reminder)

        val fetchedReminder = localRepository.getReminder(reminder.id)

        assertThat(fetchedReminder is Result.Success, `is`(true))
        fetchedReminder as Result.Success
        assertThat(fetchedReminder.data.title, `is`(reminder.title))
        assertThat(fetchedReminder.data.description, `is`(reminder.description))
        assertThat(fetchedReminder.data.location, `is`(reminder.location))
        assertThat(fetchedReminder.data.latitude, `is`(reminder.latitude))
        assertThat(fetchedReminder.data.longitude, `is`(reminder.longitude))
    }

    @Test
    fun deleteAllRemindersAndGetRemindersById() = runBlocking {
        val reminder = getReminder()
        localRepository.saveReminder(reminder)
        localRepository.deleteAllReminders()

        val fetchedReminder = localRepository.getReminder(reminder.id)

        assertThat(fetchedReminder is Result.Error, `is`(true))
        fetchedReminder as Result.Error
        assertThat(fetchedReminder.message, `is`("Reminder not found!"))
    }
}