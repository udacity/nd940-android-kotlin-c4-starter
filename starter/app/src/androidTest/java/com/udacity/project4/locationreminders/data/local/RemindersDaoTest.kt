package com.udacity.project4.locationreminders.data.local

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.test.AutoCloseKoinTest
import java.util.concurrent.Executors


@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
// Unit test the DAO
@SmallTest
class RemindersDaoTest : AutoCloseKoinTest() {
    private lateinit var remindersDatabase: RemindersDatabase
    private lateinit var data1: ReminderDTO
    private lateinit var dao1: RemindersDao
    private lateinit var remindersLocalRepository: RemindersLocalRepository
    lateinit var appContext: Application
    lateinit var localedao: RemindersDao

    //(DONE)Todo:// test database
//    @get:Rule
//    var instantExecutorRule = InstantTaskExecutorRule()

    /*  @Before
      fun init() {
          stopKoin()//stop the original app koin
          appContext = ApplicationProvider.getApplicationContext()
          val myModule = module {
              viewModel {
                  RemindersListViewModel(
                      appContext,
                      get() as ReminderDataSource
                  )
              }
              single {
                  SaveReminderViewModel(
                      appContext,
                      get() as ReminderDataSource
                  )
              }
              single { RemindersLocalRepository(get()) }
              single { remindersDatabase.reminderDao() } //not sure if it's right or wrong
          }
          //declare a new koin module
          startKoin {
              appContext
              modules(listOf(myModule))
          }
          //Get our real repository
  //        repository = get()

          //clear the data to start fresh
  //        runBlocking {
  //            repository.deleteAllReminders()
  //        }
      }*/

    @Before
    fun initDataBase() {
//        stopKoin()
        remindersDatabase = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            RemindersDatabase::class.java
        ).setTransactionExecutor(Executors.newSingleThreadExecutor()).build()

        remindersLocalRepository = RemindersLocalRepository(
            remindersDao = remindersDatabase.reminderDao(),
            Dispatchers.Main
        )
        localedao = LocalDB.createRemindersDao(ApplicationProvider.getApplicationContext())

//        dao1 = remindersDatabase.reminderDao()
        /*runTest {
            //add data to database
            data1 = ReminderDTO("title", "description", "Location", 10.5, 5.5)
            remindersDatabase.reminderDao().saveReminder(data1)
            dao1.saveReminder(data1)
        }*/
    }

    @Test
    fun testDataBase_getDataById() = runTest {
        //Given
        //UpThere
        data1 = ReminderDTO("title", "description", "Location", 10.5, 5.5)
//        remindersLocalRepository.saveReminder(data1)

        //When
//        val dataObtained = remindersDatabase.reminderDao().getReminderById(data1.id)
//        val dataObtained = remindersLocalRepository.getReminder(id = data1.id)

        //Then
//        assertThat(dataObtained as ReminderDTO, notNullValue())
//        assertThat(dataObtained.id, `is`(data1.id))
//        assertThat(dataObtained.title, `is`(data1.title))
//        assertThat(dataObtained.description, `is`(data1.description))
//        assertThat(dataObtained.latitude, `is`(data1.latitude))
//        assertThat(dataObtained.longitude, `is`(data1.longitude))
    }
/*
    @Test
    fun testDataBase_clearAllData() = runTest {
        //Given
//        remindersDatabase.reminderDao().deleteAllReminders()
        data1  = ReminderDTO("title", "description", "Location", 10.5, 5.5)
        dao1.saveReminder(data1)
        dao1.deleteAllReminders()
        //When
//        val dataObtained = remindersDatabase.reminderDao().getReminders()
        val dataObtained =dao1.getReminders()

        //Then
        assertThat(dataObtained as ReminderDTO, notNullValue())
        assertThat(dataObtained, `is`(emptyList<ReminderDTO>()))
    }

    @After
    fun terminateDataBase() {
        remindersDatabase.close()
    }
 */
}
