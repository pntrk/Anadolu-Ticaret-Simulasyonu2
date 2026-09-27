package com.example.data

import android.util.Log
import com.example.viewmodel.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference

/**
 * SaveSyncCoordinator:
 * Arka plan bulut senkronizasyonunu 3 dakikalık (180_000 ms) debounce ile koordine eder.
 * Borsa alım-satım gibi sık aralıklarla gerçekleşen işlemlerde sunucu yükünü ve gereksiz ağ isteklerini önler.
 */
@OptIn(FlowPreview::class)
object SaveSyncCoordinator {
    private val scope = CoroutineScope(Dispatchers.IO)
    private val syncTriggerFlow = MutableSharedFlow<Unit>(
        replay = 0,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    @Volatile
    private var viewModelRef: WeakReference<GameViewModel>? = null

    @Volatile
    private var repositoryRef: WeakReference<GameRepository>? = null

    init {
        scope.launch {
            syncTriggerFlow
                .debounce(180_000L)
                .collect {
                    performSync()
                }
        }
    }

    fun bindViewModel(viewModel: GameViewModel) {
        viewModelRef = WeakReference(viewModel)
    }

    fun bindRepository(repository: GameRepository) {
        repositoryRef = WeakReference(repository)
    }

    fun markDirty() {
        if (!syncTriggerFlow.tryEmit(Unit)) {
            scope.launch {
                syncTriggerFlow.emit(Unit)
            }
        }
    }

    fun flushImmediately() {
        scope.launch {
            performSync()
        }
    }

    private suspend fun performSync() {
        try {
            val vm = viewModelRef?.get()
            val repo = repositoryRef?.get()
            if (vm != null) {
                vm.saveEconomicDataToDataStore(immediate = true)
                vm.forceSyncCloudSaveToSupabase(immediate = true)
                val p = vm.player.value
                if (p != null && repo != null) {
                    val netWorth = p.money + p.depositBalance
                    repo.syncPlayerMeta(
                        playerId = p.id,
                        name = p.name,
                        level = p.level,
                        netWorth = netWorth,
                        hash = ""
                    )
                }
            }
        } catch (e: Throwable) {
            Log.w("SaveSyncCoordinator", "Cloud sync execution error: ${e.message}", e)
        }
    }
}
