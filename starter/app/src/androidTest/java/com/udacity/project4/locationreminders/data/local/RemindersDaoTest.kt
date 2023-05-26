package com.udacity.project4.locationreminders.data.local

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.notNullValue
import org.hamcrest.CoreMatchers.nullValue
import org.hamcrest.MatcherAssert.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Unit test the DAO.
 */
@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
@SmallTest
class RemindersDaoTest {

	@get:Rule
	var instantTaskExecutorRule = InstantTaskExecutorRule()

	private lateinit var database: RemindersDatabase

	private val item1 = ReminderDTO("1", "first", "loc", 122.6, 122.7)
	private val item2 = ReminderDTO("2", "second", "loc2", 0.0, 0.0)

	@Before
	fun initDb() {
		database = Room.inMemoryDatabaseBuilder(
			getApplicationContext(), RemindersDatabase::class.java
		).allowMainThreadQueries().build()
	}

	@After
	fun closeDb() {
		database.close()
	}

	@Test
	fun insertAReminder_thenGettingItById_returnsThisSameReminder() = runBlocking {
		// GIVEN - Insert a Reminder.
		database.reminderDao().saveReminder(item1)

		// WHEN - Getting a Reminder by ID.
		val check = database.reminderDao().getReminderById(item1.id)

		// THEN - The loaded data contains the expected values.
		assertThat(check as ReminderDTO, notNullValue())
		assertThat(check.latitude, `is`(item1.latitude))
		assertThat(check.location, `is`(item1.location))
		assertThat(check.id, `is`(item1.id))
		assertThat(check.longitude, `is`(item1.longitude))
		assertThat(check.description, `is`(item1.description))
		assertThat(check.title, `is`(item1.title))
	}

	@Test
	fun insertAReminder_thenDeletingAllReminders_returnsNullForThisInsertedReminder() = runBlocking {
		// GIVEN - Insert a Reminder.
		database.reminderDao().saveReminder(item1)

		// WHEN - Deleting all Reminders.
		database.reminderDao().deleteAllReminders()

		// THEN - Getting the inserted Reminder returns null.
		assertThat(database.reminderDao().getReminderById(item1.id), `is`(nullValue()))
	}

	@Test
	fun insertTwoReminders_thenGetAllReminders_returnsANonEmptyListOfReminders() = runBlocking {
		// GIVEN - Inserting two Reminders.
		database.reminderDao().saveReminder(item1)
		database.reminderDao().saveReminder(item2)

		// WHEN - Getting all Reminders.
		val list = database.reminderDao().getReminders()

		// THEN - Check if list of Reminders is not null.
		assertThat(list.isNotEmpty(), `is`(true))
	}
}