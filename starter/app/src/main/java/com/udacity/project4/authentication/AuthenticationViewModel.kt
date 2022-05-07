package com.udacity.project4.authentication

import androidx.lifecycle.ViewModel
import androidx.lifecycle.map

class AuthenticationViewModel : ViewModel() {

    enum class AuthenticationState {
        AUTHENTICATED, UNAUTHENTICATED
    }

    /**
     * Checks authentication state of the user, enabling other classes to observe it
     */
    val authenticationState = FirebaseUserLiveData().map { user ->
        if( user != null) {
            AuthenticationState.AUTHENTICATED
        } else {
            AuthenticationState.UNAUTHENTICATED
        }
    }
}