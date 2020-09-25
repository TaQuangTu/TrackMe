package com.example.trackme.fragments

import android.Manifest
import android.app.ActivityManager
import android.content.*
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.example.trackme.dialogs.MessageDialog
import com.example.trackme.R
import com.example.trackme.data.Point
import com.example.trackme.viewmodels.RecordViewModel
import com.example.trackme.services.LocationService
import com.example.trackme.services.LocationService.Companion.LAT
import com.example.trackme.services.LocationService.Companion.LNG
import com.example.trackme.services.LocationService.Companion.ACTION_LOCATION_UPDATE
import com.example.trackme.services.LocationService.Companion.TIME
import com.example.trackme.viewmodels.RecordViewModel.Companion.STATE_PAUSE
import com.example.trackme.viewmodels.RecordViewModel.Companion.STATE_RECORDING
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.tasks.Task
import kotlinx.android.synthetic.main.main_fragment2.*
import java.util.*

class RecordFragment : Fragment(), OnMapReadyCallback {

    companion object {
        val REQUEST_PERMISSION_CODE = 3
        val SESSION_ID = "SESSION_ID"
        fun newInstance() = RecordFragment()
    }

    private lateinit var fragmentMap: SupportMapFragment
    private lateinit var viewModel: RecordViewModel
    private var messageDialog: MessageDialog? = null
    private lateinit var mLocationBroadcastReceiver: BroadcastReceiver
    private var mGoogleMap: GoogleMap? = null
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.main_fragment2, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProvider(this).get(RecordViewModel::class.java)
        fragmentMap = childFragmentManager.findFragmentByTag("fragmentMap") as SupportMapFragment
        fragmentMap.getMapAsync(this)
        observeViewModel()
        if (isFineLocationPermissionGuaranteed()) {
            checkLocalSettings() //check if "Your Location" turned on
        } else {
            requestFineLocationPermission()
        }
    }

    fun observeViewModel() {
        viewModel.mNewPoint.observe(viewLifecycleOwner,
            Observer<Point> {
                drawNewPoint(it)
            })
    }

    private fun drawNewPoint(point: Point) {
        mGoogleMap?.let {
            val latlng = LatLng(point.lat, point.lng)
            val markerOptions = MarkerOptions().position(latlng)
            it.addMarker(markerOptions)
            it.moveCamera(CameraUpdateFactory.newLatLngZoom(latlng, 14f))
        }
    }

    private fun requestFineLocationPermission() {
        //it means that user checked "don't ask again"
        if (shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)) {
            showPermissionExplanationDialog()
        } else {
            requestPermissions(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                REQUEST_PERMISSION_CODE
            )
        }
    }

    private fun isFineLocationPermissionGuaranteed(): Boolean {
        return ContextCompat.checkSelfPermission(
            context!!,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PERMISSION_GRANTED
    }

    private fun registReceiver() {
        val filter = IntentFilter().apply {
            addAction(ACTION_LOCATION_UPDATE)
        }
        mLocationBroadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent != null && intent.action == ACTION_LOCATION_UPDATE) {
                    val lat = intent.getDoubleExtra(LAT, 0.0)
                    val lng = intent.getDoubleExtra(LNG, 0.0)
                    val time = intent.getLongExtra(TIME, 0)
                    val sessionId = intent.getStringExtra(SESSION_ID)
                    val point = Point(time, lat, lng, sessionId!!)
                    viewModel.mNewPoint.value = point
                }
            }
        }
        context!!.registerReceiver(mLocationBroadcastReceiver, filter)
    }

    private fun isMyServiceRunning(serviceClass: Class<*>): Boolean {
        val manager = context!!.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        return manager.getRunningServices(Integer.MAX_VALUE)
            .any { it.service.className == serviceClass.name }
    }

    private fun startLocationTracking() {
        registReceiver()
        if (!isMyServiceRunning(LocationService::class.java)) {
            Intent(context!!, LocationService::class.java).also {
                it.putExtra(SESSION_ID, UUID.randomUUID().toString())
                ContextCompat.startForegroundService(context!!, it)
            }
        }
        else{ //ask for state of running service (pause or running)

        }
        presentData()
    }

    override fun onResume() {
        super.onResume()
        presentData()
    }

    private fun presentData() {
        if (viewModel.mRecordState.value == STATE_RECORDING) {
            lnActions.visibility = GONE
            imvPause.visibility = VISIBLE
        } else if (viewModel.mRecordState.value == STATE_PAUSE) {
            lnActions.visibility = VISIBLE
            imvPause.visibility = GONE
        } else { //state NONE
            lnActions.visibility = GONE
            imvPause.visibility = GONE
        }
    }

    private fun checkLocalSettings() {
        val locationRequest = LocationRequest.create().apply {
            interval = LocationService.LOCATION_REQUEST_INTERVAL
            fastestInterval = LocationService.LOCATION_REQUEST_FASTEST_INTERVAL
            priority = LocationService.LOCATION_REQUEST_PRIORITY
        }

        val builder = LocationSettingsRequest.Builder()
        builder.addLocationRequest(locationRequest)
        val client: SettingsClient = LocationServices.getSettingsClient(context!!)
        val task: Task<LocationSettingsResponse> = client.checkLocationSettings(builder.build())
        task.addOnSuccessListener { locationSettingsResponse ->
            startLocationTracking()
        }
        task.addOnFailureListener { exception ->
            if (exception is ResolvableApiException) {
                // Location settings are not satisfied, but this can be fixed
                // by showing the user a dialog.
                try {
                    // Show the dialog by calling startResolutionForResult(),
                    // and check the result in onActivityResult().
                    exception.startResolutionForResult(
                        this@RecordFragment.activity,
                        REQUEST_PERMISSION_CODE
                    )
                } catch (sendEx: IntentSender.SendIntentException) {
                    // Ignore the error.
                }
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        //just use one-time checking because we request location permision only
        if (grantResults.isNotEmpty() && grantResults[0] == PERMISSION_GRANTED) {
            checkLocalSettings() //check if "My Location" feature is on
        } else {
            //it means that user checked "dont ask again"
            if (shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)) {
                showPermissionExplanationDialog()
            } else {
                activity!!.finish()
            }
        }
    }

    private fun showPermissionExplanationDialog() {
        if (messageDialog == null) {
            messageDialog = MessageDialog().apply {
                setTitle("Message")
                setContent("We need location permission for tracking your location")
                setShowActionButton(true)
                setListener(object : MessageDialog.ActionClickListener {
                    override fun onActionClicked(action: Int) {
                        if (action == MessageDialog.ACTION_CANCEL) {
                            activity!!.finish()
                        } else if (action == MessageDialog.ACTION_NEXT) {
                            goToAppSettings()
                        }
                    }
                })
            }
        }
        messageDialog!!.show(childFragmentManager, MessageDialog.javaClass.name)
    }

    fun goToAppSettings() {
        val intent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", context!!.packageName, null)
        )
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        context!!.unregisterReceiver(mLocationBroadcastReceiver)
    }

    override fun onMapReady(p0: GoogleMap?) {
        mGoogleMap = p0
    }
}