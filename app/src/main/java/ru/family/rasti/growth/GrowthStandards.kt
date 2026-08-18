package ru.family.rasti.growth

import android.content.Context
import ru.family.rasti.data.ChildSex
import java.time.LocalDate
import java.time.temporal.ChronoUnit

enum class GrowthMetric { HEIGHT, WEIGHT }

data class GrowthBand(val day: Int, val low: Float, val median: Float, val high: Float)

class GrowthStandards(context: Context) {
    private val assets = context.assets
    private val cache = mutableMapOf<String, List<GrowthBand>>()

    fun curve(metric: GrowthMetric, sex: ChildSex): List<GrowthBand> {
        val key = "${metric.name}_${sex.name}"
        return cache.getOrPut(key) {
            val metricName = if (metric == GrowthMetric.HEIGHT) "height" else "weight"
            val sexName = if (sex == ChildSex.GIRL) "girl" else "boy"
            assets.open("who/${metricName}_${sexName}.csv").bufferedReader().useLines { lines ->
                lines.dropWhile { it.startsWith("#") }
                    .drop(1)
                    .filter { it.isNotBlank() }
                    .map { row ->
                        val columns = row.split(',')
                        GrowthBand(
                            day = columns[0].toInt(),
                            low = columns[1].toFloat(),
                            median = columns[2].toFloat(),
                            high = columns[3].toFloat(),
                        )
                    }
                    .toList()
            }
        }
    }

    fun ageInDays(birthDate: String, measurementDate: String): Int? = runCatching {
        ChronoUnit.DAYS.between(LocalDate.parse(birthDate), LocalDate.parse(measurementDate)).toInt()
    }.getOrNull()?.takeIf { it >= 0 }
}
