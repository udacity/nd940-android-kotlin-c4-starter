package com.udacity.project4.utils

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.TaskStackBuilder
import com.udacity.project4.BuildConfig
import com.udacity.project4.R
import com.udacity.project4.locationreminders.ReminderDescriptionActivity
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem

private const val CHANNEL_ID = BuildConfig.APPLICATION_ID + ".channel"

/**
 * We need to create a NotificationChannel associated with our CHANNEL_ID before sending a
 * notification.
 */
@SuppressLint("NewApi")
fun createChannel(context: Context, notificationManager: NotificationManager) {
    Log.d(TAG, "NotificationUtils.createChannel().")
    val sdkAboveOreo = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
    val channelNull = notificationManager.getNotificationChannel(CHANNEL_ID) == null
    if (sdkAboveOreo && channelNull) {
        val notificationChannel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.channel_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            setShowBadge(false)
        }
        notificationChannel.enableLights(true)
        notificationChannel.lightColor = Color.RED
        notificationChannel.enableVibration(true)
        notificationChannel.description = context.getString(R.string.notification_channel_description)
        notificationManager.createNotificationChannel(notificationChannel)
    }
}

fun sendNotification(context: Context, reminderDataItem: ReminderDataItem) {
    Log.d(TAG, "NotificationUtils.sendNotification().")
    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    createChannel(context, notificationManager)
    val intent = ReminderDescriptionActivity.newIntent(context.applicationContext, reminderDataItem)

    // Create a pending intent that opens ReminderDescriptionActivity when the user clicks on the
    // notification.
    var intentFlagTypeUpdateCurrent = PendingIntent.FLAG_UPDATE_CURRENT
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        intentFlagTypeUpdateCurrent = PendingIntent.FLAG_IMMUTABLE
    }
    val stackBuilder = TaskStackBuilder.create(context)
        .addParentStack(ReminderDescriptionActivity::class.java)
        .addNextIntent(intent)
    val notificationPendingIntent = stackBuilder.getPendingIntent(
        getUniqueId(), intentFlagTypeUpdateCurrent
    )

    val mapIcon = R.drawable.map
    val mapImage = BitmapFactory.decodeResource(context.resources, mapIcon)
    val bigPicStyle = NotificationCompat.BigPictureStyle()
        .bigPicture(mapImage)
        .bigLargeIcon(null)

    // Build the notification object with the data to be shown.
    val notification = NotificationCompat.Builder(context, CHANNEL_ID)
        .setSmallIcon(R.drawable.map_small)
        .setContentTitle(reminderDataItem.title)
        .setContentText(reminderDataItem.location)
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setContentIntent(notificationPendingIntent)
        .setStyle(bigPicStyle)
        .setLargeIcon(mapImage)
        .setAutoCancel(true)
        .build()

    notificationManager.notify(getUniqueId(), notification)
}

private fun getUniqueId() = ((System.currentTimeMillis() % 10000).toInt())