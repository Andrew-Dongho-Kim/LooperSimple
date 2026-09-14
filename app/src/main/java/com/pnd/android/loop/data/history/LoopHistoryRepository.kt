package com.pnd.android.loop.data.history

import com.pnd.android.loop.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.shareIn
import javax.inject.Inject
import javax.inject.Singleton

/** One shared transactional DB stream for home, details, history and statistics. */
@Singleton
class LoopHistoryRepository @Inject constructor(private val db: AppDatabase) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val snapshots = db.loopHistoryDao().observeAll()
        .map(::LoopHistorySnapshot)
        .flowOn(Dispatchers.Default)
        .shareIn(scope, SharingStarted.WhileSubscribed(5_000), replay = 1)

    suspend fun snapshot(): LoopHistorySnapshot = LoopHistorySnapshot(db.loopHistoryDao().getAll())
}
