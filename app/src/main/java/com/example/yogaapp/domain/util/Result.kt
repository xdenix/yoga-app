package com.example.yogaapp.domain.util

import kotlin.coroutines.cancellation.CancellationException

/**
 * App-wide Result type. Sealed so that exhaustive `when` matching is enforced by the compiler
 * and the UI never silently drops an error case.
 *
 * - [Success] carries the typed [value].
 * - [Failure] carries a structured [error] (see [AppError]).
 *
 * Used by use cases and repository methods so the ViewModels can map to UI state without
 * `try {} catch (e: Exception) {}` swallowing — see "Запрещено" in ТЗ.
 */
sealed interface Result<out T> {
    data class Success<T>(val value: T) : Result<T>
    data class Failure(val error: AppError) : Result<Nothing>
}

inline fun <T> resultOf(block: () -> T): Result<T> = try {
    Result.Success(block())
} catch (e: Throwable) {
    if (e is CancellationException) throw e // never swallow coroutine cancellation
    Result.Failure(AppError.from(e))
}

inline fun <T, R> Result<T>.map(transform: (T) -> R): Result<R> = when (this) {
    is Result.Success -> Result.Success(transform(value))
    is Result.Failure -> this
}

inline fun <T> Result<T>.getOrDefault(default: T): T = when (this) {
    is Result.Success -> value
    is Result.Failure -> default
}

inline fun <T> Result<T>.getOrThrow(): T = when (this) {
    is Result.Success -> value
    is Result.Failure -> throw error.cause ?: IllegalStateException(error.toString())
}

/**
 * Structured application error. Every UI state machine maps a [Failure] to a user-facing
 * Russian string resource; the [kind] enum drives which string is selected so that the
 * mapping is one-line in the ViewModel.
 */
sealed interface AppError {
    val cause: Throwable?
    val kind: ErrorKind

    data class Database(override val cause: Throwable? = null) : AppError {
        override val kind = ErrorKind.Database
    }

    data class Network(override val cause: Throwable? = null) : AppError {
        override val kind = ErrorKind.Network
    }

    data class NotFound(val what: String, override val cause: Throwable? = null) : AppError {
        override val kind = ErrorKind.NotFound
    }

    data class Validation(val field: String, override val cause: Throwable? = null) : AppError {
        override val kind = ErrorKind.Validation
    }

    data class Unknown(override val cause: Throwable? = null) : AppError {
        override val kind = ErrorKind.Unknown
    }

    companion object {
        fun from(throwable: Throwable): AppError = when (throwable) {
            is IllegalArgumentException -> Validation(field = "unknown", cause = throwable)
            is java.io.IOException -> Network(cause = throwable)
            is NoSuchElementException -> NotFound(what = "entity", cause = throwable)
            else -> Unknown(cause = throwable)
        }
    }
}

/** Drives the user-facing message; UI resolves [kind] → strings.xml string resource id. */
enum class ErrorKind {
    Database,
    Network,
    NotFound,
    Validation,
    Unknown,
}
