package com.neal.xposed.alipay

import android.app.Application
import android.content.Context
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage

class AlipayHook {
    companion object {
        fun hookAlipayRpcCall(lpparam: XC_LoadPackage.LoadPackageParam) {
            try {
                XposedHelpers.findAndHookMethod(
                    Application::class.java,
                    "attach",
                    Context::class.java,
                    ApplicationAttachMethodHook()
                )
            } catch (e2: Exception) {
                e2.printStackTrace()
            }
        }
    }
}