package com.example.trackme.dialogs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import com.example.trackme.R
import kotlinx.android.synthetic.main.fragment_dialog_message.*

class MessageDialog : DialogFragment() {
    private var mTitle: String = "Title"
    private var mContent: String = "Content"
    private var mCancelMessage = "Cancel"
    private var mShowActionNextButton = false
    private var mActionNextMessage = "OK"
    private var mListener: ActionClickListener? = null
    private var mIsShowing = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isCancelable = false
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_dialog_message, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        btnCancel.setOnClickListener{
            mListener?.onActionClicked(ACTION_CANCEL)
            dismiss()
        }
        btnActionNext.setOnClickListener{
            mListener?.onActionClicked(ACTION_NEXT)
            dismiss()
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    }
    override fun onResume() {
        super.onResume()
        tvTitle.text = mTitle
        tvMessage.text = mContent
        btnCancel.visibility = if (mShowActionNextButton) VISIBLE else GONE
        btnCancel.text = mCancelMessage
        btnActionNext.text = mActionNextMessage
    }

    fun setTitle(title: String) {
        mTitle = title
    }

    fun setContent(content: String) {
        mContent = content
    }

    fun setShowActionButton(showActionNext: Boolean) {
        mShowActionNextButton = showActionNext
    }
    fun setListener(listener: ActionClickListener){
        mListener = listener
    }

    companion object {
        val ACTION_CANCEL = 0
        val ACTION_NEXT = 1
    }

    fun setActionNextMessage(message: String) {
        mActionNextMessage = message
    }

    fun setCancelMessage(message: String) {
        mCancelMessage = message
    }

    override fun show(manager: FragmentManager, tag: String?) {
        if (!mIsShowing) {
            manager.beginTransaction().add(this,tag).commitNowAllowingStateLoss()
            mIsShowing = true
        }
    }

    override fun dismiss() {
        super.dismiss()
        mIsShowing = false
    }

    interface ActionClickListener {
        fun onActionClicked(action: Int)
    }
}