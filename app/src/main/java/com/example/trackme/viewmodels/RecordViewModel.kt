package com.example.trackme.viewmodels

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.trackme.data.Point

class RecordViewModel : ViewModel() {
    companion object{
        val STATE_NONE = 0
        val STATE_PAUSE = 1
        val STATE_RECORDING = 2
    }
    var mRecordState = MutableLiveData<Int>(STATE_NONE)
    var mPointArray = MutableLiveData<ArrayList<Point>>()
    var mNewPoint = MutableLiveData<Point>()
}