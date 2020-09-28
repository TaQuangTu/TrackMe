package com.example.trackme.dialogs

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import com.example.trackme.R

class LoadingDialog(var mMessage: String) : DialogFragment() {
    var mIsShowing = false
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        var alertDialogBuilder = AlertDialog.Builder(context!!)
        alertDialogBuilder.setView(R.layout.dialog_loading)
        alertDialogBuilder.setCancelable(false)
        return alertDialogBuilder.create()
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setBackgroundDrawable(null)
    }

    override fun onResume() {
        super.onResume()
        (dialog?.findViewById(R.id.tvMessage) as TextView).text = mMessage
    }

    fun show(fragmentManager: FragmentManager, tag: String, message: String) {
        mMessage = message
        if (!mIsShowing) {
            fragmentManager.beginTransaction().add(this,tag).commitNowAllowingStateLoss()
            mIsShowing = true
        }
    }

    fun dismissNow(childFragmentManager: FragmentManager) {
        if (mIsShowing) {
            childFragmentManager.beginTransaction().remove(this).commitNowAllowingStateLoss()
            mIsShowing = false
        }
    }
}