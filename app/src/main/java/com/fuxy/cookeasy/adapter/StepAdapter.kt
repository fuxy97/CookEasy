package com.fuxy.cookeasy.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
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
        holder.stepDescriptionTextView?.text = item.description

        if (item.stepBucketImage != null) {
            holder.stepImageView?.setImageBitmap(item.stepBucketImage.bitmap)
            /*holder.stepTextView?.setCompoundDrawablesRelativeWithIntrinsicBounds(
                null, null, null, BitmapDrawable(holder.itemView.resources, item.stepBucketImage.bitmap)
            )*/
        }
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var stepNumberTextView: TextView? = null
        var stepDescriptionTextView: TextView? = null
        var stepImageView: ImageView? = null

        init {
            stepNumberTextView = itemView.findViewById(R.id.tv_step_number)
            stepDescriptionTextView = itemView.findViewById(R.id.tv_step_description)
            stepImageView = itemView.findViewById(R.id.iv_step_image)
        }
    }
}