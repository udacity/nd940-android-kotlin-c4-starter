package com.udacity.project4.locationreminders.reminderslist

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.rules.activityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.udacity.project4.locationreminders.data.FakeDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result
import com.udacity.project4.locationreminders.getOrAwaitValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.MainCoroutineDispatcher
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.*
import org.hamcrest.MatcherAssert
import org.hamcrest.Matchers
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import org.junit.rules.TestWatcher
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import org.mockito.Mock
import org.mockito.Mockito
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
@ExperimentalCoroutinesApi
@OptIn(ExperimentalCoroutinesApi::class)
class RemindersListViewModelTest {
    //(DONE)TODO: provide testing to the RemindersListViewModel and its live data objects
    var reminder = ReminderDTO("title", "description", "location", 5.5, 10.5)
    var list = mutableListOf(reminder)

    @Mock
    private lateinit var viewModel: RemindersListViewModel
    private lateinit var fakeDataSource: FakeDataSource


    // Executes each task synchronously using Architecture Components.
    @get: Rule
    var instantTaskExecutor = InstantTaskExecutorRule()

    @ExperimentalCoroutinesApi
    class MainDispatcherRule(
        val testDispatcher: TestDispatcher = StandardTestDispatcher()
    ) : TestWatcher() {
        override fun starting(description: org.junit.runner.Description) {
            Dispatchers.setMain(testDispatcher)
        }

        override fun finished(description: org.junit.runner.Description) {
            Dispatchers.resetMain()
        }
    }
    @ExperimentalCoroutinesApi
    @get: Rule
    var mainDispatcherRule = MainDispatcherRule()

    @Before
    fun startKoinForTestAndInitRepository() {
//        stopKoin()
//        reminder = ReminderDTO("title", "description", "location", 5.5, 10.5)
//        list = mutableListOf(reminder)
//        runBlocking {
//            fakeDataSource = FakeDataSource(list)
//            viewModel =
//                RemindersListViewModel(ApplicationProvider.getApplicationContext(), fakeDataSource)
//        }
        Dispatchers.setMain(mainDispatcherRule.testDispatcher)
        fakeDataSource = FakeDataSource(list, false)
        viewModel =
            RemindersListViewModel(ApplicationProvider.getApplicationContext(), fakeDataSource)
//
//        startKoin {
//            androidContext(ApplicationProvider.getApplicationContext())
//            modules(
//                module {
//                    viewModel {
//                        RemindersListViewModel(
//                            ApplicationProvider.getApplicationContext(),
//                            get()
//                        )
//                    }
//                    single {
//                        SaveReminderViewModel(
//                            ApplicationProvider.getApplicationContext(),
//                            get()
//                        )
//                    }
//                    single {
//                        Room.inMemoryDatabaseBuilder(
//                            ApplicationProvider.getApplicationContext(),
//                            RemindersDatabase::class.java
//                        ).allowMainThreadQueries()
//                            .build() as RemindersDao
//                    }
//                    single { RemindersLocalRepository(get()) }
//                    single { FakeDataSource(list) }
//                }
//            )
//        }
    }

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    // Other code…

    @get:Rule
    val testInstantTaskExecutorRule: TestRule = InstantTaskExecutorRule()

    @Test
    fun first_testFakeDataBase() {
//        mainDispatcherRule.runCatching {
//        }
//        testInstantTaskExecutorRule.runCatching {
//        }
        runTest {
            mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

            val result = viewModel.loadReminders()

            assertTrue( viewModel.showLoading.getOrAwaitValue())
            MatcherAssert.assertThat(viewModel.remindersList.getOrAwaitValue(), Matchers.`is`(Result.Success<List<ReminderDTO>>(list)))
        }
    }

    @Test
    fun getReminders() {

        viewModel.loadReminders()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()
        val value = viewModel.remindersList.getOrAwaitValue()
        val showLoading = viewModel.remindersList.getOrAwaitValue()

        assertEquals(value, Result.Success<List<ReminderDTO>>(list))
    }

    @Test
    fun second_testFakeDataBaseShouldReturnError() {
        runTest {
            fakeDataSource.setIsReturnError(true)

            System.out.println("TAG DEBUGGING" + fakeDataSource.getReminders().toString())
            System.out.println("TAG DEBUGGING" + list.toString())

            MatcherAssert.assertThat(fakeDataSource.getReminders(), Matchers.`is`(Result.Error("Error")))
        }
    }

    @Test
    fun getReminders_invalidateShowNoData() {
//   val res = viewModel.invalidateShowNoData()
//        MatcherAssert.assertThat(res, Matchers.`is`(mutableListOf(list)))
    }

    @Test
    fun getReminders_invalidateData() {
        Dispatchers.setMain(StandardTestDispatcher())
        runTest {
            //Given
//            val fake = FakeDataSource(null)

            //When
//            var listOfRetrievedReminders = fake.getReminders()

            //then
//            MatcherAssert.assertThat(listOfRetrievedReminders, Matchers.`is`(Result.Error("Error")))
        }
    }

    @After
    fun DataBaseStop() = runBlocking {
        Dispatchers.resetMain()
        stopKoin()
    }
}