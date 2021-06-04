package com.udacity.project4.authentication

import android.app.Activity
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.IdpResponse
import com.google.firebase.auth.FirebaseAuth
import com.udacity.project4.R
import com.udacity.project4.locationreminders.RemindersActivity


/**
 * This class should be the starting point of the app, It asks the users to sign in / register, and redirects the
 * signed in users to the RemindersActivity.
 */
class AuthenticationActivity : AppCompatActivity() {
    private val RC_SIGN_IN = 123

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_authentication)

        val auth = FirebaseAuth.getInstance()

//          TODO: If the user was authenticated, send him to RemindersActivity
        if (auth.currentUser != null) {
            showReminderActivity()
        } else {
            userLogin()
        }

    }

    private fun registerUser() {
        //TODO: Implement the create account and sign in using FirebaseUI, use sign in using email and sign in using Google
        startActivityForResult(
            // Get an instance of AuthUI based on the default app
            AuthUI.getInstance().createSignInIntentBuilder().build(),
            RC_SIGN_IN
        )
    }

    private fun userLogin() {
        // Choose authentication providers
        val providers = arrayListOf(
            AuthUI.IdpConfig.EmailBuilder().build(),
            AuthUI.IdpConfig.GoogleBuilder().build()
        )

// Create and launch sign-in intent
        startActivityForResult(
            AuthUI.getInstance()
                .createSignInIntentBuilder()
                .setAvailableProviders(providers)
                .build(),
            RC_SIGN_IN
        )
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            val response = IdpResponse.fromResultIntent(data)

            if (resultCode == Activity.RESULT_OK) {
                // Successfully signed in
                val user = FirebaseAuth.getInstance().currentUser
                showReminderActivity()

            } else {
                registerUser()
            }
        }

    }

    private fun showReminderActivity() {
        // already signed in
        val reminderActivity = Intent(this, RemindersActivity::class.java)
        reminderActivity.flags = FLAG_ACTIVITY_CLEAR_TOP
        startActivity(reminderActivity)
    }
}
