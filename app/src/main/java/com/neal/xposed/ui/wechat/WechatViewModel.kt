package com.neal.xposed.ui.wechat

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class WechatViewModel : ViewModel() {

    private val _text = MutableLiveData<String>().apply {
        value = "徽信模块设置"
    }
    val text: LiveData<String> = _text
}