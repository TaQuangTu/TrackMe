package com.example.trackme

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.trackme.ui.main.RecordFragment

class RecordActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.record_activity)
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.container, RecordFragment.newInstance())
                .commitNow()
        }
    }
}