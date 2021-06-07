package com.udacity.project4.locationreminders.reminderslist

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.ViewModel
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.udacity.project4.locationreminders.MainCoroutineRule
import com.udacity.project4.locationreminders.data.FakeDataSource
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.getOrAwaitValue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin

@RunWith(AndroidJUnit4::class)
@ExperimentalCoroutinesApi
class RemindersListViewModelTest {
    private lateinit var dataSource: FakeDataSource
    private lateinit var remindersListViewModel: RemindersListViewModel

    /**
     * Rules
     */
    // Executes each task synchronously using Architecture Components.
    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()


    @Before
    fun setupViewModel() {
        stopKoin()
        dataSource = FakeDataSource()
        dataSource.reminders = getRemindersForTesting()
        remindersListViewModel = RemindersListViewModel(
            ApplicationProvider.getApplicationContext(),
            dataSource)
    }


    private fun getRemindersForTesting(): MutableList<ReminderDTO> {

        val r1 = ReminderDTO("Call a friend","calling ...",
            "Parc de La Victoire",
            36.74208242672705,3.072958588600159)

        val r2 = ReminderDTO("Do this task","the task",
            "camp international de scouts musulmans…",
            36.74985838811116,2.8545945882797237)

        return mutableListOf(r1, r2)
    }


    // 1st test
    @Test
    fun loadReminders_loadingData()= runBlockingTest {
        remindersListViewModel.loadReminders()
        assert(remindersListViewModel.remindersList.value?.size!! > 0)
    }

    // 2nd test
    @Test
    fun showLoadingTest() {
        // Given - repo with 2 reminders
        mainCoroutineRule.pauseDispatcher()

        // When - loading reminders
        remindersListViewModel.loadReminders()

        // Then - show loading
        assert(remindersListViewModel.showLoading.getOrAwaitValue() == true)

        // Then - hide loading
        mainCoroutineRule.resumeDispatcher()
        assert(remindersListViewModel.showLoading.getOrAwaitValue() == false)
    }

    //3rd test
    @Test
    fun invalidateShowNoData_noData_UpdateShowNoDataValue() = runBlockingTest {
        dataSource.deleteAllReminders()
        remindersListViewModel.loadReminders()
        assert(remindersListViewModel.showNoData.value!!)

    }

    //4st test
    @Test
    fun loadReminders_hasError_returnError(){
        // Giving - repo with 2 reminders
        dataSource.shouldReturnError = true
        // When -
        remindersListViewModel.loadReminders()
        // Then -
        val error = remindersListViewModel.showSnackBar.getOrAwaitValue()
        assert(error.contains("Exception"))
    }
}