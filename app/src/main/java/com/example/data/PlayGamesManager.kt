package com.example.data

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.games.GamesSignInClient
import com.google.android.gms.games.PlayGames
import com.google.android.gms.games.PlayGamesSdk

object PlayGamesManager {
    private const val TAG = "PlayGamesManager"
    private var isGmsAvailable = false

    fun initialize(context: Context) {
        try {
            val availability = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context)
            if (availability != ConnectionResult.SUCCESS) {
                Log.w(TAG, "Google Play Services not available (code: $availability), skipping Play Games SDK init.")
                isGmsAvailable = false
                return
            }
            isGmsAvailable = true
            PlayGamesSdk.initialize(context)
            Log.d(TAG, "Play Games SDK initialized successfully.")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Play Games SDK", e)
            isGmsAvailable = false
        }
    }

    fun checkAuthentication(activity: Activity, onResult: (Boolean, String?, String?) -> Unit) {
        if (!isGmsAvailable) {
            onResult(false, null, null)
            return
        }
        try {
            val gamesSignInClient: GamesSignInClient = PlayGames.getGamesSignInClient(activity)
            gamesSignInClient.isAuthenticated.addOnCompleteListener { task ->
                if (task.isSuccessful && task.result.isAuthenticated) {
                    try {
                        PlayGames.getPlayersClient(activity).currentPlayer.addOnCompleteListener { playerTask ->
                            if (playerTask.isSuccessful && playerTask.result != null) {
                                val player = playerTask.result
                                val playerId = player.playerId
                                val displayName = player.displayName
                                Log.d(TAG, "User is authenticated with Play Games Services: $displayName ($playerId)")
                                onResult(true, playerId, displayName)
                            } else {
                                Log.d(TAG, "Authenticated with Play Games Services but could not fetch player profile.")
                                onResult(true, null, null)
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error fetching current Play Games player", e)
                        onResult(true, null, null)
                    }
                } else {
                    Log.d(TAG, "User is not authenticated with Play Games Services.")
                    onResult(false, null, null)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking Play Games authentication", e)
            onResult(false, null, null)
        }
    }

    fun checkAuthentication(activity: Activity, onResult: (Boolean, String?) -> Unit) {
        checkAuthentication(activity) { success, playerId, _ ->
            onResult(success, playerId)
        }
    }
}

