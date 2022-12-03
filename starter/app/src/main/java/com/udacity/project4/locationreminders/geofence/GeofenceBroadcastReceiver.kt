package com.udacity.project4.locationreminders.geofence

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.JobIntentService.enqueueWork
import androidx.core.content.ContextCompat
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofenceStatusCodes
import com.google.android.gms.location.GeofencingEvent
import com.udacity.project4.locationreminders.geofence.GeofenceTransitionsJobIntentService.Companion.ACTION_GEOFENCE_EVENT
import com.udacity.project4.utils.sendNotification
import org.koin.android.ext.koin.ERROR_MSG

/**
 * Triggered by the Geofence.  Since we can have many Geofences at once, we pull the request
 * ID from the first Geofence, and locate it within the cached data in our Room DB
 *
 * Or users can add the reminders and then close the app, So our app has to run in the background
 * and handle the geofencing in the background.
 * To do that you can use https://developer.android.com/reference/android/support/v4/app/JobIntentService to do that.
 *
 */

class GeofenceBroadcastReceiver : BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent) {
            Log.v("GEOFENCE","RECIEVED")
            Log.v("GEOFENCE",intent.action.toString())

            if (intent.action == ACTION_GEOFENCE_EVENT) {
            val geofencingEvent = GeofencingEvent.fromIntent(intent)
                Log.v("GEOFENCE","GEOFENCE ")
                if (geofencingEvent!!.hasError()) {
                    val errorMessage = GeofenceStatusCodes
                        .getStatusCodeString(geofencingEvent.errorCode)
                    Log.v("GEOFENCING ERROR", errorMessage.toString())
                    return
                }

            // Get the transition type.
            val geofenceTransition = geofencingEvent.geofenceTransition
                Log.v("GEOFENCE",geofenceTransition.toString())
            // Test that the reported transition was of interest.
            if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER ||
            geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT) {
                Log.v("GEOFENCE","ENQUED")
                GeofenceTransitionsJobIntentService.enqueueWork(context,intent)
            }
        }
    }
}
