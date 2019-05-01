package com.fuxy.cookeasy.adapter

import android.graphics.drawable.BitmapDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.fuxy.cookeasy.R
import com.fuxy.cookeasy.entity.Step

class StepAdapter(private val steps: List<Step>) : RecyclerView.Adapter<StepAdapter.ViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.step_item, parent, false)
        return ViewHolder(itemView)
    }

    override fun getItemCount(): Int {
        return steps.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = steps[position]

        holder.stepNumberTextView?.text = item.stepNumber.toString()
        holder.stepTextView?.text = item.description

        if (item.stepBucketImage != null) {
            holder.stepTextView?.setCompoundDrawablesRelativeWithIntrinsicBounds(
                null, null, null, BitmapDrawable(holder.itemView.resources, item.stepBucketImage.bitmap)
            )
        }
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var stepNumberTextView: TextView? = null
        var stepTextView: TextView? = null

        init {
            stepNumberTextView = itemView.findViewById(R.id.tv_step_number)
            stepTextView = itemView.findViewById(R.id.tv_step)
        }
    }
}