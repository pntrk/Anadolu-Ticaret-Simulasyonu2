package com.example.data

object XpLevelEngine {
    /**
     * XP required to complete level [level] and advance to [level] + 1.
     * Max level is 20, with progressive challenging milestones.
     */
    fun getXpNeededForLevel(level: Int): Long {
        return when (level) {
            1 -> 1_000L
            2 -> 2_500L
            3 -> 5_000L
            4 -> 8_500L
            5 -> 13_000L
            6 -> 19_000L
            7 -> 27_000L
            8 -> 37_000L
            9 -> 50_000L
            10 -> 65_000L
            11 -> 85_000L
            12 -> 110_000L
            13 -> 140_000L
            14 -> 175_000L
            15 -> 220_000L
            16 -> 275_000L
            17 -> 340_000L
            18 -> 420_000L
            19 -> 510_000L
            else -> 620_000L
        }
    }

    /**
     * Total cumulative XP needed from Level 1 to start level [targetLevel].
     */
    fun getCumulativeXpForLevel(targetLevel: Int): Long {
        var total = 0L
        for (lvl in 1 until targetLevel.coerceIn(1, 20)) {
            total += getXpNeededForLevel(lvl)
        }
        return total
    }

    /**
     * Calculates the player level given total accumulated XP, capped at max level 20.
     */
    fun calculateLevel(totalXp: Long): Int {
        var lvl = 1
        var accum = 0L
        while (lvl < 20) {
            val needed = getXpNeededForLevel(lvl)
            if (totalXp >= accum + needed) {
                accum += needed
                lvl++
            } else {
                break
            }
        }
        return lvl.coerceIn(1, 20)
    }

    data class LevelProgress(
        val level: Int,
        val currentLevelXp: Long,
        val targetLevelXp: Long,
        val progressFraction: Float
    )

    fun getProgress(totalXp: Long): LevelProgress {
        val lvl = calculateLevel(totalXp)
        if (lvl >= 20) {
            return LevelProgress(20, getXpNeededForLevel(20), getXpNeededForLevel(20), 1f)
        }
        val startXp = getCumulativeXpForLevel(lvl)
        val targetXp = getXpNeededForLevel(lvl)
        val currentXp = (totalXp - startXp).coerceAtLeast(0L)
        val fraction = (currentXp.toFloat() / targetXp.toFloat()).coerceIn(0f, 1f)
        return LevelProgress(lvl, currentXp, targetXp, fraction)
    }
}

