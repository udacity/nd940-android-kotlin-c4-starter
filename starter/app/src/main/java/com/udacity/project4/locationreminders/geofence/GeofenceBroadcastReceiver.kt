package com.udacity.project4.locationreminders.geofence

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import com.udacity.project4.MyApp
import com.udacity.project4.R
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import com.udacity.project4.locationreminders.savereminder.SaveReminderFragment.Companion.ACTION_GEOFENCE_EVENT
import com.udacity.project4.locationreminders.savereminder.SaveReminderFragment.Companion.GEO_FENCE_ID
import com.udacity.project4.locationreminders.savereminder.SaveReminderFragment.Companion.GEO_FENCE_LAT
import com.udacity.project4.locationreminders.savereminder.SaveReminderFragment.Companion.GEO_FENCE_LNG
import com.udacity.project4.utils.TAG
import com.udacity.project4.utils.sendNotification

/**
 * Triggered by the Geofence. Since we can have many GeoFences at once, we pull the request
 * ID from the first Geofence, and locate it within the cached data in our Room DB.
 *
 * Or users can add the reminders and then close the app, so our app has to run in the background
 * and handle the geofencing in the background.
 * To do that you can use:
 * https://developer.android.com/reference/android/support/v4/app/JobIntentService to do that.
 */
class GeofenceBroadcastReceiver : BroadcastReceiver() {

	override fun onReceive(context: Context, intent: Intent) {
		Log.d(TAG, "GeofenceBroadcastReceiver.onReceive().")
		if (intent.action == ACTION_GEOFENCE_EVENT) {
			val extras = intent.extras
			var id = ""
			var lat = 0.0
			var lng = 0.0
			if (extras != null) {
				id = extras.getString(GEO_FENCE_ID).toString()
				lat = extras.getDouble(GEO_FENCE_LAT)
				lng = extras.getDouble(GEO_FENCE_LNG)
			}
			callNotification(context,
				ReminderDataItem(
					title = id,
					description = id,
					location = id,
					latitude = lat,
					longitude = lng
				)
			)
		}
	}

	private fun callNotification(context: Context, reminderDataItem: ReminderDataItem) {
		val hasPermission = (context.applicationContext as MyApp).hasNotificationPermission
		if (hasPermission) {
			sendNotification(context, reminderDataItem)
		} else {
			val message = context.getString(R.string.no_notification_permission)
			Toast.makeText(context, message, Toast.LENGTH_LONG).show()
		}
	}
}