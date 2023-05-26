package com.udacity.project4.locationreminders.reminderslist

import android.os.Bundle
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.udacity.project4.R
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.local.LocalDB
import com.udacity.project4.locationreminders.data.local.RemindersLocalRepository
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.get
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify

/**
 * UI Testing.
 */
@RunWith(AndroidJUnit4::class)
@ExperimentalCoroutinesApi
@MediumTest
class ReminderListFragmentTest : KoinTest {

	private val mockNavController = mock(NavController::class.java)
	private lateinit var remindersListViewModel: RemindersListViewModel
	private lateinit var repository: ReminderDataSource

	@Before
	fun init() {
		stopKoin()
		val module = module {
			single {
				SaveReminderViewModel(
					ApplicationProvider.getApplicationContext(),
					get() as ReminderDataSource
				)
			}
			viewModel {
				RemindersListViewModel(
					ApplicationProvider.getApplicationContext(),
					get() as ReminderDataSource
				)
			}
			single {
				LocalDB.createRemindersDao(ApplicationProvider.getApplicationContext())
			}
			single { RemindersLocalRepository(get()) as ReminderDataSource }

		}
		startKoin {
			modules(listOf(module))
		}
		repository = get()
		runBlocking {
			repository.deleteAllReminders()
		}
		remindersListViewModel =
			RemindersListViewModel(ApplicationProvider.getApplicationContext(), repository)
	}

	@Test
	fun homeScreen_afterOpeningApp_checkNoDataMessageIsDisplayed() {
		// GIVEN - On the home screen.
		launchFragmentInContainer<ReminderListFragment>(Bundle(), R.style.AppTheme)

		// THEN - Verify that we have No Data in our RecyclerView.
		onView(withText("No Data")).check(matches(isDisplayed()))
	}

	@Test
	fun homeScreen_onClick_navigateToSaveReminderScreen() {
		// GIVEN - On the home screen.
		val sec = launchFragmentInContainer<ReminderListFragment>(Bundle(), R.style.AppTheme)
		sec.onFragment { Navigation.setViewNavController(it.view!!, mockNavController) }

		// WHEN  - Click on the FAB.
		onView(withId(R.id.addReminderFAB)).perform(click())

		// THEN - Verify that we navigate to the first Save Reminder screen.
		verify(mockNavController).navigate(
			ReminderListFragmentDirections.actionReminderListFragmentToSaveReminderFragment()
		)
	}

	@Test
	fun homeScreen_savingAReminderon_reminderInfoIsDisplayed() {
		// GIVEN - Saving a Reminder.
		val remind = ReminderDTO("1", "2", "3", 0.0, 0.9)
		runBlocking {
			repository.saveReminder(remind)
		}

		// WHEN - On the home screen.
		launchFragmentInContainer<ReminderListFragment>(Bundle(), R.style.AppTheme)

		// THEN - Verify that the title, description and location are displayed.
		onView(withText(remind.title)).check(matches(isDisplayed()))
		onView(withText(remind.description)).check(matches(isDisplayed()))
		onView(withText(remind.location)).check(matches(isDisplayed()))
	}
}