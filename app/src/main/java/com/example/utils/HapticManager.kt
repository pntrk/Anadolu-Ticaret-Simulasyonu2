package com.example.utils

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

object HapticManager {
    private var vibrator: Vibrator? = null
    private var sharedPrefs: android.content.SharedPreferences? = null
    private var isVibratorWorking: Boolean? = null

    fun init(context: Context) {
        val appContext = context.applicationContext
        vibrator = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = appContext.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                appContext.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
        } catch (_: Throwable) {
            null
        }
        sharedPrefs = try { appContext.getSharedPreferences("performance_prefs", Context.MODE_PRIVATE) } catch (_: Throwable) { null }
        isHapticEnabled = sharedPrefs?.getBoolean("is_haptic_enabled", false) ?: false
        isAnimationsEnabled = sharedPrefs?.getBoolean("is_animations_enabled", false) ?: false

        isVibratorWorking = try {
            vibrator?.hasVibrator() == true
        } catch (_: Throwable) {
            false
        }
    }

    enum class HapticType {
        LIGHT_CLICK,         // Light tap for minor UI interactions
        BUY_SELL,            // Distinct double pulse for stock/commodity trades
        CONSORTIUM_APPROVAL, // Strong multi-pulse vibration for consortium approvals & serial production
        WAREHOUSE_CHANGE,    // Crisp vibration on inventory deposit/withdrawal
        WAREHOUSE_WARNING,   // Alert pattern when warehouse is critically full (>85%)
        ERROR                // Distinct error warning pattern for transaction rollbacks
    }

    var isHapticEnabled: Boolean = false
        set(value) {
            field = value
            sharedPrefs?.edit()?.putBoolean("is_haptic_enabled", value)?.apply()
        }

    var isAnimationsEnabled: Boolean = false
        set(value) {
            field = value
            sharedPrefs?.edit()?.putBoolean("is_animations_enabled", value)?.apply()
        }

    fun performHaptic(type: HapticType) {
        if (!isHapticEnabled) return
        if (isVibratorWorking == false) return
        val vib = vibrator ?: return

        try {
            if (isVibratorWorking == null) {
                isVibratorWorking = vib.hasVibrator()
                if (isVibratorWorking == false) return
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val effect = when (type) {
                    HapticType.LIGHT_CLICK -> VibrationEffect.createOneShot(18, 120)
                    HapticType.BUY_SELL -> VibrationEffect.createWaveform(
                        longArrayOf(0, 30, 40, 35),
                        intArrayOf(0, 180, 0, 240),
                        -1
                    )
                    HapticType.CONSORTIUM_APPROVAL -> VibrationEffect.createWaveform(
                        longArrayOf(0, 50, 50, 60, 50, 80),
                        intArrayOf(0, 200, 0, 255, 0, 255),
                        -1
                    )
                    HapticType.WAREHOUSE_CHANGE -> VibrationEffect.createOneShot(45, 200)
                    HapticType.WAREHOUSE_WARNING -> VibrationEffect.createWaveform(
                        longArrayOf(0, 60, 80, 60),
                        intArrayOf(0, 220, 0, 220),
                        -1
                    )
                    HapticType.ERROR -> VibrationEffect.createWaveform(
                        longArrayOf(0, 80, 60, 80),
                        intArrayOf(0, 255, 0, 255),
                        -1
                    )
                }
                vib.vibrate(effect)
            } else {
                @Suppress("DEPRECATION")
                when (type) {
                    HapticType.LIGHT_CLICK -> vib.vibrate(20)
                    HapticType.BUY_SELL -> vib.vibrate(longArrayOf(0, 30, 40, 35), -1)
                    HapticType.CONSORTIUM_APPROVAL -> vib.vibrate(longArrayOf(0, 50, 50, 60, 50, 80), -1)
                    HapticType.WAREHOUSE_CHANGE -> vib.vibrate(45)
                    HapticType.WAREHOUSE_WARNING, HapticType.ERROR -> vib.vibrate(longArrayOf(0, 60, 80, 60), -1)
                }
            }
        } catch (_: Throwable) {
            isVibratorWorking = false
        }
    }
}
