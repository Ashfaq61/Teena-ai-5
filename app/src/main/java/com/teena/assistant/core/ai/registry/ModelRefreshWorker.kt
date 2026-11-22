package com.teena.assistant.core.ai.registry

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class ModelRefreshWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val modelRegistry: ModelRegistry
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return modelRegistry.refreshAll(probeCandidates = true)
            .fold(
                onSuccess = { Result.success() },
                onFailure = { Result.retry() }
            )
    }
}
