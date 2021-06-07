package com.udacity.project4

import android.app.Application
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.udacity.project4.locationreminders.RemindersActivity
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.local.LocalDB
import com.udacity.project4.locationreminders.data.local.RemindersLocalRepository
import com.udacity.project4.locationreminders.reminderslist.RemindersListViewModel
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.AutoCloseKoinTest
import org.koin.test.get
import androidx.test.espresso.assertion.ViewAssertions.matches
import com.udacity.project4.util.DataBindingIdlingResource
import com.udacity.project4.util.monitorActivity

@RunWith(AndroidJUnit4::class)
@LargeTest
//END TO END test to black box test the app
class RemindersActivityTest :
    AutoCloseKoinTest() {// Extended Koin Test - embed autoclose @after method to close Koin after every test

    private lateinit var repository: ReminderDataSource
    private lateinit var appContext: Application
    // An idling resource that waits for Data Binding to have no pending bindings.
    private val dataBindingIdlingResource = DataBindingIdlingResource()
    /**
     * As we use Koin as a Service Locator Library to develop our code, we'll also use Koin to test our code.
     * at this step we will initialize Koin related code to be able to use it in out testing.
     */
    @Before
    fun init() {
        stopKoin()//stop the original app koin
        appContext = getApplicationContext()
        val myModule = module {
            viewModel {
                RemindersListViewModel(
                    appContext,
                    get() as ReminderDataSource
                )
            }
            single {
                SaveReminderViewModel(
                    appContext,
                    get() as ReminderDataSource
                )
            }
            single { RemindersLocalRepository(get()) as ReminderDataSource }
            single { LocalDB.createRemindersDao(appContext) }
        }
        //declare a new koin module
        startKoin {
            modules(listOf(myModule))
        }
        //Get our real repository
        repository = get()

        //clear the data to start fresh
        runBlocking {
            repository.deleteAllReminders()
        }
    }


//    TODO: add End to End testing to the app
@Test
fun addReminder_showReminder() = runBlocking {

    // Set initial state
    val reminder = ReminderDTO("Call a friend","calling ...",
        "Parc de La Victoire",
        36.74208242672705,3.072958588600159)

    repository.saveReminder(reminder)

    //Start up Tasks screen
    val activityScenario = ActivityScenario.launch(RemindersActivity::class.java)
    dataBindingIdlingResource.monitorActivity(activityScenario)

    onView(withText(reminder.title)).check(matches(isDisplayed()))
    onView(withText(reminder.location)).check(matches(isDisplayed()))
    onView(withText(reminder.description)).check(matches(isDisplayed()))

    onView(withId(R.id.addReminderFAB)).perform(click())
    onView(withId(R.id.reminderTitle)).perform(replaceText("NEW TITLE"))
    onView(withId(R.id.reminderDescription)).perform(replaceText("NEW DESCRIPTION"))

    onView(withId(R.id.selectLocation)).perform(click())

    onView(withId(R.id.map)).check(matches(isDisplayed()))
    onView(withId(R.id.map)).perform(click())

    onView(withId(R.id.save)).perform(click())

    onView(withId(R.id.saveReminder)).check(matches(isDisplayed()))
    onView(withId(R.id.saveReminder)).perform(click())

    onView(withText("NEW TITLE")).check(matches(isDisplayed()))
    onView(withText("NEW DESCRIPTION")).check(matches(isDisplayed()))

    // Make sure the activity is closed before the db
    activityScenario.close()

}
}
