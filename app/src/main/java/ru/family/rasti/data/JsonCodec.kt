package ru.family.rasti.data

import org.json.JSONArray
import org.json.JSONObject

object JsonCodec {
    fun encodeAppData(data: AppData): String = JSONObject()
        .put("version", 1)
        .put("profile", profileToJson(data.profile))
        .put("days", JSONArray(data.days.values.sortedBy { it.date }.map(::dayToJson)))
        .toString(2)

    fun decodeAppData(raw: String): AppData {
        val root = JSONObject(raw)
        val profile = root.optJSONObject("profile")?.let(::profileFromJson) ?: ChildProfile()
        val array = root.optJSONArray("days") ?: JSONArray()
        val days = buildMap {
            for (index in 0 until array.length()) {
                val day = dayFromJson(array.getJSONObject(index))
                put(day.date, day)
            }
        }
        return AppData(profile = profile, days = days)
    }

    fun encodeProfile(profile: ChildProfile): String = profileToJson(profile).toString(2)
    fun decodeProfile(raw: String): ChildProfile = profileFromJson(JSONObject(raw))
    fun encodeDay(day: DayRecord): String = dayToJson(day).toString(2)
    fun decodeDay(raw: String): DayRecord = dayFromJson(JSONObject(raw))

    private fun profileToJson(profile: ChildProfile) = JSONObject()
        .put("name", profile.name)
        .put("birthDate", profile.birthDate)
        .put("sex", profile.sex.name.lowercase())
        .put("updatedAt", profile.updatedAt)

    private fun profileFromJson(json: JSONObject) = ChildProfile(
        name = json.optString("name", "Малыш"),
        birthDate = json.optString("birthDate", ChildProfile().birthDate),
        sex = runCatching { ChildSex.valueOf(json.optString("sex", "girl").uppercase()) }
            .getOrDefault(ChildSex.GIRL),
        updatedAt = json.optString("updatedAt"),
    )

    private fun foodToJson(item: FoodEntry) = JSONObject()
        .put("id", item.id)
        .put("time", item.time)
        .put("name", item.name)
        .put("amount", item.amount)
        .put("unit", item.unit)
        .put("updatedAt", item.updatedAt)

    private fun foodFromJson(json: JSONObject) = FoodEntry(
        id = json.getString("id"),
        time = json.optString("time"),
        name = json.optString("name"),
        amount = json.optDouble("amount", 0.0),
        unit = json.optString("unit"),
        updatedAt = json.optString("updatedAt"),
    )

    private fun vitaminToJson(item: VitaminEntry) = JSONObject()
        .put("id", item.id)
        .put("time", item.time)
        .put("name", item.name)
        .put("dose", item.dose)
        .put("updatedAt", item.updatedAt)

    private fun vitaminFromJson(json: JSONObject) = VitaminEntry(
        id = json.getString("id"),
        time = json.optString("time"),
        name = json.optString("name"),
        dose = json.optString("dose"),
        updatedAt = json.optString("updatedAt"),
    )

    private fun measurementToJson(value: Measurement) = JSONObject()
        .apply {
            value.heightCm?.let { put("heightCm", it) }
            value.weightKg?.let { put("weightKg", it) }
        }
        .put("updatedAt", value.updatedAt)

    private fun measurementFromJson(json: JSONObject) = Measurement(
        heightCm = json.optDoubleOrNull("heightCm"),
        weightKg = json.optDoubleOrNull("weightKg"),
        updatedAt = json.optString("updatedAt"),
    )

    private fun dayToJson(day: DayRecord) = JSONObject()
        .put("date", day.date)
        .put("food", JSONArray(day.food.map(::foodToJson)))
        .put("vitamins", JSONArray(day.vitamins.map(::vitaminToJson)))
        .put("deletedFoodIds", JSONArray(day.deletedFoodIds.toList()))
        .put("deletedVitaminIds", JSONArray(day.deletedVitaminIds.toList()))
        .apply { day.measurement?.let { put("measurement", measurementToJson(it)) } }
        .put("note", day.note)
        .put("updatedAt", day.updatedAt)

    private fun dayFromJson(json: JSONObject): DayRecord {
        val foodArray = json.optJSONArray("food") ?: JSONArray()
        val vitaminArray = json.optJSONArray("vitamins") ?: JSONArray()
        return DayRecord(
            date = json.getString("date"),
            food = List(foodArray.length()) { foodFromJson(foodArray.getJSONObject(it)) },
            vitamins = List(vitaminArray.length()) { vitaminFromJson(vitaminArray.getJSONObject(it)) },
            deletedFoodIds = json.optJSONArray("deletedFoodIds").toStringSet(),
            deletedVitaminIds = json.optJSONArray("deletedVitaminIds").toStringSet(),
            measurement = json.optJSONObject("measurement")?.let(::measurementFromJson),
            note = json.optString("note"),
            updatedAt = json.optString("updatedAt"),
        )
    }

    private fun JSONObject.optDoubleOrNull(key: String): Double? =
        if (has(key) && !isNull(key)) optDouble(key).takeUnless { it.isNaN() } else null

    private fun JSONArray?.toStringSet(): Set<String> = buildSet {
        if (this@toStringSet != null) {
            for (index in 0 until length()) add(getString(index))
        }
    }
}
