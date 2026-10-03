package com.androidapp.vshopandroidapp.core.common

import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

abstract class BaseUseCase<in Params, out T>(
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    suspend operator fun invoke(params: Params): AppResult<T> = withContext(dispatcher) {
        try {
            execute(params)
        } catch (e: CancellationException) {
            // Leaving a screen cancels its work. That's not an error to show.
            throw e
        } catch (e: Exception) {
            AppResult.Error(AppError.Unknown(e.message ?: "Unexpected error"))
        }
    }

    protected abstract suspend fun execute(params: Params): AppResult<T>
}

abstract class BaseNoParamsUseCase<out T>(
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    suspend operator fun invoke(): AppResult<T> = withContext(dispatcher) {
        try {
            execute()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AppResult.Error(AppError.Unknown(e.message ?: "Unexpected error"))
        }
    }

    protected abstract suspend fun execute(): AppResult<T>
}