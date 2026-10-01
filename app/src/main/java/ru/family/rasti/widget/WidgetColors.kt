package ru.family.rasti.widget

import android.content.Context
import android.widget.RemoteViews
import ru.family.rasti.R

/** Change the background and foreground together; never blend the label colour. */
internal fun RemoteViews.bindVitaminAndSleepColors(
    context: Context,
    vitaminId: Int,
    sleepId: Int,
    snapshot: WidgetSnapshot,
) {
    setInt(vitaminId, "setBackgroundResource", if (snapshot.vitaminTaken) {
        R.drawable.widget_action_milk_background
    } else {
        R.drawable.widget_vitamin_background
    })
    setTextColor(vitaminId, context.getColor(if (snapshot.vitaminTaken) {
        R.color.widget_on_milk
    } else {
        R.color.widget_on_alert
    }))
    val attention = snapshot.wakeAttention > 0f
    setInt(sleepId, "setBackgroundResource", if (attention) {
        R.drawable.widget_vitamin_background
    } else {
        R.drawable.widget_action_sleep_background
    })
    setTextColor(sleepId, context.getColor(if (attention) {
        R.color.widget_on_alert
    } else {
        R.color.widget_on_sleep
    }))
}
