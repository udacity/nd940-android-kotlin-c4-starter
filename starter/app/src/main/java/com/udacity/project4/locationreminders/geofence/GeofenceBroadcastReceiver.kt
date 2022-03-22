package com.udacity.project4.locationreminders.geofence

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofenceStatusCodes
import com.google.android.gms.location.GeofencingEvent
import com.udacity.project4.locationreminders.geofence.GeofenceTransitionsJobIntentService.Companion.enqueueWork
import com.udacity.project4.locationreminders.savereminder.ACTION_GEOFENCE_INTENT

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
    private val TAG = "GeofenceBroadcastR"
    override fun onReceive(context: Context, intent: Intent) {

       if (intent.action == ACTION_GEOFENCE_INTENT){
            val geofencingEvent = GeofencingEvent.fromIntent(intent)

            if (geofencingEvent.hasError())
            {
                val errorCode = geofencingEvent.errorCode
                val errorMessage = when(errorCode){
                    GeofenceStatusCodes.GEOFENCE_NOT_AVAILABLE -> "Geofence service is not available now"
                    GeofenceStatusCodes.GEOFENCE_TOO_MANY_GEOFENCES -> "Too many geofences"
                    GeofenceStatusCodes.GEOFENCE_TOO_MANY_PENDING_INTENTS -> "Too many pending intent"
                    else -> "Unknown error, Geofence service is not available now"

                }
                Log.e(TAG, errorMessage)
                return
            }
            if (geofencingEvent.geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER) {
                enqueueWork(context, intent)
            }
        }

    }
}