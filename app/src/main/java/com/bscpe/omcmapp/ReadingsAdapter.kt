package com.bscpe.omcmapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView

class ReadingsAdapter(private var readingsList: List<Readings>) : RecyclerView.Adapter<ReadingsAdapter.ReadingViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReadingViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_reading, parent, false)
        return ReadingViewHolder(view)
    }

    override fun onBindViewHolder(holder: ReadingViewHolder, position: Int) {
        val currentItem = readingsList[position]
        holder.bind(currentItem, position)
    }

    inner class ReadingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val dateView: TextView = itemView.findViewById(R.id.dateTextView)
        private val timeView: TextView = itemView.findViewById(R.id.timeTextView)
        private val internalTempView: TextView = itemView.findViewById(R.id.internalTempTextView)
        private val externalTempView: TextView = itemView.findViewById(R.id.externalTempTextView)

        fun bind(readings: Readings, position: Int) {
            with(itemView) {
                dateView.text = readings.date
                timeView.text = readings.time
                internalTempView.text = readings.internalTemperature.toString()
                externalTempView.text = readings.externalTemperature.toString()

                if (position % 2 != 0) {
                    setBackgroundColor(ContextCompat.getColor(context, R.color.detail))
                } else {
                    setBackgroundColor(ContextCompat.getColor(context, android.R.color.white))
                }
            }
        }
    }

    override fun getItemCount() = readingsList.size

    fun updateList(newList: List<Readings>) {
        readingsList = newList
        notifyDataSetChanged()
    }
}