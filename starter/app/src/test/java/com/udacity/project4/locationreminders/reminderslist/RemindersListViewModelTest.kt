package com.udacity.project4.locationreminders.reminderslist

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.udacity.project4.locationreminders.MainCoroutineRule
import com.udacity.project4.locationreminders.data.FakeDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.getOrAwaitValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.setMain
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.core.Is.`is`
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin

@RunWith(AndroidJUnit4::class)
@ExperimentalCoroutinesApi
class RemindersListViewModelTest {

	// Executes each task synchronously using Architecture Components.
	@get:Rule
	var instantExecutorRule = InstantTaskExecutorRule()

	// Set the main coroutines dispatcher for unit testing.
	@ExperimentalCoroutinesApi
	@get:Rule
	var mainCoroutineRule = MainCoroutineRule()

	// Subject under test
	private lateinit var remindersListViewModel: RemindersListViewModel

	// Use a fake repository to be injected into the view model.
	private lateinit var fakeDataSource: FakeDataSource

	private val item1 = ReminderDTO(
		"Reminder1",
		"Description1",
		"Location1",
		1.0,
		1.0,
		"1"
	)
	private val item2 = ReminderDTO(
		"Reminder2",
		"Description2",
		"location2",
		2.0,
		2.0,
		"2"
	)
	private val item3 = ReminderDTO(
		"Reminder3",
		"Description3",
		"location3",
		3.0,
		3.0,
		"3"
	)

	@Before
	fun setupRemindersListViewModel() {
		stopKoin()
		fakeDataSource = FakeDataSource()
		remindersListViewModel = RemindersListViewModel(
			ApplicationProvider.getApplicationContext(),
			fakeDataSource
		)
	}

	@After
	fun clearData() = runBlocking {
		fakeDataSource.deleteAllReminders()
	}

	/**
	 * This function tries to load the Reminders from our ViewModel after removing all Reminders.
	 */
	@Test
	fun invalidateShowNoDataShowNoDataIsTrue() = runBlocking {
		// Delete all current Reminders.
		fakeDataSource.deleteAllReminders()

		// Try to load them.
		remindersListViewModel.loadReminders()

		// Expect that our remindersList LiveData size is 0 and showNoData is true.
		assertThat(remindersListViewModel.remindersList.getOrAwaitValue().size, `is` (0))
		assertThat(remindersListViewModel.showNoData.getOrAwaitValue(), `is` (true))
	}

	/**
	 * Test retrieving the 3 reminders added to our Data Source.
	 */
	@Test
	fun loadRemindersLoadsThreeReminders()= runBlocking {
		// Add 3 Reminders in our Data Source.
		fakeDataSource.deleteAllReminders()
		fakeDataSource.saveReminder(item1)
		fakeDataSource.saveReminder(item2)
		fakeDataSource.saveReminder(item3)

		// Try to load the Reminders.
		remindersListViewModel.loadReminders()

		// Expect to have only 3 reminders in remindersList,
		// and showNoData is false because we have data.
		assertThat(remindersListViewModel.remindersList.getOrAwaitValue().size, `is` (3))
		assertThat(remindersListViewModel.showNoData.getOrAwaitValue(), `is` (false))
	}

	/**
	 * Here, we are testing checkLoading in this test.
	 */
	@Test
	fun loadRemindersCheckLoading() = runBlocking {
		// Set Main dispatcher to not run coroutines eagerly, for just this one test.
		val dispatcher = StandardTestDispatcher()
		Dispatchers.setMain(dispatcher)

		// Stop dispatcher so we may inspect initial values.
//		mainCoroutineRule.pauseDispatcher() // <--- deprecated

		// Only 1 Reminder.
		fakeDataSource.deleteAllReminders()
		fakeDataSource.saveReminder(item1)

		// Load Reminders.
		remindersListViewModel.loadReminders()

		// The loading indicator is displayed, then it is hidden after we are done.
		assertThat(remindersListViewModel.showLoading.getOrAwaitValue(), `is`(true))

		// Execute pending coroutines actions.
//		mainCoroutineRule.resumeDispatcher() // <--- deprecated
		dispatcher.scheduler.advanceUntilIdle() // https://kt.academy/article/cc-testing

		// Then, loading indicator is hidden.
		assertThat(remindersListViewModel.showLoading.getOrAwaitValue(), `is`(false))
	}

	/**
	 * Testing showing an Error.
	 */
	@Test
	fun loadRemindersShouldReturnError() = runBlocking {
		// GIVEN - Set returnError to true.
		fakeDataSource.returnError(true)
		// WHEN - We load the Reminders.
		remindersListViewModel.loadReminders()
		// THEN - We get showSnackBar MutableLiveData in the ViewModel returning us "not found".
		assertThat(remindersListViewModel.showSnackBar.getOrAwaitValue(), `is`("No Reminders found"))
	}
}