package vn.tek4tv.radioip.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.provider.Settings;

import java.net.NetworkInterface;
import java.util.Collections;
import java.util.List;

public class Utils {
    public static final String DEVICE_ID = "device_id";
    public static final String callback_hub = "CALLBACK_HUB";
    public static final String ping_hub = "PING_HUB";
    public static final String device_info = "DEVICE_INFO";
    public static final String current_playlist = "CURRENT_PLAYLIST";
    public static final String CURRENT_VOLUME = "CURRENT_VOLUME";
    public static final String CURRENT_SOURCE = "CURRENT_SOURCE";
    public static final String CURRENT_PA = "CURRENT_PA";
    public static final String CURRENT_FM = "CURRENT_FM";
    public static final String CURRENT_AM = "CURRENT_AM";
    public static final String CURRENT_TEMPERATURE = "CURRENT_TEMPERATURE";

    @SuppressLint("HardwareIds")
    public static String getDeviceId(Context context) {
        String deviceId = ConfigUtil.getString(context, DEVICE_ID, null);
        if (deviceId == null) {
            deviceId = getMacAddr();
            ConfigUtil.putString(context, DEVICE_ID, deviceId);
        }
        return deviceId;
    }
    public static String getMacAddr() {
        try {
            List<NetworkInterface> all = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface nif: all) {
                if (!nif.getName().equalsIgnoreCase("wlan0")) continue;

                byte[] macBytes = nif.getHardwareAddress();
                if (macBytes == null) {
                    return "";
                }

                StringBuilder res1 = new StringBuilder();
                for (byte b: macBytes) {
                    //res1.append(Integer.toHexString(b & 0xFF) + ":");
                    res1.append(String.format("%02X:", b));
                }

                if (res1.length() > 0) {
                    res1.deleteCharAt(res1.length() - 1);
                }
                return res1.toString();
            }
        } catch (Exception ex) {}
        return "02:00:00:00:00:00";
    }
}
