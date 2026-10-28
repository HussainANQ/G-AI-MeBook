package com.example

import android.app.Application
import androidx.room.Room
import com.example.data.database.AppDatabase
import com.example.data.repository.AppRepository

class MeasurementsApp : Application() {
    lateinit var database: AppDatabase
    lateinit var repository: AppRepository

    override fun onCreate() {
        super.onCreate()
        database = Room.databaseBuilder(
            this,
            AppDatabase::class.java,
            "measurements_notebook_db"
        )
        .fallbackToDestructiveMigration()
        .build()

        repository = AppRepository(database, this)
    }
}
