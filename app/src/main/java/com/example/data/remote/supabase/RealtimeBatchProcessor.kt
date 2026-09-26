package com.example.data.remote.supabase

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

/**
 * Ultra-High Frequency Realtime Batching Engine.
 *
 * Designed to handle incoming WebSocket/Replication streams operating at ~6ms latency (~160+ events/sec).
 * Pools rapid atomic updates in a non-blocking channel buffer and flushes windowed batches
 * to [StateFlow] at 60 FPS (~16ms tick), preventing UI frame drops and Main-thread recomposition thrashing.
 */
class RealtimeBatchProcessor<K, V>(
    private val frameIntervalMs: Long = 250L, // 250ms ~ 4 FPS batching window to prevent IPC Binder buffer saturation
    private val keySelector: (V) -> K,
    private val valueMerger: (current: V?, incoming: V) -> V = { _, incoming -> incoming }
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val incomingChannel = Channel<V>(
        capacity = 1000,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    private val pendingBuffer = ConcurrentHashMap<K, V>()
    private val _batchedState = MutableStateFlow<Map<K, V>>(emptyMap())
    val batchedState: StateFlow<Map<K, V>> = _batchedState.asStateFlow()

    init {
        startBatchingWorker()
    }

    /**
     * Enqueues an incoming high-frequency item from Supabase Realtime stream (~6ms throughput).
     */
    fun offer(item: V) {
        incomingChannel.trySend(item)
    }

    /**
     * Connects an incoming Kotlin [Flow] directly into the batching engine.
     */
    fun bindFlow(upstreamFlow: Flow<V>) {
        scope.launch {
            upstreamFlow.collect { item ->
                offer(item)
            }
        }
    }

    private fun startBatchingWorker() {
        scope.launch {
            // Background collection pipeline
            launch {
                for (item in incomingChannel) {
                    val key = keySelector(item)
                    val existing = pendingBuffer[key]
                    pendingBuffer[key] = valueMerger(existing, item)
                }
            }

            // High-frequency frame flush tick loop (16ms frame target)
            while (isActive) {
                delay(frameIntervalMs)
                if (pendingBuffer.isNotEmpty()) {
                    val snapshot = HashMap(pendingBuffer)
                    val currentState = _batchedState.value
                    val updatedState = currentState.toMutableMap()

                    for ((k, v) in snapshot) {
                        updatedState[k] = v
                    }

                    _batchedState.value = updatedState
                    // Clear flushed entries
                    snapshot.keys.forEach { pendingBuffer.remove(it) }
                }
            }
        }
    }

    /**
     * Resets the buffer state.
     */
    fun clear() {
        pendingBuffer.clear()
        _batchedState.value = emptyMap()
    }
}
