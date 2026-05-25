package com.freqvox.ui.tag

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector
import java.util.Locale

/** Title-case every word: "late night" → "Late Night". Storage stays lowercase. */
fun formatTagDisplayName(raw: String): String =
    raw.trim()
        .split(Regex("\\s+"))
        .filter { it.isNotEmpty() }
        .joinToString(" ") { word ->
            word.replaceFirstChar { c ->
                if (c.isLowerCase()) c.titlecase(Locale.getDefault()) else c.toString()
            }
        }

val tagIcons: Map<String, ImageVector> = mapOf(
    "liked"      to Icons.Default.Favorite,
    "disliked"   to Icons.Default.ThumbDown,
    "chill"      to Icons.Default.AcUnit,
    "energetic"  to Icons.Default.Bolt,
    "focus"      to Icons.Default.School,
    "hype"       to Icons.Default.Whatshot,
    "melancholy" to Icons.Default.Cloud,
    "late night" to Icons.Default.Bedtime,
    "workout"    to Icons.Default.FitnessCenter,
    "drive"      to Icons.Default.DirectionsCar,
    "sleep"      to Icons.Default.Hotel,
    "party"      to Icons.Default.Celebration,
    "morning"    to Icons.Default.WbSunny,
    "throwback"  to Icons.Default.History,
    "underrated" to Icons.Default.Diamond,
    "sad"        to Icons.Default.SentimentDissatisfied,
    "vibes"      to Icons.Default.AutoAwesome
)

fun iconForTag(name: String): ImageVector =
    tagIcons[name.lowercase()] ?: Icons.Default.Label
