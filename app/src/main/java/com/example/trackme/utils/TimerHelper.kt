package com.example.trackme.utils

import io.reactivex.rxjava3.core.Observable
import java.util.*
import java.util.concurrent.TimeUnit

object TimerHelper {
    private var mTimer: Timer? = null
    private var mCurrentTimeInSecond = 0L
    private var mIsRunning = false
    fun getCurrentTimeInSecond():Long{
        return mCurrentTimeInSecond
    }
    fun continueTimer() {
        if(mIsRunning) return //do nothing if the timer is running now
        mTimer?.cancel()
        mTimer = Timer()
        mTimer!!.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                mCurrentTimeInSecond+=1
            }
        }, 0, 1000)
        mIsRunning = true
    }

    fun pauseTimer() {
        mTimer?.cancel()
        mIsRunning = false
    }

    fun resetTimer() {
        mTimer?.cancel()
        mCurrentTimeInSecond = 0L
        mIsRunning = false
    }

    fun resetTimerAndStart(){
        resetTimer()
        continueTimer()
        mIsRunning = true
    }

    fun getObservableTime(): Observable<Long>{
        return Observable.interval(1,TimeUnit.SECONDS).map { mCurrentTimeInSecond }
    }
}