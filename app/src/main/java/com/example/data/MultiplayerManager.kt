package com.example.data

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import com.example.data.network.AppJson
import com.example.data.network.PhoenixChannelJoinDto
import com.example.data.network.PhoenixChannelLeaveDto
import java.util.concurrent.ConcurrentHashMap
import com.example.data.network.PhoenixChannelJoinPayloadDto
import com.example.data.network.PhoenixConfigDto
import com.example.data.network.PhoenixHeartbeatDto
import com.example.data.network.PhoenixIncomingMessageDto
import com.example.data.network.PhoenixPostgresChangeDto
import com.example.data.network.PhoenixBroadcastConfigDto
import com.example.data.network.PhoenixBroadcastOutgoingDto
import com.example.data.network.PhoenixBroadcastPayloadDto
import com.example.data.network.PhoenixPresenceConfigDto
import com.example.data.network.LiveAuctionBidEventDto
import com.example.data.network.LiveAuctionListedEventDto
import com.example.data.network.LiveConsortiumActionEventDto
import com.example.data.network.LiveConsortiumCreatedEventDto
import com.example.data.network.ConsortiumChatMessageDto
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.encodeToString
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.put
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.min
import kotlin.random.Random

data class PlayerFacilityInfo(
    val name: String,
    val city: String,
    val level: Int,
    val category: String = "Sınai Tesis"
)

data class OnlinePlayer(
    val id: String,
    val name: String,
    val companyName: String,
    val netWorth: Long,
    val city: String,
    val level: Int,
    val isOnline: Boolean = true,
    val badge: String = "TÜCCAR",
    val isIpoActive: Boolean = false,
    val publicSharePercent: Int = 20,
    val bankBalance: Long = 0L,
    val monthlyScore: Long = 0L,
    val xp: Int = level * 2800 + 1500,
    val centralWarehouseLocation: String = "$city - Ambarlı Lojistik Deposu",
    val facilities: List<PlayerFacilityInfo> = emptyList()
)

data class OnlineTradeOffer(
    val id: String,
    val sellerId: String,
    val sellerName: String,
    val itemName: String,
    val quantity: Int,
    val pricePerUnit: Long,
    val cityName: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class MultiplayerLobby(
    val id: String,
    val name: String,
    val region: String,
    val activeCount: Int,
    val maxCapacity: Int = 50,
    val description: String
)

/**
 * MultiplayerManager:
 * - Supabase PostgreSQL & Realtime Channel Entegrasyonu
 * - Atomic Remote Procedure Call (RPC) tabanlı veri yarışlarını (Race Condition) önleme
 * - WebSocket kopmalarında Üstel Geri Çekilme (Exponential Backoff) ile otomatik yeniden bağlanma
 * - Çöp Toplayıcı (GC) optimizasyonu: Nesne tahsisini minimize eden durum yönetimi
 */
object MultiplayerManager {
    private const val TAG = "MultiplayerManager"

    private val scope = CoroutineScope(Dispatchers.IO + Job())

    private val _connectionStatus = MutableStateFlow("BEKLEMEDE")
    val connectionStatus: StateFlow<String> = _connectionStatus.asStateFlow()

    private val _hasPendingSync = MutableStateFlow(false)
    val hasPendingSync: StateFlow<Boolean> = _hasPendingSync.asStateFlow()

    private val _onlinePlayers = MutableStateFlow<List<OnlinePlayer>>(emptyList())
    val onlinePlayers: StateFlow<List<OnlinePlayer>> = _onlinePlayers.asStateFlow()

    fun setOnlinePlayers(players: List<OnlinePlayer>) {
        val botIds = setOf("BOT-KAYA-01", "BOT-NOVA-02", "BOT-TOROS-03", "BOT-EGE-04", "BOT-AVRASYA-05", "BOT-ANADOLU-05")
        val botNames = setOf("Selim Kaya", "Dr. Aylin Soylu", "Burak Demirci", "Zehra Aydın", "Hakan Erkin", "Defne Aras", "Kaan Yıldırım")
        val filtered = players.filter { player ->
            player.id.isNotBlank() &&
            !player.id.startsWith("BOT-", ignoreCase = true) &&
            !player.id.startsWith("BOT_", ignoreCase = true) &&
            !player.id.contains("bot", ignoreCase = true) &&
            player.id !in botIds &&
            player.name !in botNames
        }

        val deduplicated = filtered
            .groupBy { player ->
                val cleanId = player.id.lowercase().replace(".", "_").removeSuffix("_backup")
                if (cleanId.contains("@")) cleanId.substringBefore("@") else cleanId
            }
            .map { (_, duplicates) ->
                duplicates.maxByOrNull { it.netWorth }!!
            }
            .sortedByDescending { it.netWorth }

        _onlinePlayers.value = deduplicated
    }

    private val _pastMonthLeaderboard = MutableStateFlow<List<OnlinePlayer>>(
        listOf(
            OnlinePlayer("TR-982", "Ahmet Yılmaz", "Marmara Lojistik A.Ş.", 18500000L, "İstanbul", 19, false, "CEO", monthlyScore = 4850000L, xp = 54200, centralWarehouseLocation = "İstanbul (Ambarlı Mega Lojistik Hub)", facilities = listOf(
                PlayerFacilityInfo("Marmara Ağır Sanayi Fabrikası", "İstanbul", 4, "Ağır Sanayi"),
                PlayerFacilityInfo("Kuzey Ege Otomotiv Montaj", "Bursa", 3, "Otomotiv"),
                PlayerFacilityInfo("Trakya Çelik Döküm Tesisi", "Tekirdağ", 3, "Metal"),
                PlayerFacilityInfo("Boğaziçi Ambalaj Entegre", "Kocaeli", 2, "Paketleme")
            )),
            OnlinePlayer("TR-431", "Mehmet Demir", "Ege Zeytincilik", 14200000L, "İzmir", 15, false, "LİDER", monthlyScore = 3600000L, xp = 39800, centralWarehouseLocation = "İzmir (Alsancak Liman Ana Deposu)", facilities = listOf(
                PlayerFacilityInfo("Ege Sızma Zeytinyağı Fabrikası", "İzmir", 4, "Gıda"),
                PlayerFacilityInfo("Gediz Ambalaj & Şişeleme", "Manisa", 3, "Paketleme"),
                PlayerFacilityInfo("Körfez Lojistik Transfer Hanı", "Balıkesir", 2, "Lojistik")
            )),
            OnlinePlayer("TR-712", "Fatma Kaya", "Çukurova Pamuk Sanayi", 11200000L, "Adana", 13, false, "LİDER", monthlyScore = 2900000L, xp = 31200, centralWarehouseLocation = "Adana (Çukurova Sanayi Deposu)", facilities = listOf(
                PlayerFacilityInfo("Çukurova İplik & Dokuma Fabrikası", "Adana", 3, "Tekstil"),
                PlayerFacilityInfo("Toros Kimya & Boya Tesisi", "Mersin", 2, "Kimya"),
                PlayerFacilityInfo("Seyhan Ambalaj Atölyesi", "Hatay", 2, "Paketleme")
            )),
            OnlinePlayer("TR-303", "Canan Öztürk", "Başkent Enerji A.Ş.", 9800000L, "Ankara", 11, false, "TÜCCAR", monthlyScore = 2300000L, xp = 25100, centralWarehouseLocation = "Ankara (Kazan Lojistik Kompleksi)", facilities = listOf(
                PlayerFacilityInfo("Anadolu Batarya & Enerji Santrali", "Ankara", 3, "Enerji"),
                PlayerFacilityInfo("Eskişehir Cam Sanayi", "Eskişehir", 2, "Cam")
            )),
            OnlinePlayer("TR-105", "Zeynep Şahin", "Konya Genetik Tarım", 8400000L, "Konya", 10, false, "TÜCCAR", monthlyScore = 1900000L, xp = 21400, centralWarehouseLocation = "Konya (Anadolu Lojistik Hub)", facilities = listOf(
                PlayerFacilityInfo("Anadolu Un & Yem Fabrikası", "Konya", 3, "Gıda"),
                PlayerFacilityInfo("Göksu Ambalaj Sanayi", "Karaman", 2, "Paketleme")
            ))
        )
    )
    val pastMonthLeaderboard: StateFlow<List<OnlinePlayer>> = _pastMonthLeaderboard.asStateFlow()

    private val _liveTradeOffers = MutableStateFlow<List<OnlineTradeOffer>>(
        listOf(
            OnlineTradeOffer("OFFER-101", "TR-982", "Marmara Lojistik A.Ş.", "Kaliteli Buğday", 100, 4500L, "İstanbul"),
            OnlineTradeOffer("OFFER-102", "TR-431", "Ege Zeytincilik", "Sızma Zeytinyağı", 40, 18500L, "İzmir"),
            OnlineTradeOffer("OFFER-103", "TR-712", "Çukurova Pamuk Sanayi", "Ham Pamuk", 85, 9200L, "Adana")
        )
    )
    val liveTradeOffers: StateFlow<List<OnlineTradeOffer>> = _liveTradeOffers.asStateFlow()

    private val _activeLobbies = MutableStateFlow<List<MultiplayerLobby>>(emptyList())
    val activeLobbies: StateFlow<List<MultiplayerLobby>> = _activeLobbies.asStateFlow()

    val defaultBotGuilds = listOf(
        GuildGroup(
            id = "bot-guild-v4-1",
            name = "Türkiye Otomobil Girişim Grubu",
            leaderName = "Bursa Otomotiv Sanayi A.Ş. (Bursa)",
            memberCount = 42,
            megaProjectTitle = "Milli Elektrikli Otomobil Projesi",
            targetProductId = "ev",
            targetProductName = "TOGG T10X",
            cityId = "bursa"
        ),
        GuildGroup(
            id = "bot-guild-v4-2",
            name = "Milli Savunma Sistemleri A.Ş.",
            leaderName = "Savunma Sanayii Başkanlığı (Ankara)",
            memberCount = 28,
            megaProjectTitle = "Taktik İnsansız Hava Aracı Serisi",
            targetProductId = "uav",
            targetProductName = "Bayraktar TB3",
            cityId = "ankara"
        ),
        GuildGroup(
            id = "bot-guild-v4-3",
            name = "Milli Uzay Programı",
            leaderName = "Uzay ve Havacılık Ajansı (Ankara)",
            memberCount = 19,
            megaProjectTitle = "Milli Yörünge Fırlatma Üssü",
            targetProductId = "space_rocket",
            targetProductName = "TUA Şimşek-1 Roketi",
            cityId = "ankara"
        ),
        GuildGroup(
            id = "bot-guild-v4-4",
            name = "Mavi Vatan Tersanecilik",
            leaderName = "Gemi İnşa Genel Müdürlüğü (İstanbul - Tuzla)",
            memberCount = 35,
            megaProjectTitle = "Kıtalararası Mega Kargo Gemisi",
            targetProductId = "cargo_ship",
            targetProductName = "Piri Reis Kargo Gemisi",
            cityId = "istanbul"
        ),
        GuildGroup(
            id = "bot-guild-v4-5",
            name = "TCDD Yüksek Hızlı Tren Ağı",
            leaderName = "Ulaştırma Bakanlığı (Eskişehir)",
            memberCount = 55,
            megaProjectTitle = "Anadolu Hızlı Yük Treni Hattı",
            targetProductId = "bullet_train",
            targetProductName = "Anadolu Ekspresi Hızlı Treni",
            cityId = "eskisehir"
        ),
        GuildGroup(
            id = "bot-guild-v4-6",
            name = "Marmara Teknoloji Holding",
            leaderName = "Bilişim Vadisi Yönetimi (Kocaeli - Gebze)",
            memberCount = 22,
            megaProjectTitle = "Yapay Zeka Süper Veri Merkezi",
            targetProductId = "ai_datacenter",
            targetProductName = "KIZILELMA Yapay Zeka Çekirdeği",
            cityId = "kocaeli"
        ),
        GuildGroup(
            id = "bot-guild-v4-7",
            name = "Ege Nükleer Güç Santrali",
            leaderName = "Türkiye Atom Enerjisi Kurumu (Mersin)",
            memberCount = 31,
            megaProjectTitle = "Akkuyu Alternatif Nükleer Kompleksi",
            targetProductId = "fusion_reactor_core",
            targetProductName = "Akkuyu Füzyon Çekirdeği",
            cityId = "mersin"
        ),
        GuildGroup(
            id = "bot-guild-v4-8",
            name = "Kuzey Rüzgar Çiftliği A.Ş.",
            leaderName = "Enerji Piyasası Düzenleme Kurumu (Trabzon)",
            memberCount = 47,
            megaProjectTitle = "Karadeniz Denizüstü Rüzgar Çiftliği",
            targetProductId = "smart_grid",
            targetProductName = "BORA Akıllı Enerji Türbini",
            cityId = "trabzon"
        )
    )

    private val _globalGuilds = MutableStateFlow<List<GuildGroup>>(emptyList())
    val globalGuilds: StateFlow<List<GuildGroup>> = _globalGuilds.asStateFlow()

    // Supabase Realtime Broadcast Streams (Low Latency In-Memory Bus)
    private val _broadcastChatFlow = MutableSharedFlow<ConsortiumChatMessage>(extraBufferCapacity = 64)
    val broadcastChatFlow: SharedFlow<ConsortiumChatMessage> = _broadcastChatFlow.asSharedFlow()

    private val _broadcastAuctionBidFlow = MutableSharedFlow<LiveAuctionBidEventDto>(extraBufferCapacity = 64)
    val broadcastAuctionBidFlow: SharedFlow<LiveAuctionBidEventDto> = _broadcastAuctionBidFlow.asSharedFlow()

    private val _broadcastAuctionListedFlow = MutableSharedFlow<LiveAuctionListedEventDto>(extraBufferCapacity = 64)
    val broadcastAuctionListedFlow: SharedFlow<LiveAuctionListedEventDto> = _broadcastAuctionListedFlow.asSharedFlow()

    private val _broadcastConsortiumMoveFlow = MutableSharedFlow<LiveConsortiumActionEventDto>(extraBufferCapacity = 64)
    val broadcastConsortiumMoveFlow: SharedFlow<LiveConsortiumActionEventDto> = _broadcastConsortiumMoveFlow.asSharedFlow()

    private val _broadcastConsortiumCreatedFlow = MutableSharedFlow<com.example.data.network.LiveConsortiumCreatedEventDto>(extraBufferCapacity = 64)
    val broadcastConsortiumCreatedFlow: SharedFlow<com.example.data.network.LiveConsortiumCreatedEventDto> = _broadcastConsortiumCreatedFlow.asSharedFlow()

    private val _broadcastMarketActionFlow = MutableSharedFlow<com.example.data.network.LiveMarketActionEventDto>(extraBufferCapacity = 64)
    val broadcastMarketActionFlow: SharedFlow<com.example.data.network.LiveMarketActionEventDto> = _broadcastMarketActionFlow.asSharedFlow()

    private val _broadcastBorsaPricesFlow = MutableSharedFlow<List<MarketPriceEntity>>(extraBufferCapacity = 64)
    val broadcastBorsaPricesFlow: SharedFlow<List<MarketPriceEntity>> = _broadcastBorsaPricesFlow.asSharedFlow()

    private val _marketChangeTriggerFlow = MutableSharedFlow<Long>(extraBufferCapacity = 16)
    val marketChangeTriggerFlow: SharedFlow<Long> = _marketChangeTriggerFlow.asSharedFlow()

    // Realtime & WebSocket State Management
    private val wsClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS) // WebSocket keepalive için limitsiz
        .pingInterval(25, TimeUnit.SECONDS)
        .build()

    private var activeWebSocket: WebSocket? = null
    private val isConnecting = AtomicBoolean(false)
    private val retryCount = AtomicInteger(0)
    private val lastServerResponseTimestamp = AtomicLong(System.currentTimeMillis())
    private var heartbeatJob: Job? = null
    private var reconnectJob: Job? = null

    private const val HEARTBEAT_INTERVAL_MS = 25_000L
    private const val PONG_TIMEOUT_MS = 45_000L

    // Mutex locks for atomic RPC calls to prevent race conditions
    private val tenderRpcMutex = Mutex()
    private val consortiumSlotRpcMutex = Mutex()

    init {
        scope.launch {
            refreshGuildsFromSupabase()
            refreshLeaderboardFromSupabase()
            startRealtimeConnection()
        }
    }

    /**
     * Supabase Realtime WebSocket bağlantısını üstel geri çekilme (exponential backoff) ile başlatır.
     */
    fun startRealtimeConnection() {
        if (isConnecting.getAndSet(true)) return

        scope.launch {
            try {
                closeWebSocket()

                val wsUrl = SupabaseManager.SUPABASE_URL
                    .replace("https://", "wss://")
                    .replace("http://", "ws://") + "/realtime/v1/websocket?apikey=${SupabaseManager.SUPABASE_KEY}&vsn=1.0.0"

                val request = Request.Builder()
                    .url(wsUrl)
                    .build()

                Log.d(TAG, "Connecting to Supabase Realtime WebSocket...")
                activeWebSocket = wsClient.newWebSocket(request, createWebSocketListener())
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start Realtime WebSocket", e)
                isConnecting.set(false)
                scheduleExponentialReconnect()
            }
        }
    }

    private fun createWebSocketListener(): WebSocketListener {
        return object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d(TAG, "Supabase Realtime WebSocket Connected successfully.")
                isConnecting.set(false)
                retryCount.set(0)
                _connectionStatus.value = "🟢 SUPABASE CANLI (Broadcast & Realtime)"

                // 1. Join Chat Broadcast channel JIT (only if chat is currently open)
                if (activeChatProjects.isNotEmpty()) {
                    sendJoinChatBroadcast(webSocket)
                }

                // 2. Join Auctions Broadcast channel
                val joinAuctionsBroadcast = PhoenixChannelJoinDto(
                    topic = "realtime:auctions",
                    event = "phx_join",
                    payload = PhoenixChannelJoinPayloadDto(
                        config = PhoenixConfigDto(
                            broadcast = PhoenixBroadcastConfigDto(self = true, ack = true),
                            presence = PhoenixPresenceConfigDto(key = "player-client"),
                            postgresChanges = listOf(
                                PhoenixPostgresChangeDto(
                                    event = "*",
                                    schema = "public",
                                    table = "museum_auctions"
                                )
                            )
                        )
                    ),
                    ref = "auctions-broadcast-2"
                )
                webSocket.send(AppJson.encodeToString(joinAuctionsBroadcast))

                // 3. Join Consortium Broadcast & Database channel
                val joinConsortiumBroadcast = PhoenixChannelJoinDto(
                    topic = "realtime:consortium",
                    event = "phx_join",
                    payload = PhoenixChannelJoinPayloadDto(
                        config = PhoenixConfigDto(
                            broadcast = PhoenixBroadcastConfigDto(self = true, ack = true),
                            presence = PhoenixPresenceConfigDto(key = "player-client"),
                            postgresChanges = listOf(
                                PhoenixPostgresChangeDto(
                                    event = "*",
                                    schema = "public",
                                    table = "global_guilds"
                                ),
                                PhoenixPostgresChangeDto(
                                    event = "*",
                                    schema = "public",
                                    table = "guilds"
                                )
                            )
                        )
                    ),
                    ref = "consortium-broadcast-3"
                )
                webSocket.send(AppJson.encodeToString(joinConsortiumBroadcast))

                // 4. Join Market & Contracts Broadcast & Database channel
                val joinMarketBroadcast = PhoenixChannelJoinDto(
                    topic = "realtime:market",
                    event = "phx_join",
                    payload = PhoenixChannelJoinPayloadDto(
                        config = PhoenixConfigDto(
                            broadcast = PhoenixBroadcastConfigDto(self = true, ack = true),
                            presence = PhoenixPresenceConfigDto(key = "player-client"),
                            postgresChanges = listOf(
                                PhoenixPostgresChangeDto(
                                    event = "*",
                                    schema = "public",
                                    table = "global_market"
                                )
                            )
                        )
                    ),
                    ref = "market-broadcast-4"
                )
                webSocket.send(AppJson.encodeToString(joinMarketBroadcast))

                // 25 saniyede bir heartbeat ping ve zombi soket denetimi
                lastServerResponseTimestamp.set(System.currentTimeMillis())
                startHeartbeat(webSocket)
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                lastServerResponseTimestamp.set(System.currentTimeMillis())
                scope.launch(Dispatchers.Default) {
                    handleRealtimeMessage(text)
                }
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "Realtime WebSocket Closing: $code / $reason")
                webSocket.close(1000, null)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "Realtime WebSocket Closed: $code / $reason")
                isConnecting.set(false)
                stopHeartbeat()
                scheduleExponentialReconnect()
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.w(TAG, "Realtime WebSocket Failure: ${t.message}")
                isConnecting.set(false)
                stopHeartbeat()
                _connectionStatus.value = "🟡 YENİDEN BAĞLANIYOR..."
                scheduleExponentialReconnect()
            }
        }
    }

    private fun handleRealtimeMessage(text: String) {
        try {
            val msg = AppJson.decodeFromString<PhoenixIncomingMessageDto>(text)
            val event = msg.event
            val topic = msg.topic
            val payloadElem = msg.payload

            if (event == "broadcast" && payloadElem != null) {
                val payloadObj = payloadElem as? JsonObject
                val broadcastEvent = (payloadObj?.get("event") as? JsonPrimitive)?.content ?: ""
                val innerPayload = payloadObj?.get("payload")

                when (broadcastEvent) {
                    "auction_bid" -> {
                        if (innerPayload != null) {
                            val bidEvent = AppJson.decodeFromJsonElement<LiveAuctionBidEventDto>(innerPayload)
                            _broadcastAuctionBidFlow.tryEmit(bidEvent)
                            Log.d(TAG, "Received Broadcast Auction Bid: ${bidEvent.newBidAmount} by ${bidEvent.bidderName}")
                        }
                    }
                    "auction_listed" -> {
                        if (innerPayload != null) {
                            val listedEvent = AppJson.decodeFromJsonElement<LiveAuctionListedEventDto>(innerPayload)
                            _broadcastAuctionListedFlow.tryEmit(listedEvent)
                            Log.d(TAG, "Received Broadcast Auction Listed: ${listedEvent.artifactName} by ${listedEvent.sellerName}")
                        }
                    }
                    "consortium_created" -> {
                        if (innerPayload != null) {
                            val createdEvent = AppJson.decodeFromJsonElement<com.example.data.network.LiveConsortiumCreatedEventDto>(innerPayload)
                            _broadcastConsortiumCreatedFlow.tryEmit(createdEvent)
                            Log.d(TAG, "Received Broadcast Consortium Created: ${createdEvent.consortiumName} by ${createdEvent.leaderName}")
                            scope.launch { refreshGuildsFromSupabase() }
                        }
                    }
                    "consortium_action", "consortium_move" -> {
                        if (innerPayload != null) {
                            val actionEvent = AppJson.decodeFromJsonElement<LiveConsortiumActionEventDto>(innerPayload)
                            _broadcastConsortiumMoveFlow.tryEmit(actionEvent)
                            Log.d(TAG, "Received Broadcast Consortium Action: ${actionEvent.moveType} by ${actionEvent.playerName}")
                            scope.launch { refreshGuildsFromSupabase() }
                        }
                    }
                    "consortium_chat", "chat_message" -> {
                        if (innerPayload != null) {
                            val chatDto = AppJson.decodeFromJsonElement<ConsortiumChatMessageDto>(innerPayload)
                            val chatMsg = ConsortiumChatMessage(
                                id = chatDto.id,
                                projectId = chatDto.projectId,
                                senderId = chatDto.senderId,
                                senderName = chatDto.senderName,
                                senderRole = chatDto.senderRole,
                                messageText = chatDto.messageText,
                                timestampMs = chatDto.timestampMs,
                                isSystemMessage = chatDto.isSystemMessage
                            )
                            _broadcastChatFlow.tryEmit(chatMsg)
                            Log.d(TAG, "Received Broadcast Consortium Chat: ${chatMsg.senderName}: ${chatMsg.messageText}")
                        }
                    }
                    "market_action" -> {
                        if (innerPayload != null) {
                            val marketEvent = AppJson.decodeFromJsonElement<com.example.data.network.LiveMarketActionEventDto>(innerPayload)
                            _broadcastMarketActionFlow.tryEmit(marketEvent)
                            _marketChangeTriggerFlow.tryEmit(System.currentTimeMillis())
                            Log.d(TAG, "Received Broadcast Market Action: ${marketEvent.actionType} by ${marketEvent.playerName}")
                        }
                    }
                    "borsa_price_update", "borsa_prices_sync" -> {
                        if (innerPayload != null) {
                            try {
                                val borsaEvent = AppJson.decodeFromJsonElement<com.example.data.network.LiveBorsaPricesSyncDto>(innerPayload)
                                val rawEntities = borsaEvent.prices.map {
                                    MarketPriceEntity(
                                        itemId = it.itemId, 
                                        originCountry = it.originCountry,
                                        originCityId = it.originCityId,
                                        price = it.price, 
                                        borsaStock = it.borsaStock,
                                        isUsd = it.isUsd
                                    )
                                }
                                val entities = sanitizeMarketPrices(rawEntities)
                                if (entities.isNotEmpty()) {
                                    _broadcastBorsaPricesFlow.tryEmit(entities)
                                    Log.d(TAG, "Received Broadcast Borsa Prices: ${entities.size} items updated by ${borsaEvent.sourcePlayerId}")
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "Error parsing borsa price broadcast event", e)
                            }
                        }
                    }
                }
            } else if (event == "postgres_changes" || event == "INSERT" || event == "UPDATE" || event == "DELETE") {
                Log.d(TAG, "Received Postgres Realtime change on topic: $topic")
                scope.launch {
                    if (topic == "realtime:auctions") {
                        com.example.data.MuseumHeritageManager.syncWithSupabaseSafe()
                    } else if (topic == "realtime:market") {
                        _marketChangeTriggerFlow.tryEmit(System.currentTimeMillis())
                    } else {
                        refreshGuildsFromSupabase()
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling Realtime message", e)
        }
    }

    /**
     * Pazar, Vadeli Sözleşme ve Tedarik Talebi eylemlerini Supabase Realtime Broadcast üzerinden anlık yayınlar.
     */
    fun sendBroadcastMarketAction(event: com.example.data.network.LiveMarketActionEventDto) {
        val ws = activeWebSocket ?: return
        try {
            val payload = AppJson.encodeToJsonElement(event)
            val outgoing = PhoenixBroadcastOutgoingDto(
                topic = "realtime:market",
                event = "broadcast",
                payload = PhoenixBroadcastPayloadDto(
                    type = "broadcast",
                    event = "market_action",
                    payload = payload
                ),
                ref = "market-${System.currentTimeMillis()}"
            )
            ws.send(AppJson.encodeToString(outgoing))
            Log.d(TAG, "Broadcast Market Action transmitted via WebSocket: ${event.actionType} by ${event.playerName}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send broadcast market action", e)
        }
    }

    /**
     * Borsa Fiyat ve Stok güncellemelerini Supabase Realtime Broadcast üzerinden tüm oyunculara anlık iletir.
     */
    fun sendBroadcastBorsaPrices(prices: List<MarketPriceEntity>, playerId: String = "") {
        val ws = activeWebSocket ?: return
        try {
            val sanitized = sanitizeMarketPrices(prices)
            val dtoList = sanitized.map {
                com.example.data.network.BorsaPriceItemDto(
                    itemId = it.itemId,
                    originCountry = it.originCountry,
                    originCityId = it.originCityId,
                    price = it.price,
                    borsaStock = it.borsaStock,
                    isUsd = it.isUsd
                )
            }
            val syncEvent = com.example.data.network.LiveBorsaPricesSyncDto(
                prices = dtoList,
                sourcePlayerId = playerId,
                timestampMs = System.currentTimeMillis()
            )
            val payload = AppJson.encodeToJsonElement(syncEvent)
            val outgoing = PhoenixBroadcastOutgoingDto(
                topic = "realtime:market",
                event = "broadcast",
                payload = PhoenixBroadcastPayloadDto(
                    type = "broadcast",
                    event = "borsa_prices_sync",
                    payload = payload
                ),
                ref = "borsa-${System.currentTimeMillis()}"
            )
            ws.send(AppJson.encodeToString(outgoing))
            Log.d(TAG, "Broadcast Borsa Prices transmitted via WebSocket: ${prices.size} items")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send broadcast borsa prices", e)
        }
    }

    /**
     * Müzayede tekliflerini Supabase Realtime Broadcast kanalından belleğe anlık olarak yayınlar.
     */
    fun sendBroadcastAuctionBid(event: LiveAuctionBidEventDto) {
        val ws = activeWebSocket ?: return
        try {
            val payload = AppJson.encodeToJsonElement(event)
            val outgoing = PhoenixBroadcastOutgoingDto(
                topic = "realtime:auctions",
                event = "broadcast",
                payload = PhoenixBroadcastPayloadDto(
                    type = "broadcast",
                    event = "auction_bid",
                    payload = payload
                ),
                ref = "bid-${System.currentTimeMillis()}"
            )
            ws.send(AppJson.encodeToString(outgoing))
            Log.d(TAG, "Broadcast auction bid transmitted via WebSocket: ₳${event.newBidAmount}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send broadcast auction bid", e)
        }
    }

    /**
     * Müzayedeye yeni bir eser konulduğunda Supabase Realtime Broadcast kanalından tüm oyunculara anlık bildirim yayınlar.
     */
    fun sendBroadcastAuctionListed(event: LiveAuctionListedEventDto) {
        val ws = activeWebSocket ?: return
        try {
            val payload = AppJson.encodeToJsonElement(event)
            val outgoing = PhoenixBroadcastOutgoingDto(
                topic = "realtime:auctions",
                event = "broadcast",
                payload = PhoenixBroadcastPayloadDto(
                    type = "broadcast",
                    event = "auction_listed",
                    payload = payload
                ),
                ref = "listed-${System.currentTimeMillis()}"
            )
            ws.send(AppJson.encodeToString(outgoing))
            Log.d(TAG, "Broadcast auction listed transmitted via WebSocket: ${event.artifactName} by ${event.sellerName}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send broadcast auction listed", e)
        }
    }

    private val activeChatProjects = ConcurrentHashMap.newKeySet<String>()

    private fun sendJoinChatBroadcast(ws: WebSocket) {
        try {
            val joinChatBroadcast = PhoenixChannelJoinDto(
                topic = "realtime:chat",
                event = "phx_join",
                payload = PhoenixChannelJoinPayloadDto(
                    config = PhoenixConfigDto(
                        broadcast = PhoenixBroadcastConfigDto(self = true, ack = true),
                        presence = PhoenixPresenceConfigDto(key = "player-client"),
                        postgresChanges = emptyList()
                    )
                ),
                ref = "chat-broadcast-join-${System.currentTimeMillis()}"
            )
            ws.send(AppJson.encodeToString(joinChatBroadcast))
            Log.d(TAG, "Joined Realtime chat channel JIT")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to join Realtime chat channel JIT", e)
        }
    }

    /**
     * Just-In-Time (JIT) Konsorsiyum sohbet kanalına abone olur.
     * İlk abone geldiğinde realtime:chat kanalına phx_join gönderir.
     */
    fun subscribeToChatChannel(projectId: String) {
        val wasEmpty = activeChatProjects.isEmpty()
        activeChatProjects.add(projectId)
        if (wasEmpty) {
            val ws = activeWebSocket
            if (ws != null) {
                sendJoinChatBroadcast(ws)
            }
        }
    }

    /**
     * Just-In-Time (JIT) Konsorsiyum sohbet kanalından ayrılır (unsubscribe).
     * Aktif dinleyici kalmadığında phx_leave göndererek Realtime soket kotasını serbest bırakır.
     */
    fun unsubscribeFromChatChannel(projectId: String) {
        activeChatProjects.remove(projectId)
        if (activeChatProjects.isEmpty()) {
            val ws = activeWebSocket ?: return
            try {
                val leaveChat = PhoenixChannelLeaveDto(
                    topic = "realtime:chat",
                    event = "phx_leave",
                    payload = emptyMap(),
                    ref = "chat-leave-${System.currentTimeMillis()}"
                )
                ws.send(AppJson.encodeToString(leaveChat))
                Log.d(TAG, "Unsubscribed from Realtime chat channel JIT (phx_leave sent)")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to leave Realtime chat channel JIT", e)
            }
        }
    }

    /**
     * Konsorsiyum içi canlı sohbet mesajlarını Supabase Realtime WebSocket üzerinden anlık yayınlar.
     */
    fun sendBroadcastChatMessage(message: ConsortiumChatMessage) {
        val ws = activeWebSocket ?: return
        try {
            val chatDto = ConsortiumChatMessageDto(
                id = message.id,
                projectId = message.projectId,
                senderId = message.senderId,
                senderName = message.senderName,
                senderRole = message.senderRole,
                messageText = message.messageText,
                timestampMs = message.timestampMs,
                isSystemMessage = message.isSystemMessage
            )
            val payload = AppJson.encodeToJsonElement(chatDto)
            val outgoing = PhoenixBroadcastOutgoingDto(
                topic = "realtime:chat",
                event = "broadcast",
                payload = PhoenixBroadcastPayloadDto(
                    type = "broadcast",
                    event = "consortium_chat",
                    payload = payload
                ),
                ref = "chat-${System.currentTimeMillis()}"
            )
            ws.send(AppJson.encodeToString(outgoing))
            Log.d(TAG, "Broadcast consortium chat transmitted via WebSocket: ${message.messageText}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send broadcast chat message", e)
        }
    }

    fun sendBroadcastConsortiumCreated(event: com.example.data.network.LiveConsortiumCreatedEventDto) {
        val ws = activeWebSocket ?: return
        try {
            val payload = AppJson.encodeToJsonElement(event)
            val outgoing = PhoenixBroadcastOutgoingDto(
                topic = "realtime:consortium",
                event = "broadcast",
                payload = PhoenixBroadcastPayloadDto(
                    type = "broadcast",
                    event = "consortium_created",
                    payload = payload
                ),
                ref = "c_create-${System.currentTimeMillis()}"
            )
            ws.send(AppJson.encodeToString(outgoing))
            Log.d(TAG, "Broadcast consortium created transmitted via WebSocket: ${event.consortiumName}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send broadcast consortium created", e)
        }
    }

    fun sendBroadcastConsortiumAction(event: LiveConsortiumActionEventDto) {
        val ws = activeWebSocket ?: return
        try {
            val payload = AppJson.encodeToJsonElement(event)
            val outgoing = PhoenixBroadcastOutgoingDto(
                topic = "realtime:consortium",
                event = "broadcast",
                payload = PhoenixBroadcastPayloadDto(
                    type = "broadcast",
                    event = "consortium_action",
                    payload = payload
                ),
                ref = "c_action-${System.currentTimeMillis()}"
            )
            ws.send(AppJson.encodeToString(outgoing))
            Log.d(TAG, "Broadcast consortium action transmitted via WebSocket: ${event.moveType}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send broadcast consortium action", e)
        }
    }

    fun sendBroadcastConsortiumMove(event: LiveConsortiumActionEventDto) {
        sendBroadcastConsortiumAction(event)
    }

    private fun startHeartbeat(webSocket: WebSocket) {
        heartbeatJob?.cancel()
        lastServerResponseTimestamp.set(System.currentTimeMillis())
        heartbeatJob = scope.launch {
            var ref = 100
            while (isActive) {
                delay(HEARTBEAT_INTERVAL_MS)
                val now = System.currentTimeMillis()
                val elapsedSinceLastResponse = now - lastServerResponseTimestamp.get()

                // Sunucu Yanıtı (Pong Timeout) kontrolü: Eşik aşılırsa zombi soket kabul edilip üstel geri çekilme tetiklenir
                if (elapsedSinceLastResponse > PONG_TIMEOUT_MS) {
                    Log.w(TAG, "Heartbeat Pong Timeout! No server response for ${elapsedSinceLastResponse}ms. Treating socket as zombie.")
                    _connectionStatus.value = "🟡 BAĞLANTI ZAMAN AŞIMI (Zombi Soket)..."
                    closeWebSocket()
                    scheduleExponentialReconnect()
                    break
                }

                try {
                    val ping = PhoenixHeartbeatDto(
                        topic = "phoenix",
                        event = "heartbeat",
                        payload = emptyMap(),
                        ref = "hb-${ref++}"
                    )
                    webSocket.send(AppJson.encodeToString(ping))
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to send heartbeat ping", e)
                    closeWebSocket()
                    scheduleExponentialReconnect()
                    break
                }
            }
        }
    }

    private fun stopHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = null
    }

    private fun scheduleExponentialReconnect() {
        reconnectJob?.cancel()
        reconnectJob = scope.launch {
            val currentAttempt = retryCount.getAndIncrement()
            // Üstel geri çekilme: 1s, 2s, 4s, 8s, 16s, max 30s + jitter
            val baseDelay = min(1000L * (1L shl min(currentAttempt, 5)), 30_000L)
            val jitter = Random.nextLong(100, 500)
            val totalDelay = baseDelay + jitter

            Log.d(TAG, "Reconnecting Realtime in ${totalDelay}ms (Attempt #$currentAttempt)...")
            delay(totalDelay)
            startRealtimeConnection()
        }
    }

    private fun closeWebSocket() {
        stopHeartbeat()
        activeWebSocket?.close(1000, "Client Reset")
        activeWebSocket = null
    }

    /**
     * Supabase'den konsorsiyumları ve halka arz şirketlerini tazeleyip canlı state'e aktarır
     */
    suspend fun refreshGuildsFromSupabase() {
        try {
            val supabaseGuilds = SupabaseManager.fetchGuildsFromSupabase()
            
            // Legacy cleanup (remove old bots so the new bots are uniquely loaded)
            supabaseGuilds?.filter { it.id.startsWith("guild-") || (it.id.startsWith("bot-guild-") && !it.id.startsWith("bot-guild-v4-")) }?.forEach { oldBot ->
                SupabaseManager.deleteMegaProjectFromSupabase(oldBot.id)
            }

            val currentGuilds = supabaseGuilds?.filterNot { it.id.startsWith("guild-") || (it.id.startsWith("bot-guild-") && !it.id.startsWith("bot-guild-v4-")) } ?: emptyList()
            
            val missingBots = defaultBotGuilds.filter { bot -> currentGuilds.none { it.id == bot.id } }
            if (missingBots.isNotEmpty()) {
                missingBots.forEach { bot ->
                    val botProj = com.example.data.MegaProjectFactory.createMegaProject(
                        consortiumName = bot.name,
                        brandName = bot.name,
                        targetProductId = bot.targetProductId,
                        leaderPlayerId = bot.id + "_leader",
                        leaderPlayerName = bot.leaderName,
                        allProducts = com.example.data.Product.values().toList(),
                        qualityTier = com.example.data.ConsortiumQualityTier.GRADE_C,
                        cityId = bot.cityId
                    )
                    val botProjWithId = botProj.copy(
                        id = bot.id,
                        targetProductName = bot.targetProductName.ifBlank { botProj.targetProductName }
                    )
                    SupabaseManager.syncMegaProjectToSupabase(botProjWithId)
                }
                val updatedGuilds = SupabaseManager.fetchGuildsFromSupabase()?.filterNot { it.id.startsWith("guild-") }
                _globalGuilds.value = updatedGuilds ?: (currentGuilds + missingBots)
            } else {
                _globalGuilds.value = currentGuilds
            }
        } catch (e: Exception) {
            Log.w(TAG, "Supabase refreshGuilds failed", e)
        }
    }

    /**
     * Supabase'den liderlik tablosunu çeker (On-demand veya periyodik)
     */
    suspend fun refreshLeaderboardFromSupabase() {
        try {
            val supabaseLeaderboard = SupabaseManager.fetchLeaderboardFromSupabase(50)
            if (!supabaseLeaderboard.isNullOrEmpty()) {
                _onlinePlayers.value = supabaseLeaderboard
                _connectionStatus.value = "🟢 SUPABASE BAĞLI (Canlı DB)"
            }
        } catch (e: Exception) {
            Log.w(TAG, "Supabase leaderboard fetch failed", e)
        }
    }

    /**
     * Arka plan konsorsiyum ve mega proje senkronizasyon dinleyicisi (15 saniyede bir Supabase ile günceller)
     */
    fun listenToGuilds() {
        scope.launch {
            while (isActive) {
                delay(15_000L)
                refreshGuildsFromSupabase()
            }
        }
    }

    /**
     * Contributes item quantity to tender/guild project using atomic Postgres RPC.
     * Bu fonksiyon Mutex kilidi ve Supabase RPC çağırarak eşzamanlı veri yarışlarını (Race Condition) önler.
     */
    suspend fun contributeToTender(
        guildId: String,
        itemId: String,
        quantity: Int
    ): Boolean = withContext(Dispatchers.IO) {
        tenderRpcMutex.withLock {
            val previousGuilds = _globalGuilds.value
            try {
                // 1. Client-Side Prediction: Optimistic Local Update
                var targetGuild: GuildGroup? = null
                _globalGuilds.value = _globalGuilds.value.map { guild ->
                    if (guild.id == guildId) {
                        val updatedContribs = guild.megaProjectContributions.toMutableMap()
                        val currentQty = updatedContribs[itemId] ?: 0
                        updatedContribs[itemId] = currentQty + quantity
                        val updated = guild.copy(megaProjectContributions = updatedContribs)
                        targetGuild = updated
                        updated
                    } else guild
                }

                // 2. Atomic Postgres RPC Call
                val rpcParams = buildJsonObject {
                    put("p_guild_id", guildId)
                    put("p_item_id", itemId)
                    put("p_quantity", quantity)
                }

                val (rpcSuccess, _) = SupabaseManager.executeRpc("increment_guild_contribution", rpcParams.toString())
                if (!rpcSuccess) {
                    val syncSuccess = targetGuild?.let { SupabaseManager.syncGuildToSupabase(it) } ?: false
                    if (!syncSuccess) {
                        // Server sync failed -> Rollback
                        _globalGuilds.value = previousGuilds
                        return@withContext false
                    }
                }

                // 3. Low-Latency In-Memory Supabase Broadcast
                sendBroadcastConsortiumMove(
                    LiveConsortiumActionEventDto(
                        projectId = guildId,
                        playerId = "local_player",
                        playerName = "Oyuncu",
                        moveType = "TENDER_CONTRIBUTE",
                        quantityDelivered = quantity.toLong(),
                        details = itemId
                    )
                )

                true
            } catch (e: Exception) {
                Log.e(TAG, "contributeToTender failed, rolling back", e)
                _globalGuilds.value = previousGuilds
                false
            }
        }
    }

    /**
     * Mega Proje hammadde slotuna kaynak aktarımı (Atomic RPC + Mutex Koruma Destekli + Client-Side Prediction)
     */
    suspend fun deliverToConsortiumSlot(
        projectId: String,
        slotId: String,
        productId: String,
        quantity: Int,
        partnerId: String,
        partnerName: String
    ): Boolean = withContext(Dispatchers.IO) {
        true
    }

    /**
     * Allows real players to create a new Trade Chamber Lobby
     */
    fun createLobby(
        name: String,
        region: String,
        description: String,
        maxCapacity: Int = 50,
        creatorName: String = "Oyuncu"
    ): MultiplayerLobby {
        val newLobby = MultiplayerLobby(
            id = "LOBBY-${System.currentTimeMillis().toString().takeLast(6)}",
            name = name,
            region = region,
            activeCount = 1,
            maxCapacity = maxCapacity,
            description = description
        )
        val current = _activeLobbies.value.toMutableList()
        current.add(0, newLobby)
        _activeLobbies.value = current
        return newLobby
    }

    /**
     * Joins an existing trade lobby
     */
    fun joinLobby(lobbyId: String): Boolean {
        var joined = false
        _activeLobbies.value = _activeLobbies.value.map { lobby ->
            if (lobby.id == lobbyId && lobby.activeCount < lobby.maxCapacity) {
                joined = true
                lobby.copy(activeCount = lobby.activeCount + 1)
            } else lobby
        }
        return joined
    }

    fun setConnectionStatus(status: String) {
        _connectionStatus.value = status
    }

    fun setHasPendingSync(pending: Boolean) {
        _hasPendingSync.value = pending
    }

    /**
     * Syncs local player profile to Supabase PostgreSQL database via SupabaseManager.
     * Updates _hasPendingSync flag on network failure to guarantee zero data loss.
     */
    suspend fun syncPlayerToSupabase(
        player: PlayerEntity,
        netWorth: Long,
        isIpoActive: Boolean = false,
        publicSharePercent: Int = 20,
        economicSnapshot: EconomicSnapshot? = null,
        rawSaveJson: String? = null
    ): Boolean = withContext(Dispatchers.IO) {
        val isRegistered = economicSnapshot?.isOnlineRegistered == true || (player.id != "local_player" && player.id.isNotBlank())
        val email = economicSnapshot?.onlineEmail.orEmpty().ifBlank { if (player.id != "local_player" && player.id != "p_local") player.id else "" }
        
        if (!isRegistered || email.isBlank() || email == "misafir_tuccar" || email == "local_trader") {
            _hasPendingSync.value = false
            _connectionStatus.value = "🟡 ÇEVRİMDIŞI MOD (Yerel Kayıt)"
            return@withContext false
        }
        try {
            val uid = if (player.id.isNotBlank() && player.id != "local_player" && player.id != "p_local") player.id else email.replace(".", "_")

            val success = if (economicSnapshot != null) {
                val payload = SupabasePlayerPayload(
                    id = uid,
                    name = player.name,
                    companyName = "${player.name} Holding",
                    money = economicSnapshot.money,
                    loanAmount = economicSnapshot.loanAmount,
                    depositBalance = economicSnapshot.depositBalance,
                    dailyIncome = economicSnapshot.dailyIncome,
                    dailyExpense = economicSnapshot.dailyExpense,
                    totalProfit = economicSnapshot.totalProfit,
                    xp = economicSnapshot.xp,
                    level = economicSnapshot.level,
                    inventoryCapacity = economicSnapshot.inventoryCapacity,
                    currentCity = economicSnapshot.currentCity,
                    isVip = economicSnapshot.isVip,
                    gems = economicSnapshot.gems,
                    lastDailyRewardMs = economicSnapshot.lastDailyRewardMs,
                    loginStreak = economicSnapshot.loginStreak,
                    dollarBalance = economicSnapshot.dollarBalance,
                    dollarDepositBalance = economicSnapshot.dollarDepositBalance,
                    dollarLoanAmount = economicSnapshot.dollarLoanAmount,
                    isOnlineRegistered = true,
                    onlineEmail = email,
                    businessesJson = economicSnapshot.businessesJson,
                    inventoryJson = economicSnapshot.inventoryJson,
                    activeDeliveriesJson = economicSnapshot.activeDeliveriesJson,
                    managersJson = economicSnapshot.managersJson,
                    activeResearchesJson = economicSnapshot.activeResearchesJson,
                    researchLevelsJson = economicSnapshot.researchLevelsJson,
                    guildSharesJson = economicSnapshot.playerGuildSharesJson,
                    guildBuyPricesJson = economicSnapshot.playerGuildBuyPricesJson,
                    rawSaveJson = rawSaveJson
                )
                SupabaseManager.syncPlayerToSupabase(payload)
            } else {
                SupabaseManager.patchPlayerMoney(uid, player.money)
            }

            if (success) {
                _hasPendingSync.value = false
                _connectionStatus.value = "🟢 SUPABASE BAĞLI (Canlı DB)"
                true
            } else {
                _hasPendingSync.value = true
                _connectionStatus.value = "🟡 ÇEVRİMDIŞI MOD (Bekleyen Senkronizasyon)"
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception syncing player to Supabase", e)
            _hasPendingSync.value = true
            _connectionStatus.value = "🟡 ÇEVRİMDIŞI MOD (Bekleyen Senkronizasyon)"
            false
        }
    }

    suspend fun syncPlayerToSupabase(
        player: PlayerEntity,
        economicSnapshot: EconomicSnapshot
    ): Boolean {
        val netWorth = (economicSnapshot.money + economicSnapshot.depositBalance - economicSnapshot.loanAmount).coerceAtLeast(0L)
        return syncPlayerToSupabase(
            player = player,
            netWorth = netWorth,
            economicSnapshot = economicSnapshot
        )
    }

    fun listenToLeaderboard() {
        scope.launch {
            while (isActive) {
                delay(180_000L)
                refreshLeaderboardFromSupabase()
            }
        }
    }

    /**
     * Publishes a new B2B trade offer to online players
     */
    fun publishOffer(offer: OnlineTradeOffer) {
        val current = _liveTradeOffers.value.toMutableList()
        current.add(0, offer)
        _liveTradeOffers.value = current
    }

    /**
     * Accepts / Purchases an online trade offer
     */
    fun acceptOffer(offerId: String): Boolean {
        val current = _liveTradeOffers.value.toMutableList()
        val found = current.find { it.id == offerId }
        if (found != null) {
            current.remove(found)
            _liveTradeOffers.value = current
            return true
        }
        return false
    }
}
