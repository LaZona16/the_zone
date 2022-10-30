package com.lazona.core

import android.content.Intent
import android.os.Build.VERSION.SDK_INT
import android.os.Parcelable

const val PARCELABLE_EXTRA_MIN_VERSION = 33
inline fun <reified T : Parcelable> Intent.parcelable(key: String): T? = when {
    SDK_INT >= PARCELABLE_EXTRA_MIN_VERSION -> getParcelableExtra(key, T::class.java)
    else -> @Suppress("DEPRECATION") getParcelableExtra(key) as? T
}