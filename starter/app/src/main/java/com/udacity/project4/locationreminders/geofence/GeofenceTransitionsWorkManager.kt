package com.udacity.project4.locationreminders.geofence

import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.work.Data
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
 * Source:
 * https://www.kodeco.com/7372-geofencing-api-tutorial-for-android
 */
/*
class GeofenceTransitionsJobIntentService : JobIntentService(), CoroutineScope {

    private var coroutineJob: Job = Job()
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO + coroutineJob

    companion object {
        private const val JOB_ID = 573

        // TODO: Call this to start the JobIntentService to handle the geofencing transition events.
        fun enqueueWork(context: Context, intent: Intent) {
            enqueueWork(
                context,
                GeofenceTransitionsJobIntentService::class.java, JOB_ID,
                intent
            )
        }
    }

    override fun onHandleWork(intent: Intent) {
        // TODO: Handle the geofencing transition events and
        //  send a notification to the user when he enters the geofence area.
        // TODO: Call @sendNotification
    }

    // TODO: Get the request id of the current geofence.
    private fun sendNotification(triggeringGeofences: List<Geofence>) {
        val requestId = ""

        // Get the local repository instance.
        val remindersLocalRepository: ReminderDataSource by inject()
        // Interaction to the repository has to be through a coroutine scope.
        CoroutineScope(coroutineContext).launch(SupervisorJob()) {
            // Get the reminder with the request id.
            val result = remindersLocalRepository.getReminder(requestId)
            if (result is Result.Success<ReminderDTO>) {
                val reminderDTO = result.data
                // Send a notification to the user with the reminder details.
                sendNotification(
                    this@GeofenceTransitionsJobIntentService, ReminderDataItem(
                        reminderDTO.title,
                        reminderDTO.description,
                        reminderDTO.location,
                        reminderDTO.latitude,
                        reminderDTO.longitude,
                        reminderDTO.id
                    )
                )
            }
        }
    }
}
 */

/**
 * JobIntentService is deprecated:
 * https://medium.com/tech-takeaways/how-to-migrate-the-deprecated-jobintentservice-a0071a7957ed
 */
/*
class GeofenceTransitionsWorkManager : JobIntentService() {
    companion object {
        private const val JOB_ID = 573
        fun enqueueWork(context: Context, intent: Intent) {
            Log.d(TAG, "GeofenceTransitionsJobIntentService.enqueueWork().")
            enqueueWork(
                context,
                GeofenceTransitionsWorkManager::class.java, JOB_ID,
                intent)
        }
    }

    override fun onHandleWork(intent: Intent) {
        Log.d(TAG, "GeofenceTransitionsJobIntentService.onHandleWork() -> [5]")
        val geofencingEvent = GeofencingEvent.fromIntent(intent)
        if (geofencingEvent != null) {
            Log.d(TAG, "GeofenceTransitionsJobIntentService.onHandleWork() -> [1]")
            if (geofencingEvent.hasError()) {
                Log.d(TAG, "GeofenceTransitionsJobIntentService.onHandleWork() -> [2]")
                val errorMessage = errorMessage(this, geofencingEvent.errorCode)
                Log.e(TAG, "GeofenceTransitionsJobIntentService.onHandleWork() -> $errorMessage")
                return
            } else {
                Log.d(TAG, "GeofenceTransitionsJobIntentService.onHandleWork() -> [3]")
            }
            handleEvent(geofencingEvent)
        } else {
            Log.d(TAG, "GeofenceTransitionsJobIntentService.onHandleWork() -> [4]")
        }
    }

    private fun handleEvent(event: GeofencingEvent) {
        Log.d(TAG, "GeofenceTransitionsJobIntentService.handleEvent().")
        if (event.geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER) {
            Log.d(TAG, "GeofenceTransitionsJobIntentService.handleEvent() -> [1]")
            val reminderDataItem = getFirstReminder(event.triggeringGeofences as List<Geofence>)
            callNotification(this, reminderDataItem)
        } else {
            Log.d(TAG, "GeofenceTransitionsJobIntentService.handleEvent() -> [2]")
        }
    }

    private fun getFirstReminder(triggeringGeoFences: List<Geofence>): ReminderDataItem {
        val first = triggeringGeoFences[0]
        val id = first.requestId
        val lat = first.latitude
        val lng = first.longitude
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
            val reminderDataItem = getFirstReminder(event.triggeringGeofences as List<Geofence>)
            callNotification(context, reminderDataItem)
        } else {
            Log.d(TAG, "GeofenceTransitionsWorkManager.handleEvent() -> [2]")
        }
    }

    private fun getFirstReminder(triggeringGeoFences: List<Geofence>): ReminderDataItem {
        val first = triggeringGeoFences[0]
        val id = first.requestId
        val lat = first.latitude
        val lng = first.longitude
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