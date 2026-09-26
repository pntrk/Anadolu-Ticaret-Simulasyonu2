package com.example.data.remote.supabase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlin.reflect.KClass

/**
 * Interface contract for Supabase Realtime Logical Replication (Postgres CDC) & WebSockets Broadcast.
 * Provides asynchronous Kotlin Flow streams for real-time Postgres table mutations (~6ms latency).
 */
interface SupabaseRealtimeClient {

    /**
     * Real-time connection status indicator.
     */
    val isConnected: StateFlow<Boolean>

    /**
     * Connects to Supabase Realtime WebSocket engine.
     */
    suspend fun connect()

    /**
     * Disconnects from Supabase Realtime WebSocket engine.
     */
    suspend fun disconnect()

    /**
     * Listens to Postgres Logical Replication (CDC - Change Data Capture) on a specific table.
     * Emits [RealtimeChangeEvent] whenever an INSERT, UPDATE, or DELETE occurs in PostgreSQL.
     */
    fun <T : Any> observeTableChanges(
        table: String,
        schema: String = "public",
        eventFilter: PostgresEventType = PostgresEventType.ALL,
        clazz: KClass<T>
    ): Flow<RealtimeChangeEvent<T>>

    /**
     * Listens to low-latency in-memory WebSockets Broadcast channels.
     */
    fun <T : Any> observeBroadcastChannel(
        channelName: String,
        eventName: String,
        clazz: KClass<T>
    ): Flow<T>

    /**
     * Sends an in-memory broadcast message over WebSockets to all connected peers.
     */
    suspend fun sendBroadcastMessage(
        channelName: String,
        eventName: String,
        payload: Any
    ): Boolean
}
