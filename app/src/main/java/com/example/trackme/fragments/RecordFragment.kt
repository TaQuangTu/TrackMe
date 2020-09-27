package com.example.trackme.fragments

import android.Manifest
import android.app.ActivityManager
import android.content.*
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.room.Room
import com.example.trackme.R
import com.example.trackme.data.AppDatabase
import com.example.trackme.data.History
import com.example.trackme.data.Point
import com.example.trackme.dialogs.MessageDialog
import com.example.trackme.services.LocationService
import com.example.trackme.services.LocationService.Companion.ACTION_ACTIVITY_CONTROL_CHANGE
import com.example.trackme.services.LocationService.Companion.ACTION_LOCATION_UPDATE
import com.example.trackme.services.LocationService.Companion.ACTION_PAUSE
import com.example.trackme.services.LocationService.Companion.ACTION_RESPONSE_FOR_ASKING_RECORD_STATE
import com.example.trackme.services.LocationService.Companion.ACTION_RESUME
import com.example.trackme.services.LocationService.Companion.ACTION_STOP
import com.example.trackme.services.LocationService.Companion.LAT
import com.example.trackme.services.LocationService.Companion.LNG
import com.example.trackme.services.LocationService.Companion.TIME
import com.example.trackme.utils.LocationHelper
import com.example.trackme.utils.ViewHelper
import com.example.trackme.viewmodels.RecordViewModel
import com.example.trackme.viewmodels.RecordViewModel.Companion.STATE_NONE
import com.example.trackme.viewmodels.RecordViewModel.Companion.STATE_PAUSE
import com.example.trackme.viewmodels.RecordViewModel.Companion.STATE_RECORDING
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.LocationSettingsResponse
import com.google.android.gms.location.SettingsClient
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import com.google.android.gms.tasks.Task
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.schedulers.Schedulers
import kotlinx.android.synthetic.main.main_fragment2.*
import java.util.*

class RecordFragment : Fragment(), OnMapReadyCallback, View.OnClickListener {

    companion object {
        const val REQUEST_PERMISSION_CODE = 3
        const val SESSION_ID = "SESSION_ID"
        const val ACTION_ASK_FOR_RUNNING_STATE = "ACTION_ASK_FOR_RUNNING_STATE"
        const val RECORD_STATE = "RECORD_STATE"
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

        if (isLocationPermissionGuaranteed()) {
            checkLocalSettings() //check if "Your Location" turned on
        } else {
            requestLocationPermission()
        }
        imvPause.setOnClickListener(this)
        imvResume.setOnClickListener(this)
        imvStop.setOnClickListener(this)
    }

    fun observeViewModel() {
        viewModel.mNewPoint.observe(viewLifecycleOwner,
            Observer<Point> {
                onNewPoint(it)
            })
        viewModel.mRecordState.observe(viewLifecycleOwner,
            Observer<Int> {
                presentData()
            })
    }

    private fun onNewPoint(point: Point) {
        presentData()
    }

    private fun requestLocationPermission() {
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

    private fun isLocationPermissionGuaranteed(): Boolean {
        return ContextCompat.checkSelfPermission(
            context!!,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PERMISSION_GRANTED
    }

    private fun registReceivers() {
        val filter = IntentFilter().apply {
            addAction(ACTION_LOCATION_UPDATE)
            addAction(ACTION_RESPONSE_FOR_ASKING_RECORD_STATE)
        }
        mLocationBroadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent != null) {
                    if (intent.action == ACTION_LOCATION_UPDATE) {
                        val lat = intent.getDoubleExtra(LAT, 0.0)
                        val lng = intent.getDoubleExtra(LNG, 0.0)
                        val time = intent.getLongExtra(TIME, 0)
                        val sessionId = intent.getStringExtra(SESSION_ID)
                        val point = Point(time, lat, lng, sessionId!!)
                        viewModel.mPointArray.value!!.add(point)
                        viewModel.mNewPoint.value = point
                    } else if (intent.action == ACTION_RESPONSE_FOR_ASKING_RECORD_STATE) {
                        viewModel.mRecordState.value = intent.getIntExtra(RECORD_STATE, STATE_NONE)
                        viewModel.mSessionId.value = intent.getStringExtra(SESSION_ID)
                        //reload current state
                        loadHistory(viewModel.mSessionId.value!!)
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribeOn(Schedulers.newThread())
                            .subscribe({
                                viewModel.mPointArray.value =
                                    LocationHelper.stringToArrayList(it.points)
                            }, {
                                Toast.makeText(context!!, it.localizedMessage, Toast.LENGTH_SHORT)
                                    .show()
                            })
                    }
                }
            }
        }
        context!!.registerReceiver(mLocationBroadcastReceiver, filter)
    }

    private fun isLocationServiceRunning(serviceClass: Class<*>): Boolean {
        val manager = context!!.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        return manager.getRunningServices(Integer.MAX_VALUE)
            .any { it.service.className == serviceClass.name }
    }

    private fun startLocationTracking() {
        registReceivers()
        if (!isLocationServiceRunning(LocationService::class.java)) { //if service have not run, start new one
            Intent(context!!, LocationService::class.java).also {
                viewModel.mSessionId.value = UUID.randomUUID().toString()
                it.putExtra(SESSION_ID, viewModel.mSessionId.value)
                ContextCompat.startForegroundService(context!!, it)
            }
        } else { //ask for state of running service (pause or running)
            context!!.sendBroadcast(Intent().apply {
                action = ACTION_ASK_FOR_RUNNING_STATE
            })
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
        }
        if (mGoogleMap != null && !viewModel.mPointArray.value.isNullOrEmpty()) {
            mGoogleMap?.let {
                it.clear()
                val latlngs = LocationHelper.pointsToLatLngs(viewModel.mPointArray.value!!)
                val bound = LocationHelper.getBound(viewModel.mPointArray.value!!)
                val mapPadding = 40
                it.addPolyline(PolylineOptions().addAll(latlngs).color(Color.BLUE).width(7f))
                it.moveCamera(CameraUpdateFactory.newLatLngBounds(bound, mapPadding))
                //draw last point
                val lastPointView = ImageView(context!!, null)
                lastPointView.setImageDrawable(
                    ContextCompat.getDrawable(
                        context!!,
                        R.drawable.layer_list_current_location
                    )
                )
                val lastBitmap = ViewHelper.bitmapFromView(lastPointView)
                it.addMarker(
                    MarkerOptions().title("Current Position").position(latlngs[latlngs.size - 1])
                        .anchor(0.5f, 0.5f)
                        .icon(BitmapDescriptorFactory.fromBitmap(lastBitmap))
                )
                //draw first point
                it.addMarker(MarkerOptions().position(latlngs[0]).title("Start Position"))
            }
            val distance = LocationHelper.distance(viewModel.mPointArray.value!!)
            val time = LocationHelper.timeInSeconds(viewModel.mPointArray.value!!)
            val velocity = LocationHelper.avgVelocity(viewModel.mPointArray.value!!)
            tvDistance.text = "Distance\n " + distance + " meters"
            tvVelocity.text = "Velocity\n " + velocity + " m/s"
            tvTime.text =
                "Duration\n" + time / 3600 + ":" + (time % 3600) / 60 + ":" + (time % 3600) % 60
        }

    }

    private fun checkLocalSettings() {
        val locationRequest = LocationService.Companion.LocationUtils.getLocationRequest()

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
                setContent("We need location permission for tracking your location. Open settings and guarantee permission?")
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

    override fun onClick(v: View?) {
        if (v == imvPause) {
            viewModel.mRecordState.value = STATE_PAUSE
            sendActionBroadcast(ACTION_PAUSE)
        } else if (v == imvResume) {
            viewModel.mRecordState.value = STATE_RECORDING
            sendActionBroadcast(ACTION_RESUME)
        } else if (v == imvStop) {
            sendActionBroadcast(ACTION_STOP)
        }
    }

    private fun sendActionBroadcast(newAction: Int) {
        context!!.sendBroadcast(Intent().apply {
            action = ACTION_ACTIVITY_CONTROL_CHANGE
            putExtra(RECORD_STATE, newAction)
        })
    }

    fun loadHistory(sessionId: String): Observable<History> {
        return Observable.fromCallable {
            Room.databaseBuilder(
                context!!,
                AppDatabase::class.java, AppDatabase.NAME
            ).build().historyDao().findBySession(sessionId)
        }
    }
}