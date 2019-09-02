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

    private val booleanValues: List<String> =
        listOf(
            AUTO_COLLECT_INTERVAL_OPEN_KEY,
            AUTO_COLLECT_OPEN_KEY,
            AUTO_COLLECT_WHITE_LIST,
            AUTO_FEED
        )

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
        return context.run {
            if (this == null) {
                false
            } else {
                preferences[PREFERENCE_NAME_SETTINGS] =
                    this.getProtectedSharedPreferences(PREFERENCE_NAME_SETTINGS, MODE_PRIVATE)
                true
            }
        }
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
        return uri?.run {
            if (pathSegments.size == 1) {
                return preferences[this.pathSegments[0]]?.run {
                    val pref = this
                    getQueryParameter("key").run {
                        val key = this
                        if (this != null) {
                            MatrixCursor(arrayOf("key", "value", "type")).apply {
                                pref.all.filter { entry ->
                                    entry.key == key
                                }.forEach { entry ->
                                    val type = getPreferenceType(entry.value)
                                    addRow(arrayOf(entry.key, entry.value, type))
                                }
                            }
                        } else {
                            MatrixCursor(arrayOf("key", "value", "type")).apply {
                                pref.all.forEach { entry ->
                                    val type = getPreferenceType(entry.value)
                                    addRow(arrayOf(entry.key, entry.value, type))
                                }
                            }
                        }
                    }
                }
            } else {
                null
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

        booleanValues.forEach { key ->
            values?.getAsBoolean(key)?.apply {
                val value = this
                preference.edit {
                    putBoolean(key, value)
                }
                return 1
            }
        }

        values?.getAsString(AUTO_COLLECT_INTERVAL_KEY)?.apply {
            val interval: Long = preference.getLong(AUTO_COLLECT_INTERVAL_KEY, 20000)
            return when (this) {
                "plus" -> {
                    preference.edit {
                        putLong(
                            AUTO_COLLECT_INTERVAL_KEY,
                            min(1000 * 60 * 10, interval + 5000)
                        )
                    }
                    1
                }
                "minus" -> {
                    preference.edit {
                        putLong(
                            AUTO_COLLECT_INTERVAL_KEY,
                            max(5000, interval - 5000)
                        )
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