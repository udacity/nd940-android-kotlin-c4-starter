package com.udacity.project4.locationreminders.geofence

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.ExistingWorkPolicy
import androidx.work.WorkManager
import com.google.gson.Gson
import com.udacity.project4.MyApp
import com.udacity.project4.utils.TAG

/**
 * Triggered by the Geofence. Since we can have many GeoFences at once, we pull the request
 * ID from the first Geofence, and locate it within the cached data in our Room DB.
 *
 * Or users can add the reminders and then close the app, so our app has to run in the background
 * and handle the geofencing in the background.
 * To do that you can use:
 * https://developer.android.com/reference/android/support/v4/app/JobIntentService to do that.
 */
/*
class GeofenceBroadcastReceiver : BroadcastReceiver() {

	override fun onReceive(context: Context, intent: Intent) {
		Log.d(TAG, "GeofenceBroadcastReceiver.onReceive().")
		if (intent.action == ACTION_GEOFENCE_EVENT) {
			Log.d(TAG, "GeofenceBroadcastReceiver.onReceive() -> [1]")
			val geofencingEvent = GeofencingEvent.fromIntent(intent)
			if (geofencingEvent != null) {
				Log.d(TAG, "GeofenceBroadcastReceiver.onReceive() -> [2]")
				if (geofencingEvent.hasError()) {
					Log.d(TAG, "GeofenceBroadcastReceiver.onReceive() -> [3]")
					val errorMessage = errorMessage(context, geofencingEvent.errorCode)
					Log.e(TAG, "GeofenceBroadcast().onReceive() -> $errorMessage")
					return
				} else {
					Log.d(TAG, "GeofenceBroadcastReceiver.onReceive() -> [4]")
					val extras = intent.extras
					var id = ""
					var lat = 0.0
					var lng = 0.0
					if (extras != null) {
						Log.d(TAG, "GeofenceBroadcastReceiver.onReceive() -> [5]")
						id = extras.getString(GEO_FENCE_ID).toString()
						lat = extras.getDouble(GEO_FENCE_LAT)
						lng = extras.getDouble(GEO_FENCE_LNG)
					} else {
						Log.d(TAG, "GeofenceBroadcastReceiver.onReceive() -> [6]")
					}
					Log.d(TAG, "GeofenceBroadcastReceiver.onReceive() -> [7]")
					callNotification(
						context,
						ReminderDataItem(
							title = id,
							description = id,
							location = id,
							latitude = lat,
							longitude = lng
						)
					)
				}
			} else {
				Log.d(TAG, "GeofenceBroadcastReceiver.onReceive() -> [8]")
			}
		} else {
			Log.d(TAG, "GeofenceBroadcastReceiver.onReceive() -> [9]")
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
 */

//
class GeofenceBroadcastReceiver : BroadcastReceiver() {
	override fun onReceive(context: Context, intent: Intent) {
		Log.d(TAG, "GeofenceBroadcastReceiver.onReceive().")
//		GeofenceTransitionsJobIntentService.enqueueWork(context, intent)
		(context.applicationContext as MyApp).broadcastIntent = intent
		val workManager = WorkManager.getInstance(context)
		val intentToBundle = Gson().toJson(intent)
		val exampleWorkRequest = GeofenceTransitionsWorkManager.buildWorkRequest(intentToBundle)
		workManager.enqueueUniqueWork(
			UNIQUE_WORK_NAME,
			ExistingWorkPolicy.KEEP,
			exampleWorkRequest
		)
	}
}
 //