package com.lazona.data.local

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
class WallDatasource(
    private val id: String = "",
    private val name: String = "UNNAMED",
    private var intensity: Int = 0
    ) : Parcelable
