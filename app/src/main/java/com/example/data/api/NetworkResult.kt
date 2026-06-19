package com.example.data.api

sealed class NetworkResult<out T> {
    object Idle : NetworkResult<Nothing>()
    object Loading : NetworkResult<Nothing>()
    data class Success<out T>(val data: T) : NetworkResult<T>()
    data class Error(val message: String, val exception: Throwable? = null) : NetworkResult<Nothing>()
}
