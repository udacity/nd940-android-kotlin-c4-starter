package com.udacity.project4.locationreminders.reminderslist

import android.os.Bundle
import androidx.fragment.app.testing.launchFragmentInContainer
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
import com.udacity.project4.locationreminders.data.local.FakeDataSource
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runBlockingTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
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
        val reminder1 = ReminderDTO("TITLE1", "DESCRIPTION1", "LOCATION1", 0.0, 0.0)
        repository.saveReminder(reminder1)
        remindersListViewModel = RemindersListViewModel(ApplicationProvider.getApplicationContext(), repository)

    }


    @After
    fun clearData() = runBlocking {
        repository.deleteAllReminders()
    }

    //    TODO: test the navigation of the fragments.
    // TODO uncomment the following code
    @Test
    fun addReminderClicked_navigateToAddReminder(){
        // GIVEN - A reminder list fragment
        val scenario = launchFragmentInContainer<ReminderListFragment>(themeResId = R.style.AppTheme)
        val navController = mock(NavController::class.java)

        scenario.onFragment {
            fragment ->
            Navigation.setViewNavController(fragment.requireView(), navController)
        }

        // WHEN - Click on add reminder button
        onView(withId(R.id.addReminderFAB)).perform(click())

        // THEN - Navigate to add reminder fragment
        verify(navController).navigate(R.id.saveReminderFragment)
    }

//    TODO: test the displayed data on the UI.

    @Test
    fun saveReminder_reminderDisplayedInUI() {
        // GIVEN - A fragment with on saved reminder
        val scenario = launchFragmentInContainer<ReminderListFragment>(themeResId = R.style.AppTheme)
        val navController = mock(NavController::class.java)

        scenario.onFragment {
            Navigation.setViewNavController(it.requireView(), navController)
        }
        // WHEN - Fragment launched
        onView(withText("TITLE1")).check(matches(isDisplayed()))
    }

//    TODO: add testing for the error messages.

}