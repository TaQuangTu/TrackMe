package com.example.trackme.utils

import android.location.Location
import android.util.Log
import com.example.trackme.R
import com.example.trackme.TrackMeApplication
import com.example.trackme.data.History
import com.example.trackme.data.Point
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import kotlin.math.roundToInt

object LocationHelper {
    fun addPoint(
        currentString: String,
        lat: Double,
        lng: Double,
        time: Long,
        sessionId: String
    ): String {
        return if (currentString == "") {
            "$lat,$lng,$time,$sessionId"
        } else {
            "$currentString*$lat,$lng,$time,$sessionId"
        }
    }

    //lat1,lng1,time,session_id*lat2,lng2,time,session_id
    fun stringToArrayList(points: String): ArrayList<Point> {
        val result = ArrayList<Point>()
        val pointArray = points.split("*")
        for (pointItems in pointArray) {
            val pointItemArray = pointItems.split(",")
            if (pointItemArray.size == 4) {
                val lat = pointItemArray[0].trim().toDouble()
                val lng = pointItemArray[1].trim().toDouble()
                val time = pointItemArray[2].trim().toLong()
                val sessionId = pointItemArray[3].trim()
                result.add(Point(time, lat, lng, sessionId))
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

    fun distance(pointArray: ArrayList<Point>): Double {
        var distance = 0.0
        for (i in 0 until pointArray.size - 1) {
            val sP = pointArray[i]
            val eP = pointArray[i + 1]
            val res = floatArrayOf(0f)
            Location.distanceBetween(sP.lat, sP.lng, eP.lat, eP.lng, res)
            distance += res[0]
        }
        return distance.roundToInt().toDouble()
    }

    fun timeInSeconds(points: String): Long {
        val pointArray = stringToArrayList(points)
        if (pointArray.size < 2) return 0
        return pointArray[pointArray.size - 1].time/1000 - pointArray[0].time/1000
    }

    fun timeInSeconds(pointArray: ArrayList<Point>): Long {
        if (pointArray.size < 2) return 0
        return (pointArray[pointArray.size - 1].time - pointArray[0].time) / 1000
    }

    fun avgVelocity(points: ArrayList<Point>): Double {
        val distance = distance(points)
        val time = timeInSeconds(points)
        return Math.round(distance / time * 100) / 100.0 //take two digits after decimal
    }

    fun avgVelocity(points: String): Double {
        val distance = distance(points)
        val time = timeInSeconds(points)
        return Math.round(distance / time * 100) / 100.0 //take two digits after decimal
    }

    fun pointsToLatLngs(points: ArrayList<Point>): ArrayList<LatLng> {
        val results = ArrayList<LatLng>()
        for (point in points) {
            results.add(LatLng(point.lat, point.lng))
        }
        return results
    }

    fun getBound(points: ArrayList<Point>): LatLngBounds {
        val boundBuider = LatLngBounds.builder()
        for (point in points) {
            boundBuider.include(LatLng(point.lat, point.lng))
        }
        return boundBuider.build()
    }

    fun getStaticMapUrl(history: History): String {
        var url = "https://maps.googleapis.com/maps/api/staticmap?size=420x225&path="
        var path = ""
        val pointArray = stringToArrayList(history.points);
        for (point in pointArray) {
            path = path + point.lat + "," + point.lng + "|"
        }
        if (path.endsWith("|")) {
            path = path.substring(0, path.length - 1)
        }
        var markerParams = "&markers=size:mid|color:red"
        if (pointArray.size > 0) {
            markerParams = markerParams + "|" + pointArray[0].lat + "," + pointArray[0].lng
        }
        if (pointArray.size > 1) {
            markerParams =
                markerParams + "|" + pointArray[pointArray.size - 1].lat + "," + pointArray[pointArray.size - 1].lng
        }
        url =
            url + path + markerParams + "&key=" + TrackMeApplication.appContext!!.getString(R.string.google_api_key)
        Log.d("TAG", "getStaticMapUrl: $url")
        return url
    }
}