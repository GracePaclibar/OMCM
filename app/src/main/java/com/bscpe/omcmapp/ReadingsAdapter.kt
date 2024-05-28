package com.bscpe.omcmapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ReadingsAdapter(private var readingsList: List<Readings>) : RecyclerView.Adapter<ReadingsAdapter.ReadingViewHolder>() {

    inner class ReadingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val dateView: TextView = itemView.findViewById(R.id.dateTextView)
        val timeView: TextView = itemView.findViewById(R.id.timeTextView)
        val internalTempView: TextView = itemView.findViewById(R.id.internalTempTextView)
        val externalTempView: TextView = itemView.findViewById(R.id.externalTempTextView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReadingViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_reading, parent, false)
        return ReadingViewHolder(view)
    }

    override fun onBindViewHolder(holder: ReadingViewHolder, position: Int){
        val currentItem = readingsList[position]
        holder.dateView.text = currentItem.date
        holder.timeView.text = currentItem.time
        holder.internalTempView.text = currentItem.internalTemperature.toString()
        holder.externalTempView.text = currentItem.externalTemperature.toString()
    }

    override fun getItemCount() = readingsList.size

    fun updateList(newList: List<Readings>) {
        readingsList = newList
        notifyDataSetChanged()
    }
}