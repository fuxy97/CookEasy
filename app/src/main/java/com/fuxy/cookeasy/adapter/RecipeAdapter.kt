package com.fuxy.cookeasy.adapter

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.fuxy.cookeasy.R
import com.fuxy.cookeasy.activity.RecipeActivity
import com.fuxy.cookeasy.activity.RecipeActivityConstants
import com.fuxy.cookeasy.entity.Recipe
import com.fuxy.cookeasy.fragment.RecipesFragment
import org.threeten.bp.format.DateTimeFormatter

class RecipeAdapter(private val fragment: RecipesFragment, private val recipes: List<Recipe>)
    : RecyclerView.Adapter<RecipeAdapter.ViewHolder>() {

    private val minuteTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("m мин.")
    private val hourTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("H ч.")

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

        if (recipe.cookingTime.hour > 0)
            holder.cookingTimeTextView?.text = hourTimeFormatter.format(recipe.cookingTime)
        else
            holder.cookingTimeTextView?.text = minuteTimeFormatter.format(recipe.cookingTime)

        holder.ratingBar?.rating = recipe.rating
    }

    inner class ViewHolder(val context: Context, itemView: View)
        : RecyclerView.ViewHolder(itemView), View.OnClickListener {
        var dishTextView: TextView? = null
        var dishImageView: ImageView? = null
        var cookingTimeTextView: TextView? = null
        var ratingBar: RatingBar? = null

        init {
            dishTextView = itemView.findViewById(R.id.tv_dish)
            dishImageView = itemView.findViewById(R.id.iv_dish_image)
            cookingTimeTextView = itemView.findViewById(R.id.tv_cooking_time)
            ratingBar = itemView.findViewById(R.id.rb_rating)

            itemView.setOnClickListener(this)
        }

        override fun onClick(v: View?) {
            val recipe = recipes[adapterPosition]

            val intent = Intent(context, RecipeActivity::class.java)
            intent.putExtra(RecipeActivityConstants.EXTRA_RECIPE_ID, recipe.id!!)
            fragment.startActivityForResult(intent, RecipeActivityConstants.GET_RECIPE_STATE_REQUEST)
        }
    }
}