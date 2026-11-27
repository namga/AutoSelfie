package com.yumy.intruderselfie

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object EventStore {
    private const val PREFS = "intrusion_events"
    private const val KEY = "items"

    @Synchronized fun record(context: Context, type: String, detail: String) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val old = runCatching { JSONArray(prefs.getString(KEY, "[]")) }.getOrElse { JSONArray() }
        val item = JSONObject().apply {
            put("id", System.currentTimeMillis())
            put("type", type)
            put("detail", detail)
            put("time", SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()))
        }
        val next = JSONArray().put(item)
        for (i in 0 until minOf(old.length(), 99)) next.put(old.getJSONObject(i))
        prefs.edit().putString(KEY, next.toString()).apply()
    }

    fun all(context: Context): List<SecurityEvent> {
        val json = runCatching { JSONArray(context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, "[]")) }.getOrElse { JSONArray() }
        return (0 until json.length()).mapNotNull { i ->
            runCatching {
                val o = json.getJSONObject(i)
                SecurityEvent(o.optString("type"), o.optString("detail"), o.optString("time"))
            }.getOrNull()
        }
    }
}

data class SecurityEvent(val type: String, val detail: String, val time: String)
