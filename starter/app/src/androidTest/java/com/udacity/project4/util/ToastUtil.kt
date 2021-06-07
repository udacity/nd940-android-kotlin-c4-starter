package com.udacity.project4.util

import android.os.IBinder
import android.view.WindowManager
import androidx.test.espresso.Root
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.TypeSafeMatcher

class ToastUtil: TypeSafeMatcher<Root?>() {
    override fun describeTo(description: Description) {
        description.appendText("is toast")
    }

    override fun matchesSafely(item: Root?): Boolean {
        val type: Int? = item?.windowLayoutParams?.get()?.type
        if (type == WindowManager.LayoutParams.TYPE_TOAST) {
            val windowToken: IBinder? = item.decorView?.windowToken
            val appToken: IBinder? = item.decorView?.applicationWindowToken
            if (windowToken === appToken) {
                return true
            }
        }
        return false
    }
}

fun isToast(): Matcher<Root?> {
    return ToastUtil()
}