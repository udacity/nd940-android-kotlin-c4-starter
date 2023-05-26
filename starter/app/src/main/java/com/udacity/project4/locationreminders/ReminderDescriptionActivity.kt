package com.udacity.project4.locationreminders

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.udacity.project4.R
import com.udacity.project4.databinding.ActivityReminderDescriptionBinding
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import com.udacity.project4.utils.TAG

/**
 * Activity that displays the reminder details after the user clicks on the notification
 */
class ReminderDescriptionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityReminderDescriptionBinding

    companion object {
        private const val EXTRA_ReminderDataItem = "EXTRA_ReminderDataItem"

        private var reminderTitle = ""
        private var description = ""
        private var location = ""
        private var latitude = 0.0
        private var longitude = 0.0

        // Receive the reminder object after the user clicks on the notification
        fun newIntent(context: Context, reminderDataItem: ReminderDataItem): Intent {
            Log.d(TAG, "ReminderDescriptionActivity.newIntent() -> reminderDataItem: $reminderDataItem")
            val intent = Intent(context, ReminderDescriptionActivity::class.java)
            intent.putExtra(EXTRA_ReminderDataItem, reminderDataItem)

            reminderTitle = reminderDataItem.title.toString()
            description = reminderDataItem.description.toString()
            location = reminderDataItem.location.toString()
            reminderDataItem.latitude?.let {
                latitude = it
            }
            reminderDataItem.longitude?.let {
                longitude = it
            }
            return intent
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "ReminderDescriptionActivity.onCreate().")

        val layoutId = R.layout.activity_reminder_description
        binding = DataBindingUtil.setContentView(this, layoutId)

        binding.title.text = reminderTitle
        binding.description.text = description
        binding.location.text = location
        binding.latitude.text = latitude.toString()
        binding.longitude.text = longitude.toString()
    }
}