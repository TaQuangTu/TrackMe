package com.example.trackme.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.text.HtmlCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.trackme.R
import com.example.trackme.data.History
import com.example.trackme.utils.LocationHelper
import kotlinx.android.synthetic.main.view_history.view.*

class HistoriesAdapter : RecyclerView.Adapter<HistoriesAdapter.HistoryViewHolder>() {
    var mData: List<History>? = null
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    class HistoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.view_history, parent, false)
        return HistoryViewHolder(view)
    }

    override fun getItemCount(): Int {
        return mData?.size ?: 0
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        val history = mData!![position]
        history?.let {
            Glide.with(holder.itemView.context).load(LocationHelper.getStaticMapUrl(history))
                .placeholder(R.drawable.place_holder_map)
                .into(holder.itemView.imvMap)
            val distance = LocationHelper.distance(history.points)
            val time = LocationHelper.timeInSeconds(history.points)
            val velocity = LocationHelper.avgVelocity(history.points)

            holder.itemView.tvDistance.text =
                HtmlCompat.fromHtml(
                    "<b>Distance</b><br>" + distance + " meters",
                    HtmlCompat.FROM_HTML_MODE_LEGACY
                )
            holder.itemView.tvVelocity.text = HtmlCompat.fromHtml(
                "<b>Velocity</b><br>" + velocity + " m/s",
                HtmlCompat.FROM_HTML_MODE_LEGACY
            )
            holder.itemView.tvTime.text = HtmlCompat.fromHtml(
                "<b>Duration</b><br>" + time / 3600 + ":" + (time % 3600) / 60 + ":" + (time % 3600) % 60,
                HtmlCompat.FROM_HTML_MODE_LEGACY
            )
        }
    }
}