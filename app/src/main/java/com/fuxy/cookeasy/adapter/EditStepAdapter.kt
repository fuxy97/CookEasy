package com.fuxy.cookeasy.adapter

import android.content.Context
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.fuxy.cookeasy.R
import com.fuxy.cookeasy.entity.EditStep

abstract class EditStepAdapter(val steps: MutableList<EditStep>) : RecyclerView.Adapter<EditStepAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.edit_step_item, parent, false)
        return ViewHolder(parent.context, view)
    }

    override fun getItemCount(): Int {
        return steps.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val step = steps[position]

        if (step.stepNumber != null)
            holder.stepNumberTextView.text = step.stepNumber.toString()
        if (step.description != null)
            holder.descriptionEditText.setText(step.description)
        if (step.imageUri != null)
            holder.stepImageView.setImageURI(step.imageUri)
        else if (step.imageBitmap != null)
            holder.stepImageView.setImageBitmap(step.imageBitmap)
    }

    inner class ViewHolder(context: Context, itemView: View) : RecyclerView.ViewHolder(itemView) {
        val stepNumberTextView: TextView = itemView.findViewById(R.id.tv_step_number)
        val descriptionEditText: EditText = itemView.findViewById(R.id.et_description)
        val stepImageView: ImageView = itemView.findViewById(R.id.iv_step_image)
        private val popupMenu: PopupMenu = PopupMenu(context, itemView)

        init {
            descriptionEditText.filters = arrayOf(InputFilter { source, start, end,
                                                                dest, dstart, dend ->
                for (i in start until end) {
                    if (!Character.isLetterOrDigit(source[i]) && !Character.isSpaceChar(source[i]) &&
                        source[i] != '"' && source[i] != '(' && source[i] != ')' && source[i] != '-' && source[i] != '!'
                        && source[i] != '?' && source[i] != '.' && source[i] != ',')
                        return@InputFilter ""
                }
                return@InputFilter null
            })

            descriptionEditText.addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable?) {
                    steps[adapterPosition].description = descriptionEditText.text.toString()
                }

                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                    return
                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    return
                }

            })

            popupMenu.inflate(R.menu.popupmenu_step_image)
            popupMenu.setOnMenuItemClickListener {
                when(it.itemId) {
                    R.id.step_image_change -> {
                        setStepImage(stepImageView, steps[adapterPosition])
                        return@setOnMenuItemClickListener true
                    }
                    R.id.step_image_delete -> {
                        steps[adapterPosition].imageUri = null
                        steps[adapterPosition].imageBitmap = null
                        stepImageView.setImageURI(null)
                        return@setOnMenuItemClickListener true
                    }
                    else -> return@setOnMenuItemClickListener false
                }
            }

            stepImageView.setOnClickListener {
                val step = steps[adapterPosition]
                if (step.imageUri == null && step.imageBitmap == null) {
                    setStepImage(stepImageView, step)
                } else {
                    popupMenu.show()
                }
            }
        }
    }

    abstract fun setStepImage(stepImageView: ImageView, step: EditStep)
}