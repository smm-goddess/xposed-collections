package com.neal.xposed.tools.pref


import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import androidx.core.content.edit
import com.neal.xposed.tools.utils.getProtectedSharedPreferences
import kotlin.math.max
import kotlin.math.min

// PrefProvider shares the preferences using content provider model.
class PrefProvider : ContentProvider() {
    private val PREFERENCE_NAME_SETTINGS: String = "xposed_settings"
    private val AUTO_COLLECT_INTERVAL_OPEN_KEY: String = "auto_collect_interval_open"
    private val AUTO_COLLECT_OPEN_KEY: String = "auto_collect_open"
    private val AUTO_COLLECT_INTERVAL_KEY: String = "auto_collect_interval"

    private val preferences: MutableMap<String, SharedPreferences> = mutableMapOf()

    private fun getPreferenceType(value: Any?): String {
        return when (value) {
            null -> "Null"
            is Int -> "Int"
            is Long -> "Long"
            is Float -> "Float"
            is Boolean -> "Boolean"
            is String -> "String"
            is Set<*> -> "StringSet"
            else -> "${value::class.java}"
        }
    }

    override fun onCreate(): Boolean {
        preferences[PREFERENCE_NAME_SETTINGS] =
            context.getProtectedSharedPreferences(PREFERENCE_NAME_SETTINGS, MODE_PRIVATE)
        return true
    }

    override fun getType(uri: Uri?): String? = null

    override fun insert(uri: Uri?, values: ContentValues?): Uri {
        throw UnsupportedOperationException("PrefProvider: Cannot modify read-only preferences!")
    }

    override fun query(
        uri: Uri?,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?
    ): Cursor? {
        if (uri == null) {
            return null
        }
        val segments = uri.pathSegments
        if (segments.size != 1) {
            return null
        }
        val preference = preferences[segments[0]] ?: return null
        val key: String? = uri.getQueryParameter("key")
        if (key != null) {
            return MatrixCursor(arrayOf("key", "value", "type")).apply {
                preference.all.filter { entry ->
                    entry.key == key
                }.forEach { entry ->
                    val type = getPreferenceType(entry.value)
                    addRow(arrayOf(entry.key, entry.value, type))
                }
            }
        } else {
            return MatrixCursor(arrayOf("key", "value", "type")).apply {
                preference.all.forEach { entry ->
                    val type = getPreferenceType(entry.value)
                    addRow(arrayOf(entry.key, entry.value, type))
                }
            }
        }
    }

    override fun update(
        uri: Uri?,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?
    ): Int {
        if (uri == null || uri.pathSegments.size != 1) {
            return -1
        }
        val preference = preferences[uri.pathSegments[0]] ?: return -1

        val autoCollectIntervalOpen: Boolean? = values?.getAsBoolean(AUTO_COLLECT_INTERVAL_OPEN_KEY)
        if (autoCollectIntervalOpen != null) {
            preference.edit {
                putBoolean(AUTO_COLLECT_INTERVAL_OPEN_KEY, autoCollectIntervalOpen)
            }
            return 1
        }
        val autoCollectOpen: Boolean? = values?.getAsBoolean(AUTO_COLLECT_OPEN_KEY)
        if (autoCollectOpen != null) {
            preference.edit {
                putBoolean(AUTO_COLLECT_OPEN_KEY, autoCollectOpen)
            }
            return 1
        }

        val autoCollectIntervalAction: String? = values?.getAsString(AUTO_COLLECT_INTERVAL_KEY)
        if (autoCollectIntervalAction != null) {
            val interval: Long = preference.getLong(AUTO_COLLECT_INTERVAL_KEY, 20000)
            return when (autoCollectIntervalAction) {
                "plus" -> {
                    preference.edit {
                        putLong(AUTO_COLLECT_INTERVAL_KEY, min(1000 * 60 * 10, interval + 5000))
                    }
                    1
                }
                "minus" -> {
                    preference.edit {
                        putLong(AUTO_COLLECT_INTERVAL_KEY, max(5000, interval - 5000))
                    }
                    1
                }
                else -> -1
            }
        }
        return -1
    }

    override fun delete(uri: Uri?, selection: String?, selectionArgs: Array<out String>?): Int {
        throw UnsupportedOperationException("PrefProvider: Cannot modify read-only preferences!")
    }
}