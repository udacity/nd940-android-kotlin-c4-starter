package com.udacity.project4.locationreminders.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.hamcrest.CoreMatchers
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Medium Test to test the repository.
 */
@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
@MediumTest
class RemindersLocalRepositoryTest {

	private lateinit var repository: RemindersLocalRepository
	private lateinit var database: RemindersDatabase

	private val item1 = ReminderDTO("1", "2", "3", 100.0, 120.0)
	private val item2 = ReminderDTO("2", "second", "loc2", 0.0, 0.0)

	@Before
	fun setup() {
		database = Room.inMemoryDatabaseBuilder(
			ApplicationProvider.getApplicationContext(),
			RemindersDatabase::class.java
		).allowMainThreadQueries().build()
		repository = RemindersLocalRepository(database.reminderDao(), UnconfinedTestDispatcher())
	}

	@After
	fun cleanUp() = database.close()

	@Test
	fun gettingANewReminderWithoutSavingItOnDatabase_returnsNoReminderFound() = runBlocking {
		// GIVEN - Trying to get a new Reminder without saving it on database.
		val result = repository.getReminder(item1.id)

		// THEN - No Reminder was found.
		assertThat(result is Result.Error, `is`(true))
		result as Result.Error
		assertThat(result.message, `is`("Reminder not found!"))
	}

	@Test
	fun gettingANewReminderAfterSavingItOnDatabase_returnsThisSameReminder() = runBlocking {
		// GIVEN - A new Reminder saved in the database.
		repository.saveReminder(item1)

		// WHEN  - Reminder retrieved by ID.
		val result = repository.getReminder(item1.id) as Result.Success<ReminderDTO>
		val loaded = result.data

		// THEN - Same Reminder is returned.
		assertThat(loaded.longitude, `is`(item1.longitude))
		assertThat(loaded.latitude, `is`(item1.latitude))
		assertThat(loaded, CoreMatchers.notNullValue())
		assertThat(loaded.id, `is`(item1.id))
		assertThat(loaded.description, `is`(item1.description))
		assertThat(loaded.location, `is`(item1.location))
		assertThat(loaded.title, `is`(item1.title))
	}

	@Test
	fun deletingAllReminders_returnsARepoWithNoReminders() = runBlocking {
		// GIVEN - Deleting all Reminders.
		repository.deleteAllReminders()

		// THEN - There are no Reminders in our repo.
		val result = repository.getReminders() as Result.Success
		val data = result.data
		assertThat(data, `is`(emptyList()))
	}
}