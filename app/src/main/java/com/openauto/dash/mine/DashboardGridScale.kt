package com.openauto.dash.mine

import com.openauto.dash.DashboardItem

internal object DashboardGridScale {
    const val FACTOR = 2

    fun columns(vertical: Boolean): Int = (if (vertical) 7 else 12) * FACTOR
    fun rows(vertical: Boolean): Int = (if (vertical) 12 else 7) * FACTOR
    fun span(cells: Int): Int = cells * FACTOR
    fun size(width: Int, height: Int): Pair<Int, Int> = span(width) to span(height)
    fun scaleSize(size: Pair<Int, Int>): Pair<Int, Int> = this.size(size.first, size.second)

    fun scale(items: List<DashboardItem>): List<DashboardItem> = items.map { item ->
        val x = span(item.x)
        val y = span(item.y)
        val w = span(item.w)
        val h = span(item.h)
        when (item) {
            is DashboardItem.AppShortcut -> item.copy(x = x, y = y, w = w, h = h)
            is DashboardItem.SplitPair -> item.copy(x = x, y = y, w = w, h = h)
            is DashboardItem.LaunchBar -> item.copy(x = x, y = y, w = w, h = h)
            is DashboardItem.VerticalLaunchBar -> item.copy(x = x, y = y, w = w, h = h) // MINE: #34
            is DashboardItem.BuiltinWidget -> item.copy(x = x, y = y, w = w, h = h)
            is DashboardItem.SystemWidget -> item.copy(x = x, y = y, w = w, h = h)
            is DashboardItem.AppWindow -> item.copy(x = x, y = y, w = w, h = h)
        }
    }
}
