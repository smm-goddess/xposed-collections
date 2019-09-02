package com.neal.xposed.alipay.collectenergy

import android.app.Activity
import android.content.Context
import android.net.Uri
import android.text.TextUtils
import android.util.Log
import android.widget.Toast
import de.robv.android.xposed.XC_MethodHook
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.lang.ref.WeakReference
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.text.SimpleDateFormat
import java.util.*

class AliMobileAutoCollectEnergyUtils {
    companion object {
        private val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA)
        private val TAG = "XposedHookAlipay"
        private val friendsWhiteListId = object : ArrayList<String>() {
            init {
                add("2088032707539521")
                add("2088202931109144")
            }
        }
        private var autoCollectIntervalOpen = true
        private var autoCollectOpen = true
        private var autoCollectInterval: Long = 2000
        private const val DEFAULT_COLLECT_DELAY = (1000 * 5).toLong()
        var ctxRef: WeakReference<Context>? = null

        private var curH5PageImpl: Any? = null
        var curH5FragmentRef: WeakReference<Any>? = null
        private val collectData = ArrayList<CollectData>()
        private var totalEnergy: Int = 0
        private var totalHelpEnergy: Int = 0
        private var pageCount: Int = 0
        private var timer: Timer? = null

        fun diagnoseRpcHookParams(param: XC_MethodHook.MethodHookParam) {
            if (autoCollectOpen) {
                val args = param.args
                // Log.i(TAG, "params:" + args[0] + "," + args[1] + "," + args[2] + "," + args[3]
                //                + "," + args[4] + "," + args[5] + "," + args[6] + "," + "H5" + "," + args[8]
                //                + "," + args[9] + "," + args[10] + "," + args[11] + "," + args[12]);
                val funcName = args[0] as String
                val jsonArgs = args[1] as String
                Log.d(TAG, "funcName:$funcName")
                when (funcName) {
                    QUERY_FRIEND_RANKING -> if (parseFriendRankPageDataResponse(
                            parseResponseData(
                                param.result
                            )
                        )
                    ) {
                        val jsonArray = JSONArray(jsonArgs)
                        val jsonObject: JSONObject = jsonArray.get(0) as JSONObject
                        pageCount = jsonObject.getInt("pageSize") / 20 + 1
                        rpcCall_QueryFriendRanking()
                    } else {
                        showToast("开始获取每个好友能够偷取的能量信息...")
                        for (data in collectData) {
                            rpcCall_QueryFriendPage(data)
                        }
                        postCollect()
                    }
                    QUERY_FRIEND_ACTION -> if (!jsonArgs.contains("userId")) {
                        // 刚打开页面,请求不带userId,获取的是自己的数据
                        reset()
                        parseCollectBubbles(parseResponseData(param.result))
                        pageCount = 0
                        rpcCall_QueryFriendRanking()
                    } else {
                        // 其他用户的能量球信息
                        parseCollectBubbles(parseResponseData(param.result))
                    }
                    HELP_COLLECT_ENERGY -> parseHelpCollectEnergyResponse(parseResponseData(param.result))
                    COLLECT_ENERGY -> parseCollectEnergyResponse(parseResponseData(param.result))
                    else -> {
                    }
                }
            }
        }

        fun loadEnable() {
            val ctx = ctxRef!!.get()
            if (ctx != null) {
                val uri =
                    Uri.parse("content://com.neal.xposed.preference/xposed_settings?key=auto_collect_open")
                val cursor = ctx.contentResolver.query(uri, null, null, null, null)
                cursor?.moveToFirst()
                if (cursor?.count ?: 0 > 0) {
                    autoCollectOpen =
                        cursor?.getString(cursor.getColumnIndex("value"))?.toBoolean() ?: true
                }
                cursor?.close()
            } else {
                autoCollectOpen = true
            }
        }

        private fun reload() {
            Log.i(TAG, "reload")
            val ctx = ctxRef!!.get()
            if (ctx != null) {
                val uri = Uri.parse("content://com.neal.xposed.preference/xposed_settings")
                val cursor = ctx.contentResolver.query(uri, null, null, null, null)
                while (cursor?.moveToNext() == true) {
                    val key = cursor.getString(cursor.getColumnIndex("key"))
                    val value = cursor.getString(cursor.getColumnIndex("value"))
                    when (key) {
                        "auto_collect_interval_open" -> autoCollectIntervalOpen =
                            value?.toBoolean() ?: true
                        "auto_collect_open" -> autoCollectOpen = value?.toBoolean() ?: true
                        "auto_collect_interval" -> autoCollectInterval = value?.toLong() ?: 2000
                    }
                }
                cursor?.close()
            } else {
                autoCollectInterval = DEFAULT_COLLECT_DELAY
                autoCollectIntervalOpen = true
                autoCollectOpen = true
            }
            if (autoCollectInterval < DEFAULT_COLLECT_DELAY) {
                autoCollectInterval = DEFAULT_COLLECT_DELAY
            }
            Log.i(TAG, "autoCollectIntervalOpen:$autoCollectIntervalOpen")
            Log.i(TAG, "autoCollectOpen:$autoCollectOpen")
            Log.i(TAG, "autoCollectInterval:$autoCollectInterval")
        }

        private fun reset() {
            collectData.clear()
            totalEnergy = 0
            totalHelpEnergy = 0
            pageCount = 0
            if (timer != null) {
                timer!!.cancel()
            }
            timer = Timer()
        }

        private fun postCollect() {
            showToast("一共收取了" + totalEnergy + "g能量" + ",帮助收取了" + totalHelpEnergy + "g能量")
            reload()
            if (autoCollectIntervalOpen) {
                val currentTime = System.currentTimeMillis()
                timer!!.schedule(object : TimerTask() {
                    override fun run() {
                        showToast("下次收集时间:" + simpleDateFormat.format(Date(currentTime + autoCollectInterval)))
                    }
                }, 1000)
                timer!!.schedule(object : TimerTask() {
                    override fun run() {
                        restartCollect()
                    }
                }, autoCollectInterval)
            }
        }

        private fun restartCollect() {
            try {
                val jsonArray = JSONArray().apply {
                    put(JSONObject().apply {
                        put("version", "20181220")
                    })
                }
                //            Log.i(TAG, "call restartCollect energy params:" + jsonArray);
                rpcCall("alipay.antmember.forest.h5.queryNextAction", jsonArray.toString())
            } catch (e: JSONException) {
                e.printStackTrace()
            }

        }

        private fun rpcCall_QueryFriendRanking() {
            try {
                val jsonArray = JSONArray().apply {
                    put(
                        JSONObject().apply {
                            put("av", "5")
                            put("ct", "android")
                            put("pageSize", pageCount * 20)
                            put("startPoint", "" + (pageCount * 20 + 1))
                        }
                    )
                }
                showToast("开始获取可以收取第 ${pageCount + 1} 页好友信息的能量...")
                Log.i(TAG, "rpcCall_QueryFriendRanking params:$jsonArray");
                rpcCall(QUERY_FRIEND_RANKING, jsonArray.toString())
            } catch (e: Exception) {
                // Log.i(TAG, "rpcCall_QueryFriendRanking err: " + Log.getStackTraceString(e));
            }

        }

        private fun rpcCall_QueryFriendPage(data: CollectData) {
            try {
                val jsonArray = JSONArray().apply {
                    put(JSONObject().apply {
                        put("canRobFlags", data.robFlags)
                        put("userId", data.collectUserId)
                        put("version", "20181220")
                    })
                }
                // Log.i(TAG, "call cancollect energy params:" + jsonArray);

                rpcCall(QUERY_FRIEND_ACTION, jsonArray.toString())

                val pkArray = JSONArray().apply {
                    put(JSONObject().apply {
                        put("pkType", "Week")
                        put("pkUser", data.collectUserId)
                    })
                }
                rpcCall(QUERY_PK_RECORDS, pkArray.toString())

                val jArray = JSONArray().apply {
                    JSONObject().apply {
                        put("pageSize", 10)
                        put("startIndex", 0)
                        put("userId", data.collectUserId)
                    }
                }
                rpcCall(QUERY_PAGE_DYNAMICS, jArray.toString())
            } catch (e: Exception) {
                // Log.i(TAG, "rpcCall_CanCollectEnergy err: " + Log.getStackTraceString(e));
            }

        }

        private fun parseCollectBubbles(response: String) {
            try {
                val jsonObject = JSONObject(response)
                val jsonArray = jsonObject.optJSONArray("bubbles")
                if (jsonArray != null && jsonArray.length() > 0) {
                    for (i in 0 until jsonArray.length()) {
                        val jsonObject1 = jsonArray.getJSONObject(i)
                        val userId = jsonObject1.optString("userId")
                        val collectStatus = jsonObject1.optString("collectStatus")
                        if (!friendsWhiteListId.contains(userId)) {
                            if ("AVAILABLE" == collectStatus) {
                                rpcCall_CollectEnergy(
                                    jsonObject1.optString("userId"),
                                    jsonObject1.optLong("id")
                                )
                            }
                        }
                        if ("INSUFFICIENT" == collectStatus && jsonObject1.optBoolean("canHelpCollect")) {
                            rpcCall_HelpCollectEnergy(
                                jsonObject1.optString("userId"),
                                jsonObject1.optLong("id")
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        /**
         * 解析好友信息
         *
         * @param response
         * @return
         */
        private fun parseFriendRankPageDataResponse(response: String): Boolean {
            try {
                val jObject = JSONObject(response)
                val optJSONArray = jObject.optJSONArray("friendRanking")
                if (optJSONArray != null) {
                    for (i in 0 until optJSONArray.length()) {
                        val jsonObject = optJSONArray.getJSONObject(i)
                        val canCollect = jsonObject.optBoolean("canCollectEnergy")
                        val canHelpCollect = jsonObject.optBoolean("canHelpCollect")
                        val userId = jsonObject.optString("userId")
                        if (!friendsWhiteListId.contains(userId)) {
                            val data =
                                CollectData(
                                    userId,
                                    canCollect,
                                    canHelpCollect,
                                    jsonObject.optLong("canCollectLaterTime") != -1L
                                )
                            if ((canCollect || canHelpCollect) && !collectData.contains(data)) {
                                collectData.add(data)
                            }
                        }
                    }
                }
                return jObject.optBoolean("hasMore")
            } catch (e: Exception) {
                e.printStackTrace()
            }

            return false
        }

        private fun parseResponseData(resp: Any): String {
            try {
                val method = resp.javaClass.getMethod("getResponse")
                val response = method.invoke(resp) as String
//                Log.d(TAG, "response:$response")
                return response
            } catch (e: NoSuchMethodException) {
                e.printStackTrace()
            } catch (e: IllegalAccessException) {
                e.printStackTrace()
            } catch (e: InvocationTargetException) {
                e.printStackTrace()
            }
            return ""
        }

        private fun rpcCall(funcName: String, jsonArrayString: String) {
            try {
                val rpcCallMethod = getRpcCallMethod()
                val jsonClazz =
                    ctxRef!!.get()!!.classLoader.loadClass("com.alibaba.fastjson.JSONObject")
                val obj = jsonClazz.newInstance()
                rpcCallMethod!!.invoke(
                    null, funcName, jsonArrayString, "", true, obj, null, false,
                    curH5PageImpl, 0, "", false, -1, ""
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }

        }

        /**
         * 帮助收取能量命令
         *
         * @param userId
         * @param bubbleId
         */
        private fun rpcCall_HelpCollectEnergy(userId: String, bubbleId: Long) {
            try {
                val jsonArray = JSONArray().apply {
                    put(JSONObject().apply {
                        put("targetUserId", userId)
                        put("bubbleIds", JSONArray().apply {
                            put(bubbleId)
                        })
                    })
                }
                // Log.i(TAG, "call HelpCollectEnergy energy params:" + jsonArray);

                rpcCall(HELP_COLLECT_ENERGY, jsonArray.toString())
            } catch (e: Exception) {
                // Log.i(TAG, "rpcCall_HelpCollectEnergy err: " + Log.getStackTraceString(e));
            }

        }

        /**
         * 收取能量命令
         *
         * @param userId
         * @param bubbleId
         */
        private fun rpcCall_CollectEnergy(userId: String, bubbleId: Long) {
            try {
                val jsonArray = JSONArray().apply {
                    put(JSONObject().apply {
                        put("userId", userId)
                        put("bubbleIds", JSONArray().apply {
                            put(bubbleId)
                        })
                    })
                }
                // Log.i(TAG, "call rpcCall_CollectEnergy energy params:" + jsonArray);

                rpcCall(COLLECT_ENERGY, jsonArray.toString())
            } catch (e: Exception) {
                // Log.i(TAG, "rpcCall_CollectEnergy err: " + Log.getStackTraceString(e));
            }

        }

        private fun getRpcCallMethod(): Method? {
            val curH5Fragment = curH5FragmentRef!!.get()
            if (curH5Fragment != null) {
                try {
                    val aF = curH5Fragment.javaClass.getDeclaredField("a")
                    aF.isAccessible = true
                    val viewHolder = aF.get(curH5Fragment)
                    val hF = viewHolder.javaClass.getDeclaredField("h")
                    hF.isAccessible = true
                    curH5PageImpl = hF.get(viewHolder)
                    val classLoader: ClassLoader = ctxRef!!.get()!!.classLoader
                    val h5PageClazz =
                        classLoader.loadClass("com.alipay.mobile.h5container.api.H5Page")
                    val jsonClazz = classLoader.loadClass("com.alibaba.fastjson.JSONObject")
                    val rpcClazz =
                        classLoader.loadClass("com.alipay.mobile.nebulaappproxy.api.rpc.H5RpcUtil")
                    if (curH5PageImpl != null) {
                        return rpcClazz.getMethod(
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
                            String::class.java
                        )
                    }
                } catch (e: Exception) {
                    // Log.i(TAG, "getRpcCallMethod err: " + Log.getStackTraceString(e));
                }

            }
            return null
        }

        private fun parseHelpCollectEnergyResponse(response: String) {
            if (!TextUtils.isEmpty(response) && response.contains("failedBubbleIds")) {
                try {
                    val jsonObject = JSONObject(response)
                    val jsonArray = jsonObject.optJSONArray("bubbles")
                    for (i in 0 until jsonArray.length()) {
                        totalHelpEnergy += jsonArray.getJSONObject(i).optInt("collectedEnergy")
                    }
                } catch (e: Exception) {
                }

            }
        }

        private fun parseCollectEnergyResponse(response: String) {
            if (!TextUtils.isEmpty(response) && response.contains("failedBubbleIds")) {
                try {
                    val jsonObject = JSONObject(response)
                    val jsonArray = jsonObject.optJSONArray("bubbles")
                    for (i in 0 until jsonArray.length()) {
                        totalEnergy += jsonArray.getJSONObject(i).optInt("collectedEnergy")
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }

            }
        }

        private fun getActivity(): Activity? {
            val h5Fragment = curH5FragmentRef!!.get()
            if (h5Fragment != null) {
                try {
                    val getActivity = h5Fragment.javaClass.getMethod("getActivity")
                    getActivity.isAccessible = true
                    return getActivity.invoke(h5Fragment) as Activity
                } catch (e: Exception) {
                    e.printStackTrace()
                }

            }
            return null
        }

        private fun showToast(str: String) {
            val activity = getActivity()
            if (activity != null) {
                try {
                    activity.runOnUiThread {
                        Toast.makeText(activity, str, Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    // Log.i(TAG, "showToast err: " + Log.getStackTraceString(e));
                }

            }
        }

        private class CollectData internal constructor(
            internal var collectUserId: String,
            internal var canCollect: Boolean,
            internal var canHelpCollect: Boolean,
            internal var canCollectAfter: Boolean
        ) {

            internal val robFlags: String
                get() {
                    var flags: String
                    if (canCollect) {
                        flags = "T"
                    } else {
                        flags = "F"
                    }
                    if (canHelpCollect) {
                        flags = "$flags,T"
                    } else {
                        flags = "$flags,F"
                    }
                    if (canCollectAfter) {
                        flags = "$flags,T"
                    } else {
                        flags = "$flags,F"
                    }
                    return flags
                }

            override fun equals(other: Any?): Boolean {
                return if (other is CollectData) {
                    this.collectUserId == other.collectUserId
                } else {
                    false
                }
            }

            override fun hashCode(): Int {
                return collectUserId.hashCode()
            }
        }
    }
}