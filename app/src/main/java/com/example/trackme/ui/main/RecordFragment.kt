package com.example.trackme.ui.main

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.trackme.MessageDialog
import com.example.trackme.R
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import kotlinx.android.synthetic.main.main_fragment2.*

class RecordFragment : Fragment(), OnMapReadyCallback {

    companion object {
        val REQUEST_PERMISSION_CODE = 3
        fun newInstance() = RecordFragment()
    }

    private lateinit var fragmentMap: SupportMapFragment
    private lateinit var viewModel: RecordViewModel
    private var messageDialog: MessageDialog? = null

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
    }

    override fun onResume() {
        super.onResume()
        if (requestLocationPermission()) { //location permission granted
            presentData()
        }
    }

    private fun presentData() {

    }

    private fun requestLocationPermission(): Boolean {
        if (ContextCompat.checkSelfPermission(
                context!!,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PERMISSION_GRANTED
        ) {
            if (shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)) {
                showPermissionExplainationDialog()
            } else {
                requestPermissions(
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    REQUEST_PERMISSION_CODE
                )
            }

            return false
        }
        return true
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        //just use one-time checking because we request location permision only
        if (grantResults.isNotEmpty() && grantResults[0] == PERMISSION_GRANTED) {
            presentData()
        } else {
            //it means that user checked "dont ask again"
            if (shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)) {
                showPermissionExplainationDialog()
            } else {
                activity!!.finish()
            }
        }
    }

    private fun showPermissionExplainationDialog() {
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

    override fun onMapReady(p0: GoogleMap?) {

    }
}