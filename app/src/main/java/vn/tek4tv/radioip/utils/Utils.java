package vn.tek4tv.radioip.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.provider.Settings;

import androidx.annotation.RequiresApi;

import java.net.NetworkInterface;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import vn.tek4tv.radioip.model.Playlist;

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

    public static int getCurrentPosition(List<Playlist> list){
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("HH:mm:ss");
        if(list == null || list.size() == 0){
            return 0;
        }
        try{
            for(int i = 0 ; i < list.size() ; i++){
                Date dateStartOld = simpleDateFormat.parse(list.get(i).getStart());
                Calendar calDateStart = Calendar.getInstance();
                calDateStart.setTime(dateStartOld);
                Calendar dateStartNow  = Calendar.getInstance();
                dateStartNow.set(Calendar.YEAR, Calendar.getInstance().get(Calendar.YEAR));
                dateStartNow.set(Calendar.MONTH, Calendar.getInstance().get(Calendar.MONTH));
                dateStartNow.set(Calendar.DAY_OF_MONTH, Calendar.getInstance().get(Calendar.DAY_OF_MONTH));
                dateStartNow.set(Calendar.HOUR_OF_DAY, calDateStart.get(Calendar.HOUR_OF_DAY));
                dateStartNow.set(Calendar.MINUTE, calDateStart.get(Calendar.MINUTE));
                dateStartNow.set(Calendar.SECOND, calDateStart.get(Calendar.SECOND));
                Date dateEndOld = simpleDateFormat.parse(list.get(i).getEnd());
                Calendar calDateOld = Calendar.getInstance();
                calDateOld.setTime(dateEndOld);
                Calendar dateEndNow  = Calendar.getInstance();
                dateEndNow.set(Calendar.YEAR, Calendar.getInstance().get(Calendar.YEAR));
                dateEndNow.set(Calendar.MONTH, Calendar.getInstance().get(Calendar.MONTH));
                dateEndNow.set(Calendar.DAY_OF_MONTH, Calendar.getInstance().get(Calendar.DAY_OF_MONTH));
                dateEndNow.set(Calendar.HOUR_OF_DAY, calDateOld.get(Calendar.HOUR_OF_DAY));
                dateEndNow.set(Calendar.MINUTE, calDateOld.get(Calendar.MINUTE));
                dateEndNow.set(Calendar.SECOND, calDateOld.get(Calendar.SECOND));
                Date date = new Date();
                if(date.after(dateStartNow.getTime()) && date.before(dateEndNow.getTime())){
                    return i;
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return list.size() - 1;
    }

    public static String getTimeCurrent(){
        try{
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("HH:mm:ss");
            return simpleDateFormat.format(new Date());
        }catch (Exception e){
        }
        return "";
    }

    public static long getTimeBettween(String start , String end){
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("HH:mm:ss");
        try{
            Date dateStartOld = simpleDateFormat.parse(start);
            Date dateEndOld = simpleDateFormat.parse(end);
            return printDifference(dateStartOld, dateEndOld);
        }catch (Exception e){
            e.printStackTrace();
        }
        return 0;
    }

    public static long printDifference(Date startDate, Date endDate) {
        //milliseconds
        return endDate.getTime() - startDate.getTime();
    }
}
