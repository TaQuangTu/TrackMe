package com.example.trackme.activities

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.trackme.R
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import kotlinx.android.synthetic.main.view_history.fragmentMap

class TestMapActivity : AppCompatActivity(), OnMapReadyCallback {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_test_map)
        (fragmentMap as SupportMapFragment).getMapAsync(this)
    }

    override fun onMapReady(p0: GoogleMap?) {
        p0?.addMarker(MarkerOptions().position(LatLng(10.7947129,106.6669183)))
    }
}