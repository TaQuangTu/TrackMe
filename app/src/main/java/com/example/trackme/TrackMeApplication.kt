package com.example.trackme

import android.app.Application
import android.content.Context

class TrackMeApplication : Application() {
    companion object{
        var appContext : Context? = null
    }

    override fun onCreate() {
        super.onCreate()
        appContext = this
    }
}