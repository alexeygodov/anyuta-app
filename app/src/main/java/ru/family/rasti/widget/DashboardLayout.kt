package ru.family.rasti.widget

internal enum class DashboardLayout { FULL, COMPACT, NARROW }

/** Switch the content itself while the launcher controls the outer widget size. */
internal fun dashboardLayout(width: Int, height: Int): DashboardLayout =
    when {
        width < 230 -> DashboardLayout.NARROW
        height < 132 -> DashboardLayout.COMPACT
        else -> DashboardLayout.FULL
    }
