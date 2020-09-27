package com.example.trackme.dialogs

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import com.example.trackme.R
import kotlinx.android.synthetic.main.dialog_loading.*

class LoadingDialog(var message:String) : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        var alertDialogBuilder = AlertDialog.Builder(context!!)
        alertDialogBuilder.setView(R.layout.dialog_loading)
        return alertDialogBuilder.create()
    }

    override fun onResume() {
        super.onResume()
        tvMessage.text = message
    }
}