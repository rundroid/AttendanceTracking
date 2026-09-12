package com.learning.attendancetracking.utils

sealed interface DataState<out R> {
    data object Loading : DataState<Nothing>
    data class Progress(val progress: Int) : DataState<Nothing>
    data class Success<out R>(val result: R) : DataState<R>
    data class Failure(val throwable: Throwable) : DataState<Nothing>
    data object Uninitialized : DataState<Nothing>

    fun success(): R? = (this as? Success)?.result

    fun isFailure(): Boolean = this is Failure

    fun failure(): Throwable? = (this as? Failure)?.throwable

    fun isLoading(): Boolean = this is Loading
}