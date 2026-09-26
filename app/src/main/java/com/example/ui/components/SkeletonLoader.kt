package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.ThemeBorder
import com.example.ui.theme.ThemeGold
import com.example.ui.theme.ThemeNeonCyan

/**
 * High-performance, hardware-accelerated Skeleton Shimmer Modifier.
 * Produces a sleek, glowing sweep animation across UI placeholders.
 */
fun Modifier.skeletonShimmer(
    enabled: Boolean = true,
    shape: Shape = RoundedCornerShape(8.dp),
    baseColor: Color = Color(0xFF131D2E),
    highlightColor: Color = Color(0xFF24344D),
    durationMillis: Int = 1200
): Modifier = composed {
    if (!enabled) return@composed this

    val transition = rememberInfiniteTransition(label = "skeleton_shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = -500f,
        targetValue = 1200f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = durationMillis, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_offset"
    )

    this
        .clip(shape)
        .background(baseColor, shape)
        .drawWithContent {
            drawContent()
            drawRect(
                brush = Brush.linearGradient(
                    colors = listOf(
                        baseColor.copy(alpha = 0.2f),
                        highlightColor.copy(alpha = 0.85f),
                        baseColor.copy(alpha = 0.2f)
                    ),
                    start = Offset(translateAnim, translateAnim),
                    end = Offset(translateAnim + 250f, translateAnim + 250f)
                )
            )
        }
}

/**
 * Generic Skeleton Box for placeholder blocks
 */
@Composable
fun SkeletonBox(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(8.dp),
    baseColor: Color = Color(0xFF131D2E),
    highlightColor: Color = Color(0xFF24344D)
) {
    Box(
        modifier = modifier.skeletonShimmer(
            enabled = true,
            shape = shape,
            baseColor = baseColor,
            highlightColor = highlightColor
        )
    )
}

/**
 * Generic Skeleton Text Bar
 */
@Composable
fun SkeletonText(
    modifier: Modifier = Modifier,
    width: Dp = 100.dp,
    height: Dp = 14.dp,
    shape: Shape = RoundedCornerShape(4.dp)
) {
    SkeletonBox(
        modifier = modifier
            .width(width)
            .height(height),
        shape = shape
    )
}

/**
 * Generic Skeleton Circle for avatars and badges
 */
@Composable
fun SkeletonCircle(
    size: Dp = 40.dp,
    modifier: Modifier = Modifier
) {
    SkeletonBox(
        modifier = modifier.size(size),
        shape = CircleShape
    )
}

/**
 * Generic Skeleton Card Container with glowing border
 */
@Composable
fun SkeletonCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(12.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier,
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0D1524)),
        border = BorderStroke(1.dp, Color(0xFF1E293B))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            content = content
        )
    }
}

/**
 * Full-screen / Component Skeleton for Warehouse & Inventory (Depo)
 */
@Composable
fun InventoryScreenSkeleton(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Warehouse Capacity & Valuation Header Card Skeleton
        SkeletonCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SkeletonCircle(size = 42.dp)
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        SkeletonText(width = 120.dp, height = 16.dp)
                        SkeletonText(width = 80.dp, height = 12.dp)
                    }
                }
                SkeletonBox(
                    modifier = Modifier
                        .width(90.dp)
                        .height(32.dp),
                    shape = RoundedCornerShape(6.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Capacity Bar Placeholder
            SkeletonBox(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                shape = RoundedCornerShape(4.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Stats row (Valuation & Net worth)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                SkeletonText(width = 100.dp, height = 12.dp)
                SkeletonText(width = 110.dp, height = 12.dp)
            }
        }

        // Search & Filter Row Skeleton
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SkeletonBox(
                modifier = Modifier
                    .weight(1f)
                    .height(42.dp),
                shape = RoundedCornerShape(8.dp)
            )
            SkeletonBox(
                modifier = Modifier
                    .width(42.dp)
                    .height(42.dp),
                shape = RoundedCornerShape(8.dp)
            )
        }

        // Tier Filter Chips Row Skeleton
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(5) {
                SkeletonBox(
                    modifier = Modifier
                        .width(70.dp)
                        .height(28.dp),
                    shape = RoundedCornerShape(14.dp)
                )
            }
        }

        // Inventory Items Grid Skeleton
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(6) {
                SkeletonCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SkeletonCircle(size = 32.dp)
                        SkeletonBox(
                            modifier = Modifier
                                .width(45.dp)
                                .height(16.dp),
                            shape = RoundedCornerShape(4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    SkeletonText(width = 110.dp, height = 14.dp)
                    Spacer(modifier = Modifier.height(4.dp))
                    SkeletonText(width = 70.dp, height = 11.dp)

                    Spacer(modifier = Modifier.height(12.dp))
                    SkeletonBox(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(30.dp),
                        shape = RoundedCornerShape(6.dp)
                    )
                }
            }
        }
    }
}

/**
 * Full-screen / Component Skeleton for Consortium Hub (Konsorsiyum Merkezi)
 */
@Composable
fun MegaProjectHubSkeleton(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Consortium Hub Overview Header Card Skeleton
        SkeletonCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SkeletonCircle(size = 40.dp)
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        SkeletonText(width = 160.dp, height = 16.dp)
                        SkeletonText(width = 100.dp, height = 12.dp)
                    }
                }
                SkeletonBox(
                    modifier = Modifier
                        .width(75.dp)
                        .height(28.dp),
                    shape = RoundedCornerShape(6.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // System Strain Indicator Skeleton
            SkeletonBox(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(36.dp),
                shape = RoundedCornerShape(8.dp)
            )
        }

        // Tab Filter Buttons Skeleton
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(4) {
                SkeletonBox(
                    modifier = Modifier
                        .width(90.dp)
                        .height(32.dp),
                    shape = RoundedCornerShape(6.dp)
                )
            }
        }

        // Projects List Skeleton
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(4) {
                SkeletonCard(modifier = Modifier.fillMaxWidth()) {
                    // Project Title & Stage Badge
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            SkeletonText(width = 140.dp, height = 15.dp)
                            SkeletonText(width = 90.dp, height = 11.dp)
                        }
                        SkeletonBox(
                            modifier = Modifier
                                .width(80.dp)
                                .height(22.dp),
                            shape = RoundedCornerShape(11.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Progress & Investment Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SkeletonText(width = 110.dp, height = 12.dp)
                        SkeletonText(width = 70.dp, height = 12.dp)
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    SkeletonBox(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp),
                        shape = RoundedCornerShape(3.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Supplier Slot Rows Placeholder
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        repeat(3) {
                            SkeletonBox(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp),
                                shape = RoundedCornerShape(6.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Action Button
                    SkeletonBox(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(36.dp),
                        shape = RoundedCornerShape(6.dp)
                    )
                }
            }
        }
    }
}

/**
 * Full-screen / Component Skeleton for Antique Museum & Live Auctions (Müzayede)
 */
@Composable
fun AntiqueAuctionSkeleton(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Museum Prestige & Visitor Revenue Header Skeleton
        SkeletonCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SkeletonCircle(size = 40.dp)
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        SkeletonText(width = 150.dp, height = 15.dp)
                        SkeletonText(width = 110.dp, height = 12.dp)
                    }
                }
                SkeletonBox(
                    modifier = Modifier
                        .width(85.dp)
                        .height(30.dp),
                    shape = RoundedCornerShape(6.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Income / Prestige Stat Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SkeletonBox(
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp),
                    shape = RoundedCornerShape(6.dp)
                )
                SkeletonBox(
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp),
                    shape = RoundedCornerShape(6.dp)
                )
            }
        }

        // Tabs Row Skeleton (Vitrini, Canlı Müzayede, Gelir)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            repeat(3) {
                SkeletonBox(
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp),
                    shape = RoundedCornerShape(8.dp)
                )
            }
        }

        // Auction Items Grid Skeleton
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(6) {
                SkeletonCard(modifier = Modifier.fillMaxWidth()) {
                    // Rarity pill & Era
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        SkeletonBox(
                            modifier = Modifier
                                .width(50.dp)
                                .height(16.dp),
                            shape = RoundedCornerShape(4.dp)
                        )
                        SkeletonBox(
                            modifier = Modifier
                                .width(40.dp)
                                .height(16.dp),
                            shape = RoundedCornerShape(4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Artifact Thumbnail Placeholder
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(75.dp)
                            .skeletonShimmer(shape = RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        SkeletonCircle(size = 36.dp)
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    SkeletonText(width = 120.dp, height = 14.dp)
                    Spacer(modifier = Modifier.height(4.dp))
                    SkeletonText(width = 80.dp, height = 11.dp)

                    Spacer(modifier = Modifier.height(10.dp))

                    // Bid & Button Placeholder
                    SkeletonBox(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(32.dp),
                        shape = RoundedCornerShape(6.dp)
                    )
                }
            }
        }
    }
}
