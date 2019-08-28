package com.neal.xposed.zfb.collectenergy

import android.app.Activity
import android.text.TextUtils
import android.widget.Toast
import com.neal.xposed.tools.pref.PreferenceUtils
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
        private val TAG = "XposedHookZFB"
        private val friendsWhiteListId = object : ArrayList<String>() {
            init {
                add("2088032707539521")
                add("2088202931109144")
            }
        }
        private val sharedPreferences = PreferenceUtils.getInstance(TARGET_PACKAGE_NAME)
        private var autoCollect = true
        private var autoCollectGap: Long = 2000
        private const val DEFAULT_COLLECT_DELAY = (1000 * 20).toLong()

        var loader: ClassLoader? = null
        private var curH5PageImpl: Any? = null
        var curH5FragmentRef: WeakReference<Any>? = null
        private val collectData = ArrayList<CollectData>()
        private var mostRecentCollectTime = java.lang.Long.MAX_VALUE
        private var totalEnergy: Int = 0
        private var totalHelpEnergy: Int = 0
        private var pageCount: Int = 0
        private var timer: Timer? = null

        fun DiagnoseRpcHookParams(param: XC_MethodHook.MethodHookParam) {
            val args = param.args
            // Log.i(TAG, "params:" + args[0] + "," + args[1] + "," + args[2] + "," + args[3]
            //                + "," + args[4] + "," + args[5] + "," + args[6] + "," + "H5" + "," + args[8]
            //                + "," + args[9] + "," + args[10] + "," + args[11] + "," + args[12]);
            val funcName = args[0] as String
            val jsonArgs = args[1] as String
            when (funcName) {
                QUERY_FRIEND_RANKING -> if (parseFriendRankPageDataResponse(parseResponseData(param.result))) {
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

        private fun reload() {
            sharedPreferences.reload()
            autoCollect = sharedPreferences.getBoolean("autoCollect", true)
            autoCollectGap = sharedPreferences.getLong("autoCollectGap", DEFAULT_COLLECT_DELAY)
            if (autoCollectGap < DEFAULT_COLLECT_DELAY) {
                autoCollectGap = DEFAULT_COLLECT_DELAY
            }
        }

        private fun reset() {
            collectData.clear()
            mostRecentCollectTime = java.lang.Long.MAX_VALUE
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
            if (autoCollect) {
                val currentTime = System.currentTimeMillis()
                timer!!.schedule(object : TimerTask() {
                    override fun run() {
                        showToast("下次收集时间:" + simpleDateFormat.format(Date(currentTime + 1000)))
                    }
                }, 1000)
                timer!!.schedule(object : TimerTask() {
                    override fun run() {
                        restartCollect()
                    }
                }, autoCollectGap)
            }
        }

        private fun restartCollect() {
            try {
                val jsonArray = JSONArray()
                val json = JSONObject()
                json.put("version", "20181220")
                jsonArray.put(json)
                //            Log.i(TAG, "call restartCollect energy params:" + jsonArray);
                rpcCall("alipay.antmember.forest.h5.queryNextAction", jsonArray.toString())
            } catch (e: JSONException) {
                e.printStackTrace()
            }

        }

        private fun rpcCall_QueryFriendRanking() {
            try {
                val jsonArray = JSONArray()
                val json = JSONObject()
                json.put("av", "5")
                json.put("ct", "android")
                json.put("pageSize", pageCount!! * 20)
                json.put("startPoint", "" + (pageCount!! * 20 + 1))
                pageCount++
                jsonArray.put(json)
                showToast("开始获取可以收取第" + pageCount + "页好友信息的能量...")
                // Log.i(TAG, "rpcCall_QueryFriendRanking params:" + jsonArray);
                rpcCall(QUERY_FRIEND_RANKING, jsonArray.toString())
            } catch (e: Exception) {
                // Log.i(TAG, "rpcCall_QueryFriendRanking err: " + Log.getStackTraceString(e));
            }

        }

        private fun rpcCall_QueryFriendPage(data: CollectData) {
            try {
                val jsonArray = JSONArray()
                val json = JSONObject()
                json.put("canRobFlags", data.robFlags)
                json.put("userId", data.collectUserId)
                json.put("version", "20181220")
                jsonArray.put(json)
                // Log.i(TAG, "call cancollect energy params:" + jsonArray);

                rpcCall(QUERY_FRIEND_ACTION, jsonArray.toString())

                val pkArray = JSONArray()
                val pkObject = JSONObject()
                pkObject.put("pkType", "Week")
                pkObject.put("pkUser", data.collectUserId)
                pkArray.put(pkObject)
                rpcCall(QUERY_PK_RECORDS, pkArray.toString())

                val jArray = JSONArray()
                val jObject = JSONObject()
                jObject.put("pageSize", 10)
                jObject.put("startIndex", 0)
                jObject.put("userId", data.collectUserId)
                jArray.put(jObject)
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
                            val collectLaterTime = jsonObject.optLong("canCollectLaterTime")
                            if (collectLaterTime != -1L && mostRecentCollectTime > collectLaterTime) {
                                mostRecentCollectTime = collectLaterTime
                                //                            Log.i(TAG, "most recent collect time change: " + mostRecentCollectTime);
                            }
                            val data =
                                CollectData(
                                    userId,
                                    canCollect,
                                    canHelpCollect,
                                    collectLaterTime != -1L
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
                val method = resp.javaClass.getMethod("getResponse", *arrayOf())
                return method.invoke(resp, *arrayOf()) as String
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
                val jsonClazz = loader!!.loadClass("com.alibaba.fastjson.JSONObject")
                val obj = jsonClazz.newInstance()
                rpcCallMethod!!.invoke(
                    null, funcName, jsonArrayString,
                    "", true, obj, null, false, curH5PageImpl, 0, "", false, -1, ""
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
                val jsonArray = JSONArray()
                val bubbleAry = JSONArray()
                bubbleAry.put(bubbleId)
                val json = JSONObject()
                json.put("targetUserId", userId)
                json.put("bubbleIds", bubbleAry)
                jsonArray.put(json)
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
                val jsonArray = JSONArray()
                val bubbleAry = JSONArray()
                bubbleAry.put(bubbleId)
                val json = JSONObject()
                json.put("userId", userId)
                json.put("bubbleIds", bubbleAry)
                jsonArray.put(json)
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
                    val h5PageClazz = loader!!.loadClass("com.alipay.mobile.h5container.api.H5Page")
                    val jsonClazz = loader!!.loadClass("com.alibaba.fastjson.JSONObject")
                    val rpcClazz =
                        loader!!.loadClass("com.alipay.mobile.nebulaappproxy.api.rpc.H5RpcUtil")
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
        }
    }
}