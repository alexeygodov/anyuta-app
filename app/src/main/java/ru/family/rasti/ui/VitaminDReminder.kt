package ru.family.rasti.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import android.os.Build
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.delay
import java.time.Clock
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZonedDateTime

internal fun shouldPulseVitaminD(selectedDate: LocalDate, vitaminDTaken: Boolean, now: LocalDateTime): Boolean =
    selectedDate == now.toLocalDate() && !vitaminDTaken && now.toLocalTime().isBefore(LocalTime.of(14, 0))

/** Uses an instant duration so midnight also works on days with a timezone offset change. */
internal fun nextReminderBoundary(now: ZonedDateTime): ZonedDateTime =
    if (now.toLocalTime().isBefore(LocalTime.of(14, 0))) now.toLocalDate().atTime(14, 0).atZone(now.zone)
    else now.toLocalDate().plusDays(1).atStartOfDay(now.zone)

/** Re-read the device zone rather than keeping Clock.systemDefaultZone from initial composition. */
@Composable
internal fun rememberReminderTime(clock: Clock? = null): LocalDateTime {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var revision by remember { mutableIntStateOf(0) }
    var now by remember(clock) { mutableStateOf(LocalDateTime.now(clock ?: Clock.systemDefaultZone())) }
    DisposableEffect(context, lifecycleOwner, clock) {
        fun refresh() {
            now = LocalDateTime.now(clock ?: Clock.systemDefaultZone())
            revision++
        }
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) refresh() }
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) = refresh()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_TIME_CHANGED)
            addAction(Intent.ACTION_TIMEZONE_CHANGED)
            addAction(Intent.ACTION_DATE_CHANGED)
        }
        // These are protected system broadcasts. The native pre-33 API needs no
        // app-defined receiver permission (including when tested without APK installation).
        if (Build.VERSION.SDK_INT >= 33) context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        else context.registerReceiver(receiver, filter)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            context.unregisterReceiver(receiver)
        }
    }
    LaunchedEffect(clock, revision) {
        while (true) {
            val current = ZonedDateTime.now(clock ?: Clock.systemDefaultZone())
            now = current.toLocalDateTime()
            delay(Duration.between(current, nextReminderBoundary(current)).toMillis().coerceAtLeast(1))
        }
    }
    return now
}

@Composable
internal fun VitaminDReminder(shouldPulse: Boolean, onClick: () -> Unit) {
    // Keeping the transition in this branch disposes it immediately at the time boundary.
    val borderAlpha = if (shouldPulse) {
        val transition = rememberInfiniteTransition(label = "vitamin-d-reminder")
        val alpha by transition.animateFloat(.45f, 1f,
            infiniteRepeatable(tween(480), RepeatMode.Reverse), label = "vitamin-d-border")
        alpha
    } else 1f
    FilledTonalButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().heightIn(min = 88.dp).testTag("vitamin-d-reminder")
            .semantics { stateDescription = if (shouldPulse) "Напоминание до 14:00" else "Статичное напоминание" },
        border = BorderStroke(2.dp, MaterialTheme.colorScheme.error.copy(alpha = borderAlpha)),
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
        ),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("ВИТАМИН D НЕ ПРИНЯТ", style = MaterialTheme.typography.titleMedium)
            Text("НАЖМИТЕ СЕЙЧАС · 2 капли", style = MaterialTheme.typography.bodySmall)
        }
    }
}
