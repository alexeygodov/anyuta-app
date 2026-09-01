package ru.family.rasti.widget

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.RectF
import ru.family.rasti.data.AppData
import ru.family.rasti.sleep.sleepsForDate
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

internal fun renderWidgetTimeline(
    data: AppData,
    date: LocalDate,
    now: LocalDateTime,
): Bitmap {
    // RemoteViews transfers the bitmap through Binder. Keeping a fixed canvas
    // prevents high-density launchers from producing a multi-megabyte payload.
    val density = 2f
    fun dp(value: Float): Float = value * density
    val width = 640
    val height = 184
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    bitmap.density = Bitmap.DENSITY_NONE
    val canvas = Canvas(bitmap)
    val left = dp(8f)
    val right = width - dp(8f)
    val top = dp(5f)
    val baseline = height - dp(25f)
    val plotWidth = right - left
    fun x(minute: Int): Float = left + minute.coerceIn(0, 1440) / 1440f * plotWidth

    val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(32, 255, 255, 255)
        strokeWidth = dp(1f)
    }
    val axisPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(160, 255, 255, 255)
        textSize = dp(8.5f)
        textAlign = Paint.Align.CENTER
    }
    listOf(0, 360, 720, 1080, 1440).forEach { minute ->
        canvas.drawLine(x(minute), top, x(minute), baseline, gridPaint)
        canvas.drawText((minute / 60).toString(), x(minute), height - dp(6f), axisPaint)
    }
    canvas.drawLine(left, baseline, right, baseline, gridPaint.apply { color = Color.argb(72, 255, 255, 255) })

    val sleeps = sleepsForDate(data, date, now)
    val sleepPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(135, 166, 235) }
    sleeps.forEach { sleep ->
        val startX = x(sleep.startMinute)
        val endX = x(sleep.endMinute)
        canvas.drawRoundRect(
            RectF(startX, top, endX.coerceAtLeast(startX + dp(3f)), top + dp(7f)),
            dp(4f),
            dp(4f),
            sleepPaint.apply { alpha = if (sleep.ongoing) 255 else 205 },
        )
    }

    val day = data.days[date.toString()]
    val feedings = day?.food.orEmpty().mapNotNull { entry ->
        val measured = entry.unit.trim().lowercase() in setOf("мл", "ml") &&
            entry.name.trim().lowercase() in setOf("молоко", "смесь")
        val time = runCatching { LocalTime.parse(entry.time) }.getOrNull()
        if (!measured || time == null) null else Triple(time.toSecondOfDay() / 60, entry.amount, entry.name.equals("Молоко", true))
    }.sortedBy { it.first }
    feedings.forEach { (minute, amount, isMilk) ->
        val barHeight = dp(8f + (amount / 200.0).coerceIn(0.0, 1.0).toFloat() * 39f)
        val center = x(minute)
        val halfWidth = dp(3.4f)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (isMilk) Color.rgb(105, 201, 158) else Color.rgb(238, 177, 105)
        }
        canvas.drawRoundRect(
            RectF(center - halfWidth, baseline - barHeight, center + halfWidth, baseline),
            dp(3.4f),
            dp(3.4f),
            paint,
        )
    }

    if (date == now.toLocalDate()) {
        val nowMinute = now.hour * 60 + now.minute
        val nowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(180, 255, 199, 191)
            strokeWidth = dp(1f)
            pathEffect = DashPathEffect(floatArrayOf(dp(3f), dp(3f)), 0f)
        }
        canvas.drawLine(x(nowMinute), top + dp(10f), x(nowMinute), baseline, nowPaint)
    }

    if (feedings.isEmpty() && sleeps.isEmpty()) {
        val emptyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(150, 255, 255, 255)
            textSize = dp(10f)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("События появятся здесь", width / 2f, baseline - dp(13f), emptyPaint)
    }
    return bitmap
}
