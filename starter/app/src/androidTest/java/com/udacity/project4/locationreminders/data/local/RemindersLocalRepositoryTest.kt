package com.udacity.project4.locationreminders.data.local

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.idling.CountingIdlingResource
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result
import com.udacity.project4.util.DataBindingIdlingResource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.hamcrest.CoreMatchers
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private const val RESOURCE = "GLOBAL"

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
//Medium Test to test the repository
@MediumTest
class RemindersLocalRepositoryTest {
    //  (DONE)  TODO: Add testing implementation to the RemindersLocalRepository.kt
    private lateinit var repository: RemindersLocalRepository
    private lateinit var database: RemindersDatabase

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    @Before
    fun dataBaseSetup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            RemindersDatabase::class.java
        )
            .allowMainThreadQueries()
            .build()
        repository =
            RemindersLocalRepository(
                database.reminderDao(),
                Dispatchers.Main
            )
    }
    @Rule
    @JvmField
    val dispatchers = DataBindingIdlingResource()

    // LoginTest.kt:
    @Before
    fun registerIdlingResource() {
        IdlingRegistry.getInstance().register(CountingIdlingResourceSingleton.countingIdlingResource)
    }

    @After
    fun unregisterIdlingResource() {
        IdlingRegistry.getInstance().unregister(CountingIdlingResourceSingleton.countingIdlingResource)
    }

    @JvmField val countingIdlingResource = CountingIdlingResource(RESOURCE)
    @Test
    fun saveReminder_retrievesReminder() {
CoroutineScope(Dispatchers.Main).launch {
             try {
                 countingIdlingResource.increment()
                //Given
                val newTask =
                    ReminderDTO("title", "description", "22.8745, 88.6971", 22.8745, 88.6971)
                repository.saveReminder(newTask)

                //When
                val result = repository.getReminder(newTask.id)

                //Then
                result as Result.Success
                ViewMatchers.assertThat(result.data.title, CoreMatchers.`is`("title"))
                ViewMatchers.assertThat(result.data.description, CoreMatchers.`is`("description"))
            }finally {
                 countingIdlingResource.decrement() // Set app as idle.
                 }
        }
    }

    @Test
    fun completeReminder_retrievedReminderIsComplete() {
        runTest {
            //Given
            val newTask = ReminderDTO("title", "description", "10.5, 11.5", 10.5, 11.5)
            repository.saveReminder(newTask)

            //When
            val result = repository.getReminder(newTask.id)

            //Then
            result as Result.Success
            ViewMatchers.assertThat(result.data.title, CoreMatchers.`is`(newTask.title))
        }
    }

    @Test
    fun errorReminder_retrievesReminder() {
        runTest {
            //Given
            val newTask = ReminderDTO("title", "description", "10.5, 11.5", 10.5, 11.5)

            //When
            val result = repository.getReminder(newTask.id)

            //Then
            result as Result.Error
            ViewMatchers.assertThat(result.message, CoreMatchers.`is`("Reminder not found!"))
        }
    }

    @After
    fun dataBaseCloseAfterFinish() {
        database.close()
    }
}