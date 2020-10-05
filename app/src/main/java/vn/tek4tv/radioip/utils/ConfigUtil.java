package vn.tek4tv.radioip.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class ConfigUtil {
    public static final String PREFERENCE_FILE_NAME = "vn.tek4tv.radioip";
    public static final String ACTION_SCHEDULED = "vn.tek4tv.radioip";

    public static void putString(Context context, String key, String value) {
        SharedPreferences sharedPref = context.getSharedPreferences(PREFERENCE_FILE_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(key, value);
        editor.commit();
    }
    public static String getString(Context context, String key, String defaultValue) {
        SharedPreferences sharedPref = context.getSharedPreferences(PREFERENCE_FILE_NAME, Context.MODE_PRIVATE);
        return sharedPref.getString(key, defaultValue);
    }
}
