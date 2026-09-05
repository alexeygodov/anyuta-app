package ru.family.rasti.widget

internal data class DashboardLayout(val mainTextSp: Float, val showSleepDetail: Boolean, val maxLines: Int)

internal fun dashboardLayout(width: Int, height: Int): DashboardLayout {
    val compact = height < 180 || width < 280
    return DashboardLayout(if (width < 280 || height < 150) 15f else 17f, !compact, if (compact) 1 else 2)
}
