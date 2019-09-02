package com.neal.xposed.ui.alipay

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.net.Uri
import androidx.core.content.contentValuesOf
import androidx.lifecycle.*
import com.neal.xposed.tools.pref.*

class AlipayViewModel : ViewModel() {

    private val _interval = MutableLiveData<Long>()
    private val _autoCollectIntervalOpen = MutableLiveData<Boolean>()
    private val _autoCollectOpen = MutableLiveData<Boolean>()
    private val _autoCollectWhitelist = MutableLiveData<Boolean>()
    private val _autoFeed = MutableLiveData<Boolean>()
    private val TAG = "XposedHookAlipay"

    val interval: LiveData<Long> = _interval
    val autoCollectIntervalOpen: LiveData<Boolean> = _autoCollectIntervalOpen
    val autoCollectOpen: LiveData<Boolean> = _autoCollectOpen
    val autoCollectWhitelist: LiveData<Boolean> = _autoCollectWhitelist
    val autoFeed: LiveData<Boolean> = _autoFeed

    private fun updateInterval(ctx: Context) {
        query(ctx, AUTO_COLLECT_INTERVAL_KEY)?.apply {
            moveToFirst()
            _interval.apply {
                value = if (count > 0) {
                    getLong(getColumnIndex("value")) / 1000
                } else {
                    20
                }
            }
            close()
        }
    }

    private fun setupBoolean(ctx: Context, key: String, mutableLiveData: MutableLiveData<Boolean>) {
        query(ctx, key)?.apply {
            moveToFirst()
            mutableLiveData.apply {
                value = if (count > 0) {
                    with(getString(getColumnIndex("value"))) { toBoolean() }
                } else {
                    true
                }
            }
            close()
        }
    }

    private fun query(ctx: Context, key: String): Cursor? {
        val uri = Uri.parse("content://com.neal.xposed.preference/xposed_settings?key=$key")
        return ctx.contentResolver.query(uri, null, null, null, null)
    }

    fun setup(ctx: Context) {
        setupBoolean(ctx, AUTO_COLLECT_INTERVAL_OPEN_KEY, _autoCollectIntervalOpen)
        setupBoolean(ctx, AUTO_COLLECT_OPEN_KEY, _autoCollectOpen)
        setupBoolean(ctx, AUTO_COLLECT_WHITE_LIST, _autoCollectWhitelist)
        setupBoolean(ctx, AUTO_FEED, _autoFeed)
        updateInterval(ctx)
    }

    fun plus5Seconds(ctx: Context) {
        val uri = Uri.parse("content://com.neal.xposed.preference/xposed_settings")
        val values = contentValuesOf(AUTO_COLLECT_INTERVAL_KEY to "plus")
        if (ctx.contentResolver.update(uri, values, null, null) > 0) {
            updateInterval(ctx)
        }
    }

    fun minus5Seconds(ctx: Context) {
        val uri = Uri.parse("content://com.neal.xposed.preference/xposed_settings")
        val values = contentValuesOf(AUTO_COLLECT_INTERVAL_KEY to "minus")
        if (ctx.contentResolver.update(uri, values, null, null) > 0) {
            updateInterval(ctx)
        }
    }

    private fun tryToggle(
        ctx: Context,
        isChecked: Boolean,
        values: ContentValues,
        liveData: MutableLiveData<Boolean>
    ) {
        val uri = Uri.parse("content://com.neal.xposed.preference/xposed_settings")
        liveData.apply {
            value = if (ctx.contentResolver.update(uri, values, null, null) < 0) {
                !isChecked
            } else {
                isChecked
            }
        }
    }

    fun intervalCollect(ctx: Context, isChecked: Boolean) {
        tryToggle(
            ctx, isChecked, contentValuesOf(AUTO_COLLECT_INTERVAL_OPEN_KEY to isChecked)
            , _autoCollectIntervalOpen
        )
    }

    fun autoCollectWhitelist(ctx: Context, isChecked: Boolean) {
        tryToggle(
            ctx,
            isChecked,
            contentValuesOf(AUTO_COLLECT_WHITE_LIST to isChecked)
            , _autoCollectWhitelist
        )
    }

    fun autoFeed(ctx: Context, isChecked: Boolean) {
        tryToggle(
            ctx,
            isChecked,
            contentValuesOf(AUTO_FEED to isChecked),
            _autoFeed
        )
    }

    fun autoCollect(ctx: Context, isChecked: Boolean) {
        tryToggle(
            ctx,
            isChecked,
            contentValuesOf(AUTO_COLLECT_OPEN_KEY to isChecked),
            _autoCollectOpen
        )
    }

}