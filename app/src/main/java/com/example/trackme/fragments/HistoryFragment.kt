package com.example.trackme.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import kotlinx.android.synthetic.main.main_fragment.*

class HistoryFragment : Fragment() {

    companion object {
        fun newInstance() = HistoryFragment()
    }

    private lateinit var viewModel: HistoriesViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.main_fragment, container, false)
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
        viewModel = ViewModelProvider(this).get(HistoriesViewModel::class.java)
        viewModel.histories.observe(viewLifecycleOwner,
            Observer<List<History>> { presentData() })
        fetchData()
    }

    override fun onResume() {
        super.onResume()
        fetchData()
        presentData()
    }

    fun fetchData() {
        viewModel.loadAllHistory()
    }

    fun presentData() {
        swipeRefreshLayout.isRefreshing = false
        (rcvHistories.adapter as HistoriesAdapter).mData = viewModel.histories.value
    }
}