package com.udacity.project4.locationreminders.reminderslist

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.udacity.project4.locationreminders.data.FakeDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.local.RemindersDao
import com.udacity.project4.locationreminders.data.local.RemindersDatabase
import com.udacity.project4.locationreminders.data.local.RemindersLocalRepository
import com.udacity.project4.locationreminders.getOrAwaitValue
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import org.junit.runner.RunWith
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.mockito.Mock

@RunWith(AndroidJUnit4::class)
@ExperimentalCoroutinesApi
@OptIn(ExperimentalCoroutinesApi::class)
class RemindersListViewModelTest {
    //(DONE)TODO: provide testing to the RemindersListViewModel and its live data objects
    var reminder = ReminderDTO("title", "description", "location", 5.5, 10.5)
    var list = mutableListOf(reminder)
    var list2 = mutableListOf(reminder, reminder, reminder)

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
                    single { FakeDataSource(list, isReturnErrors = false) }
                }
            )
        }
//        stopKoin()
//        reminder = ReminderDTO("title", "description", "location", 5.5, 10.5)
//        list = mutableListOf(reminder)
//        runBlocking {
//            fakeDataSource = FakeDataSource(list)
//            viewModel =
//                RemindersListViewModel(ApplicationProvider.getApplicationContext(), fakeDataSource)
//        }

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

    @Mock
    private lateinit var viewModel: RemindersListViewModel
    private lateinit var fakeDataSource: FakeDataSource

    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()


    @ExperimentalCoroutinesApi
    class MainCoroutineRule(private val dispatcher: TestDispatcher = UnconfinedTestDispatcher()) :
        TestWatcher() {

        override fun starting(description: Description?) {
            super.starting(description)
            Dispatchers.setMain(dispatcher)
        }

        override fun finished(description: Description?) {
            super.finished(description)
            Dispatchers.resetMain()
        }
    }

    @ExperimentalCoroutinesApi
    class notMain(private val dispatcher: TestDispatcher = UnconfinedTestDispatcher()) :
        TestWatcher() {

        override fun starting(description: Description?) {
            super.starting(description)
            Dispatchers.setMain(dispatcher)
        }

        override fun finished(description: Description?) {
            super.finished(description)
            Dispatchers.resetMain()
        }
    }

//@Before
//fun setCoroutine(){
//    Dispatchers.setMain(dispatcher = doubleIsDifferent())
//}

    @Test
    fun first_loadRemindersWithNoErrors() {
        runTest {
            fakeDataSource = FakeDataSource(list, false)
            viewModel =
                RemindersListViewModel(ApplicationProvider.getApplicationContext(), fakeDataSource)
            var oneItem = list[0]
            var dtoList = ReminderDataItem(
                title = oneItem.title,
                id = oneItem.id,
                description = oneItem.description,
                latitude = oneItem.latitude,
                location = oneItem.location,
                longitude = oneItem.longitude
            )
//pauseDispatcher()
            viewModel.loadReminders()
//resumeDispatcher()
            assertEquals(viewModel.remindersList.getOrAwaitValue(), arrayListOf(dtoList))

        }
    }

    @Test
    fun second_loadRemindersWithErrors() {
        runTest {
            fakeDataSource = FakeDataSource(list, true)
            viewModel =
                RemindersListViewModel(ApplicationProvider.getApplicationContext(), fakeDataSource)

            viewModel.loadReminders()

            assertEquals(viewModel.showSnackBar.getOrAwaitValue(), "Error")
        }
    }

    @Test
    fun third_loadRemindersAndCheckList() {
        runTest {
            fakeDataSource = FakeDataSource(list2, false)
            viewModel =
                RemindersListViewModel(ApplicationProvider.getApplicationContext(), fakeDataSource)

            viewModel.loadReminders()
            val res = viewModel.remindersList.getOrAwaitValue()

            assertEquals(res.size, list2.size)
        }
    }

    @Test
    fun fourth_loadRemindersAndCheckListWithErrors() {
        runTest {
            fakeDataSource = FakeDataSource(list2, true)
            viewModel =
                RemindersListViewModel(ApplicationProvider.getApplicationContext(), fakeDataSource)

            viewModel.loadReminders()

            val res = viewModel.showSnackBar.getOrAwaitValue()

            assertEquals(res, "Error")

        }
    }

    @Test
    fun fifth_loadRemindersAndCheckLoadingData() {
        runTest {
            fakeDataSource = FakeDataSource(list2, false)
            viewModel =
                RemindersListViewModel(ApplicationProvider.getApplicationContext(), fakeDataSource)

//            pauseDispatcher()
//            advanceUntilIdle()
//            runCurrent()
            viewModel.loadReminders()

            this.testScheduler.runCurrent()
//                advanceUntilIdle()
            assertEquals(viewModel.showLoading.getOrAwaitValue(), true)
//                    resumeDispatcher()
//                runCurrent()
            this.testScheduler.advanceUntilIdle()
            assertEquals(viewModel.showLoading.getOrAwaitValue(), false)

//                advanceUntilIdle()
//                runCurrent()

        }
    }


    @Test
    fun sixth_loadRemindersShowNoData() {
        runTest {
            fakeDataSource = FakeDataSource(mutableListOf<ReminderDTO>(), false)
            viewModel =
                RemindersListViewModel(ApplicationProvider.getApplicationContext(), fakeDataSource)

            viewModel.loadReminders()
            val res = viewModel.remindersList.getOrAwaitValue()
            advanceUntilIdle()
            assertEquals(res.size, 0)
            runCurrent()
        }
    }

    @After
    fun koinStop() = runBlocking {
        Dispatchers.resetMain()
        stopKoin()
    }
}