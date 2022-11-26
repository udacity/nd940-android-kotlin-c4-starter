package com.udacity.project4.locationreminders.reminderslist

import android.os.Bundle
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.lifecycle.ViewModel
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.udacity.project4.R
import com.udacity.project4.locationreminders.data.FakeDataSource
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.local.LocalDB
import com.udacity.project4.locationreminders.data.local.RemindersDao
import com.udacity.project4.locationreminders.data.local.RemindersDatabase
import com.udacity.project4.locationreminders.data.local.RemindersLocalRepository
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.*
import org.hamcrest.MatcherAssert
import org.hamcrest.Matchers
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.RunWith
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.junit.runner.Description
import org.koin.test.inject
import org.mockito.ArgumentMatchers.matches

import javax.inject.Inject

@RunWith(AndroidJUnit4::class)
@ExperimentalCoroutinesApi
class RemindersListViewModelTest {

    //TODO: provide testing to the RemindersListViewModel and its live data objects

    //    test the navigation of the fragments.
//    test the displayed data on the UI.
//    add testing for the error messages.
//    private val reminderDataSource: ReminderDataSource by Inject()
    val reminder = ReminderDTO("title", "description", "location", 5.5, 10.5)
    val list = mutableListOf(reminder)

    // provide testing to the RemindersListViewModel and its live data objects

    // Subject under test
    private lateinit var viewModel: RemindersListViewModel

    // Use a fake repository to be injected into the viewmodel
    private lateinit var repository: FakeDataSource


    // Executes each task synchronously using Architecture Components.
    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()
    @ExperimentalCoroutinesApi
    class MainDispatcherRule(
        val testDispatcher: TestDispatcher = UnconfinedTestDispatcher()
    ) : TestWatcher() {

        override fun starting(description: org.junit.runner.Description) {
            Dispatchers.setMain(testDispatcher)
        }

        override fun finished(description: org.junit.runner.Description) {
            Dispatchers.resetMain()
        }
    }

    @Before
    fun setUp() {
        stopKoin()
        startKoin {
            modules(modules = module {
                viewModel {
                    RemindersListViewModel(ApplicationProvider.getApplicationContext(), get()) }
                single { RemindersLocalRepository(get()) } //
                single { LocalDB.createRemindersDao(ApplicationProvider.getApplicationContext()) }
                single {
                    SaveReminderViewModel(ApplicationProvider.getApplicationContext(), get())
                }
                single {
                    Room.inMemoryDatabaseBuilder(
                        ApplicationProvider.getApplicationContext(),
                        RemindersDatabase::class.java
                    ).allowMainThreadQueries()
                        .build() as RemindersDao
                }
                single { RemindersLocalRepository(get()) }
                single { FakeDataSource(list) }
            })

        }
    }

    @Before
    fun startKoinForTestAndInitRepository() {
        stopKoin()
        startKoin {
            androidContext(ApplicationProvider.getApplicationContext())
            modules(
                module {
                    viewModel {
                        RemindersListViewModel(
                            ApplicationProvider.getApplicationContext(),
                            get()
                        )
                    }
                    single {
                        SaveReminderViewModel(
                            ApplicationProvider.getApplicationContext(),
                            get()
                        )
                    }
                    single {
                        Room.inMemoryDatabaseBuilder(
                            ApplicationProvider.getApplicationContext(),
                            RemindersDatabase::class.java
                        ).allowMainThreadQueries()
                            .build() as RemindersDao
                    }
                    single { RemindersLocalRepository(get()) }
                    single { FakeDataSource(list) }
                }
            )
        }
    }
    @Test
    fun getReminders_success() {
//        // Pause dispatcher so you can verify initial values.
//        mainCoroutineRule.pauseDispatcher()

        repository.setShouldFail(false)
        viewModel.loadReminders()

        // Then assert that the progress indicator is shown.
        MatcherAssert.assertThat(viewModel.showLoading.value, Matchers.`is`(true))

        // Execute pending coroutines actions.
        mainCoroutineRule.resumeDispatcher()

        // Then assert that the progress indicator is hidden.
        MatcherAssert.assertThat(viewModel.showLoading.value, Matchers.`is`(false))

        // Then assert that an error message is not shown.
        MatcherAssert.assertThat(viewModel.showSnackBar.value.isNullOrEmpty(), Matchers.`is`(true))
    }

    @Test
    fun getReminders_failure() {
        // Pause dispatcher so you can verify initial values.
        mainCoroutineRule.pauseDispatcher()

        repository.setShouldFail(true)
        viewModel.loadReminders()

        // Then assert that the progress indicator is shown.
        MatcherAssert.assertThat(viewModel.showLoading.value, Matchers.`is`(true))

        // Execute pending coroutines actions.
        mainCoroutineRule.resumeDispatcher()

        // Then assert that the progress indicator is hidden.
        MatcherAssert.assertThat(viewModel.showLoading.value, Matchers.`is`(false))

        // Then assert that the error message is shown.
        MatcherAssert.assertThat(viewModel.showSnackBar.value.isNullOrEmpty(), Matchers.`is`(false))
    }

    @After
    fun DataBaseStop() = runBlocking {
//        reminderDataSource.deleteAllReminders()
        stopKoin()
    }
}