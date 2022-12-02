package com.udacity.project4.locationreminders.data.local

import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withContext
import org.hamcrest.CoreMatchers
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import org.junit.runner.RunWith
import java.io.IOException

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
//Medium Test to test the repository
@MediumTest
class RemindersLocalRepositoryTest {
    //  (DONE)  TODO: Add testing implementation to the RemindersLocalRepository.kt
    private lateinit var repository: RemindersLocalRepository
    private lateinit var database: RemindersDatabase
    private lateinit var dao: RemindersDao

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(
            context, RemindersDatabase::class.java
        ).build()
        dao = database.reminderDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        database.close()
    }

    @ExperimentalCoroutinesApi
    class CoroutineTestRule(val testDispatcher: TestCoroutineDispatcher = TestCoroutineDispatcher()) :
        TestWatcher() {
        override fun starting(description: Description?) {
            super.starting(description)
            Dispatchers.setMain(testDispatcher)
        }

        override fun finished(description: Description?) {
            super.finished(description)
            Dispatchers.resetMain()
            testDispatcher.cleanupTestCoroutines()
        }
    }

    @get:Rule
    var coroutinesTestRule = CoroutineTestRule()

    @Test
    suspend fun testRoom() {
        return withContext(Dispatchers.Default) {
            val newTask = ReminderDTO("title", "description", "22.8745, 88.6971", 22.8745, 88.6971)
            dao.saveReminder(newTask)
            val byId = dao.getReminderById(newTask.id)
            ViewMatchers.assertThat(byId?.id, CoreMatchers.`is`(newTask.id))
            return@withContext
        }
    }

    @Test
    fun saveReminder_retrievesReminder() {
        runBlocking {
            //Given
            val newTask = ReminderDTO("title", "description", "22.8745, 88.6971", 22.8745, 88.6971)
            repository.saveReminder(newTask)

            //When
            val result = repository.getReminder(newTask.id)

            //Then
            result as Result.Success
            ViewMatchers.assertThat(result.data.title, CoreMatchers.`is`("title"))
            ViewMatchers.assertThat(result.data.description, CoreMatchers.`is`("description"))
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

}