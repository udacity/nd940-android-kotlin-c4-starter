package com.udacity.project4.authentication


import android.app.Activity
import android.app.Application
import android.view.View
import android.view.ViewGroup
import androidx.room.Room
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.*
import androidx.test.espresso.matcher.RootMatchers.withDecorView
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.rule.GrantPermissionRule
import com.udacity.project4.R
import com.udacity.project4.locationreminders.RemindersActivity
import com.udacity.project4.locationreminders.data.local.LocalDB
import com.udacity.project4.locationreminders.data.local.RemindersDatabase
import com.udacity.project4.util.DataBindingIdlingResource
import com.udacity.project4.util.monitorActivity
import kotlinx.coroutines.runBlocking
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.Matchers.*
import org.hamcrest.TypeSafeMatcher
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module

@LargeTest
@RunWith(AndroidJUnit4::class)
class AuthenticationActivityTest2 {
    @Before
    fun init() {
        stopKoin()//stop the original app koin
        val appContext = ApplicationProvider.getApplicationContext<Application>()
        val myModule = module {
            single {
                Room.databaseBuilder(
                    appContext,
                    RemindersDatabase::class.java,
                    RemindersDatabase::class.java.simpleName
                )
                    .fallbackToDestructiveMigration().build()
            }
            single { get<LocalDB>().createRemindersDao(appContext) }
            single { LocalDB.createRemindersDao(appContext) }
        }

        //declare a new koin module
        startKoin {
//            androidContext(appContext)
            modules(listOf(myModule))
        }
        //Get our real repository
//        repository = get()

        //clear the data to start fresh
        runBlocking {
//            repository.deleteAllReminders()
        }
    }

    @Rule
    @JvmField
    var mActivityScenarioRule = ActivityScenarioRule(RemindersActivity::class.java)

    @Rule
    @JvmField
    var mGrantPermissionRule =
        GrantPermissionRule.grant(
            "android.permission.ACCESS_BACKGROUND_LOCATION"
        )

    private fun getCurrentActivity(): Activity? {
        val activity = arrayOfNulls<Activity>(1)
        onView(isRoot()).check { view, noViewFoundException ->
            activity[0] = view.context as Activity
        }
        return activity[0]
    }

    @Test
    fun checkOnAddingRemindersValidation() {
        Thread.sleep(1000)
        val scenario = ActivityScenario.launch(AuthenticationActivity::class.java)
        DataBindingIdlingResource().monitorActivity(scenario)
        Thread.sleep(1000)
        Thread.sleep(1000)
        val floatingActionButton = onView(
            allOf(
                withId(R.id.addReminderFAB),
                childAtPosition(
                    childAtPosition(
                        withId(R.id.refreshLayout),
                        0
                    ),
                    3
                ),
                isDisplayed()
            )
        )
        floatingActionButton.perform(click())
        Thread.sleep(1000)
        val appCompatEditText = onView(
            allOf(
                withId(R.id.reminderDescription),
                childAtPosition(
                    childAtPosition(
                        withId(R.id.nav_host_fragment),
                        0
                    ),
                    1
                ),
                isDisplayed()
            )
        )
        Thread.sleep(1000)
        appCompatEditText.perform(
            replaceText("lets edit description and for "),
            closeSoftKeyboard()
        )
        Thread.sleep(1000)
        pressBack()
        Thread.sleep(1000)
        val appCompatEditText2 = onView(
            allOf(
                withId(R.id.reminderDescription), withText("lets edit description and for "),
                childAtPosition(
                    childAtPosition(
                        withId(R.id.nav_host_fragment),
                        0
                    ),
                    1
                ),
                isDisplayed()
            )
        )
        appCompatEditText2.perform(replaceText("lets edit description and forget titl "))

        val appCompatEditText3 = onView(
            allOf(
                withId(R.id.reminderDescription),
                withText("lets edit description and forget titl "),
                childAtPosition(
                    childAtPosition(
                        withId(R.id.nav_host_fragment),
                        0
                    ),
                    1
                ),
                isDisplayed()
            )
        )
        appCompatEditText3.perform(closeSoftKeyboard())

        val floatingActionButton2 = onView(
            allOf(
                withId(R.id.saveReminder),
                childAtPosition(
                    childAtPosition(
                        withId(R.id.nav_host_fragment),
                        0
                    ),
                    4
                ),
                isDisplayed()
            )
        )
        floatingActionButton2.perform(click())

        val floatingActionButton3 = onView(
            allOf(
                withId(R.id.saveReminder),
                childAtPosition(
                    childAtPosition(
                        withId(R.id.nav_host_fragment),
                        0
                    ),
                    4
                ),
                isDisplayed()
            )
        )
        floatingActionButton3.perform(click())
        onView(withText(R.string.add_title)).inRoot(
            withDecorView(
                not(
                    `is`(
                        getCurrentActivity()?.window?.decorView
                    )
                )
            )
        ).check(
            matches(
                isDisplayed()
            )
        )
    }

    private fun childAtPosition(
        parentMatcher: Matcher<View>, position: Int
    ): Matcher<View> {

        return object : TypeSafeMatcher<View>() {
            override fun describeTo(description: Description) {
                description.appendText("Child at position $position in parent ")
                parentMatcher.describeTo(description)
            }

            public override fun matchesSafely(view: View): Boolean {
                val parent = view.parent
                return parent is ViewGroup && parentMatcher.matches(parent)
                        && view == parent.getChildAt(position)
            }
        }
    }
}
