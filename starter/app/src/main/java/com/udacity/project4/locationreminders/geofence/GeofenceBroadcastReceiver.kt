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
class GeofenceBroadcastReceiver : BroadcastReceiver() {
	override fun onReceive(context: Context, intent: Intent) {
		Log.d(TAG, "GeofenceBroadcastReceiver.onReceive().")
		(context.applicationContext as MyApp).broadcastIntent = intent
		val workManager = WorkManager.getInstance(context)
		val intentToBundle = Gson().toJson(intent)
		val workRequest = GeofenceTransitionsWorkManager.buildWorkRequest(intentToBundle)
		workManager.enqueueUniqueWork(
			UNIQUE_WORK_NAME,
			ExistingWorkPolicy.KEEP,
			workRequest
		)
	}
}