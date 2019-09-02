package com.neal.xposed

import com.neal.xposed.alipay.AlipayHook
import com.neal.xposed.alipay.collectenergy.TARGET_PACKAGE_NAME
import com.neal.xposed.alipay.collectenergy.TARGET_PACKAGE_PROCESS_NAME
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.callbacks.XC_LoadPackage

public class MainHook : IXposedHookLoadPackage {
    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        if (TARGET_PACKAGE_NAME == lpparam.packageName && TARGET_PACKAGE_PROCESS_NAME == lpparam.processName) {
            AlipayHook.hookAlipayRpcCall(lpparam)
        }
    }
}