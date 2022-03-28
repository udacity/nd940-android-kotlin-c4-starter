package com.udacity.project4.locationreminders.reminderslist

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.udacity.project4.MainCoroutineRule
import com.udacity.project4.R
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result
import com.udacity.project4.locationreminders.data.local.FakeDataSource
import com.udacity.project4.locationreminders.reminderslist.ReminderListFragment
import com.udacity.project4.locationreminders.reminderslist.ReminderListFragmentDirections
import com.udacity.project4.locationreminders.reminderslist.RemindersListViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runBlockingTest
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.Matchers
import org.hamcrest.TypeSafeMatcher
import org.hamcrest.core.Is.`is`
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.AutoCloseKoinTest
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
    fun initRepository() = runBlocking {
        repository = FakeDataSource()
        val reminder1 = ReminderDTO("TITLE1", "DESCRIPTION1", "LOCATION1", 0.0, 0.0)
        repository.saveReminder(reminder1)
        remindersListViewModel = RemindersListViewModel(ApplicationProvider.getApplicationContext(), repository)
    }


    @After
    fun clearData() = runBlocking {
        repository.deleteAllReminders()
        stopKoin()
    }

    //    TODO: test the navigation of the fragments.
    @Test
    fun addReminderClicked_navigateToAddReminder(){
        // GIVEN - Reminder fragment list launched
        val scenario = launchFragmentInContainer<ReminderListFragment>(Bundle(), R.style.AppTheme)
        val navController = mock(NavController::class.java)
        scenario.onFragment {
            Navigation.setViewNavController(it.requireView(), navController)
        }

        // WHEN - click on add element
        onView(withId(R.id.addReminderFAB)).perform(click())

        // THEN - Verify if navigated to add reminder fragment
        verify(navController).navigate(ReminderListFragmentDirections.toSaveReminder())

    }

//    TODO: test the displayed data on the UI.

    @Test
    fun saveReminder_reminderDisplayedInUI() {
        remindersListViewModel.loadReminders()
        val scenario = launchFragmentInContainer<ReminderListFragment>(themeResId = R.style.AppTheme)

        onView(withId(R.id.refreshLayout)).check(matches(isDisplayed()))
        onView(withText("TITLE1")).check(matches(isDisplayed()))
    }

    // TODO delete this test
    @Test
    fun one_equalsOne(){
        assertThat(1, `is`(1))
    }

//    TODO: add testing for the error messages.

}