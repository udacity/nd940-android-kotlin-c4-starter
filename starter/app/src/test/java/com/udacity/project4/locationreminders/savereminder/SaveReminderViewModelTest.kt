package com.udacity.project4.locationreminders.savereminder

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.udacity.project4.R
import com.udacity.project4.locationreminders.MainCoroutineRule
import com.udacity.project4.locationreminders.data.FakeDataSource
import com.udacity.project4.locationreminders.data.dto.Result
import com.udacity.project4.locationreminders.getOrAwaitValue
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.setMain
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.core.Is.`is`
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.GlobalContext.stopKoin

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
class SaveReminderViewModelTest {

	// Executes each task synchronously using Architecture Components.
	@get:Rule
	var instantExecutorRule = InstantTaskExecutorRule()

	// Set the main coroutines dispatcher for unit testing.
	@ExperimentalCoroutinesApi
	@get:Rule
	var mainCoroutineRule = MainCoroutineRule()

	// Subject under test
	private lateinit var saveReminderViewModel: SaveReminderViewModel

	// Use a fake repository to be injected into the view model.
	private lateinit var fakeDataSource: FakeDataSource

	private val item1 = ReminderDataItem(
		"Reminder1",
		"Description1",
		"Location1",
		1.0,
		1.0,
		"1"
	)
	private val item2 = ReminderDataItem(
		"",
		"Description2",
		"location2",
		2.0,
		2.0,
		"2"
	)
	private val item3 = ReminderDataItem(
		"Reminder3",
		"Description3",
		"",
		3.0,
		3.0,
		"3"
	)

	@Before
	fun setUpViewModel() {
		stopKoin()
		fakeDataSource = FakeDataSource()
		saveReminderViewModel = SaveReminderViewModel(
			ApplicationProvider.getApplicationContext(),
			fakeDataSource
		)
	}

	@Test
	fun saveReminderAndCheckItOnDataSource() = runBlocking {
		// GIVEN - saveReminder for item1.
		saveReminderViewModel.saveReminder(item1)

		// WHEN - getReminder for item with id 1.
		val reminderFromDataSource = fakeDataSource.getReminder("1") as Result.Success

		// THEN - Expect to get item1.
		assertThat(reminderFromDataSource.data.title, `is` (item1.title))
		assertThat(reminderFromDataSource.data.description, `is` (item1.description))
		assertThat(reminderFromDataSource.data.location, `is` (item1.location))
		assertThat(reminderFromDataSource.data.latitude, `is` (item1.latitude))
		assertThat(reminderFromDataSource.data.longitude, `is` (item1.longitude))
		assertThat(reminderFromDataSource.data.id, `is` (item1.id))
	}

	@Test
	fun saveReminderAndCheckLoadingIndicator() = runBlocking {
		// GIVEN - saveReminder for item1.
		// Set Main dispatcher to not run coroutines eagerly, for just this one test.
		val dispatcher = StandardTestDispatcher()
		Dispatchers.setMain(dispatcher)
		saveReminderViewModel.saveReminder(item1)

		// WHEN - Loading indicator is shown.
		assertThat(saveReminderViewModel.showLoading.getOrAwaitValue(), `is`(true))
		dispatcher.scheduler.advanceUntilIdle()

		// THEN - Loading indicator is hidden.
		assertThat(saveReminderViewModel.showLoading.getOrAwaitValue(), `is`(false))
	}

	@Test
	fun validateEnteredData_missingTitle_showSnackAndReturnFalse() {
		// GIVEN - validateEnteredData and WHEN - passing no title.
		val validation = saveReminderViewModel.validateEnteredData(item2)

		// THEN - Expect a SnackBar to display err_enter_title string.
		// THEN - Expect validation to return false.
		assertThat(saveReminderViewModel.showSnackBarInt.getOrAwaitValue(), `is` (R.string.err_enter_title))
		assertThat(validation, `is` (false))
	}

	@Test
	fun validateEnteredData_missingLocation_showSnackAndReturnFalse() {
		// GIVEN - validateEnteredData and WHEN - passing no location.
		val valid = saveReminderViewModel.validateEnteredData(item3)

		// THEN - Expect a SnackBar to be shown displaying err_select_location string
		// THEN - Expect validation return false.
		assertThat(saveReminderViewModel.showSnackBarInt.getOrAwaitValue(), `is` (R.string.err_select_location))
		assertThat(valid, `is` (false))
	}
}