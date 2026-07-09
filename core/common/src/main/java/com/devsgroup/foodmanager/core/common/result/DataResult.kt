package com.devsgroup.foodmanager.core.common.result

/** A minimal typed result wrapper for operations that can fail. */
sealed interface DataResult<out T> {
    data class Success<T>(val data: T) : DataResult<T>
    data class Failure(val error: DataError) : DataResult<Nothing>
}

enum class DataError {
    NETWORK,
    NOT_FOUND,
    UNKNOWN,
}
