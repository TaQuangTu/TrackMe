package com.example.trackme.services

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.room.Room
import com.example.trackme.data.AppDatabase
import com.example.trackme.data.History
import com.example.trackme.ui.main.RecordFragment.Companion.SESSION_ID
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult

class LocationService : Service() {
    lateinit var mFusedLocationProviderClient: FusedLocationProviderClient
    lateinit var mLocationCallback: LocationCallback
    lateinit var mSessionId: String
    var mPoints: String = ""  //recorded points will be save under string format
    var mRecordState = ACTION_NONE

    companion object {
        const val TAG = "location service test"

        const val NOTIFICATION_ID = 1
        const val NOTIFICATION_CHANEL_ID = "LOCATION_CHANEL_ID"
        const val NOTIFICATION_CHANEL_NAME = "Location chanel"

        const val ACTION_LOCATION_UPDATE = "LOCATION_UPDATE"
        const val ACTION_ACTIVITY_CONTROL_CHANGE = "ACTION_ACTIVITY_CONTROL_CHANGE"

        const val ACTION_NONE = 0 //running
        const val ACTION_PAUSE = 1
        const val ACTION_RESUME = 2
        const val ACTION_STOP = 3

        const val LAT = "LAT"
        const val LNG = "LNG"
        const val TIME = "TIME"

        const val LOCATION_REQUEST_INTERVAL = 10000L
        const val LOCATION_REQUEST_FASTEST_INTERVAL = 5000L
        const val LOCATION_REQUEST_PRIORITY = LocationRequest.PRIORITY_HIGH_ACCURACY

        object LocationUtils {
            fun getLocationRequest(): LocationRequest {
                return LocationRequest.create().apply {
                    interval = LOCATION_REQUEST_INTERVAL
                    fastestInterval = LOCATION_REQUEST_FASTEST_INTERVAL
                    priority = LOCATION_REQUEST_PRIORITY
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    override fun onCreate() {
        super.onCreate()
        listenToActivityControls()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManagerCompat =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManagerCompat.createNotificationChannel(
                NotificationChannel(
                    NOTIFICATION_CHANEL_ID,
                    NOTIFICATION_CHANEL_NAME,
                    NotificationManager.IMPORTANCE_DEFAULT
                )
            )
            val notification =
                NotificationCompat.Builder(this, NOTIFICATION_CHANEL_ID).setContentTitle("TrackMe")
                    .build()
            startForeground(NOTIFICATION_ID, notification)
        }
        listenToLocationUpdate(intent)
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        mFusedLocationProviderClient.removeLocationUpdates(mLocationCallback)
    }

    @SuppressLint("MissingPermission")
    fun listenToLocationUpdate(intent: Intent?) {
        mFusedLocationProviderClient = FusedLocationProviderClient(this)
        mLocationCallback = object : LocationCallback() {
            override fun onLocationResult(p0: LocationResult?) {
                p0?.let {
                    if (mRecordState == ACTION_NONE) {
                        val intent = Intent()
                        intent.action = ACTION_LOCATION_UPDATE
                        intent.putExtra(LAT, p0?.lastLocation.latitude)
                        intent.putExtra(LNG, p0?.lastLocation.longitude)
                        val time = System.currentTimeMillis()
                        intent.putExtra(TIME, time)
                        intent.putExtra(SESSION_ID, mSessionId)
                        sendBroadcast(intent)
                    }
                }
            }
        }
        mSessionId = intent!!.getStringExtra(SESSION_ID)!!
        mFusedLocationProviderClient.requestLocationUpdates(
            LocationUtils.getLocationRequest(), mLocationCallback,
            Looper.getMainLooper()
        )
    }

    private fun listenToActivityControls() {
        val intentFilter = IntentFilter().apply { addAction(ACTION_ACTIVITY_CONTROL_CHANGE) }
        registerReceiver(object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent == null) return
                mRecordState = intent.getIntExtra(ACTION_ACTIVITY_CONTROL_CHANGE, ACTION_NONE)
                if (mRecordState == ACTION_STOP) {
                    saveToDatabase()
                    stopSelf()
                } else if (mRecordState == ACTION_RESUME) {
                    mRecordState = ACTION_NONE //mean that user want to continue recording location
                }
            }
        }, intentFilter)
    }

    fun saveToDatabase() {
        val distance = 0.0
        val velocity = 0.0
        val history = History(mSessionId, mPoints, distance, velocity)
        Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java, "TRACKME"
        ).build().historyDao().insertAll(history)
    }
}