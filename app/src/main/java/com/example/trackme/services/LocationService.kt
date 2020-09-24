package com.example.trackme.services

import android.app.Service
import android.content.Intent
import android.location.LocationManager
import android.location.LocationProvider
import android.os.IBinder
import android.os.PowerManager
import com.google.android.gms.location.LocationRequest

class LocationService: Service() {
    lateinit var locationRequest: LocationRequest
    override fun onBind(intent: Intent?): IBinder? {

    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        locationRequest = LocationRequest()
        locationRequest.
        return super.onStartCommand(intent, flags, startId)
    }
}