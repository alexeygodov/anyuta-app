package ru.family.rasti.widget

internal enum class DashboardLayout { STRIP, GRID }

/** One-row layouts remain genuinely one row; extra height changes structure, not padding. */
internal fun dashboardLayout(width: Int, height: Int): DashboardLayout =
    if (height >= 140 && width < height * 3) DashboardLayout.GRID else DashboardLayout.STRIP
