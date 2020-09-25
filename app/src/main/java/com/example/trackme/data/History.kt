package com.example.trackme.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class History(@PrimaryKey val session:String, @ColumnInfo(name = "points") val points: String, var distance:Double, var avgVelocity:Double)
