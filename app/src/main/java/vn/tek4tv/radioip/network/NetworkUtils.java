package vn.tek4tv.radioip.network;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.NetworkRequest;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

import java.util.LinkedList;
import java.util.List;

public class NetworkUtils {
    public static final String BASE_URL = "https://iotdevice.tek4tv.vn/";
    public static final String URL_HUB = "https://iot.tek4tv.vn/iothub";
    private final String TAG = "NetworkUtils";
    private static NetworkUtils INSTANCE;
    private boolean isConnected = false;

    private List<ConnectionCallback> mCallbacks = new LinkedList<>();
    public MutableLiveData<Boolean> mNetworkLive = new MutableLiveData<>();

    private NetworkRequest mNetRequest = new NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build();

    private ConnectivityManager.NetworkCallback mNetCallback = new ConnectivityManager.NetworkCallback() {

        @Override
        public void onAvailable(@NonNull Network network) {
            super.onAvailable(network);
            Log.d(TAG, "available");
            mNetworkLive.postValue(true);
            isConnected = true;
            for (ConnectionCallback callback : mCallbacks) {
                if (callback != null) {
                    callback.onChange(true);
                }
            }
        }

        @Override
        public void onLost(@NonNull Network network) {
            super.onLost(network);
            Log.d(TAG, "lost");
            mNetworkLive.postValue(false);
            isConnected = false;
            for (NetworkUtils.ConnectionCallback callback : mCallbacks) {
                if (callback != null) {
                    callback.onChange(false);
                }
            }
        }
    };

    private NetworkUtils() {
        // This class is not publicly instantiable
    }

    public static NetworkUtils getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new NetworkUtils();
        }
        return INSTANCE;
    }

    public static boolean isNetworkConnected(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm != null) {
            NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
            return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
        }
        return false;
    }

    public boolean isNetworkConnected() {
        return isConnected;
    }

    public void startNetworkListener(Context context) {
        ConnectivityManager manager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (manager != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                manager.registerDefaultNetworkCallback(mNetCallback);
            } else {
                manager.registerNetworkCallback(mNetRequest, mNetCallback);
            }
            Log.d(TAG, "#startNetworkListener() success");
        } else {
            Log.d(TAG, "#startNetworkListener() failed");
        }
    }

    public void stopNetworkListener(Context context) {
        ConnectivityManager manager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (manager != null) {
            manager.unregisterNetworkCallback(mNetCallback);
            Log.d(TAG, "#stopNetworkListener() success");
        } else {
            Log.d(TAG, "#stopNetworkListener() failed");
        }
    }

    public void addObserver(LifecycleOwner owner, Observer<Boolean> observer) {
        mNetworkLive.observe(owner, observer);
    }

    public void removeObserver(Observer<Boolean> observer) {
        mNetworkLive.removeObserver(observer);
    }

    public void addCallback(ConnectionCallback callback) {
        if (callback != null) {
            mCallbacks.add(callback);
        }
    }

    public void removeCallback(ConnectionCallback callback) {
        if (callback != null && !mCallbacks.isEmpty()) {
            mCallbacks.remove(callback);
        }
    }

    public void clearCallbacks() {
        mCallbacks.clear();
    }

    public interface ConnectionCallback {
        void onChange(boolean netWorkState);
    }
}
