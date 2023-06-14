package com.udacity.project4.locationreminders.savereminder.selectreminderlocation

import android.app.Application
import androidx.lifecycle.MutableLiveData
import com.udacity.project4.base.BaseViewModel

class SelectLocationViewModel(
    val app: Application
): BaseViewModel(app) {

    val testing = MutableLiveData<Boolean>()

    init {
        testing.postValue(false)
    }

    fun enableTestingMode() {
        testing.postValue(true)
    }
}