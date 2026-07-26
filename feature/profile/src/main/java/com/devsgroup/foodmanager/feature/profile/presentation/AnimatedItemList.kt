package com.devsgroup.foodmanager.feature.profile.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

private class AnimatedEntry<T>(item: T) {
    var item by mutableStateOf(item)
    val visibleState = MutableTransitionState(false)
}

/**
 * Renders [items] inside a caller-supplied [layout] (a [FlowRow]/[Column]/etc.),
 * animating additions in (fade + scale) and removals out, while the surviving
 * siblings reflow into place — the caller only needs `Modifier.animateContentSize()`
 * on the [layout] it hands back. A removed item stays composed (and keeps playing
 * its exit animation) even after it drops out of [items], so callers can update
 * their backing state immediately without racing the animation.
 */
@Composable
internal fun <T> AnimatedItemList(
    items: List<T>,
    keySelector: (T) -> Any,
    layout: @Composable (content: @Composable () -> Unit) -> Unit,
    itemContent: @Composable (T) -> Unit,
) {
    val order = remember { mutableStateListOf<Any>() }
    val entries = remember { mutableStateMapOf<Any, AnimatedEntry<T>>() }

    LaunchedEffect(items) {
        val currentKeys = items.map(keySelector).toSet()
        items.forEach { item ->
            val itemKey = keySelector(item)
            val entry = entries[itemKey]
            if (entry == null) {
                entries[itemKey] = AnimatedEntry(item).apply { visibleState.targetState = true }
                order.add(itemKey)
            } else {
                entry.item = item
            }
        }
        order.toList().forEach { itemKey ->
            if (itemKey !in currentKeys) {
                entries[itemKey]?.visibleState?.targetState = false
            }
        }
    }

    layout {
        order.toList().forEach { itemKey ->
            val entry = entries[itemKey] ?: return@forEach
            key(itemKey) {
                AnimatedVisibility(
                    visibleState = entry.visibleState,
                    enter = fadeIn() + scaleIn(initialScale = 0.85f),
                    exit = fadeOut() + scaleOut(targetScale = 0.85f),
                ) {
                    itemContent(entry.item)
                }
                if (!entry.visibleState.targetState && !entry.visibleState.currentState) {
                    LaunchedEffect(itemKey) {
                        entries.remove(itemKey)
                        order.remove(itemKey)
                    }
                }
            }
        }
    }
}
