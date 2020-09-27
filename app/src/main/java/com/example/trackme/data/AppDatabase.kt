package com.example.trackme.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [History::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    companion object{
        val NAME = "TRACKME"
    }
    abstract fun historyDao(): HistoryDao
}