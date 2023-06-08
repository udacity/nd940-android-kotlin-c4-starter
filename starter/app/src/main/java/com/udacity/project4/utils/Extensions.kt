package com.udacity.project4.utils

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.maps.model.LatLng
import com.udacity.project4.R
import com.udacity.project4.base.BaseRecyclerViewAdapter
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem

const val TAG = "Project4"

/**
 * Extension function to setup the RecyclerView.
 */
fun <T> RecyclerView.setup(
    adapter: BaseRecyclerViewAdapter<T>
) {
    this.apply {
        layoutManager = LinearLayoutManager(this.context)
        this.adapter = adapter
    }
}

fun Fragment.setTitle(title: String) {
    if (activity is AppCompatActivity) {
        (activity as AppCompatActivity).supportActionBar?.title = title
    }
}

fun Fragment.setDisplayHomeAsUpEnabled(bool: Boolean) {
    if (activity is AppCompatActivity) {
        (activity as AppCompatActivity).supportActionBar?.setDisplayHomeAsUpEnabled(
            bool
        )
    }
}

/**
 * Animate changing the view visibility.
 */
fun View.fadeIn() {
    this.visibility = View.VISIBLE
    this.alpha = 0f
    this.animate().alpha(1f).setListener(object : AnimatorListenerAdapter() {
        override fun onAnimationEnd(animation: Animator) {
            this@fadeIn.alpha = 1f
        }
    })
}

/**
 * Animate changing the view visibility.
 */
fun View.fadeOut() {
    this.animate().alpha(0f).setListener(object : AnimatorListenerAdapter() {
        override fun onAnimationEnd(animation: Animator) {
            this@fadeOut.alpha = 1f
            this@fadeOut.visibility = View.GONE
        }
    })
}

fun Context.toast(textId: Int) {
    Toast.makeText(this, textId, Toast.LENGTH_LONG).show()
}

fun Context.toast(text: String) {
    Toast.makeText(this, text, Toast.LENGTH_SHORT).show()
}

fun Context.permissionGranted(permission: String): Boolean {
    return ContextCompat.checkSelfPermission(
        this, permission
    ) == PackageManager.PERMISSION_GRANTED
}

fun FragmentActivity.requestTrackingPermissions(array: Array<String>, requestCode: Int) {
    ActivityCompat.requestPermissions(
        this,
        array,
        requestCode
    )
}

fun FragmentActivity.showRequestPermissionRationale(permission: String): Boolean =
    ActivityCompat.shouldShowRequestPermissionRationale(
        this,
        permission
    )

/**
 * Source:
 * https://stackoverflow.com/a/60757744/1354788
 */
fun Fragment.setNavigationResult(result: Any, key: String = "key") {
    this.findNavController().previousBackStackEntry?.savedStateHandle?.set(key, result)
}

fun Fragment.getNavigationResult(key: String = "key") =
    findNavController().currentBackStackEntry?.savedStateHandle?.getLiveData<Any>(key)

fun getFakeReminderItem() : ReminderDataItem {
    return ReminderDataItem(
        title = "some title",
        description = "some description",
        location = "some location",
        latitude = -34.0, // Sydney, Australia
        longitude = 151.0 // Sydney, Australia
    )
}

fun FragmentActivity.isGpsEnabled(): Boolean {
    Log.d(TAG, "Extensions.isGpsEnabled().")
    val manager = this.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    return manager.isProviderEnabled(LocationManager.GPS_PROVIDER)
}

fun FragmentActivity.permissionDeniedFeedback() {
    Log.d(TAG, "Extensions.permissionDeniedFeedback().")
    this.toast(R.string.allow_all_time_did_not_accepted)
}

data class LandmarkDataObject(val id: String, val latLong: LatLng)