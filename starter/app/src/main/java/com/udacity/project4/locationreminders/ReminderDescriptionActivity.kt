package com.udacity.project4.locationreminders

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.google.android.gms.location.LocationServices
import com.udacity.project4.R
import com.udacity.project4.databinding.ActivityReminderDescriptionBinding
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem

/**
 * Activity that displays the reminder details after the user clicks on the notification
 */
class ReminderDescriptionActivity : AppCompatActivity() {
    private lateinit var binding: ActivityReminderDescriptionBinding
    companion object {
        private const val EXTRA_ReminderDataItem = "EXTRA_ReminderDataItem"

        //        receive the reminder object after the user clicks on the notification
        fun newIntent(context: Context, reminderDataItem: ReminderDataItem): Intent {
            val intent = Intent(context, ReminderDescriptionActivity::class.java)
            intent.putExtra(EXTRA_ReminderDataItem, reminderDataItem)
            return intent
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(
            this,
            R.layout.activity_reminder_description
        )

        val reminderDescription =
            intent.getSerializableExtra(EXTRA_ReminderDataItem) as ReminderDataItem

       val geofencingClient = LocationServices.getGeofencingClient(this)

        binding.reminderDataItem = reminderDescription

        geofencingClient.removeGeofences(listOf(reminderDescription.id)).run {
            addOnCompleteListener {
                if (it.isSuccessful) {
                    binding.textViewStatus.text = getString(R.string.done)
                    Toast.makeText(
                        applicationContext,
                        R.string.done,
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    binding.textViewStatus.text = getString(R.string.error)
                    Toast.makeText(
                        applicationContext,
                        R.string.error,
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }
}