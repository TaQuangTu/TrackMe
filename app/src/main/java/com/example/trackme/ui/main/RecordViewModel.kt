package com.example.trackme.ui.main

import androidx.lifecycle.ViewModel

class RecordViewModel : ViewModel() {
    companion object{
        val STATE_NONE = 0
        val STATE_PAUSE = 1
        val STATE_RECORDING = 2
    }
    var mRecordState:Int = STATE_NONE
}