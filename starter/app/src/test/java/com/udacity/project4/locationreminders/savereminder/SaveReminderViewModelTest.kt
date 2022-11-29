package com.udacity.project4.locationreminders.savereminder

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.udacity.project4.locationreminders.data.FakeDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.*
import org.hamcrest.MatcherAssert
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers
import org.hamcrest.Matchers.`is`
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import org.mockito.Mock

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
class SaveReminderViewModelTest {


    //(DONE)TODO: provide testing to the SaveReminderView and its live data objects
    var reminder = ReminderDTO("title", "description", "location", 5.5, 10.5)
    var list = mutableListOf(reminder)

    @Mock
    private lateinit var viewModel: SaveReminderViewModel
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

    @get: Rule
    var mainDispatcherRule = MainDispatcherRule()

    @Before
    fun startKoinForTestAndInitRepository() {
//        stopKoin()
        Dispatchers.setMain(mainDispatcherRule.testDispatcher)
        fakeDataSource = FakeDataSource(list, false)
        viewModel =
            SaveReminderViewModel(ApplicationProvider.getApplicationContext(), fakeDataSource)
    }

    @Test
    fun first_testFakeDataBase() {
        runTest {
            System.out.println("TAG DEBUGGING" + fakeDataSource.getReminders().toString())
            System.out.println("TAG DEBUGGING" + list.toString())

            MatcherAssert.assertThat(
                fakeDataSource.getReminders(),
                Matchers.`is`(Result.Success<List<ReminderDTO>>(list))
            )
        }
    }

    @Test
    fun second_testFakeDataBaseShouldReturnError() {
        runTest {
            fakeDataSource.setIsReturnError(true)

            System.out.println("TAG DEBUGGING" + fakeDataSource.getReminders().toString())
            System.out.println("TAG DEBUGGING" + list.toString())

            MatcherAssert.assertThat(
                fakeDataSource.getReminders(),
                Matchers.`is`(Result.Error("Error"))
            )
        }
    }

    @Test
    fun saveReminderValidate_shouldBeValid() {
//  Given
        val remindersData =
            ReminderDataItem("my title", "my descrioption", "location", 56.5, 549.85)

//      When
        val result = viewModel.validateEnteredData(remindersData)

//      Then
        assertThat(result, `is`(true))
    }

    @Test
    fun saveReminderValidate_shouldBeInValid() {
//  Given
        val remindersData = ReminderDataItem("", "", "location", 56.5, 549.85)

//      When
        val result = viewModel.validateEnteredData(remindersData)

//      Then
        assertThat(result, `is`(false))
    }


    @Test
    fun saveReminder_createAndReturnData() {
        runTest {
            //      Given
            val remindersData =  ReminderDataItem("title", "description", "location", 5.5, 10.5)
            viewModel.validateAndSaveReminder(remindersData)

            //      When
            val result = viewModel.dataSource.getReminders() as Result.Success

            //      Then
            val temp = Result.Success<List<ReminderDataItem>>(listOf(remindersData))
            assertThat(result.data.get(0).title, `is`(temp.data.get(0).title))
            assertThat(result.data.get(0).description, `is`(temp.data.get(0).description))
            assertThat(result.data.get(0).location, `is`(temp.data.get(0).location))
            assertThat(result.data.get(0).latitude, `is`(temp.data.get(0).latitude))
            assertThat(result.data.get(0).longitude, `is`(temp.data.get(0).longitude))
        }
    }

    @After
    fun DataBaseStop() = runBlocking {
        Dispatchers.resetMain()
        stopKoin()
    }
}
