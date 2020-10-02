package com.example.trackme.fragments

import android.Manifest
import android.app.Activity.RESULT_CANCELED
import android.app.Activity.RESULT_OK
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
import androidx.core.text.HtmlCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.room.Room
import com.example.trackme.R
import com.example.trackme.data.AppDatabase
import com.example.trackme.data.Point
import com.example.trackme.dialogs.LoadingDialog
import com.example.trackme.dialogs.MessageDialog
import com.example.trackme.services.LocationService
import com.example.trackme.services.LocationService.Companion.ACTION_ACTIVITY_CONTROL_CHANGE
import com.example.trackme.services.LocationService.Companion.ACTION_LOCATION_UPDATE
import com.example.trackme.services.LocationService.Companion.ACTION_PAUSE
import com.example.trackme.services.LocationService.Companion.ACTION_RESPONSE_FOR_ASKING_RECORD_STATE
import com.example.trackme.services.LocationService.Companion.ACTION_RESPONSE_FOR_SAVING_REQUEST
import com.example.trackme.services.LocationService.Companion.ACTION_RESUME
import com.example.trackme.services.LocationService.Companion.ACTION_STOP
import com.example.trackme.services.LocationService.Companion.EXTRA_MESSAGE
import com.example.trackme.services.LocationService.Companion.LAT
import com.example.trackme.services.LocationService.Companion.LNG
import com.example.trackme.services.LocationService.Companion.SAVING_RESULT
import com.example.trackme.services.LocationService.Companion.TIME
import com.example.trackme.utils.LocationHelper
import com.example.trackme.utils.TimerHelper
import com.example.trackme.utils.ViewHelper
import com.example.trackme.viewmodels.RecordViewModel
import com.example.trackme.viewmodels.RecordViewModel.Companion.STATE_PAUSE
import com.example.trackme.viewmodels.RecordViewModel.Companion.STATE_RECORDING
import com.example.trackme.viewmodels.RecordViewModel.Companion.STATE_STOPPED
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
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers
import kotlinx.android.synthetic.main.fragment_record.*
import java.util.*

class RecordFragment : Fragment(), OnMapReadyCallback, View.OnClickListener {

    companion object {
        const val REQUEST_PERMISSION_CODE = 3
        const val SESSION_ID = "SESSION_ID"
        const val ACTION_ASK_FOR_RUNNING_STATE = "ACTION_ASK_FOR_RUNNING_STATE"
        const val RECORD_STATE = "RECORD_STATE"
        fun newInstance() = RecordFragment()
    }

    private lateinit var mFragmentMap: SupportMapFragment
    private lateinit var mViewModel: RecordViewModel
    private val mMessageDialog = MessageDialog()
    private var mLocationBroadcastReceiver: BroadcastReceiver? = null
    private var mGoogleMap: GoogleMap? = null
    private val mLoadingDialog: LoadingDialog = LoadingDialog("Loading session")
    private var mNeedToBoundMap = true
    private var mGotoSettings = false
    private var mUserAllowGPS = true
    private var mDisposables = CompositeDisposable()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_record, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        mViewModel = ViewModelProvider(this).get(RecordViewModel::class.java)
        mFragmentMap = childFragmentManager.findFragmentByTag("fragmentMap") as SupportMapFragment
        mFragmentMap.getMapAsync(this)

        observeDataChanges()

        imvPause.setOnClickListener(this)
        imvResume.setOnClickListener(this)
        imvStop.setOnClickListener(this)
        imvBoundMap.setOnClickListener(this)
    }

    fun observeDataChanges() {
        mViewModel.mNewPoint.observe(viewLifecycleOwner,
            Observer<Point> {
                onNewPoint(it)
            })
        mViewModel.mPointArray.observe(viewLifecycleOwner,
            Observer<ArrayList<Point>> {
                presentData()
            })
        val disposable = TimerHelper.getObservableTime().subscribeOn(Schedulers.computation())
            .observeOn(AndroidSchedulers.mainThread()).subscribe(
                {
                    presentSessionInfo()
                },
                {
                    Toast.makeText(context!!, it.localizedMessage, Toast.LENGTH_SHORT).show()
                })
        mDisposables.add(disposable)
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
            addAction(ACTION_RESPONSE_FOR_SAVING_REQUEST)
        }
        mLocationBroadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent != null) {
                    if (intent.action == ACTION_LOCATION_UPDATE) {
                        hideLoading() //hide "FINDING YOUR POSITION" dialog
                        val lat = intent.getDoubleExtra(LAT, 0.0)
                        val lng = intent.getDoubleExtra(LNG, 0.0)
                        val time = intent.getLongExtra(TIME, 0)
                        val point = Point(time, lat, lng)
                        mViewModel.mPointArray.value!!.add(point)
                        mViewModel.mNewPoint.value = point
                    } else if (intent.action == ACTION_RESPONSE_FOR_ASKING_RECORD_STATE) {
                        mViewModel.mRecordState.value =
                            intent.getIntExtra(RECORD_STATE, STATE_RECORDING)
                        mViewModel.mSessionId.value = intent.getStringExtra(SESSION_ID)

                        //reload current state
                        loadHistory(mViewModel.mSessionId.value!!)
                    }
                    if (intent.action == ACTION_RESPONSE_FOR_SAVING_REQUEST) {
                        hideLoading()
                        val savingResult = intent.getBooleanExtra(SAVING_RESULT, false)
                        if (savingResult) {
                            Toast.makeText(context!!, "SAVING DONE", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(
                                context!!, "SAVING ERROR" + intent.getStringExtra(
                                    EXTRA_MESSAGE
                                ), Toast.LENGTH_SHORT
                            ).show()
                        }
                        activity?.finish()
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
            showLoading("Finding your position...")
            Intent(context!!, LocationService::class.java).also {
                mViewModel.mSessionId.value = UUID.randomUUID().toString()
                it.putExtra(SESSION_ID, mViewModel.mSessionId.value)
                ContextCompat.startForegroundService(context!!, it)
            }
            mViewModel.mRecordState.value = STATE_RECORDING
        } else { //ask for state of running service (pause or running)
            showLoading("Loading running session")
            context!!.sendBroadcast(Intent().apply {
                action = ACTION_ASK_FOR_RUNNING_STATE
            })
        }
        presentData()
    }

    override fun onResume() {
        super.onResume()
        if (isLocationPermissionGuaranteed()) {
            if (!mUserAllowGPS) { //location permission is allowed but user does not allow local GPS turning ON
                showTurnOnLocationRequest()
            } else {
                checkLocationSettingsAndStart() //check if internet, GPS are turned ON and start immediately if they are
            }

        } else {
            requestLocationPermission()
        }
        presentData()
    }

    private fun presentData() {
        presentSessionInfo()
        presentMap()
    }

    //distance, duration, velocity
    private fun presentSessionInfo() {
        if (mViewModel.mRecordState.value == STATE_RECORDING) {
            lnActions.visibility = GONE
            imvPause.visibility = VISIBLE
        } else if (mViewModel.mRecordState.value == STATE_PAUSE) {
            lnActions.visibility = VISIBLE
            imvPause.visibility = GONE
        } else { //STOPPED
            lnActions.visibility = GONE
            imvPause.visibility = GONE
        }
        val distance = LocationHelper.distance(mViewModel.mPointArray.value!!)
        val time = TimerHelper.getCurrentTimeInSecond()
        val velocity =
            (distance / time.toFloat() * 100).toInt() / 100f //get two digit after decimal point
        tvDistance.text = HtmlCompat.fromHtml(
            "<b>Distance</b><br>" + distance + " meters",
            HtmlCompat.FROM_HTML_MODE_LEGACY
        )
        tvVelocity.text = HtmlCompat.fromHtml(
            "<b>Velocity</b><br>" + velocity + " m/s",
            HtmlCompat.FROM_HTML_MODE_LEGACY
        )
        tvTime.text = HtmlCompat.fromHtml(
            "<b>Duration</b><br>" + time / 3600 + ":" + (time % 3600) / 60 + ":" + (time % 3600) % 60,
            HtmlCompat.FROM_HTML_MODE_LEGACY
        )
    }

    fun presentMap() {
        if (mGoogleMap != null && !mViewModel.mPointArray.value.isNullOrEmpty()) {
            mGoogleMap?.let {
                it.clear()
                val latlngs = LocationHelper.pointsToLatLngs(mViewModel.mPointArray.value!!, true)
                it.addPolyline(PolylineOptions().addAll(latlngs).color(Color.BLUE).width(7f))
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

                //bound map
                if (mNeedToBoundMap && mViewModel.mPointArray.value!!.size > 0) { //assure that map is bound when only if data is not empty
                    val bound = LocationHelper.getBound(mViewModel.mPointArray.value!!)
                    val mapPadding = 40
                    it.moveCamera(CameraUpdateFactory.newLatLngBounds(bound, mapPadding))
                    mNeedToBoundMap = false
                }
            }
        }
    }

    private fun checkLocationSettingsAndStart() {
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
                    startIntentSenderForResult(
                        exception.resolution.intentSender,
                        REQUEST_PERMISSION_CODE,
                        null,
                        0,
                        0,
                        0,
                        null
                    )
                } catch (sendEx: IntentSender.SendIntentException) {
                    // Ignore the error.
                }
            }
        }
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_PERMISSION_CODE) {
            if (resultCode == RESULT_OK) {
                mUserAllowGPS = true //user allow turning on GPS
            } else if (resultCode == RESULT_CANCELED) {
                mUserAllowGPS = false
            }
        }
    }

    private fun showTurnOnLocationRequest() {
        mMessageDialog.apply {
            setTitle("Location")
            setContent("You must turn on GPS to start tracking")
            setShowActionButton(true)
            setActionNextMessage("Continue")
            setCancelMessage("Exit")
            setListener(object : MessageDialog.ActionClickListener {
                override fun onActionClicked(action: Int) {
                    if (action == MessageDialog.ACTION_CANCEL) {
                        mUserAllowGPS = false
                        activity!!.finish()
                    } else if (action == MessageDialog.ACTION_NEXT) {
                        mUserAllowGPS = true
                        checkLocationSettingsAndStart()
                    }
                }
            })
        }
        mMessageDialog.show(childFragmentManager, MessageDialog.javaClass.name)
    }

    private fun showPermissionExplanationDialog() {
        mMessageDialog.apply {
            setTitle("Message")
            setContent("We need Location permission to track your workout session. Open settings and guarantee the permission?")
            setShowActionButton(true)
            setActionNextMessage("Go to settings")
            setCancelMessage("Exit")
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
        mMessageDialog.show(childFragmentManager, MessageDialog.javaClass.name)
    }

    private fun showExitMessage() {
        mMessageDialog.apply {
            setTitle("Attention!!!")
            setContent(this@RecordFragment.context!!.getString(R.string.exit_message))
            setShowActionButton(true)
            setActionNextMessage("I'm OK")
            setCancelMessage("Cancel")
            setListener(object : MessageDialog.ActionClickListener {
                override fun onActionClicked(action: Int) {
                    if (action == MessageDialog.ACTION_NEXT) {
                        activity!!.finish()
                    }
                }
            })
        }
        mMessageDialog.show(childFragmentManager, MessageDialog.javaClass.name)
    }

    fun goToAppSettings() {
        mGotoSettings = true
        val intent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", context!!.packageName, null)
        )
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
    }

    override fun onMapReady(p0: GoogleMap?) {
        mGoogleMap = p0
    }

    override fun onClick(v: View?) {
        if (v == imvPause) {
            mViewModel.mRecordState.value = STATE_PAUSE
            sendActionBroadcast(ACTION_PAUSE)
            presentSessionInfo()
        } else if (v == imvResume) {
            mViewModel.mRecordState.value = STATE_RECORDING
            showLoading("Finding your position....")
            sendActionBroadcast(ACTION_RESUME)
            presentSessionInfo()
        } else if (v == imvStop) {
            mViewModel.mRecordState.value = STATE_STOPPED
            showLoading("Saving session...")
            sendActionBroadcast(ACTION_STOP)
            presentSessionInfo()
        } else if (v == imvBoundMap) {
            mNeedToBoundMap = true
            presentMap()
        }
    }

    private fun sendActionBroadcast(newAction: Int) {
        context!!.sendBroadcast(Intent().apply {
            action = ACTION_ACTIVITY_CONTROL_CHANGE
            putExtra(RECORD_STATE, newAction)
        })
    }

    fun loadHistory(sessionId: String) {
        showLoading("Loading data from previous session")
        Observable.fromCallable {
            Room.databaseBuilder(
                context!!,
                AppDatabase::class.java, AppDatabase.NAME
            ).build().historyDao().findBySession(sessionId)
        }.observeOn(AndroidSchedulers.mainThread())
            .subscribeOn(Schedulers.newThread())
            .subscribe({
                mViewModel.mPointArray.value =
                    LocationHelper.stringToArrayList(it.points)
                hideLoading()
            }, {
                Toast.makeText(context!!, it.localizedMessage, Toast.LENGTH_SHORT)
                    .show()
                hideLoading()
            })
    }

    fun showLoading(text: String = "Loading...") {
        if (isAdded) { //make sure RecordFragment has been attached to activity, so childFragmentManager will be available
            mLoadingDialog.show(childFragmentManager, "LOADING_DIALOG", text)
        }
    }

    fun hideLoading() {
        if (isAdded) {//make sure RecordFragment has been attached to activity, so childFragmentManager will be available
            mLoadingDialog.dismissNow(childFragmentManager)
        }
    }

    fun onBackPressed() {
        if (mViewModel.mRecordState.value == STATE_RECORDING) {
            showExitMessage()
        } else {
            activity!!.finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mLocationBroadcastReceiver?.let {
            context!!.unregisterReceiver(it)
        }
        mDisposables.dispose()
    }
}