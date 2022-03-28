package com.udacity.project4.locationreminders.savereminder


import android.content.res.Resources
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.udacity.project4.R
import com.udacity.project4.locationreminders.MainCoroutineRule
import com.udacity.project4.locationreminders.data.FakeDataSource
import com.udacity.project4.locationreminders.getOrAwaitValue
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
class SaveReminderViewModelTest {


    private lateinit var reminderViewModel: SaveReminderViewModel
    private lateinit var repository: FakeDataSource


    @get:Rule
    val mainCoroutineRule = MainCoroutineRule()

    @After
    fun resetViewModel() = runBlocking{
        repository.deleteAllReminders()
    }

    @After
    fun tearsDown(){
        stopKoin()
    }

    @Before
    fun setUpViewModel(){
        repository = FakeDataSource()
        reminderViewModel = SaveReminderViewModel(getApplicationContext(), repository)
    }

    @Test
    fun addReminder_setsNewReminderEvent() {

        // GIVEN - fresh viewModel, a fake data source and a reminder data item
        val reminder1 = ReminderDataItem("TITLE1", "DESCRIPTION1", "LOCATION1", 0.0, 0.0)
        val resource = InstrumentationRegistry.getInstrumentation().context.resources


        // WHEN - Save the reminder
        mainCoroutineRule.pauseDispatcher()
        reminderViewModel.validateAndSaveReminder(reminder1)

        // THEN - Verify that loading status is displayed, and value is saved successfully
        assertThat(reminderViewModel.showLoading.getOrAwaitValue(), `is`(true))
        mainCoroutineRule.resumeDispatcher()
        assertThat(reminderViewModel.showLoading.getOrAwaitValue(), `is`(false))
        assertThat(reminderViewModel.showToast.getOrAwaitValue(), `is`(resource.getString(R.string.reminder_saved)))
        // TODO check if it navigate back as expected

    }

    @Test
    fun saveReminderWhenTitleNull_returnError(){
        // GIVEN - A fresh viewModel, and repository
        val reminder = ReminderDataItem(null, "description", "localisation", 0.0, 0.0)

        // WHEN - Add a new task with no title
        reminderViewModel.validateAndSaveReminder(reminder)

        // THEN - Return error
        assertThat(reminderViewModel.showSnackBarInt.getOrAwaitValue(), `is`(R.string.err_enter_title))
    }

    @Test
    fun saveReminderWhenLocationEmpty_returnError(){
        // GIVEN - A fresh viewModel, and repository
        val reminder1 = ReminderDataItem("TITLE", "description", null, 0.0, 0.0)

        // WHEN - Add a new task with null location
        reminderViewModel.validateAndSaveReminder(reminder1)

        // THEN - Return error
        assertThat(reminderViewModel.showSnackBarInt.getOrAwaitValue(), `is`(R.string.err_select_location))
    }
}