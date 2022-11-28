package com.udacity.project4.authentication


import android.view.View
import android.view.ViewGroup
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.*
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.Matchers.`is`
import org.hamcrest.Matchers.allOf
import org.hamcrest.TypeSafeMatcher
import org.hamcrest.core.IsInstanceOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class AuthenticationActivityTest {

    @Rule
    @JvmField
    var mActivityScenarioRule = ActivityScenarioRule(AuthenticationActivity::class.java)

    @Test
    fun authenticationActivityTest() {
        val view = onView(
            allOf(
                withId(android.R.id.statusBarBackground),
                withParent(IsInstanceOf.instanceOf(android.widget.FrameLayout::class.java)),
                isDisplayed()
            )
        )
        view.check(matches(isDisplayed()))

        val materialButton = onView(
            allOf(
                withId(com.firebase.ui.auth.R.id.email_button), withText("Sign in with email"),
                childAtPosition(
                    allOf(
                        withId(com.firebase.ui.auth.R.id.btn_holder),
                        childAtPosition(
                            withId(com.google.android.material.R.id.container),
                            0
                        )
                    ),
                    0
                )
            )
        )
        materialButton.perform(scrollTo(), click())

        val textInputEditText = onView(
            allOf(
                withId(com.firebase.ui.auth.R.id.email),
                childAtPosition(
                    childAtPosition(
                        withId(com.firebase.ui.auth.R.id.email_layout),
                        0
                    ),
                    0
                )
            )
        )
        textInputEditText.perform(scrollTo(), replaceText("a@a.com"), closeSoftKeyboard())

        val materialButton2 = onView(
            allOf(
                withId(com.firebase.ui.auth.R.id.button_next), withText("Next"),
                childAtPosition(
                    childAtPosition(
                        withClassName(`is`("android.widget.ScrollView")),
                        0
                    ),
                    2
                )
            )
        )
        materialButton2.perform(scrollTo(), click())

        val textInputEditText2 = onView(
            allOf(
                withId(com.firebase.ui.auth.R.id.name),
                childAtPosition(
                    childAtPosition(
                        withId(com.firebase.ui.auth.R.id.name_layout),
                        0
                    ),
                    0
                )
            )
        )
        textInputEditText2.perform(scrollTo(), click())

        val textInputEditText3 = onView(
            allOf(
                withId(com.firebase.ui.auth.R.id.name),
                childAtPosition(
                    childAtPosition(
                        withId(com.firebase.ui.auth.R.id.name_layout),
                        0
                    ),
                    0
                )
            )
        )
        textInputEditText3.perform(scrollTo(), click())

        val textInputEditText4 = onView(
            allOf(
                withId(com.firebase.ui.auth.R.id.name),
                childAtPosition(
                    childAtPosition(
                        withId(com.firebase.ui.auth.R.id.name_layout),
                        0
                    ),
                    0
                )
            )
        )
        textInputEditText4.perform(scrollTo(), replaceText("12"), closeSoftKeyboard())

        val textInputEditText5 = onView(
            allOf(
                withId(com.firebase.ui.auth.R.id.password),
                childAtPosition(
                    childAtPosition(
                        withId(com.firebase.ui.auth.R.id.password_layout),
                        0
                    ),
                    0
                )
            )
        )
        textInputEditText5.perform(scrollTo(), click())

        val textInputEditText6 = onView(
            allOf(
                withId(com.firebase.ui.auth.R.id.password),
                childAtPosition(
                    childAtPosition(
                        withId(com.firebase.ui.auth.R.id.password_layout),
                        0
                    ),
                    0
                )
            )
        )
        textInputEditText6.perform(scrollTo(), click())

        val textInputEditText7 = onView(
            allOf(
                withId(com.firebase.ui.auth.R.id.password),
                childAtPosition(
                    childAtPosition(
                        withId(com.firebase.ui.auth.R.id.password_layout),
                        0
                    ),
                    0
                )
            )
        )
        textInputEditText7.perform(scrollTo(), replaceText("123456"), closeSoftKeyboard())

        val checkableImageButton = onView(
            allOf(
                withId(com.google.android.material.R.id.text_input_end_icon),
                withContentDescription("Show password"),
                childAtPosition(
                    childAtPosition(
                        withClassName(`is`("com.google.android.material.textfield.EndCompoundLayout")),
                        1
                    ),
                    0
                ),
                isDisplayed()
            )
        )
        checkableImageButton.perform(click())

        val materialButton3 = onView(
            allOf(
                withId(com.firebase.ui.auth.R.id.button_create), withText("Save"),
                childAtPosition(
                    childAtPosition(
                        withClassName(`is`("android.widget.ScrollView")),
                        0
                    ),
                    3
                )
            )
        )
        materialButton3.perform(scrollTo(), click())
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
