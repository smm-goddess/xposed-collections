package com.neal.xposed.zfb

import android.content.Context
import android.os.Bundle
import com.neal.xposed.zfb.collectenergy.AliMobileAutoCollectEnergyUtils
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
import java.lang.ref.WeakReference

class ApplicationAttachMethodHook : XC_MethodHook() {
    companion object {
        var TAG: String = "XposedHookZFB"
    }

    @Throws(Throwable::class)
    override fun afterHookedMethod(param: MethodHookParam) {
        super.afterHookedMethod(param)
        val context = param.args[0] as Context
        val loader = context.classLoader
        AliMobileAutoCollectEnergyUtils.ctxRef = WeakReference(context)
        val packageManager = context.packageManager
        val packageInfo = packageManager.getPackageInfo(context.packageName, 0)
        if (packageInfo.versionCode >= 135) {
            var clazz: Class<*>? =
                loader.loadClass("com.alipay.mobile.nebulacore.ui.H5FragmentManager")
            if (clazz != null) {
                val h5FragmentClazz =
                    loader.loadClass("com.alipay.mobile.nebulacore.ui.H5Fragment")
                if (h5FragmentClazz != null) {
                    XposedHelpers.findAndHookMethod(clazz,
                        "pushFragment",
                        h5FragmentClazz,
                        Boolean::class.javaPrimitiveType,
                        Bundle::class.java,
                        Boolean::class.javaPrimitiveType,
                        Boolean::class.javaPrimitiveType,
                        object :
                            XC_MethodHook() {

                            @Throws(Throwable::class)
                            override fun afterHookedMethod(param: MethodHookParam?) {
                                super.afterHookedMethod(param)
                                AliMobileAutoCollectEnergyUtils.curH5FragmentRef =
                                    WeakReference(param!!.args[0])
                                AliMobileAutoCollectEnergyUtils.loadEnable()
                            }
                        })
                }
            }

            clazz = loader.loadClass("com.alipay.mobile.nebulaappproxy.api.rpc.H5RpcUtil")
            if (clazz != null) {
                val h5PageClazz = loader.loadClass("com.alipay.mobile.h5container.api.H5Page")
                val jsonClazz = loader.loadClass("com.alibaba.fastjson.JSONObject")
                if (h5PageClazz != null && jsonClazz != null) {
                    XposedHelpers.findAndHookMethod(clazz,
                        "rpcCall",
                        String::class.java,
                        String::class.java,
                        String::class.java,
                        Boolean::class.javaPrimitiveType,
                        jsonClazz,
                        String::class.java,
                        Boolean::class.javaPrimitiveType,
                        h5PageClazz,
                        Int::class.javaPrimitiveType,
                        String::class.java,
                        Boolean::class.javaPrimitiveType,
                        Int::class.javaPrimitiveType,
                        String::class.java,
                        object :
                            XC_MethodHook() {

                            @Throws(Throwable::class)
                            override fun afterHookedMethod(param: MethodHookParam?) {
                                super.afterHookedMethod(param)
                                AliMobileAutoCollectEnergyUtils.diagnoseRpcHookParams(param!!)
                            }
                        })
                }
            }
        }
    }
}