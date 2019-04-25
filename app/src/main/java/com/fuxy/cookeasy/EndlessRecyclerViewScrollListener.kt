package com.fuxy.cookeasy

import android.widget.AbsListView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

abstract class EndlessRecyclerViewScrollListener(val layoutManager: GridLayoutManager)
    : RecyclerView.OnScrollListener() {
    private var isScrolling: Boolean = false

    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
        super.onScrolled(recyclerView, dx, dy)

        val currentItems = layoutManager.childCount
        val totalItems = layoutManager.itemCount
        val scrollOutItems = layoutManager.findFirstVisibleItemPosition()

        if (isScrolling && currentItems + scrollOutItems == totalItems) {
            isScrolling = false
            fetchData(totalItems)
        }
    }

    abstract fun fetchData(nextItem: Int)

    override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
        super.onScrollStateChanged(recyclerView, newState)
        if (newState == AbsListView.OnScrollListener.SCROLL_STATE_TOUCH_SCROLL)
            isScrolling = true
    }
}