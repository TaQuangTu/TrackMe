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
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.room.Room
import com.example.trackme.data.AppDatabase
import com.example.trackme.data.History
import com.example.trackme.fragments.RecordFragment.Companion.ACTION_ASK_FOR_RUNNING_STATE
import com.example.trackme.fragments.RecordFragment.Companion.RECORD_STATE
import com.example.trackme.fragments.RecordFragment.Companion.SESSION_ID
import com.example.trackme.utils.LocationHelper
import com.example.trackme.viewmodels.RecordViewModel.Companion.STATE_PAUSE
import com.example.trackme.viewmodels.RecordViewModel.Companion.STATE_RECORDING
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.schedulers.Schedulers

class LocationService : Service() {
    private lateinit var mFusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var mLocationCallback: LocationCallback
    private lateinit var mSessionId: String
    private var mPoints: String = ""  //recorded points will be save under string format
    var mRecordState = STATE_RECORDING

    companion object {
        const val TAG = "location service test"

        const val NOTIFICATION_ID = 1
        const val NOTIFICATION_CHANEL_ID = "LOCATION_CHANEL_ID"
        const val NOTIFICATION_CHANEL_NAME = "Location chanel"

        const val ACTION_LOCATION_UPDATE = "LOCATION_UPDATE"
        const val ACTION_ACTIVITY_CONTROL_CHANGE = "ACTION_ACTIVITY_CONTROL_CHANGE"
        const val ACTION_RESPONSE_FOR_ASKING_RECORD_STATE =
            "ACTION_RESPONSE_FOR_ASKING_RECORD_STATE"
        const val ACTION_RESPONSE_FOR_SAVING_REQUEST = "ACTION_RESPONSE_FOR_SAVING_REQUEST"

        const val EXTRA_MESSAGE = "EXTRA_MESSAGE"
        const val SAVING_RESULT = "SAVING_RESULT"

        const val ACTION_PAUSE = 1
        const val ACTION_RESUME = 2
        const val ACTION_STOP = 3

        const val LAT = "LAT"
        const val LNG = "LNG"
        const val TIME = "TIME"

        const val LOCATION_REQUEST_INTERVAL = 5000L
        const val LOCATION_REQUEST_FASTEST_INTERVAL = 2000L
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
        registReceivers()
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
        mSessionId = intent!!.getStringExtra(SESSION_ID)!!
        listenToLocationUpdate()
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        mFusedLocationProviderClient.removeLocationUpdates(mLocationCallback)
    }

    @SuppressLint("MissingPermission")
    fun listenToLocationUpdate() {
        mFusedLocationProviderClient = FusedLocationProviderClient(this)
        mLocationCallback = object : LocationCallback() {
            override fun onLocationResult(p0: LocationResult?) {
                p0?.let {
                    if (mRecordState == STATE_RECORDING) {
                        val intent = Intent()
                        val lat = p0.lastLocation.latitude
                        val lng = p0.lastLocation.longitude
                        val time = System.currentTimeMillis()
                        intent.action = ACTION_LOCATION_UPDATE
                        intent.putExtra(LAT, lat)
                        intent.putExtra(LNG, lng)
                        intent.putExtra(TIME, time)
                        mPoints = LocationHelper.addPoint(mPoints, lat, lng, time)
                        sendBroadcast(intent)
                    }
                }
            }
        }
        mFusedLocationProviderClient.requestLocationUpdates(
            LocationUtils.getLocationRequest(), mLocationCallback,
            Looper.getMainLooper()
        )
    }

    private fun registReceivers() {
        //regist receiver for action changes from fragment like pressing a button,..
        val intentFilter = IntentFilter().apply {
            addAction(ACTION_ACTIVITY_CONTROL_CHANGE)
            addAction(ACTION_ASK_FOR_RUNNING_STATE)
        }
        registerReceiver(object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent == null) return
                if (intent.action == ACTION_ACTIVITY_CONTROL_CHANGE) {
                    val action = intent.getIntExtra(RECORD_STATE, STATE_RECORDING)
                    if (action == ACTION_STOP) {
                        saveToDatabase().subscribeOn(Schedulers.newThread())
                            .observeOn(AndroidSchedulers.mainThread()).subscribe(
                                {
                                    sendBroadcast(Intent().apply {
                                        setAction(ACTION_RESPONSE_FOR_SAVING_REQUEST)
                                        putExtra(SAVING_RESULT, true)
                                    })
                                    stopSelf()
                                }, {
                                    sendBroadcast(Intent().apply {
                                        setAction(ACTION_RESPONSE_FOR_SAVING_REQUEST)
                                        putExtra(SAVING_RESULT, false)
                                        putExtra(EXTRA_MESSAGE, it.localizedMessage)
                                    })
                                }
                            )
                    } else if (action == ACTION_PAUSE) {
                        mRecordState = STATE_PAUSE
                    } else if (action == ACTION_RESUME) {
                        mRecordState = STATE_RECORDING
                    }
                } else if (intent.action == ACTION_ASK_FOR_RUNNING_STATE) {
                    //save all points at this time
                    saveToDatabase().subscribeOn(Schedulers.newThread())
                        .observeOn(AndroidSchedulers.mainThread()).subscribe({
                            //immediately send response to the asker
                            context?.sendBroadcast(Intent().apply {
                                action = ACTION_RESPONSE_FOR_ASKING_RECORD_STATE
                                putExtra(RECORD_STATE, mRecordState)
                                putExtra(SESSION_ID, mSessionId)
                            })
                        }, {
                            Toast.makeText(context!!, it.localizedMessage, Toast.LENGTH_SHORT)
                                .show()
                        })
                }
            }
        }, intentFilter)
    }

    fun saveToDatabase(): Observable<Unit> {
        return Observable.fromCallable {
            val database = Room.databaseBuilder(
                applicationContext,
                AppDatabase::class.java, AppDatabase.NAME
            ).build()
            //check if session exists
            val history = database.historyDao().findBySession(mSessionId)
            val distance = LocationHelper.distance(mPoints)
            val time = LocationHelper.timeInSeconds(mPoints)
            val newHistory = History(mSessionId, mPoints, distance, distance / time)
            if (history != null) { //the session exists
                database.historyDao().update(newHistory)
            } else { //history does not exist
                database.historyDao().insertAll(newHistory)
            }
        }
    }
}