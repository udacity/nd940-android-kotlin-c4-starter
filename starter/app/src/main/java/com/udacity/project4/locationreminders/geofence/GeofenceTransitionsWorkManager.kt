package com.udacity.project4.locationreminders.geofence

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import com.udacity.project4.MyApp
import com.udacity.project4.R
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import com.udacity.project4.utils.TAG
import com.udacity.project4.utils.errorMessage
import com.udacity.project4.utils.sendNotification

/**
 * - Source:
 * https://www.kodeco.com/7372-geofencing-api-tutorial-for-android
 * - JobIntentService is deprecated:
 * https://medium.com/tech-takeaways/how-to-migrate-the-deprecated-jobintentservice-a0071a7957ed
 */
const val UNIQUE_WORK_NAME = "GeofenceTransitionsWorkManager"

class GeofenceTransitionsWorkManager(
    val context: Context, workerParameters: WorkerParameters
) : Worker(context, workerParameters) {

    companion object {
        private const val INTENT_PARAM = "INTENT_PARAM"

        fun buildWorkRequest(parameter: String): OneTimeWorkRequest {
//            val data = Data.Builder().putString(INTENT_PARAM, parameter).build()
            return OneTimeWorkRequestBuilder<GeofenceTransitionsWorkManager>().apply {
//                setInputData(data)
            }.build()
        }
    }

    override fun doWork(): Result {
//        val parameter: String? = inputData.getString(INTENT_PARAM)
        // Handle your work.
        val intent = (context.applicationContext as MyApp).broadcastIntent
        Log.d(TAG, "GeofenceTransitionsWorkManager.doWork() -> [1]")
        val geofencingEvent = GeofencingEvent.fromIntent(intent)
        if (geofencingEvent != null) {
            Log.d(TAG, "GeofenceTransitionsWorkManager.doWork() -> [2]")
            if (geofencingEvent.hasError()) {
                Log.d(TAG, "GeofenceTransitionsWorkManager.doWork() -> [3]")
                val errorMessage = errorMessage(context, geofencingEvent.errorCode)
                Log.e(TAG, "GeofenceTransitionsWorkManager.doWork() -> $errorMessage")
                return Result.failure()
            } else {
                Log.d(TAG, "GeofenceTransitionsWorkManager.doWork() -> [4]")
            }
            handleEvent(geofencingEvent)
        } else {
            Log.d(TAG, "GeofenceTransitionsWorkManager.doWork() -> [5]")
        }
        return Result.success()
    }

    private fun handleEvent(event: GeofencingEvent) {
        Log.d(TAG, "GeofenceTransitionsWorkManager.handleEvent().")
        if (event.geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER) {
            Log.d(TAG, "GeofenceTransitionsWorkManager.handleEvent() -> [1]")
            event.triggeringGeofences?.let { list ->
                for (item in list) {
                    val current = getCurrentReminder(item)
                    callNotification(context, current)
                }
            }
        } else {
            Log.d(TAG, "GeofenceTransitionsWorkManager.handleEvent() -> [2]")
        }
    }

    private fun getCurrentReminder(currentGeoFence: Geofence): ReminderDataItem {
        val id = currentGeoFence.requestId
        val lat = currentGeoFence.latitude
        val lng = currentGeoFence.longitude
        return ReminderDataItem(
            title = id,
            description = "",
            location = id,
            latitude = lat,
            longitude = lng
        )
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