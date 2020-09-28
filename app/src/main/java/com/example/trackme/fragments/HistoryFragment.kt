package com.example.trackme.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.trackme.R
import com.example.trackme.activities.RecordActivity
import com.example.trackme.adapters.HistoriesAdapter
import com.example.trackme.data.History
import com.example.trackme.viewmodels.HistoriesViewModel
import kotlinx.android.synthetic.main.fragment_history.*

class HistoryFragment : Fragment() {

    companion object {
        fun newInstance() = HistoryFragment()
    }

    private lateinit var mViewModel: HistoriesViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_history, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        btnRecord.setOnClickListener {
            startActivity(Intent(context!!, RecordActivity::class.java))
        }
        swipeRefreshLayout.setOnRefreshListener {
            fetchData()
        }
        rcvHistories.layoutManager = LinearLayoutManager(context!!, RecyclerView.VERTICAL, false)
        rcvHistories.adapter = HistoriesAdapter()
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        mViewModel = ViewModelProvider(this).get(HistoriesViewModel::class.java)
        observeViewModel()
        fetchData()
    }

    fun observeViewModel(){
        mViewModel.mHistories.observe(viewLifecycleOwner,
            Observer<List<History>> { presentData() })
        mViewModel.mLiveMessage.observe(viewLifecycleOwner,Observer<String>{
            Toast.makeText(context!!,it,Toast.LENGTH_SHORT).show()
        })
    }
    override fun onResume() {
        super.onResume()
        fetchData()
        presentData()
    }

    fun fetchData() {
        mViewModel.loadAllHistory()
    }

    fun presentData() {
        swipeRefreshLayout.isRefreshing = false
        if (mViewModel.mHistories.value.isNullOrEmpty()) {
            tvHistoryEmptyMessage.visibility = VISIBLE
            swipeRefreshLayout.visibility = GONE
        }
        else{
            (rcvHistories.adapter as HistoriesAdapter).mData = mViewModel.mHistories.value?.reversed()
            tvHistoryEmptyMessage.visibility = GONE
            swipeRefreshLayout.visibility = VISIBLE
        }
    }
}