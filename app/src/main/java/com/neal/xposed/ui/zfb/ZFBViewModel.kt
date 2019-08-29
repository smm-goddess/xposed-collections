package com.neal.xposed.ui.zfb

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class ZFBViewModel : ViewModel() {

    private val _text = MutableLiveData<String>().apply {
        value = "ZFB Hook Module Settings"
    }
    private val _interval = MutableLiveData<Long>()
    private val _autoCollectIntervalOpen = MutableLiveData<Boolean>()
    private val _autoCollectOpen = MutableLiveData<Boolean>()

    val text: LiveData<String> = _text
    val interval: LiveData<Long> = _interval
    val autoCollectIntervalOpen: LiveData<Boolean> = _autoCollectIntervalOpen
    val autoCollectOpen: LiveData<Boolean> = _autoCollectOpen

    fun updateInterval(ctx: Context) {
        val uri =
            Uri.parse("content://com.neal.xposed.preference/xposed_settings?key=auto_collect_interval")
        val cursor = ctx.contentResolver.query(uri, null, null, null, null)
        cursor?.moveToFirst()
        _interval.apply {
            value = (cursor?.getLong(cursor.getColumnIndex("value")) ?: 2000) / 1000
        }
        cursor?.close()
    }

    fun setup(ctx: Context) {
        var uri =
            Uri.parse("content://com.neal.xposed.preference/xposed_settings?key=auto_collect_interval_open")
        var cursor: Cursor? =
            ctx.contentResolver.query(uri, null, null, null, null)
        cursor?.moveToFirst()
        _autoCollectIntervalOpen.apply {
            value = if (cursor == null || cursor!!.count == 0) {
                true
            } else {
                cursor!!.getString(cursor!!.getColumnIndex("value"))?.toBoolean() ?: true
            }
        }
        cursor?.close()
        uri =
            Uri.parse("content://com.neal.xposed.preference/xposed_settings?key=auto_collect_open")
        cursor = ctx.contentResolver.query(uri, null, null, null, null)
        cursor?.moveToFirst()
        _autoCollectOpen.apply {
            value = if (cursor == null || cursor.count == 0) {
                true
            } else {
                cursor.getString(cursor.getColumnIndex("value"))?.toBoolean() ?: true
            }
        }
        cursor.close()
        this.updateInterval(ctx)
    }

    fun plus5Seconds(ctx: Context) {
        val uri = Uri.parse("content://com.neal.xposed.preference/xposed_settings")
        val values = ContentValues()
        values.put("auto_collect_interval", "plus")
        if (ctx.contentResolver.update(uri, values, null, null) > 0) {
            this.updateInterval(ctx)
        }
    }

    fun minus5Seconds(ctx: Context) {
        val uri = Uri.parse("content://com.neal.xposed.preference/xposed_settings")
        val values = ContentValues()
        values.put("auto_collect_interval", "minus")
        if (ctx.contentResolver.update(uri, values, null, null) > 0) {
            this.updateInterval(ctx)
        }
    }

    fun intervalCollect(ctx: Context, isChecked: Boolean) {
        val uri = Uri.parse("content://com.neal.xposed.preference/xposed_settings")
        val values = ContentValues()
        values.put("auto_collect_interval_open", isChecked)
        _autoCollectIntervalOpen.apply {
            value = if (ctx.contentResolver.update(uri, values, null, null) < 0) {
                !isChecked
            } else {
                isChecked
            }
        }
    }

    fun autoCollect(ctx: Context, isChecked: Boolean) {
        val uri = Uri.parse("content://com.neal.xposed.preference/xposed_settings")
        val values = ContentValues()
        values.put("auto_collect_open", isChecked)
        _autoCollectOpen.apply {
            value = if (ctx.contentResolver.update(uri, values, null, null) < 0) {
                !isChecked
            } else {
                isChecked
            }
        }
    }

}