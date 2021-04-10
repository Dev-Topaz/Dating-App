package com.lak.pi.util;

import android.content.Context;
import android.content.SharedPreferences;

public class Prefs {
    private static String PREF_KEY = "datingapp";

    private static String DEF_ANY_VAL = "";
    private static Boolean DEF_BOOL = false;

    public static String GENDER_KEY = "gender";

    public static SharedPreferences getPrefs(Context context) {
        return context.getSharedPreferences(PREF_KEY, Context.MODE_PRIVATE);
    }

    public static boolean contains(Context context, String key) {
        return getPrefs(context).contains(key);
    }

    public static String getStrPref(Context context, String key, boolean firsttime) {
        return getPrefs(context).getString(key, DEF_ANY_VAL);
    }

    public static String getStrPref(Context context, String key) {
        return getPrefs(context).getString(key, DEF_ANY_VAL);
    }

    public static String getStrPref(Context context, String key, String defVal) {
        return getPrefs(context).getString(key, defVal);
    }

    public static int getIntPref(Context context, String key) {
        return getPrefs(context).getInt(key, 0);
    }

    public static int getIntPref(Context context, String key, Integer num) {
        return getPrefs(context).getInt(key, num);
    }
    public static Long getLongPref(Context context, String key, Long num) {
        return  getPrefs(context).getLong(key, 0L);
    }
    public static Boolean getBoolPref(Context context, String key) {
        return getPrefs(context).getBoolean(key, DEF_BOOL);
    }

    public static Boolean getBoolPref(Context context, String key, Boolean defVal) {
        return getPrefs(context).getBoolean(key, defVal);
    }

    public static void setStrPref(Context context, String key, String value) {
        getPrefs(context).edit().putString(key, value).apply();
    }

    public static void setIntPref(Context context, String key, int value) {
        getPrefs(context).edit().putInt(key, value).apply();
    }

    public static void clearLocalStorage(Context context) {
        getPrefs(context).edit().clear().apply();
    }

    public static void setBoolPref(Context context, String key, Boolean value) {
        getPrefs(context).edit().putBoolean(key, value).apply();
    }

    public static void removePref(Context context, String key) {
        getPrefs(context).edit().remove(key).apply();
    }
}
