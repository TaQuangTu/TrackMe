package com.example.trackme.utils

import android.location.Location
import com.example.trackme.data.Point

object LocationHelper {
    //lat1,lng1,time,session_id*lat2,lng2,time,session_id
    fun stringToArrayList(points: String): ArrayList<Point> {
        val result = ArrayList<Point>()
        val pointArray = points.split("*")
        for (pointItems in pointArray) {
            val pointItemArray = pointItems.split(",")
            if (pointItemArray.size == 4) {
                for (item in pointItemArray) {
                    val lat = item.trim().toDouble()
                    val lng = item.trim().toDouble()
                    val time = item.trim().toLong()
                    val sessionId = item.trim()
                    result.add(Point(time, lat, lng, sessionId))
                }
            }
        }
        return result
    }

    fun distance(points: String): Double {
        val pointArray = stringToArrayList(points)
        var distance = 0.0
        for (i in 0 until pointArray.size - 1) {
            val sP = pointArray[i]
            val eP = pointArray[i + 1]
            val res = floatArrayOf(0f)
            Location.distanceBetween(sP.lat, sP.lng, eP.lat, eP.lng, res)
            distance += res[0]
        }
        return distance
    }

    fun timeInSeconds(points: String): Long {
        val pointArray = stringToArrayList(points)
        if (pointArray.size < 2) return 0
        return pointArray[pointArray.size - 1].time - pointArray[0].time
    }
    fun avgVelocity(points: String): Double{
        val distance = distance(points)
        val time = timeInSeconds(points)
        return distance/time
    }
}