package com.udacity.project4.authentication

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.IdpResponse
import com.google.firebase.auth.FirebaseAuth
import com.udacity.project4.R
import com.udacity.project4.databinding.ActivityAuthenticationBinding
import com.udacity.project4.locationreminders.RemindersActivity

/**
 * This class should be the starting point of the app, It asks the users to sign in / register, and redirects the
 * signed in users to the RemindersActivity.
 */
class AuthenticationActivity : AppCompatActivity() {

    companion object {
        const val TAG = "MainFragment"
    }

    // Get a reference to the ViewModel scoped to this Fragment
    private val viewModel by viewModels<LoginViewModel>()
    private lateinit var binding: ActivityAuthenticationBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val layoutId = R.layout.activity_authentication
        binding = DataBindingUtil.setContentView(this, layoutId)
        binding.authButton.text = getString(R.string.login)

        // TODO: Line below was commented to avoid losing time Login In on Firebase
        //  every time I test the app.
//        AuthUI.getInstance().signOut(this)
    }

    override fun onResume() {
        super.onResume()
        // TODO: Line below was commented to avoid losing time Login In on Firebase
        //  every time I test the app.
//        observeAuthenticationState()

        // TODO: Lines below were commented to avoid losing time Login In on Firebase
        //  every time I test the app.
//        binding.authButton.setOnClickListener {
//            launchSignInFlow()
//        }
        startActivity(Intent(this, RemindersActivity::class.java))
    }

    /**
     * Listen to the result of the sign in process by filter for when
     * SIGN_IN_REQUEST_CODE is passed back. Start by having log statements to know
     * whether the user has signed in successfully
     */
    private val resultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // There are no request codes
            val data: Intent? = result.data
            val response = IdpResponse.fromResultIntent(data)
            if (result.resultCode == Activity.RESULT_OK) {
                // User successfully signed in
                Log.i(TAG, "Successfully signed in user " +
                    "${FirebaseAuth.getInstance().currentUser?.displayName}!"
                )
                // If the user was authenticated, send him to RemindersActivity.
                startActivity(Intent(this, RemindersActivity::class.java))
            } else {
                // Sign in failed. If response is null the user canceled the
                // sign-in flow using the back button. Otherwise check
                // response.getError().getErrorCode() and handle the error.
                Log.i(TAG, "Sign in unsuccessful ${response?.error?.errorCode}")
            }
        }
    }

    /**
     * Observes the authentication state and changes the UI accordingly.
     * If there is a logged in user: (1) show a logout button and (2) display their name.
     * If there is no logged in user: show a login button
     */
    private fun observeAuthenticationState() {
        viewModel.authenticationState.observe(this) { authenticationState ->
            // in LoginViewModel and change the UI accordingly.
            when (authenticationState) {
                LoginViewModel.AuthenticationState.AUTHENTICATED -> { logoutFromFirebase() }
                else -> { loginOnFirebase() }
            }
        }
    }

    private fun loginOnFirebase() {
        binding.authButton.text = getString(R.string.login)
        binding.authButton.setOnClickListener {
            launchSignInFlow()
        }
    }

    private fun logoutFromFirebase() {
        binding.authButton.text = getString(R.string.logout)
        binding.authButton.setOnClickListener {
            AuthUI.getInstance().signOut(this)
        }
    }

    /**
     * Implement the create account and sign in using FirebaseUI,
     * use sign in using email and sign in using Google.
     */
    private fun launchSignInFlow() {
        // Give users the option to sign in / register with their email or Google account.
        // If users choose to register with their email,
        // they will need to create a password as well.
        val providers = arrayListOf(
            AuthUI.IdpConfig.EmailBuilder().build(), AuthUI.IdpConfig.GoogleBuilder().build()
            // This is where you can provide more ways for users to register and
            // sign in.
        )

        // Create and launch sign-in intent.
        val intent = AuthUI.getInstance()
            .createSignInIntentBuilder()
            .setAvailableProviders(providers)
            .build()
        resultLauncher.launch(intent)
    }
}