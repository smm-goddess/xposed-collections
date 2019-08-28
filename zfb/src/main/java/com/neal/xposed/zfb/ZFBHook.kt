package com.neal.xposed.zfb

import android.app.Application
import android.content.Context
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage

class ZFBHook {
    companion object {
        fun hookZFBRpcCall(lpparam: XC_LoadPackage.LoadPackageParam) {
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