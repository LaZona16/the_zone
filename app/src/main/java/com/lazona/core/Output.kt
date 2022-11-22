package com.lazona.core

import java.lang.Exception

sealed class Output<out T> {
    class Loading<out T> : Output<T>()
    data class Success<out T>(val data: T) : Output<T>()
    data class Failure(val exception: Exception) : Output<Nothing>()
}