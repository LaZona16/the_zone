package com.lazona.core

import android.view.View
import androidx.recyclerview.widget.RecyclerView

abstract class BaseViewHolder<T>(itemVew: View) : RecyclerView.ViewHolder(itemVew) {
    abstract fun bind(item: T)
}