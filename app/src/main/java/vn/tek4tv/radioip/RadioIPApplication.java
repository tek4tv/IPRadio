package vn.tek4tv.radioip;

import android.app.Application;
import android.content.Context;

public class RadioIPApplication extends Application {
    public static RadioIPApplication instance;
    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
    }
    @Override
    public Context getApplicationContext() {
        return super.getApplicationContext();
    }
    public static RadioIPApplication getInstance() {
        return instance;
    }
}
