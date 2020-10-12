package vn.tek4tv.radioip.ui;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.JavascriptInterface;
import android.webkit.JsResult;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.EditText;
import android.widget.Toast;

import com.google.gson.Gson;

import org.videolan.libvlc.IVLCVout;
import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.Media;
import org.videolan.libvlc.MediaPlayer;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import vn.tek4tv.radioip.R;
import vn.tek4tv.radioip.model.ReponseHub;
import vn.tek4tv.radioip.network.NetworkUtils;
import vn.tek4tv.radioip.utils.Define;
import vn.tek4tv.radioip.utils.InternetConnection;
import vn.tek4tv.radioip.utils.MyExceptionHandler;
import vn.tek4tv.radioip.utils.Status;
import vn.tek4tv.radioip.utils.Utils;

public class MainActivity extends AppCompatActivity implements IVLCVout.Callback, NetworkUtils.ConnectionCallback {
    private Timer mTimerWatchlog;
    private TimerTask trackingTaskWatchlog;
    private Gson gson = new Gson();

    private String LOG_TAG = "MainActivity";
    // player
    private SurfaceView mSurface;
    private SurfaceHolder holder;
    // media player
    private LibVLC libvlc;
    private MediaPlayer mMediaPlayer = null;
    // connect device
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final String UART_NAME = "/dev/ttyS4";
    private SerialPort serialPort;
    private InputStream inputStream;
    private OutputStream outputStream;
    private ReadThread readThread;

    // view
    private WebView wv;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail_new);
        mSurface = findViewById(R.id.surface);
        wv = findViewById(R.id.webDetail);
        holder = mSurface.getHolder();
        Thread.setDefaultUncaughtExceptionHandler(new MyExceptionHandler(this));
        NetworkUtils.getInstance().startNetworkListener(this);
        // connect test device
        try {
            connectTestDevice();
        } catch (Exception e) {
            Log.d(TAG, "connect device fail" + e.toString());
        }
        // connect hub
        initPlayer();
        if (InternetConnection.checkConnection(this)) {
            setUpWV();
        } else {
            Toast.makeText(this, "No internet", Toast.LENGTH_SHORT).show();
            try {
                Thread.sleep(2000);

            } catch (Exception e) {

            }
        }
    }

    // load webview
    private void setUpWV() {
        wv.getSettings().setLoadsImagesAutomatically(true);
        wv.getSettings().setJavaScriptEnabled(true);
        wv.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
        wv.getSettings().setMediaPlaybackRequiresUserGesture(false);
        wv.setScrollbarFadingEnabled(true);
        wv.setScrollContainer(false);
        wv.setWebChromeClient(new MainViewClient());
        wv.addJavascriptInterface(new WebViewJavaScriptInterface(this), "MainActivity");
        wv.getSettings().setAppCacheEnabled(false);
        wv.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);
        wv.clearCache(true);
        if (Build.VERSION.SDK_INT >= 21) {
            CookieManager cookieManager = CookieManager.getInstance();
            cookieManager.setAcceptThirdPartyCookies(wv, true);
        }
        wv.loadUrl("https://iotdevice.tek4tv.vn/player?id=" + Utils.getDeviceId(this));
    }

    private class MainViewClient extends WebChromeClient {
        @Override
        public boolean onJsAlert(WebView view, String url, String message, JsResult result) {
            result.confirm();
            Log.d("TAG", "messageCategory" + message);
            if (message != null && !message.equals("")) {
                // handle alert
            }
            return true;
        }
    }

    public class WebViewJavaScriptInterface {

        private Context context;

        public WebViewJavaScriptInterface(Context context) {
            this.context = context;
        }


        @JavascriptInterface
        public void sendCommand(String cmd, String message) {
            Log.d("data from web", cmd + ":" + message);
            handleFromCommandServer(cmd, message);
        }

        @JavascriptInterface
        public void playURLVideoPosition(String url, float position, boolean isLive) {
            Log.d("position", String.valueOf(position));
//            playURLVideoPosition(url, position, isLive);
        }

        @JavascriptInterface
        public void switchToTuner(String messageHub, boolean ipMode) {
            // handle switchToTunner
        }

    }

    // connect device
    private void connectTestDevice() {
        try {
            initSerialPort();
        } catch (IOException e) {
        }
        readThread = new ReadThread();
        readThread.start();
    }

    private void initPlayer() {
        ArrayList<String> options = new ArrayList<String>();
        options.add("--aout=opensles");
        options.add("--audio-time-stretch"); // time stretching
        options.add("-vvv"); // verbosity
        options.add("--aout=opensles");
        options.add("--avcodec-codec=h264");
        options.add("--file-logging");
        options.add("--logfile=vlc-log.txt");


        libvlc = new LibVLC(getApplicationContext(), options);
        holder.setKeepScreenOn(true);

        // Create media player
        mMediaPlayer = new MediaPlayer(libvlc);
        mMediaPlayer.setEventListener(event -> {
        });
        // Set up video output
        final IVLCVout vout = mMediaPlayer.getVLCVout();
        vout.setVideoView(mSurface);
        vout.addCallback(this);
        vout.attachViews();
    }


    private void initSerialPort() throws IOException {
        serialPort = new SerialPort(new File(UART_NAME), 9600, 0);
        inputStream = serialPort.getInputStream();
        outputStream = serialPort.getOutputStream();
        if (serialPort != null && inputStream != null && outputStream != null) {
            startCountDownWatchlog();
        }
    }


    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (readThread != null) {
            readThread.interrupt();
            readThread = null;
        }
        if (serialPort != null) {
            serialPort.close();
        }
        if (mTimerWatchlog != null)
            mTimerWatchlog.cancel();
        releasePlayer();
    }


    private void handleFromCommandServer(String commamd, String message) {
        try {
            if (commamd == null || commamd.isEmpty()) {
                return;
            }
            if (message == null || message.isEmpty()) {
                return;
            }
            ReponseHub reponseHub = new ReponseHub();
            if (message.startsWith("{")) {
                reponseHub = gson.fromJson(message, ReponseHub.class);
            }
            boolean isImei = Utils.getDeviceId(this).equals(reponseHub.getImei()) ? true : false;
            if (isImei) {
                switch (commamd) {
                    case Status.GET_LIST:
                        // day lai danh sach playlist dang phat
                        break;
                    case Status.UPDATE_LIST:

                        break;
                    case Status.NEXT:

                        break;
                    case Status.PREVIEW:

                        break;
                    case Status.JUMP:
//                        edtReceive.setText(message);
//                        i = Integer.parseInt(reponseHub.getMessage().trim()) - 1;
//                        volume = reponseHub.getVolume();
//                        playURLVideo(mainViewModel.lstLiveData.getValue().get(i).getPath(), false);

                        break;
                    case Status.LIVE:
                        // jump live
//                        volume = reponseHub.getVolume();
//                        playURLVideo(reponseHub.getMessage().trim(), false);
                        break;
                    case Status.UPDATE_STATUS:
                        break;
                    case Status.GET_LOCATION:
                        if (!MainActivity.this.isFinishing()) {
//                            String date = simpleDateFormat.format(new Date());
//                            String strLocation = "";
//                            if (mlocation != null) {
//                                strLocation = mlocation.getLatitude() + "-" + mlocation.getLongitude();
//                            }
//                            PingHubRequest request = PingHubRequest.builder().connectionId(hubConnection.getConnectionId())
//                                    .imei(Utils.getDeviceId(this))
//                                    .status(mMediaPlayer.isPlaying() ? "START" : "STOP").startTine(date).message(strLocation).build();
//                            Log.d("requestlocation", new Gson().toJson(request));
//                            sendMessage(new Gson().toJson(request), Utils.DEVICE_LOCATION);

                        }
                        break;
                    case Status.SET_VOLUME:
                        mMediaPlayer.setVolume((int) Double.parseDouble(reponseHub.getMessage()));
                        break;
                    case Status.STOP:
                        if (mMediaPlayer.isPlaying()) {
                            mMediaPlayer.stop();
                        }
                        break;
                    case Status.PAUSE:
                        if (mMediaPlayer.isPlaying()) {
                            mMediaPlayer.pause();
                        }
                        break;
                    case Status.START:
                        mMediaPlayer.play();
                        break;
                    case Status.RESTART:
                        break;
                    case Status.SWITCH_MODE_FM:
                        String messageHub = reponseHub.getMessage();
                        Log.d(TAG, "FM ==> " + messageHub);
                        writeToDevice(buildWriteMessageNew(Define.FUNC_WRITE_PLAY_TUNNER_FORCE, reponseHub.getMessage()));
                        break;
                    case Status.SET_MUTE_DEVICE:
                        // 0 de tat tieng, 1 de bat tieng
                        writeToDevice(buildWriteMessageNew(Define.FUNC_WRITE_FORCE_SET_MUTE, reponseHub.getMessage()));
                        break;
                    case Status.SET_VOLUME_DEVICE:
                        setVolumeToDevice(reponseHub.getMessage());
                        break;
                    case Status.GET_VOLUME_DEVICE:
                        writeToDevice(buildReadMessage(Define.VOLUME));
                        break;
                    case Status.GET_SOURCE_AUDIO:
                        writeToDevice(buildReadMessage(Define.SOURCE_AUDIO));
                        break;
                    case Status.GET_PA:
                        writeToDevice(buildReadMessage(Define.Power_Amplifier));
                        break;
                    case Status.GET_FM_FQ:
                        writeToDevice(buildReadMessage(Define.FM));
                        break;
                    case Status.GET_AM_FQ:
                        writeToDevice(buildReadMessage(Define.AM));
                        break;
                    case Status.GET_TEMPERATURE:
                        writeToDevice(buildReadMessage(Define.Temparature));
                        break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private void startCountDownWatchlog() {
        if (mTimerWatchlog != null)
            mTimerWatchlog.cancel();
        mTimerWatchlog = new Timer();
        trackingTaskWatchlog = new TrackingTimerWatchlog();
        mTimerWatchlog.schedule(trackingTaskWatchlog, 10, 3000);
    }


    @Override
    public void onSurfacesCreated(IVLCVout vlcVout) {
    }

    @Override
    public void onSurfacesDestroyed(IVLCVout vlcVout) {
    }

    @Override
    public void onChange(boolean netWorkState) {
        // check network
        if (netWorkState) {
        }
    }


    public class TrackingTimerWatchlog extends TimerTask {
        @Override
        public void run() {
            onWatchDog();
        }
    }


    private void callBackReadDevice(String funCallBack, String data) {
        if (!MainActivity.this.isFinishing()) {
//            String date = simpleDateFormat.format(new Date());
//            PingHubRequest request = PingHubRequest.builder().connectionId(hubConnection.getConnectionId()).imei(Utils.getDeviceId(this)).startTine(date).message(data).build();
//            Log.d("request", new Gson().toJson(request));
//            sendMessage(gson.toJson(request), funCallBack);
        }
    }

    private void playURLVideo(String videoURL, boolean isRestart) {
        try {
            if (videoURL.startsWith("fm") || videoURL.startsWith("am")) {
                mMediaPlayer.stop();
                String messageHub = videoURL;
                Log.d(TAG, "FM ==> " + messageHub);
                //check case fm
                writeToDevice(buildWriteMessageNew(Define.FUNC_WRITE_PLAY_TUNNER, messageHub));
            } else if (videoURL.startsWith("rtsp") || videoURL.startsWith("http")) {
                mMediaPlayer.stop();
                Media m = new Media(libvlc, Uri.parse(videoURL));
                mMediaPlayer.setMedia(m);
                mMediaPlayer.play();
                mMediaPlayer.setVolume(100);
                String volume = "60";
                if (volume != null && !volume.isEmpty()) {
                    writeToDevice(buildWriteMessageNew(Define.FUNC_WRITE_PLAY_VOD_LIVE, volume));
                }
            }
        } catch (Exception e) {
            Log.e(LOG_TAG, "Error Play URL Video: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void releasePlayer() {
        if (libvlc == null)
            return;
        mMediaPlayer.stop();
        final IVLCVout vout = mMediaPlayer.getVLCVout();
        vout.removeCallback(this);
        vout.detachViews();
        holder = null;
        libvlc.release();
        libvlc = null;

    }

    StringBuffer dataFinal = new StringBuffer();

    private class ReadThread extends Thread {
        @Override
        public void run() {
            super.run();
            while (!isInterrupted()) {
                int size;
                try {
                    byte[] buffer = new byte[64];
                    if (inputStream == null) return;
                    size = inputStream.read(buffer);
                    if (size > 0) {
                        String data = new String(buffer, 0, size);
                        dataFinal.append(data);
                        if (dataFinal.toString().endsWith("\r\n")) {
                            String s = dataFinal.toString();
                            onDataReceived(s);
                            dataFinal = new StringBuffer();
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    return;
                }
            }
        }
    }

    private void onDataReceived(String data) {
        // read data
        Log.d(TAG, "dataFinal: " + data);
        if (data.endsWith("\r\n")) {
            if (data != null && !data.isEmpty() && data.startsWith("$$,")) {
//                try {
//                    String status = data.split(",")[1];
//                    Log.d(TAG, "status: " + status);
//                    String deviceAddress = data.split(",")[2];
//                    Log.d(TAG, "deviceAddress: " + deviceAddress);
//                    if (status.startsWith("128")) {
//                        // write fail
//                        processWriteCallBack(deviceAddress, false);
//                    } else if (status.startsWith("129")) {
//                    } else if (status.startsWith("130")) {
//                        // write success
//                        processWriteCallBack(deviceAddress, true);
//                    } else if (status.startsWith("131")) {
//                        try {
//                            String dataRead = data.split(",")[3];
//                            processReadCallBack(deviceAddress, dataRead);
//                        } catch (Exception e) {
//                            e.printStackTrace();
//                        }
//                    }
//                } catch (Exception e) {
//                }
            }
        }
    }

    private void processReadCallBack(String deviceAdrress, String data) {
        if (deviceAdrress.equals(Define.SOURCE_AUDIO_R)) {
            callBackReadDevice(Utils.CURRENT_SOURCE, data);
        }
        if (deviceAdrress.equals(Define.Power_Amplifier_R)) {
            callBackReadDevice(Utils.CURRENT_PA, data);
        }
        if (deviceAdrress.equals(Define.VOLUME_R)) {
            callBackReadDevice(Utils.CURRENT_VOLUME, data);
        }
        if (deviceAdrress.equals(Define.FM_R)) {
            callBackReadDevice(Utils.CURRENT_FM, data);
        }
        if (deviceAdrress.equals(Define.AM_R)) {
            callBackReadDevice(Utils.CURRENT_AM, data);
        }
        if (deviceAdrress.equals(Define.Temparature_R)) {
            callBackReadDevice(Utils.CURRENT_TEMPERATURE, data);
        }
    }


    // build message write to device
    private String buildWriteMessageNew(String funtionId, String data) {
        String s = "$$," + funtionId + "," + data + "," + "\r\n";
        Log.d("requestdevice", s);
        return s;
    }

    private String buildWriteMessage(String command, String data) {
        String s = "$$," + 128 + "," + command + "," + data + "," + "\r\n";
        Log.d("requestdevice", s);
        return s;
    }

    private String buildReadMessage(String command) {
        String s = "$$," + 129 + "," + command + "," + "\r\n";
        Log.d("requestdevice", s);
        return s;
    }

    // send to device
    private void setVolumeToDevice(String mes) {
        if (outputStream != null) {
            try {
                String mess = buildWriteMessage(Define.FUNC_WRITE_FORCE_SET_VOLUME, mes);
                Log.d(TAG, mess);
                outputStream.write(mess.getBytes());
            } catch (IOException e) {
            }
        }
    }

    private void onWatchDog() {
        if (outputStream != null) {
            try {
                String mes = buildWriteMessageNew(Define.FUNC_WRITE_WATCH_DOG, "1");
                outputStream.write(mes.getBytes());
            } catch (IOException e) {
            }
        }
    }

    private void writeToDevice(String message) {
        if (outputStream != null) {
            try {
                outputStream.write(message.getBytes());
            } catch (IOException e) {
            }
        }
    }


}
