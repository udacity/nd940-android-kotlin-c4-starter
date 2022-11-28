package com.udacity.project4.locationreminders.reminderslist

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.RootMatchers
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.rule.GrantPermissionRule
import com.udacity.project4.R
import com.udacity.project4.authentication.AuthenticationActivity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.Matchers
import org.hamcrest.TypeSafeMatcher
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify

@RunWith(AndroidJUnit4::class)
@ExperimentalCoroutinesApi
//UI Testing
@MediumTest
class ReminderListFragmentTest {

    @Rule
    @JvmField
    var mActivityScenarioRule = ActivityScenarioRule(AuthenticationActivity::class.java)

    @Rule
    @JvmField
    var mGrantPermissionRule =
        GrantPermissionRule.grant(
            "android.permission.ACCESS_FINE_LOCATION",
            "android.permission.ACCESS_COARSE_LOCATION",
            "android.permission.ACCESS_BACKGROUND_LOCATION"
        )

    @Test
    fun remindersTest() {
        val floatingActionButton = onView(
            Matchers.allOf(
                withId(R.id.addReminderFAB),
                childAtPosition(
                    childAtPosition(
                        withId(R.id.refreshLayout),
                        0
                    ),
                    3
                ),
                ViewMatchers.isDisplayed()
            )
        )
        floatingActionButton.perform(click())

        val appCompatEditText = onView(
            Matchers.allOf(
                withId(R.id.reminderTitle),
                childAtPosition(
                    childAtPosition(
                        withId(R.id.nav_host_fragment),
                        0
                    ),
                    0
                ),
                ViewMatchers.isDisplayed()
            )
        )
        appCompatEditText.perform(
            ViewActions.replaceText("any hbl title"),
            ViewActions.closeSoftKeyboard()
        )

        val appCompatEditText2 = onView(
            Matchers.allOf(
                withId(R.id.reminderTitle), ViewMatchers.withText("any hbl title"),
                childAtPosition(
                    childAtPosition(
                        withId(R.id.nav_host_fragment),
                        0
                    ),
                    0
                ),
                ViewMatchers.isDisplayed()
            )
        )
        appCompatEditText2.perform(click())

        val appCompatEditText3 = onView(
            Matchers.allOf(
                withId(R.id.reminderDescription),
                childAtPosition(
                    childAtPosition(
                        withId(R.id.nav_host_fragment),
                        0
                    ),
                    1
                ),
                ViewMatchers.isDisplayed()
            )
        )
        appCompatEditText3.perform(
            ViewActions.replaceText("any hb? description"),
            ViewActions.closeSoftKeyboard()
        )

        val appCompatTextView = onView(
            Matchers.allOf(
                withId(R.id.selectLocation), ViewMatchers.withText("Reminder Location"),
                childAtPosition(
                    childAtPosition(
                        withId(R.id.nav_host_fragment),
                        0
                    ),
                    2
                ),
                ViewMatchers.isDisplayed()
            )
        )
        appCompatTextView.perform(click())

        val appCompatButton = onView(
            Matchers.allOf(
                withId(R.id.map_button), ViewMatchers.withText("Confirm"),
                childAtPosition(
                    childAtPosition(
                        withId(R.id.nav_host_fragment),
                        0
                    ),
                    1
                ),
                ViewMatchers.isDisplayed()
            )
        )
        appCompatButton.perform(click())

        val appCompatTextView2 = onView(
            Matchers.allOf(
                withId(R.id.selectLocation), ViewMatchers.withText("Reminder Location"),
                childAtPosition(
                    childAtPosition(
                        withId(R.id.nav_host_fragment),
                        0
                    ),
                    2
                ),
                ViewMatchers.isDisplayed()
            )
        )
        appCompatTextView2.perform(click())

        val view = onView(
            Matchers.allOf(
                ViewMatchers.withContentDescription("Google Map"),
                ViewMatchers.withParent(ViewMatchers.withParent(withId(R.id.map))),
                ViewMatchers.isDisplayed()
            )
        )
        view.check(ViewAssertions.matches(ViewMatchers.isDisplayed()))

        val appCompatButton2 = onView(
            Matchers.allOf(
                withId(R.id.map_button), ViewMatchers.withText("Confirm"),
                childAtPosition(
                    childAtPosition(
                        withId(R.id.nav_host_fragment),
                        0
                    ),
                    1
                ),
                ViewMatchers.isDisplayed()
            )
        )
        appCompatButton2.perform(click())

        val floatingActionButton2 = onView(
            Matchers.allOf(
                withId(R.id.saveReminder),
                childAtPosition(
                    childAtPosition(
                        withId(R.id.nav_host_fragment),
                        0
                    ),
                    4
                ),
                ViewMatchers.isDisplayed()
            )
        )
        floatingActionButton2.perform(click())

        val floatingActionButton3 = onView(
            Matchers.allOf(
                withId(R.id.saveReminder),
                childAtPosition(
                    childAtPosition(
                        withId(R.id.nav_host_fragment),
                        0
                    ),
                    4
                ),
                ViewMatchers.isDisplayed()
            )
        )
        floatingActionButton3.perform(click())

        val textView = onView(
            Matchers.allOf(
                withId(R.id.title), ViewMatchers.withText("any hbl title"),
                ViewMatchers.withParent(ViewMatchers.withParent(withId(R.id.reminderCardView))),
                ViewMatchers.isDisplayed()
            )
        )
        textView.check(ViewAssertions.matches(ViewMatchers.withText("any hbl title")))
    }

    private fun getCurrentActivity(): Activity? {
        val activity = arrayOfNulls<Activity>(1)
        onView(ViewMatchers.isRoot()).check { view, noViewFoundException ->
            activity[0] = view.context as Activity
        }
        return activity[0]
    }
    @Test
    fun checkOnAddingRemindersValidation() {
        val floatingActionButton = onView(
            Matchers.allOf(
                withId(R.id.addReminderFAB),
                childAtPosition(
                    childAtPosition(
                        withId(R.id.refreshLayout),
                        0
                    ),
                    3
                ),
                ViewMatchers.isDisplayed()
            )
        )
        floatingActionButton.perform(click())

        val appCompatEditText = onView(
            Matchers.allOf(
                withId(R.id.reminderDescription),
                childAtPosition(
                    childAtPosition(
                        withId(R.id.nav_host_fragment),
                        0
                    ),
                    1
                ),
                ViewMatchers.isDisplayed()
            )
        )
        appCompatEditText.perform(
            ViewActions.replaceText("lets edit description and for "),
            ViewActions.closeSoftKeyboard()
        )

        Espresso.pressBack()

        val appCompatEditText2 = onView(
            Matchers.allOf(
                withId(R.id.reminderDescription),
                ViewMatchers.withText("lets edit description and for "),
                childAtPosition(
                    childAtPosition(
                        withId(R.id.nav_host_fragment),
                        0
                    ),
                    1
                ),
                ViewMatchers.isDisplayed()
            )
        )
        appCompatEditText2.perform(ViewActions.replaceText("lets edit description and forget titl "))

        val appCompatEditText3 = onView(
            Matchers.allOf(
                withId(R.id.reminderDescription),
                ViewMatchers.withText("lets edit description and forget titl "),
                childAtPosition(
                    childAtPosition(
                        withId(R.id.nav_host_fragment),
                        0
                    ),
                    1
                ),
                ViewMatchers.isDisplayed()
            )
        )
        appCompatEditText3.perform(ViewActions.closeSoftKeyboard())

        val floatingActionButton2 = onView(
            Matchers.allOf(
                withId(R.id.saveReminder),
                childAtPosition(
                    childAtPosition(
                        withId(R.id.nav_host_fragment),
                        0
                    ),
                    4
                ),
                ViewMatchers.isDisplayed()
            )
        )
        floatingActionButton2.perform(click())

        val floatingActionButton3 = onView(
            Matchers.allOf(
                withId(R.id.saveReminder),
                childAtPosition(
                    childAtPosition(
                        withId(R.id.nav_host_fragment),
                        0
                    ),
                    4
                ),
                ViewMatchers.isDisplayed()
            )
        )
        floatingActionButton3.perform(click())
        onView(ViewMatchers.withText(R.string.add_title)).inRoot(
            RootMatchers.withDecorView(
                Matchers.not(
                    Matchers.`is`(
                        getCurrentActivity()?.getWindow()?.getDecorView()
                    )
                )
            )
        ).check(
            ViewAssertions.matches(
                ViewMatchers.isDisplayed()
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