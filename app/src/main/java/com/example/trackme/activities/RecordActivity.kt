package com.example.trackme.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.trackme.R
import com.example.trackme.fragments.RecordFragment

class RecordActivity : AppCompatActivity() {
    val mFragment = RecordFragment.newInstance()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.record_activity)
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.container, mFragment)
                .commitNow()
        }
    }

    override fun onBackPressed() {
        mFragment.onBackPressed()
    }
}