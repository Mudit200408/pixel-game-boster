package com.pixel.gamebooster.data.db

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class Converters {
    private val gson = Gson()

    @TypeConverter
    fun fromStringSet(set: Set<String>?): String {
        return gson.toJson(set ?: emptySet<String>())
    }

    @TypeConverter
    fun toStringSet(json: String?): Set<String> {
        if (json.isNullOrEmpty()) return emptySet()
        val type = object : TypeToken<Set<String>>() {}.type
        return gson.fromJson(json, type) ?: emptySet()
    }
}
