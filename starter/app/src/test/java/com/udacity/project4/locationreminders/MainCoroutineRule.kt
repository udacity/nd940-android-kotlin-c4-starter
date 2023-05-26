package com.udacity.project4.locationreminders

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/**
 * Source:
 * https://stackoverflow.com/a/73300292
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MainCoroutineRule(
	val testDispatcher: TestDispatcher = UnconfinedTestDispatcher()
) : TestWatcher() {

	override fun starting(description: Description) {
		super.starting(description)
		Dispatchers.setMain(testDispatcher)
	}

	override fun finished(description: Description) {
		super.finished(description)
		Dispatchers.resetMain()
	}
}