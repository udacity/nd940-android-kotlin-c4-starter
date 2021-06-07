package com.udacity.project4.locationreminders.data.local

import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;
import com.udacity.project4.locationreminders.data.dto.ReminderDTO

import org.junit.Before;
import org.junit.Rule;
import org.junit.runner.RunWith;

import kotlinx.coroutines.ExperimentalCoroutinesApi;
import kotlinx.coroutines.test.runBlockingTest
import org.hamcrest.CoreMatchers.*
import org.hamcrest.MatcherAssert.assertThat
import org.junit.After
import org.junit.Test
import java.io.IOException
import java.util.*

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
//Unit test the DAO
@SmallTest
class RemindersDaoTest {
    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    private lateinit var remindersDao: RemindersDao
    private lateinit var db: RemindersDatabase

    @Before
    fun createDb(){
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(
            context, RemindersDatabase::class.java).build()
        remindersDao = db.reminderDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDB(){
        db.close()
    }

    @Test
    @Throws(Exception::class)
    fun insertReminderAndGetById() = runBlockingTest{
        // GIVEN - insert a reminder

        val reminderId = UUID.randomUUID().toString()
        val reminder = ReminderDTO("Call a friend","calling ...",
            "Parc de La Victoire",
            36.74208242672705,3.072958588600159,
            reminderId)

        remindersDao.saveReminder(reminder)

        // WHEN - Get the reminder by id from the database
        val loaded = remindersDao.getReminderById(reminderId)
        val loadedFromRandomId = remindersDao.getReminderById(UUID.randomUUID().toString())

        // THEN - The loaded data contains the expected values
        assertThat(loaded as ReminderDTO, notNullValue())
        assertThat(loaded.id, `is`(reminder.id))
        assertThat(loaded.title, `is`(reminder.title))
        assertThat(loaded.description, `is`(reminder.description))
        assertThat(loaded.latitude, `is`(reminder.latitude))
        assertThat(loaded.longitude, `is`(reminder.longitude))

        assertThat(loadedFromRandomId, `is`(nullValue()))
    }

    @Test
    fun getReminders_success() = runBlockingTest {
        // GIVING - insert three reminders
        val r1 = ReminderDTO("t1","d1",
            "l1",
            36.74208242672705,3.072958588600159)

        val r2 = ReminderDTO("t2","d2",
            "l2",
            36.74208242672705,3.072958588600159)

        val r3 = ReminderDTO("t3","d3",
            "l3",
            36.74208242672705,3.072958588600159)

        remindersDao.saveReminder(r1)
        remindersDao.saveReminder(r2)
        remindersDao.saveReminder(r3)

        // WHEN - get all reminders from the database
        val loadedReminders = remindersDao.getReminders()

        // THEN - check loaded data have the correct count
        assertThat(loadedReminders.count(), `is`(3))

        assertThat(loadedReminders.first().title, `is`(r1.title))
        assertThat(loadedReminders.first().description, `is`(r1.description))
        assertThat(loadedReminders.first().location, `is`(r1.location))

        assertThat(loadedReminders.last().title, `is`(r3.title))
        assertThat(loadedReminders.last().description, `is`(r3.description))
        assertThat(loadedReminders.last().location, `is`(r3.location))

    }

    @Test
    fun deleteAllReminders_success() = runBlockingTest {
        // GIVING - insert three reminders
        val r1 = ReminderDTO("t1","d1",
            "l1",
            36.74208242672705,3.072958588600159)

        val r2 = ReminderDTO("t2","d2",
            "l2",
            36.74208242672705,3.072958588600159)

        val r3 = ReminderDTO("t3","d3",
            "l3",
            36.74208242672705,3.072958588600159)

        remindersDao.saveReminder(r1)
        remindersDao.saveReminder(r2)
        remindersDao.saveReminder(r3)


        // WHEN - delete all reminders
        remindersDao.deleteAllReminders()
        val loadedReminders = remindersDao.getReminders()

        // THEN - check the database is empty
        assertThat(loadedReminders.count(), `is`(0))

    }

}