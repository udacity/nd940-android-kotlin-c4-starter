package com.udacity.project4.locationreminders.reminderslist

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.udacity.project4.locationreminders.MainCoroutineRule
import com.udacity.project4.locationreminders.data.FakeDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.getOrAwaitValue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.core.IsNot
import org.junit.*
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin

@RunWith(AndroidJUnit4::class)
@ExperimentalCoroutinesApi
class RemindersListViewModelTest {

    private lateinit var remindersListViewModel: RemindersListViewModel
    private lateinit var remindersRepository: FakeDataSource


    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()
    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()

    @Before
    fun setUpViewModel()= runBlocking{
        remindersRepository = FakeDataSource()
        val reminder1 = ReminderDTO(title = "Title1", description = "description1", location = "location1", latitude = 0.0, longitude = 0.0)
        val reminder2 = ReminderDTO(title = "Title2", description = "description2", location = "location2", latitude = 0.0, longitude = 0.0)
        remindersRepository.saveReminder(reminder1)
        remindersRepository.saveReminder(reminder2)

        remindersListViewModel = RemindersListViewModel(ApplicationProvider.getApplicationContext(), remindersRepository)
    }

    @After
    fun reset(){
        stopKoin()
    }

    @Test
    fun loadReminder_showLoading()= runBlocking{
        // GIVEN fresh viewModel
        remindersListViewModel = RemindersListViewModel(ApplicationProvider.getApplicationContext(), remindersRepository)

        // WHEN loading reminders
        mainCoroutineRule.pauseDispatcher()
        remindersListViewModel.loadReminders()

        // Then show loading is true
        Assert.assertThat(remindersListViewModel.showLoading.getOrAwaitValue(), `is`(true))
    }

    @Test
    fun getRemindersList() {
        remindersListViewModel.loadReminders()
        Assert.assertThat(
            remindersListViewModel.remindersList.getOrAwaitValue(),
            (IsNot.not(emptyList()))
        )
        Assert.assertThat(
            remindersListViewModel.remindersList.getOrAwaitValue().size,
            `is`(2)
        )
    }

    @Test
    fun showError(){
        // GIVEN - A viewModel

        // WHEN - Loading reminder in empty repository
        remindersListViewModel = RemindersListViewModel(ApplicationProvider.getApplicationContext(), FakeDataSource())
        remindersListViewModel.loadReminders()

        // THEN - no reminder found
        Assert.assertThat(
            remindersListViewModel.showSnackBar.getOrAwaitValue(),
            `is`("no reminder found")
        )
    }

}