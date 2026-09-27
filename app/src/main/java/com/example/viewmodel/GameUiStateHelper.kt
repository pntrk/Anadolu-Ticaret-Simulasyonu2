package com.example.viewmodel

import kotlinx.collections.immutable.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import androidx.compose.runtime.snapshotFlow
import com.example.data.*

private val FACILITY_BASE_COSTS: Map<String, Long> by lazy {
    Product.values().associateBy({ it.facilityId }, { it.facilityCost })
}

fun GameViewModel.startUiStateSync(scope: CoroutineScope) {
    scope.launch {
        snapshotFlow { macroState }.collect { macro ->
            updateUiState { copy(macroState = macro) }
        }
    }

    scope.launch {
        var lastUpdateMs = 0L
        combine(
            productionProgress,
            productionDurations
        ) { prog, durs ->
            Pair(prog, durs)
        }.collect { (prog, durs) ->
            val now = System.currentTimeMillis()
            if (now - lastUpdateMs >= 1500L || prog.isEmpty()) {
                lastUpdateMs = now
                updateUiState { copy(productionProgress = prog.toPersistentMap(), productionDurations = durs.toPersistentMap()) }
            }
        }
    }

    scope.launch {
        combine(
            player,
            totalDividendsPaid,
            publicSharePercent,
            isIpoActive
        ) { p, divs, pubShare, ipoActive ->
            PlayerUiState(
                player = p,
                totalDividendsPaid = divs,
                publicSharePercent = pubShare,
                isIpoActive = ipoActive
            )
        }.collect { playerState ->
            updateUiState { copy(playerState = playerState, isLoading = false) }
        }
    }

    scope.launch {
        combine(
            inventory,
            reservedInventory,
            activeDeliveries,
            activeProductions
        ) { inv, res, active, prods ->
            InventoryUiState(
                items = inv.toPersistentList(),
                reservedItems = res.toPersistentMap(),
                activeDeliveries = active.toPersistentList(),
                activeProductions = prods.toPersistentList()
            )
        }.collect { invState ->
            updateUiState { copy(inventoryState = invState) }
        }
    }

    scope.launch {
        combine(
            marketPrices,
            priceHistory,
            activeCityEvents,
            marketListings,
            buyOrders,
            futuresContracts,
            auctions,
            borsaLimitOrders
        ) { args: Array<Any?> ->
            @Suppress("UNCHECKED_CAST")
            val prices = args[0] as List<com.example.data.MarketPriceEntity>
            @Suppress("UNCHECKED_CAST")
            val hist = args[1] as Map<String, List<Long>>
            @Suppress("UNCHECKED_CAST")
            val events = args[2] as List<com.example.data.CityMarketEvent>
            @Suppress("UNCHECKED_CAST")
            val listings = args[3] as List<com.example.data.MarketListing>
            @Suppress("UNCHECKED_CAST")
            val bo = args[4] as List<com.example.data.BuyOrder>
            @Suppress("UNCHECKED_CAST")
            val fc = args[5] as List<com.example.data.FuturesContract>
            @Suppress("UNCHECKED_CAST")
            val auc = args[6] as List<com.example.data.Auction>
            @Suppress("UNCHECKED_CAST")
            val orders = args[7] as List<com.example.data.BorsaLimitOrder>

            val trends = mutableMapOf<String, Float>()
            for (mp in prices) {
                val h = hist[mp.itemId]
                if (h != null && h.size >= 2) {
                    val current = h.last().toFloat()
                    val prev = h[h.size - 2].toFloat()
                    if (prev > 0) {
                        trends[mp.itemId] = ((current - prev) / prev).coerceIn(-0.90f, 5.0f)
                    }
                }
            }
            MarketUiState(
                prices = prices.toPersistentList(),
                priceHistory = hist.mapValues { it.value.toPersistentList() }.toPersistentMap(),
                nextBorsaTickSeconds = nextBorsaTickSeconds.value,
                activeCityEvents = events.toPersistentList(),
                marketListings = listings.toPersistentList(),
                buyOrders = bo.toPersistentList(),
                futuresContracts = fc.toPersistentList(),
                auctions = auc.toPersistentList(),
                marketTrends = trends.toPersistentMap(),
                borsaLimitOrders = orders.toPersistentList()
            )
        }.collect { marketState ->
            updateUiState { copy(marketState = marketState) }
        }
    }

    scope.launch {
        combine(
            megaProjects,
            consortiumChatMessages,
            unreadChatProjects,
            consortiumCrisisState
        ) { projs, chat, unread, crisis ->
            ConsortiumUiState(
                megaProjects = projs.toPersistentList(),
                chatMessages = chat.mapValues { it.value.toPersistentList() }.toPersistentMap(),
                unreadChatProjects = unread.toPersistentSet(),
                crisisState = crisis
            )
        }.collect { consState ->
            updateUiState { copy(consortiumState = consState) }
        }
    }

    scope.launch {
        combine(
            managers,
            businesses,
            newsTickerMessage
        ) { mgrs, bus, news ->
            updateUiState {
                copy(
                    managers = mgrs.toPersistentList(),
                    businesses = bus.toPersistentList(),
                    newsTickerMessage = news
                )
            }
        }.collect {}
    }

    scope.launch {
        combine(
            dailyRewardDialogData,
            offlineEarningsData,
            economicSnapshot
        ) { daily, offline, snapshot ->
            updateUiState {
                copy(
                    dailyRewardData = daily,
                    offlineEarningsData = offline,
                    economicSnapshot = snapshot
                )
            }
        }.collect {}
    }

    scope.launch {
        combine(
            hasCompletedFirstTrade,
            tradeTutorialStep,
            isEntrepreneurGuideCompleted
        ) { hasFirst, tutorialStep, isEntComp ->
            updateUiState {
                copy(
                    hasCompletedFirstTrade = hasFirst,
                    tradeTutorialStep = tutorialStep,
                    isEntrepreneurGuideCompleted = isEntComp
                )
            }
        }.collect {}
    }

    scope.launch {
        combine(
            isOnlineRegistered,
            isGoogleSignedIn,
            isExpertMode,
            gameState
        ) { online, googleSignedIn, expert, state ->
            updateUiState {
                copy(
                    settingsState = SettingsUiState(
                        isOnlineRegistered = online,
                        isGoogleSignedIn = googleSignedIn,
                        isExpertMode = expert
                    ),
                    gameStateObj = state ?: GameStateEntity()
                )
            }
        }.collect {}
    }

    scope.launch {
        combine(
            activeResearches,
            researchLevels
        ) { activeResearches, researchLevels ->
            updateUiState {
                copy(hrState = HrUiState(activeResearches = activeResearches.toPersistentMap(), researchLevels = researchLevels.toPersistentMap()))
            }
        }.collect {}
    }

    scope.launch {
        combine(
            guilds,
            playerGuildShares,
            playerGuildBuyPrices
        ) { g, shares, buyPrices ->
            updateUiState {
                copy(
                    guildsState = GuildsUiState(guilds = g.toPersistentList(), playerGuildShares = shares.toPersistentMap(), playerGuildBuyPrices = buyPrices.toPersistentMap())
                )
            }
        }.collect {}
    }

    scope.launch {
        combine(
            player,
            inventory,
            businesses,
            marketPrices
        ) { p, inv, bus, prices ->
            val capacity = p?.inventoryCapacity ?: 5000
            val currentCount = inv.sumOf { it.quantity }
            val fillPerc = if (capacity > 0) kotlin.math.max(0, currentCount).toFloat() / capacity else 0f
            
            val priceMap = prices.associateBy({ it.itemId }, { it.price })

            val top = inv.sortedByDescending { i ->
                val price = priceMap[i.itemId] ?: 0L
                price * i.quantity
            }.take(3)
            
            val facVal = bus.sumOf { b ->
                val baseCost = this@startUiStateSync.calculateFacilityBaseCost(b)
                (baseCost * b.level * 0.9f).toLong()
            }
            
            val invVal = inv.sumOf { i ->
                val price = priceMap[i.itemId] ?: 100L
                (price * i.quantity * 0.8f).toLong()
            }
            
            val consortiumDeliveredVal = this@startUiStateSync.calculateConsortiumDeliveredMaterialsValuation()
            val consortiumTotalVal = this@startUiStateSync.calculateConsortiumValuation()
            val guildSharesVal = (consortiumTotalVal - consortiumDeliveredVal).coerceAtLeast(0L)
            val nw = this@startUiStateSync.calculateCompanyValuation()
            
            updateUiState {
                copy(
                    currentInvCount = currentCount,
                    inventoryFillPercentage = fillPerc,
                    topInventory = top.toPersistentList(),
                    facilityValuation = facVal,
                    inventoryValuation = invVal,
                    consortiumValuation = consortiumTotalVal,
                    consortiumDeliveredValuation = consortiumDeliveredVal,
                    guildSharesValuation = guildSharesVal,
                    netWorth = nw,
                    growthHistory = _growthHistory.value.toPersistentList()
                )
            }
        }.collect {}
    }

    scope.launch {
        _megaProjects.collect {
            val consortiumDeliveredVal = this@startUiStateSync.calculateConsortiumDeliveredMaterialsValuation()
            val consortiumTotalVal = this@startUiStateSync.calculateConsortiumValuation()
            val guildSharesVal = (consortiumTotalVal - consortiumDeliveredVal).coerceAtLeast(0L)
            val nw = this@startUiStateSync.calculateCompanyValuation()
            updateUiState {
                copy(
                    consortiumValuation = consortiumTotalVal,
                    consortiumDeliveredValuation = consortiumDeliveredVal,
                    guildSharesValuation = guildSharesVal,
                    netWorth = nw
                )
            }
        }
    }
}
