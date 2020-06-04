package com.samsung.health.neal;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.widget.Toast;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Calendar;
import java.util.Random;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class MainHook implements IXposedHookLoadPackage {
    private Activity main;
    private Method addStep;
    private Field p;
    private Field q;

    @Override
    public void handleLoadPackage(final XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if ("com.samsung.android.app.health.dataviewer".equals(lpparam.packageName)) {
            XposedBridge.log("Hook Samsung Step");

            XposedHelpers.findAndHookMethod(
                    "com.samsung.android.sdk.healthdata.HealthDataService",
                    lpparam.classLoader,
                    "initialize",
                    Context.class,
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            XposedBridge.log("Hook HealthDataService initialize");
                            main = (Activity) param.args[0];
                            showToast("Hook Steps");
                            super.beforeHookedMethod(param);
                        }
                    }
            );
            XposedHelpers.findAndHookMethod(
                    "com.specher.shealthplus.MainActivity",
                    lpparam.classLoader,
                    "onCreate",
                    Bundle.class,
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            initMethod(param.thisObject, lpparam.classLoader);
                            super.afterHookedMethod(param);
                        }
                    });

            XposedHelpers.findAndHookMethod(
                    "android.support.v7.app.b.a",
                    lpparam.classLoader,
                    "a",
                    CharSequence.class,
                    DialogInterface.OnClickListener.class,
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            XposedBridge.log("Hook DialogInterface.setOnclickListener");
                            DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    Random random = new Random();
                                    Calendar calendar = Calendar.getInstance();
                                    long time = calendar.get(Calendar.MINUTE) * 60 * 1000 +
                                            calendar.get(Calendar.SECOND) * 1000 +
                                            calendar.get(Calendar.MILLISECOND) +
                                            18000000;
                                    for (int i = 1; i <= 30; i++) {
                                        addSteps(time + i * 86400000, 7000 + random.nextInt(2000));
                                    }
                                }
                            };
                            param.args[1] = listener;
                            super.beforeHookedMethod(param);
                        }
                    }
            );
        }
    }

    private void showToast(String text) {
        if (main != null) {
            Toast.makeText(main, text, Toast.LENGTH_SHORT).show();
        }
    }

    private void addSteps(long time, int steps) {
        try {
            long base = q.getLong(main);
            addStep.invoke(p.get(main), time + base, steps);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    private void initMethod(Object object, ClassLoader loader) {
        try {
            p = object.getClass().getDeclaredField("p");
            p.setAccessible(true);
            q = object.getClass().getDeclaredField("q");
            q.setAccessible(true);
            Class a = XposedHelpers.findClass("com.specher.shealthplus.a", loader);
            addStep = a.getMethod("a", long.class, int.class);
        } catch (NoSuchFieldException | NoSuchMethodException e) {
            e.printStackTrace();
        }
    }

}
