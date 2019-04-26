package com.fuxy.cookeasy.adapter

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.fuxy.cookeasy.R
import com.fuxy.cookeasy.RecipeActivity
import com.fuxy.cookeasy.RecipeActivityExtras
import com.fuxy.cookeasy.db.LocalTimeConverter
import com.fuxy.cookeasy.entity.Recipe

class RecipeAdapter(private val recipes: List<Recipe>) : RecyclerView.Adapter<RecipeAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.recipe_item, parent, false)
        return ViewHolder(parent.context, itemView)
    }

    override fun getItemCount(): Int {
        return recipes.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val recipe = recipes[position]

        holder.dishTextView?.text = recipe.dish
        holder.dishImageView?.setImageBitmap(recipe.bucketImage.bitmap)
        holder.cookingTimeTextView?.text = LocalTimeConverter.fromLocalTime(recipe.cookingTime)
    }

    inner class ViewHolder(val context: Context, itemView: View)
        : RecyclerView.ViewHolder(itemView), View.OnClickListener {
        var dishTextView: TextView? = null
        var dishImageView: ImageView? = null
        var cookingTimeTextView: TextView? = null

        init {
            dishTextView = itemView.findViewById(R.id.tv_dish)
            dishImageView = itemView.findViewById(R.id.iv_dish_image)
            cookingTimeTextView = itemView.findViewById(R.id.tv_cooking_time)

            itemView.setOnClickListener(this)
        }

        override fun onClick(v: View?) {
            val recipe = recipes[adapterPosition]

            val intent = Intent(context, RecipeActivity::class.java)
            intent.putExtra(RecipeActivityExtras.EXTRA_RECIPE_ID, recipe.id!!)
            context.startActivity(intent)
        }
    }
}