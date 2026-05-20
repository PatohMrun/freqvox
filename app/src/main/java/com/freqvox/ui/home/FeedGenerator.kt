package com.freqvox.ui.home

import com.freqvox.data.local.entities.TrackEntity
import kotlin.random.Random

sealed class FeedItem {
    data class HorizontalCarousel(val title: String, val tracks: List<TrackEntity>) : FeedItem()
    data class DoubleHorizontalCarousel(val title: String, val tracks: List<TrackEntity>) : FeedItem()
    data class VerticalList(val title: String?, val tracks: List<TrackEntity>) : FeedItem()
    data class Grid(val title: String, val tracks: List<TrackEntity>) : FeedItem()
    data class Spotlight(val track: TrackEntity) : FeedItem()
    data class FeaturedBanner(val track: TrackEntity) : FeedItem()
}

object FeedGenerator {
    private val carouselTitles = listOf("Late Night Vibes", "Heavy Rotation", "Forgotten Gems", "Weekend Drive", "Deep Cuts", "Synth Heavy")
    private val gridTitles = listOf("Recently Played", "Quick Hits", "Your Mix", "Rediscover", "Favorites")

    /**
     * Slices a massive list of tracks into unpredictable, varied visual feed blocks.
     */
    fun generateFeed(tracks: List<TrackEntity>): List<FeedItem> {
        if (tracks.isEmpty()) return emptyList()

        val feed = mutableListOf<FeedItem>()
        var currentIndex = 0
        var previousBlockCategory = -1

        while (currentIndex < tracks.size) {
            var blockType: Int
            var currentCategory: Int
            
            // Reroll to guarantee we never get two consecutive blocks of the same Visual Category
            do {
                val roll = Random.Default.nextFloat()
                blockType = when {
                    roll < 0.05f -> 0 // Spotlight (5%)
                    roll < 0.15f -> 1 // Featured Banner (10%)
                    roll < 0.25f -> 2 // Grid (10%)
                    roll < 0.30f -> 3 // Double Carousel (5% - Extremely Rare)
                    roll < 0.55f -> 4 // Single Carousel (25%)
                    else -> 5 // Vertical List (45% standard filler)
                }
                
                // Group blocks into visual categories
                currentCategory = when(blockType) {
                    0, 1 -> 0 // Massive/Banner Cards
                    2 -> 1    // Grids
                    3, 4 -> 2 // Horizontal Carousels (Scroll X)
                    else -> 3 // Vertical Lists
                }
            } while (currentCategory == previousBlockCategory)
            
            previousBlockCategory = currentCategory
            
            when (blockType) {
                // Spotlight card (1 track)
                0 -> {
                    feed.add(FeedItem.Spotlight(tracks[currentIndex]))
                    currentIndex += 1
                }
                
                // Featured Banner (1 track)
                1 -> {
                    feed.add(FeedItem.FeaturedBanner(tracks[currentIndex]))
                    currentIndex += 1
                }
                
                // Grid (up to 4 tracks)
                2 -> {
                    val take = minOf(4, tracks.size - currentIndex)
                    if (take >= 2) { // Grids look bad with 1 item, so enforce min 2
                        feed.add(
                            FeedItem.Grid(
                                title = gridTitles.random(Random.Default),
                                tracks = tracks.subList(currentIndex, currentIndex + take)
                            )
                        )
                        currentIndex += take
                    } else {
                        feed.add(FeedItem.VerticalList(null, tracks.subList(currentIndex, currentIndex + take)))
                        currentIndex += take
                    }
                }
                
                // Double Carousel (up to 12 tracks)
                3 -> {
                    val take = minOf(12, tracks.size - currentIndex)
                    if (take >= 6) { // Need enough tracks to justify a double row
                        feed.add(
                            FeedItem.DoubleHorizontalCarousel(
                                title = carouselTitles.random(Random.Default),
                                tracks = tracks.subList(currentIndex, currentIndex + take)
                            )
                        )
                        currentIndex += take
                    } else {
                        feed.add(FeedItem.VerticalList(null, tracks.subList(currentIndex, currentIndex + take)))
                        currentIndex += take
                    }
                }
                
                // Horizontal Carousel (up to 15 tracks)
                4 -> {
                    val take = minOf(15, tracks.size - currentIndex)
                    if (take > 0) {
                        feed.add(
                            FeedItem.HorizontalCarousel(
                                title = carouselTitles.random(Random.Default),
                                tracks = tracks.subList(currentIndex, currentIndex + take)
                            )
                        )
                        currentIndex += take
                    }
                }
                
                // Vertical List (up to 3 tracks)
                5 -> {
                    val take = minOf(3, tracks.size - currentIndex)
                    if (take > 0) {
                        val title = if (Random.Default.nextBoolean()) "Keep Scrolling" else null
                        feed.add(
                            FeedItem.VerticalList(
                                title = title,
                                tracks = tracks.subList(currentIndex, currentIndex + take)
                            )
                        )
                        currentIndex += take
                    }
                }
            }
        }

        return feed
    }
}
