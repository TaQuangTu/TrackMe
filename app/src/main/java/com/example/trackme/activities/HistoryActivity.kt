package com.example.trackme.activities

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.trackme.R
import com.example.trackme.fragments.HistoryFragment

class HistoryActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.history_activity)
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.container, HistoryFragment.newInstance())
                .commitNow()
        }
    }
}