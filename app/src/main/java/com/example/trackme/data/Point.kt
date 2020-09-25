package com.example.trackme.data

import androidx.room.Entity
import androidx.room.ForeignKey

@Entity
data class Point(val time:Long, val lat:Double, val lng:Double, val session:String)