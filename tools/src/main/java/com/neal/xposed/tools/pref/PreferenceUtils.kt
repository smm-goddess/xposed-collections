package com.neal.xposed.tools.pref

import de.robv.android.xposed.XSharedPreferences
import java.io.File

class PreferenceUtils {
    companion object {
        private var sharedPreferencesMap: MutableMap<String, XSharedPreferences> = mutableMapOf()

        public fun getInstance(packageName: String): XSharedPreferences {
            var instance = sharedPreferencesMap[packageName]
            if (instance == null) {
                instance =
                    XSharedPreferences(File("/storage/emulated/0/" + packageName + "_config.xml"))
                sharedPreferencesMap[packageName] = instance
            }
            return instance
        }
    }
}