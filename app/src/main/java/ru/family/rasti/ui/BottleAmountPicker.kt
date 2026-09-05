package ru.family.rasti.ui

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AssistChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.FilledTonalButton
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

private const val FILL_TOP_FRACTION = 0.275f
private const val FILL_BOTTOM_FRACTION = 0.92f

@Composable
internal fun BottleAmountPicker(
    amountMl: Float,
    onAmountChange: (Float) -> Unit,
    suggestions: List<Int> = emptyList(),
    modifier: Modifier = Modifier,
    maxMl: Int = 200,
    stepMl: Int = 5,
) {
    val outlineColor = MaterialTheme.colorScheme.outline
    val glassColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .5f)
    val tickColor = MaterialTheme.colorScheme.outline.copy(alpha = .65f)
    val numberColor = MaterialTheme.colorScheme.onSurface
    val milkColor = MaterialTheme.colorScheme.secondaryContainer
    val numberBackground = MaterialTheme.colorScheme.surface
    val currentOnChange by rememberUpdatedState(onAmountChange)

    fun snap(value: Float): Float =
        ((value / stepMl).roundToInt() * stepMl).toFloat().coerceIn(0f, maxMl.toFloat())

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        FilledTonalButton(onClick = { currentOnChange(snap(amountMl - stepMl)) }, enabled = amountMl > 0,
            modifier = Modifier.semantics { contentDescription = "Уменьшить на $stepMl мл" }) { Text("−$stepMl") }
        Canvas(
            Modifier
                .width(120.dp)
                .height(230.dp)
                .semantics {
                    contentDescription = "Объём кормления"
                    stateDescription = "${amountMl.roundToInt()} мл"
                    progressBarRangeInfo = ProgressBarRangeInfo(amountMl, 0f..maxMl.toFloat(), maxMl / stepMl - 1)
                    setProgress { currentOnChange(snap(it)); true }
                }
                .pointerInput(maxMl, stepMl) {
                    detectVerticalDragGestures { change, _ ->
                        change.consume()
                        val topPx = FILL_TOP_FRACTION * size.height
                        val bottomPx = FILL_BOTTOM_FRACTION * size.height
                        val fraction = ((bottomPx - change.position.y) / (bottomPx - topPx)).coerceIn(0f, 1f)
                        currentOnChange(snap(fraction * maxMl))
                    }
                }
                .pointerInput(maxMl, stepMl) {
                    detectTapGestures { offset ->
                        val topPx = FILL_TOP_FRACTION * size.height
                        val bottomPx = FILL_BOTTOM_FRACTION * size.height
                        val fraction = ((bottomPx - offset.y) / (bottomPx - topPx)).coerceIn(0f, 1f)
                        currentOnChange(snap(fraction * maxMl))
                    }
                },
        ) {
            val w = size.width
            val h = size.height

            val nipplePath = Path().apply {
                val base = 0.135f * h
                val halfBase = 0.10f * w
                moveTo(w / 2 - halfBase, base)
                quadraticTo(w / 2 - halfBase, 0.05f * h, w / 2 - 0.030f * w, 0.022f * h)
                quadraticTo(w / 2, 0.008f * h, w / 2 + 0.030f * w, 0.022f * h)
                quadraticTo(w / 2 + halfBase, 0.05f * h, w / 2 + halfBase, base)
                close()
            }
            drawPath(nipplePath, color = glassColor)
            drawPath(nipplePath, color = outlineColor, style = Stroke(2.dp.toPx()))

            val bodyLeft = 0.16f * w
            val bodyRight = 0.84f * w
            val shoulderTop = 0.185f * h
            val bodyTop = 0.26f * h
            val bodyBottom = 0.97f * h
            val bottomRadius = 0.09f * w
            val bodyPath = Path().apply {
                moveTo(0.27f * w, shoulderTop + 0.004f * h)
                quadraticTo(bodyLeft + 0.03f * w, shoulderTop + 0.01f * h, bodyLeft, bodyTop)
                lineTo(bodyLeft, bodyBottom - bottomRadius)
                quadraticTo(bodyLeft, bodyBottom, bodyLeft + bottomRadius, bodyBottom)
                lineTo(bodyRight - bottomRadius, bodyBottom)
                quadraticTo(bodyRight, bodyBottom, bodyRight, bodyBottom - bottomRadius)
                lineTo(bodyRight, bodyTop)
                quadraticTo(bodyRight - 0.03f * w, shoulderTop + 0.01f * h, 0.73f * w, shoulderTop + 0.004f * h)
                close()
            }
            drawPath(bodyPath, color = glassColor)

            val fraction = (amountMl / maxMl).coerceIn(0f, 1f)
            val fillTop =
                FILL_BOTTOM_FRACTION * h - fraction * (FILL_BOTTOM_FRACTION - FILL_TOP_FRACTION) * h
            clipPath(bodyPath) {
                drawRect(
                    color = milkColor.copy(alpha = .9f),
                    topLeft = Offset(bodyLeft, fillTop),
                    size = Size(bodyRight - bodyLeft, bodyBottom - fillTop),
                )
            }
            drawPath(bodyPath, color = outlineColor, style = Stroke(2.dp.toPx()))

            val ringLeft = 0.235f * w
            val ringRight = 0.765f * w
            val ringTop = 0.128f * h
            val ringBottom = 0.19f * h
            val ringRadius = CornerRadius((ringBottom - ringTop) / 2)
            drawRoundRect(
                color = outlineColor.copy(alpha = .35f),
                topLeft = Offset(ringLeft, ringTop),
                size = Size(ringRight - ringLeft, ringBottom - ringTop),
                cornerRadius = ringRadius,
            )
            drawRoundRect(
                color = outlineColor,
                topLeft = Offset(ringLeft, ringTop),
                size = Size(ringRight - ringLeft, ringBottom - ringTop),
                cornerRadius = ringRadius,
                style = Stroke(1.6.dp.toPx()),
            )

            val tickRight = bodyRight - 0.05f * w
            var tickValue = 0
            while (tickValue <= maxMl) {
                val major = tickValue % 50 == 0
                val tickY = FILL_BOTTOM_FRACTION * h -
                    (tickValue.toFloat() / maxMl) * (FILL_BOTTOM_FRACTION - FILL_TOP_FRACTION) * h
                val tickLength = (if (major) 0.11f else 0.055f) * w
                drawLine(
                    color = tickColor,
                    start = Offset(tickRight - tickLength, tickY),
                    end = Offset(tickRight, tickY),
                    strokeWidth = (if (major) 1.8f else 1.2f).dp.toPx(),
                    cap = StrokeCap.Round,
                )
                tickValue += 10
            }

            val numberPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = numberColor.toArgb()
                textSize = 36.sp.toPx()
                textAlign = Paint.Align.CENTER
                typeface = Typeface.DEFAULT_BOLD
            }
            val unitPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = numberColor.copy(alpha = .75f).toArgb()
                textSize = 14.sp.toPx()
                textAlign = Paint.Align.CENTER
            }
            val numberY = (bodyTop + bodyBottom) / 2 + 0.02f * h
            // Opaque label stays legible both above and below the milk level, in either theme.
            drawRoundRect(numberBackground, Offset(w * .18f, numberY - 40.sp.toPx()),
                Size(w * .64f, 64.sp.toPx()), CornerRadius(12.dp.toPx()))
            drawContext.canvas.nativeCanvas.apply {
                drawText("${amountMl.roundToInt()}", w / 2, numberY, numberPaint)
                drawText("мл", w / 2, numberY + 0.075f * h, unitPaint)
            }
        }
        FilledTonalButton(onClick = { currentOnChange(snap(amountMl + stepMl)) }, enabled = amountMl < maxMl,
            modifier = Modifier.semantics { contentDescription = "Увеличить на $stepMl мл" }) { Text("+$stepMl") }
        }

        val chips = suggestions.ifEmpty { listOf(60, 90, 120, 150) }
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            chips.distinct().take(4).forEach { preset ->
                AssistChip(onClick = { currentOnChange(snap(preset.toFloat())) }, label = { Text("$preset") })
            }
        }
    }
}
