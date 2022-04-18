package com.udacity.project4.locationreminders.reminderslist

import android.app.Application
import android.os.Bundle
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.navigation.ActionOnlyNavDirections
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.udacity.project4.MainCoroutineRule
import com.udacity.project4.R
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result
import com.udacity.project4.locationreminders.data.local.FakeDataSource
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runBlockingTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify

@RunWith(AndroidJUnit4::class)
@ExperimentalCoroutinesApi
//UI Testing
@MediumTest
class ReminderListFragmentTest {

    private lateinit var repository: FakeDataSource
    private lateinit var remindersListViewModel: RemindersListViewModel

    @get:Rule
    val mainCoroutineRule = MainCoroutineRule()

    @Before
    fun initRepository() = mainCoroutineRule.runBlockingTest {
        repository = FakeDataSource()
        val application = ApplicationProvider.getApplicationContext<Application>()
        remindersListViewModel = RemindersListViewModel(application, repository)
        stopKoin()

        val myModule = module {
            single {
                remindersListViewModel
            }
        }
        // new koin module
        startKoin {
            modules(listOf(myModule))
        }
    }


    @After
    fun clearData() = runBlocking {
        repository.deleteAllReminders()
    }

    @Test
    fun addReminderClicked_navigateToAddReminder(){
        // GIVEN - A reminder list fragment
        val navController = launchFragment()

        // WHEN - Click on add reminder button
        onView(withId(R.id.addReminderFAB)).perform(click())

        // THEN - Navigate to add reminder fragment
        verify(navController)?.navigate(ReminderListFragmentDirections.toSaveReminder())
    }


    @Test
    fun saveReminder_reminderDisplayedInUI() = runBlockingTest {
        // GIVEN - A fragment with on saved reminder
        val reminder1 = ReminderDTO("TITLE1", "DESCRIPTION1", "LOCATION1", 0.0, 0.0)
        repository.saveReminder(reminder1)

        // WHEN - Fragment launched
        launchFragment()

        // THEN - Saved reminder is displayed
        val reminders = (repository.getReminders() as Result.Success).data
        for (element in reminders) {
            onView(withText(element.title)).check(matches(isDisplayed()))
            onView(withText(element.description)).check(matches(isDisplayed()))
            onView(withText(element.location)).check(matches(isDisplayed()))
        }
    }

    private fun launchFragment(): NavController {
        val scenario =
            launchFragmentInContainer<ReminderListFragment>(themeResId = R.style.AppTheme)
        val navController = mock(NavController::class.java)

        scenario.onFragment { fragment ->
            Navigation.setViewNavController(fragment.requireView(), navController)
        }
        return navController
    }

    @Test
    fun deleteReminders_getReminders_returnError() = runBlockingTest {
        // GIVEN - A repository with a null reminder and a fragment
        repository.reminders = null
        launchFragment()

        // WHEN - Load reminders
        remindersListViewModel.loadReminders()

        // THEN - Error message displayed
        onView(withText("no reminder found")).check(matches(isDisplayed()))

    }
}