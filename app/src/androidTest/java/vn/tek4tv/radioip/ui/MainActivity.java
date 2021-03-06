package vn.tek4tv.radioip.ui;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.EditText;
import android.widget.Toast;

import com.google.gson.Gson;
import com.microsoft.signalr.HubConnection;
import com.microsoft.signalr.HubConnectionBuilder;

import org.videolan.libvlc.IVLCVout;
import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.Media;
import org.videolan.libvlc.MediaPlayer;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import vn.tek4tv.radioip.R;
import vn.tek4tv.radioip.model.PingHubRequest;
import vn.tek4tv.radioip.model.Playlist;
import vn.tek4tv.radioip.model.ReponseHub;
import vn.tek4tv.radioip.network.NetworkUtils;
import vn.tek4tv.radioip.signalR.HubConnectionTask;
import vn.tek4tv.radioip.ui.adapter.PlayListAdapter;
import vn.tek4tv.radioip.ui.listener.MyPlayerListener;
import vn.tek4tv.radioip.ui.listener.OnListennerEndVideo;
import vn.tek4tv.radioip.utils.ConfigUtil;
import vn.tek4tv.radioip.utils.Define;
import vn.tek4tv.radioip.utils.InternetConnection;
import vn.tek4tv.radioip.utils.MyExceptionHandler;
import vn.tek4tv.radioip.utils.Status;
import vn.tek4tv.radioip.utils.Utils;
import vn.tek4tv.radioip.viewmodel.MainViewModel;

public class MainActivity extends AppCompatActivity implements PlayListAdapter.OnChooseDevice, IVLCVout.Callback,NetworkUtils.ConnectionCallback{
    private MainViewModel mainViewModel;
    private HubConnection hubConnection;
    private Timer mTimer;
    private TimerTask trackingTask;
    private Timer mTimerWatchlog;
    private TimerTask trackingTaskWatchlog;

    private Gson gson = new Gson();
    // khoi tao view
    private RecyclerView rcvPlayList;
    private PlayListAdapter adapter;
    private String LOG_TAG = "MainActivity";

    // player
    // display surface
    private SurfaceView mSurface;
    private SurfaceHolder holder;

    // media player
    private LibVLC libvlc;
    private MediaPlayer mMediaPlayer = null;
    private int mVideoWidth;
    private int mVideoHeight;
    private final static int VideoSizeChanged = -1;

    // connect device
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final String UART_NAME = "/dev/ttyS4";
    private static final long WRITE_DELAY = 5000;
    private SerialPort serialPort;
    private InputStream inputStream;
    private OutputStream outputStream;
    private ReadThread readThread;
    private SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
    private SimpleDateFormat simpleDateFormat2 = new SimpleDateFormat("HH:mm:ss");
    private boolean isFMAM = false;
    private String volume;

    // define hen gio phat song
    private PendingIntent myPendingIntent;
    private AlarmManager alarmManager;
    private BroadcastReceiver myBroadcastReceiver;
    private Calendar firingCal;

    private MediaPlayer.EventListener mPlayerListener = new MyPlayerListener(this, new OnListennerEndVideo() {
        @Override
        public void onEndAudio() {
            i++;
            playURLVideo(mainViewModel.lstLiveData.getValue().get(i).getPath());
        }
    });
    private int i = 0;
    private boolean isPlayVODOrLive = false;

    // view
    private EditText edtReceive;
//    private WebView wv;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail_new);
        mSurface = (SurfaceView) findViewById(R.id.surface);
        holder = mSurface.getHolder();
        rcvPlayList = findViewById(R.id.webDetail);
        edtReceive = findViewById(R.id.edtReceive);
//        wv = findViewById(R.id.webLive);
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
            hubConnection = HubConnectionBuilder.create(NetworkUtils.URL_HUB).build();
            mainViewModel = new ViewModelProvider.AndroidViewModelFactory(getApplication()).create(MainViewModel.class);
            Log.d("vaoday","vaoday");
            mainViewModel.getPlayList(this);
            mainViewModel.lstLiveData.observe(this, this::createRecyclerView);
            new HubConnectionTask(connectionId -> {
                if (connectionId != null) {
                    process();
                }
            }).execute(hubConnection);
            onMessage();
        } else {
            Toast.makeText(this, "No internet", Toast.LENGTH_SHORT).show();
        }
    }


    // connect device
    private void connectTestDevice() {
        try {
            initSerialPort();
        } catch (IOException e) {
            //
        }
        readThread = new ReadThread();
        readThread.start();
//        handler.postDelayed(writeRunner, WRITE_DELAY);
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
        mMediaPlayer.setEventListener(mPlayerListener);

        // Set up video output
        final IVLCVout vout = mMediaPlayer.getVLCVout();
        vout.setVideoView(mSurface);
        vout.addCallback(this);
        vout.attachViews();
    }

    private void createRecyclerView(List<Playlist> devicesList) {
        i = 0;
        initAlarm(devicesList.get(i).getStart(), false);
        if (adapter != null) {
            adapter.setLstDevices(devicesList);
            adapter.notifyDataSetChanged();
        } else {
            adapter = new PlayListAdapter(this, devicesList, this);
            LinearLayoutManager layoutManagerJob = new LinearLayoutManager(this);
            rcvPlayList.setLayoutManager(layoutManagerJob);
            rcvPlayList.setItemAnimator(new DefaultItemAnimator());
            rcvPlayList.addItemDecoration(new DividerItemDecoration(this, layoutManagerJob.getOrientation()));
            rcvPlayList.setAdapter(adapter);
        }

    }

    private void initSerialPort() throws IOException {
        serialPort = new SerialPort(new File(UART_NAME), 9600, 0);
        inputStream = serialPort.getInputStream();
        outputStream = serialPort.getOutputStream();
        if (serialPort != null && inputStream != null && outputStream != null) {
            startCountDownWatchlog();
        }
    }

    private void process() {
        // send thong tin PING_HUB
        if (hubConnection.getConnectionId() != null) {
            startCountDown();
        } else {
            hubConnection = HubConnectionBuilder.create(NetworkUtils.URL_HUB).build();
        }

//        if(serialPort != null){
//            startCountDownWatchlog();
//        }else{
//            try{
//                serialPort = new SerialPort(new File(UART_NAME), 9600, 0);
//                inputStream = serialPort.getInputStream();
//                outputStream = serialPort.getOutputStream();
//                startCountDownWatchlog();
//            }catch (Exception e){
//            }
//        }
    }

    @Override
    protected void onPause() {
        super.onPause();
//        if (mTimer != null)
//            mTimer.cancel();
//        hubConnection.onClosed(exception -> Log.d("onClose", "onClose"));
//        hubConnection = null;
//        releasePlayer();
    }

    @Override
    protected void onStop() {
        super.onStop();
//        if (mTimer != null)
//            mTimer.cancel();
//        hubConnection = null;
//        releasePlayer();

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
        if (mTimer != null)
            mTimer.cancel();
        if (mTimerWatchlog != null)
            mTimerWatchlog.cancel();
        hubConnection = null;
        releasePlayer();
        unregisterReceiver(myBroadcastReceiver);
    }


    // ham receive du lieu
    private void onMessage() {
        hubConnection.on("ReceiveMessage", (message, message1) -> {
            runOnUiThread(() -> {
                Log.d("command", message);
                Log.d("message", message1);
                handleFromCommandServer(message, message1);
            });
        }, String.class, String.class);
    }

    // ham gui du lieu
    private void sendMessage(String mess, String command) {
        try {
            hubConnection.invoke("SendMessage", command, mess);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleFromCommandServer(String commamd, String message) {
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
                    callBackGetPlaylist();
                    break;
                case Status.UPDATE_LIST:
                    unregisterAlarmBroadcast();
                    mainViewModel.getPlayList(this);
                    mainViewModel.lstLiveData.observe(this, this::createRecyclerView);
                    callBackUpdateList(true);
                    break;
                case Status.NEXT:
                    i++;
                    playURLVideo(mainViewModel.lstLiveData.getValue().get(i).getPath());
                    callBackUpdateList(false);
                    break;
                case Status.PREVIEW:
                    i--;
                    playURLVideo(mainViewModel.lstLiveData.getValue().get(i).getPath());
                    callBackUpdateList(false);
                    break;
                case Status.JUMP:
                    edtReceive.setText(message);
                    i = Integer.parseInt(reponseHub.getMessage().trim()) - 1;
                    volume = reponseHub.getVolume();
                    playURLVideo(mainViewModel.lstLiveData.getValue().get(i).getPath());
                    callBackUpdateList(false);
                    break;
                case Status.LIVE:
                    // jump live
                    edtReceive.setText(message);
                    volume = reponseHub.getVolume();
                    playURLVideo(reponseHub.getMessage().trim());
                    callBackUpdateList(false);
                    break;
                case Status.UPDATE_STATUS:
                    pingHub(true);
                    break;
                case Status.SET_VOLUME:
                    try {
                        mMediaPlayer.setVolume((int) Double.parseDouble(reponseHub.getMessage()));
                        callBackUpdateList(false);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    break;
                case Status.STOP:
                    Log.d("stop", "stop");
                    mMediaPlayer.stop();
                    callBackUpdateList(false);
                    break;
                case Status.PAUSE:
                    mMediaPlayer.pause();
                    callBackUpdateList(false);
                    break;
                case Status.START:
                    mMediaPlayer.play();
                    callBackUpdateList(false);
                    break;
                case Status.RESTART:
                    break;
                case Status.SWITCH_MODE_FM:
                    edtReceive.setText(message);
                    isFMAM = true;
                    String messageHub = reponseHub.getMessage();
                    Log.d(TAG, "FM ==> " + messageHub);
                    if (messageHub != null && messageHub.split(",").length > 2) {
                        String[] status = messageHub.split(",");
                        if (status.length > 2) {
                            String mode = status[0];
                            String frequency = status[1];
                            volume = status[2];
                            if (mode.equals("fm")) {
                                //check case fm
                                writeToDevice(buildWriteMessage(Define.FM, frequency));
                            } else {
                                // am
                                writeToDevice(buildWriteMessage(Define.AM, frequency));
                            }
                        }
                    }
                    break;
                case Status.SET_MUTE_DEVICE:
                    edtReceive.setText(message);
                    // 0 de tat tieng, 1 de bat tieng
                    if (reponseHub.getMessage().equals("0")) {
                        writeToDevice(buildWriteMessage(Define.Mute, "0"));
                    } else {
                        writeToDevice(buildWriteMessage(Define.Mute, "1"));
                    }
                    break;
                case Status.SET_VOLUME_DEVICE:
                    edtReceive.setText(message);
                    setVolumeToDevice(reponseHub.getMessage());
                    break;
                case Status.GET_VOLUME_DEVICE:
                    edtReceive.setText(message);
                    writeToDevice(buildReadMessage(Define.VOLUME));
                    break;
                case Status.GET_SOURCE_AUDIO:
                    edtReceive.setText(message);
                    writeToDevice(buildReadMessage(Define.SOURCE_AUDIO));
                    break;
                case Status.GET_PA:
                    edtReceive.setText(message);
                    writeToDevice(buildReadMessage(Define.Power_Amplifier));
                    break;
                case Status.GET_FM_FQ:
                    edtReceive.setText(message);
                    writeToDevice(buildReadMessage(Define.FM));
                    break;
                case Status.GET_AM_FQ:
                    edtReceive.setText(message);
                    writeToDevice(buildReadMessage(Define.AM));
                    break;
                case Status.GET_TEMPERATURE:
                    edtReceive.setText(message);
                    writeToDevice(buildReadMessage(Define.Temparature));
                    break;
            }
        }

    }


    private void startCountDown() {
        if (mTimer != null)
            mTimer.cancel();
        mTimer = new Timer();
        trackingTask = new TrackingTimer();
        mTimer.schedule(trackingTask, 10, 15000);
    }

    private void startCountDownWatchlog() {
        if (mTimerWatchlog != null)
            mTimerWatchlog.cancel();
        mTimerWatchlog = new Timer();
        trackingTaskWatchlog = new TrackingTimerWatchlog();
        mTimerWatchlog.schedule(trackingTaskWatchlog, 10, 3000);
    }

    @Override
    public void onChooseDevice(Playlist loginDevice) {
        Log.d("player path", loginDevice.getPath());
//        playURLVideo(loginDevice.getPath());
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
//        if(netWorkState){
//            if(hubConnection == null){
//                hubConnection = HubConnectionBuilder.create(NetworkUtils.URL_HUB).build();
//                mainViewModel = new ViewModelProvider.AndroidViewModelFactory(getApplication()).create(MainViewModel.class);
//                mainViewModel.getPlayList(this);
//                mainViewModel.lstLiveData.observe(this, this::createRecyclerView);
//                new HubConnectionTask(connectionId -> {
//                    if (connectionId != null) {
//                        process();
//                    }
//                }).execute(hubConnection);
//                onMessage();
//            }
//        }
    }

    public class TrackingTimer extends TimerTask {
        @Override
        public void run() {
            pingHub(false);
        }
    }

    public class TrackingTimerWatchlog extends TimerTask {
        @Override
        public void run() {
            onWatchLog();
        }
    }


    private void callBackReadDevice(String funCallBack, String data) {
        if (!MainActivity.this.isFinishing()) {
            String date = simpleDateFormat.format(new Date());
            PingHubRequest request = PingHubRequest.builder().connectionId(hubConnection.getConnectionId()).imei(Utils.getDeviceId(this)).startTine(date).message(data).build();
            Log.d("request", new Gson().toJson(request));
            sendMessage(gson.toJson(request), funCallBack);
        }
    }
    private void pingHub(boolean isUpdate) {
        if (!MainActivity.this.isFinishing()) {
            // send ping_hub || update_status
            String date = simpleDateFormat.format(new Date());
            PingHubRequest request = null;
            if (isUpdate) {
                request = PingHubRequest.builder().connectionId(hubConnection.getConnectionId())
                        .imei(Utils.getDeviceId(this))
                        .status(mMediaPlayer.isPlaying() ? "START" : "STOP").startTine(date).volume(mMediaPlayer.getVolume() + "").build();
                Log.d("request", new Gson().toJson(request));
                sendMessage(new Gson().toJson(request), Utils.device_info);
            } else {
                request = PingHubRequest.builder().connectionId(hubConnection.getConnectionId()).imei(Utils.getDeviceId(this)).status(mMediaPlayer.isPlaying() ? "START" : "STOP").startTine(date).build();
                Log.d("request", new Gson().toJson(request));
                sendMessage(gson.toJson(request), Utils.ping_hub);
            }
        }
    }

    private void callBackUpdateList(boolean isStatus) {
        if (!MainActivity.this.isFinishing()) {
            // send ping_hub || update_status
            String date = simpleDateFormat.format(new Date());
            PingHubRequest request = PingHubRequest.builder().connectionId(hubConnection.getConnectionId()).imei(Utils.getDeviceId(this)).status(isStatus ? "1" : "0").startTine(date).build();
            Log.d("request", new Gson().toJson(request));
            sendMessage(gson.toJson(request), Utils.callback_hub);
        }
    }

    private void callBackGetPlaylist() {
        if (!MainActivity.this.isFinishing()) {
            if (mainViewModel.lstLiveData.getValue() != null) {
                sendMessage(gson.toJson(mainViewModel.lstLiveData.getValue()), Utils.current_playlist);
            }
        }
    }

    private void playURLVideo(String videoURL) {
        try {
//            if(videoURL.startsWith("rtsp")){
//                mMediaPlayer.stop();
//                wv.loadUrl("http://ovp.tek4tv.vn/rtsp?url=" + videoURL);
//            }else{
                mMediaPlayer.stop();
//                playOrPauseLive(false);
                Media m = new Media(libvlc, Uri.parse(videoURL));
                mMediaPlayer.setMedia(m);
                mMediaPlayer.play();
                mMediaPlayer.setVolume(100);
//            }
            isPlayVODOrLive = true;
            writeToDevice(buildWriteMessage(Define.SOURCE_AUDIO, "2"));
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

        mVideoWidth = 0;
        mVideoHeight = 0;
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
//                        Log.d(TAG, "data: " + data);
                        dataFinal.append(data);
                        if(dataFinal.toString().endsWith("\r\n")){
                            String s = dataFinal.toString();
//                            Log.d(TAG, "dataFinal.toString(): " + s);
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
                try {
                    String status = data.split(",")[1];
                    Log.d(TAG, "status: " + status);
                    String deviceAddress = data.split(",")[2];
                    Log.d(TAG, "deviceAddress: " + deviceAddress);
                    if (status.startsWith("128")) {
                        // write fail
                        processWriteCallBack(deviceAddress, false);
                    } else if (status.startsWith("129")) {
                    } else if (status.startsWith("130")) {
                        // write success
                        processWriteCallBack(deviceAddress, true);
                    } else if (status.startsWith("131")) {
                        try{
                            String dataRead = data.split(",")[3];
                            processReadCallBack(deviceAddress, dataRead);
                        }catch (Exception e){
                            e.printStackTrace();
                        }
                    }
                } catch (Exception e) {
                }
            }
        }
    }

    private void processReadCallBack(String deviceAdrress, String data){
        if(deviceAdrress.equals(Define.SOURCE_AUDIO_R)){
            callBackReadDevice(Utils.CURRENT_SOURCE, data);
        }
        if(deviceAdrress.equals(Define.Power_Amplifier_R)){
            callBackReadDevice(Utils.CURRENT_PA, data);
        }
        if(deviceAdrress.equals(Define.VOLUME_R)){
            callBackReadDevice(Utils.CURRENT_VOLUME, data);
        }
        if(deviceAdrress.equals(Define.FM_R)){
            callBackReadDevice(Utils.CURRENT_FM, data);
        }
        if(deviceAdrress.equals(Define.AM_R)){
            callBackReadDevice(Utils.CURRENT_AM, data);
        }
        if(deviceAdrress.equals(Define.Temparature_R)){
            callBackReadDevice(Utils.CURRENT_TEMPERATURE, data);
        }
    }

    private void processWriteCallBack(String deviceAddress, boolean isSuccess) {
        Log.d("isFMAMMMMMM", isFMAM + "");
        if (isFMAM) {
            if(isSuccess){
                Log.d("deviceAddress", deviceAddress);
                if (deviceAddress.equals(Define.FM_R) || deviceAddress.equals(Define.AM_R)) {
                    try {
                        Thread.sleep(Define.delay);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    writeToDevice(buildWriteMessage(Define.Mute, "0"));
                }
                if (deviceAddress.equals(Define.Mute_R)) {
                    try {
                        Thread.sleep(Define.delay);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    writeToDevice(buildWriteMessage(Define.SOURCE_AUDIO, "1"));
                }
                if (deviceAddress.equals(Define.SOURCE_AUDIO_R)) {
                    writeToDevice(buildWriteMessage(Define.Power_Amplifier, "1"));
                }
                if (deviceAddress.equals(Define.Power_Amplifier_R)) {
                    writeToDevice(buildWriteMessage(Define.VOLUME, volume));
                    isFMAM = false;
                }

//                if (deviceAddress.equals(Define.FM_R) || deviceAddress.equals(Define.AM_R)) {
//                    try {
//                        Thread.sleep(Define.delay);
//                    } catch (InterruptedException e) {
//                        e.printStackTrace();
//                    }
//                    writeToDevice(buildWriteMessage(Define.SOURCE_AUDIO, "1"));
//                }
//                if (deviceAddress.equals(Define.SOURCE_AUDIO_R)) {
//                    try {
//                        Thread.sleep(Define.delay);
//                    } catch (InterruptedException e) {
//                        e.printStackTrace();
//                    }
//                    writeToDevice(buildWriteMessage(Define.Power_Amplifier, "1"));
//                }
//                if (deviceAddress.equals(Define.Power_Amplifier_R)) {
//                    writeToDevice(buildWriteMessage(Define.VOLUME, volume));
//                }
//                if (deviceAddress.equals(Define.VOLUME_R)) {
//                    writeToDevice(buildWriteMessage(Define.Mute, "0"));
//                    isFMAM = false;
//                }

            }
        } else if (isPlayVODOrLive) {
            if(isSuccess){
                if (deviceAddress.equals(Define.SOURCE_AUDIO_R)) {
                    try {
                        Thread.sleep(Define.delay);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    if(volume != null && !volume.isEmpty() && !volume.equals("null")){
                        writeToDevice(buildWriteMessage(Define.VOLUME, volume));
                    }else{
                        volume = "60";
                        writeToDevice(buildWriteMessage(Define.VOLUME, volume));
                    }
                }
                if (deviceAddress.equals(Define.VOLUME_R)) {
                    try {
                        Thread.sleep(Define.delay);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
//                    writeToDevice(buildWriteMessage(Define.Mute, "0"));
//                    try {
//                        Thread.sleep(Define.delay);
//                    } catch (InterruptedException e) {
//                        e.printStackTrace();
//                    }
                    writeToDevice(buildWriteMessage(Define.Power_Amplifier, "1"));
                    isPlayVODOrLive = false;
                }
            }
        } else {
            if (deviceAddress.equals(Define.VOLUME_R)) {
                callBackUpdateList(isSuccess);
            }
        }


    }

    // build message write to device
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
                String mess = buildWriteMessage(Define.VOLUME, mes);
                Log.d(TAG, mess);
                outputStream.write(mess.getBytes());
            } catch (IOException e) {
            }
        }
    }

    private void onWatchLog() {
        if (outputStream != null) {
            try {
                String mes = buildWriteMessage(Define.Watchdog, "10");
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

    // init alarm
    private void initAlarm(String startTime , boolean isDefault){
        try{
            Date date = simpleDateFormat2.parse(startTime);
            Calendar rightduration  = Calendar.getInstance();
            rightduration.setTime(date);
            firingCal= Calendar.getInstance();
            if(isDefault){
                firingCal.add(Calendar.DATE, 1);
            }
            firingCal.set(Calendar.HOUR_OF_DAY, rightduration.get(Calendar.HOUR_OF_DAY));
            firingCal.set(Calendar.MINUTE, rightduration.get(Calendar.MINUTE));
            firingCal.set(Calendar.SECOND, rightduration.get(Calendar.SECOND));
            long intendedTime = firingCal.getTimeInMillis();

            registerMyAlarmBroadcast();
            alarmManager.set(AlarmManager.RTC_WAKEUP, intendedTime, myPendingIntent);
        }catch (Exception e){

        }

    }

    // register alar manager
    private void registerMyAlarmBroadcast()
    {
        Log.i(TAG, "Going to register Intent.RegisterAlramBroadcast");
        //This is the call back function(BroadcastReceiver) which will be call when your
        //alarm time will reached.
        myBroadcastReceiver = new BroadcastReceiver()
        {
            @Override
            public void onReceive(Context context, Intent intent)
            {
                Log.i(TAG,"BroadcastReceiver::OnReceive()" + mainViewModel.lstLiveData.getValue().get(i).getStart());



                if(i + 1 == mainViewModel.lstLiveData.getValue().size()){
                    mMediaPlayer.stop();
                    i = 0;
                    initAlarm(mainViewModel.lstLiveData.getValue().get(i).getStart(), true);
                }else{
                    i = i + 1;
                    initAlarm(mainViewModel.lstLiveData.getValue().get(i).getStart(), false);
                    playURLVideo(mainViewModel.lstLiveData.getValue().get(i).getPath());
                }
            }
        };
        registerReceiver(myBroadcastReceiver, new IntentFilter(ConfigUtil.ACTION_SCHEDULED) );
        myPendingIntent = PendingIntent.getBroadcast( this, 0, new Intent(ConfigUtil.ACTION_SCHEDULED),0 );
        alarmManager = (AlarmManager)(this.getSystemService( Context.ALARM_SERVICE ));
    }
    private void unregisterAlarmBroadcast()
    {
        alarmManager.cancel(myPendingIntent);
        getBaseContext().unregisterReceiver(myBroadcastReceiver);
    }

}
