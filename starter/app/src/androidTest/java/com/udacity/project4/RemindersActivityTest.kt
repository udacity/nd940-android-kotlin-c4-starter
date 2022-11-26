package com.udacity.project4

import android.app.Application
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.core.app.launchActivity
import androidx.test.espresso.Espresso
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.udacity.project4.locationreminders.RemindersActivity
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.local.LocalDB
import com.udacity.project4.locationreminders.data.local.RemindersLocalRepository
import com.udacity.project4.locationreminders.reminderslist.RemindersListViewModel
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.util.DataBindingIdlingResource
import com.udacity.project4.util.monitorActivity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.AutoCloseKoinTest
import org.koin.test.get

@RunWith(AndroidJUnit4::class)
@LargeTest
//END TO END test to black box test the app
class RemindersActivityTest :
    AutoCloseKoinTest() {
    // Extended Koin Test - embed autoclose @after method to close Koin after every test
//    @Before
//    fun setUp() {
//        stopKoin()
//        appContext = getApplicationContext()
//
//        startKoin {
//            modules(modules = module {
//                viewModel{
//                    RemindersListViewModel(appContext,get())
//                }
//                single { RemindersLocalRepository(get()) } //
//                single {  LocalDB.createRemindersDao(appContext) }
//                single { SaveReminderViewModel(appContext, get() ) }  //remidersource
//            })
//
//        }
//    }

    private lateinit var repository: ReminderDataSource
    private lateinit var appContext: Application

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
            single { RemindersLocalRepository(get()) }
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

    @After
    fun atTheEnd() {
        stopKoin()
    }

//  (DONE)  TODO: add End to End testing to the app

    @Test
    fun addReminder_withOutAddingLocation(){
        val remindersActivity = launchActivity<RemindersActivity>()
        DataBindingIdlingResource().monitorActivity(remindersActivity)

        Thread.sleep(1000)
        //tap the FAB
        Espresso.onView(ViewMatchers.withId(R.id.addReminderFAB)).perform(ViewActions.click())
        Thread.sleep(1000)

        //add title
        Espresso.onView(ViewMatchers.withId(R.id.reminderTitle))
            .perform(ViewActions.typeText("title"), ViewActions.closeSoftKeyboard())
        Thread.sleep(1000)

        //add description
        Espresso.onView(ViewMatchers.withId(R.id.description))
            .perform(ViewActions.typeText("description"), ViewActions.closeSoftKeyboard())
        Thread.sleep(1000)

        //add description
        Espresso.onView(ViewMatchers.withId(R.id.addReminderFAB))
            .perform(ViewActions.click(), ViewActions.closeSoftKeyboard())
        Thread.sleep(1000)

        //finish the testing
        remindersActivity.close()
    }

    @Test
    fun addReminder_withOutAddingTitle(){
        val remindersActivity = launchActivity<RemindersActivity>()
        DataBindingIdlingResource().monitorActivity(remindersActivity)
        Thread.sleep(1000)

        //tap the FAB
        Espresso.onView(ViewMatchers.withId(R.id.addReminderFAB)).perform(ViewActions.click())
        Thread.sleep(1000)

        //add description
        Espresso.onView(ViewMatchers.withId(R.id.description))
            .perform(ViewActions.typeText("description"), ViewActions.closeSoftKeyboard())
        Thread.sleep(1000)

        //add description
        Espresso.onView(ViewMatchers.withId(R.id.addReminderFAB))
            .perform(ViewActions.click(), ViewActions.closeSoftKeyboard())
        Thread.sleep(1000)

        //finish the testing
        remindersActivity.close()
    }
}
