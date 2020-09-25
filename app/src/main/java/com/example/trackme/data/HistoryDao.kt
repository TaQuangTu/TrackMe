package com.example.trackme.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query

@Dao
interface HistoryDao {
    @Query("SELECT * FROM History")
    fun getAll(): List<History>

    @Query("SELECT * FROM History WHERE session IN (:sessions)")
    fun loadAllByIds(sessions: IntArray): List<History>

    @Query("SELECT * FROM History WHERE session LIKE :sessionId")
    fun findBySession(sessionId: String): History

    @Insert
    fun insertAll(vararg history: History)

    @Delete
    fun delete(history: Array<out History>)
}