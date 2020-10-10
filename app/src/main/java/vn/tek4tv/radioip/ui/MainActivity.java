package vn.tek4tv.radioip.ui;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
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
import java.util.Formatter;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

import vn.tek4tv.radioip.R;
import vn.tek4tv.radioip.model.PingHubRequest;
import vn.tek4tv.radioip.model.Playlist;
import vn.tek4tv.radioip.model.ReponseHub;
import vn.tek4tv.radioip.model.Video;
import vn.tek4tv.radioip.network.NetworkUtils;
import vn.tek4tv.radioip.signalR.HubConnectionTask;
import vn.tek4tv.radioip.ui.adapter.PlayListAdapter;
import vn.tek4tv.radioip.ui.listener.MyPlayerListener;
import vn.tek4tv.radioip.utils.ConfigUtil;
import vn.tek4tv.radioip.utils.Define;
import vn.tek4tv.radioip.utils.InternetConnection;
import vn.tek4tv.radioip.utils.MyExceptionHandler;
import vn.tek4tv.radioip.utils.Status;
import vn.tek4tv.radioip.utils.Utils;
import vn.tek4tv.radioip.viewmodel.MainViewModel;

public class MainActivity extends AppCompatActivity implements PlayListAdapter.OnChooseDevice, IVLCVout.Callback, NetworkUtils.ConnectionCallback {
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
    private SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
    private SimpleDateFormat simpleDateFormat2 = new SimpleDateFormat("HH:mm:ss");
    private boolean isFMAM = false;
    private String volume;

    // define hen gio phat song
    private PendingIntent myPendingIntent;
    private AlarmManager alarmManager;
    private BroadcastReceiver myBroadcastReceiver;
    private Calendar firingCal;

    private MediaPlayer.EventListener mPlayerListener = new MyPlayerListener(this, () -> {
//            i++;
//            playURLVideo(mainViewModel.lstLiveData.getValue().get(i).getPath());
    });
    private int i = 0;
    private boolean isPlayVODOrLive = false;
    private int count_ping_hub = 0;
    // view
    private EditText edtReceive;

    // get location
    private final int PERMISSION_ID = 44;
    private Location mlocation;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail_new);
        mSurface = (SurfaceView) findViewById(R.id.surface);
        holder = mSurface.getHolder();
        rcvPlayList = findViewById(R.id.webDetail);
        edtReceive = findViewById(R.id.edtReceive);
        mainViewModel = new ViewModelProvider.AndroidViewModelFactory(getApplication()).create(MainViewModel.class);
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
            Log.d("vaoday", "vaoday");
            mainViewModel.getPlayList(this);
            mainViewModel.lstLiveData.observe(this, this::createRecyclerView);
            try {
                new HubConnectionTask(connectionId -> {
                    if (connectionId != null) {
                        process();
                    }
                }).execute(hubConnection);
                onMessage();
                // Subscribe to the closed event
                hubConnection.onClosed(exception -> {
                    hubConnection.start();
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            Toast.makeText(this, "No internet", Toast.LENGTH_SHORT).show();
            try {
                Thread.sleep(2000);
                if (hubConnection == null) {
                    connectHub();
                }
            } catch (Exception e) {

            }
        }
        checkLocation();
    }

    private void connectHub() {
        hubConnection = HubConnectionBuilder.create(NetworkUtils.URL_HUB).build();
        Log.d("vaoday", "vaoday");
        try {
            if (mainViewModel.lstLiveData == null) {
                mainViewModel.getPlayList(this);
                mainViewModel.lstLiveData.observe(this, this::createRecyclerView);
            }
            new HubConnectionTask(connectionId -> {
                if (connectionId != null) {
                    process();
                }
            }).execute(hubConnection);
            onMessage();
        } catch (Exception e) {
            e.printStackTrace();
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
        mMediaPlayer.setEventListener(mPlayerListener);

        // Set up video output
        final IVLCVout vout = mMediaPlayer.getVLCVout();
        vout.setVideoView(mSurface);
        vout.addCallback(this);
        vout.attachViews();
    }

    private void createRecyclerView(List<Playlist> devicesList) {
        i = Utils.getCurrentPosition(devicesList);
        Log.d("vao day 0", "vao day 0" + i);

        try {
            mMediaPlayer.stop();
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (i == -1) {
            i = 0;
            initAlarm(mainViewModel.lstLiveData.getValue().get(i).getStart(), true);
        } else if (i == -2) {
            i = 0;
            initAlarm(mainViewModel.lstLiveData.getValue().get(i).getStart(), false);
        } else {
            playURLVideo(devicesList.get(i).getPath(), true);
            if (i + 1 == mainViewModel.lstLiveData.getValue().size()) {
                i = 0;
                initAlarm(mainViewModel.lstLiveData.getValue().get(i).getStart(), true);
            } else {
                i = i + 1;
                initAlarm(mainViewModel.lstLiveData.getValue().get(i).getStart(), false);
            }
        }
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
        if (mTimer != null)
            mTimer.cancel();
        if (mTimerWatchlog != null)
            mTimerWatchlog.cancel();
        hubConnection = null;
        releasePlayer();
        if (myBroadcastReceiver != null) {
            unregisterReceiver(myBroadcastReceiver);
        }
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
        try {
            if (commamd == null || commamd.isEmpty()) {
                return;
            }
            if (message == null || message.isEmpty()) {
                return;
            }
            count_ping_hub = 0;
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
                        mainViewModel.getPlayList(this);
                        mainViewModel.lstLiveData.observe(this, this::createRecyclerView);
                        callBackUpdateList(true);
                        break;
                    case Status.NEXT:
                        i++;
                        playURLVideo(mainViewModel.lstLiveData.getValue().get(i).getPath(), false);
                        callBackUpdateList(false);
                        break;
                    case Status.PREVIEW:
                        i--;
                        playURLVideo(mainViewModel.lstLiveData.getValue().get(i).getPath(), false);
                        callBackUpdateList(false);
                        break;
                    case Status.JUMP:
                        edtReceive.setText(message);
                        i = Integer.parseInt(reponseHub.getMessage().trim()) - 1;
                        volume = reponseHub.getVolume();
                        playURLVideo(mainViewModel.lstLiveData.getValue().get(i).getPath(), false);
                        callBackUpdateList(false);
                        break;
                    case Status.LIVE:
                        // jump live
                        edtReceive.setText(message);
                        volume = reponseHub.getVolume();
                        playURLVideo(reponseHub.getMessage().trim(), false);
                        callBackUpdateList(false);
                        break;
                    case Status.UPDATE_STATUS:
                        pingHub(true);
                        break;
                    case Status.GET_LOCATION:
                        if (!MainActivity.this.isFinishing()) {
                            String date = simpleDateFormat.format(new Date());
                            String strLocation = "";
                            if (mlocation != null) {
                                strLocation = mlocation.getLatitude() + "-" + mlocation.getLongitude();
                            }
                            PingHubRequest request = PingHubRequest.builder().connectionId(hubConnection.getConnectionId())
                                    .imei(Utils.getDeviceId(this))
                                    .status(mMediaPlayer.isPlaying() ? "START" : "STOP").startTine(date).message(strLocation).build();
                            Log.d("requestlocation", new Gson().toJson(request));
                            sendMessage(new Gson().toJson(request), Utils.DEVICE_LOCATION);

                        }
                        break;
                    case Status.SET_VOLUME:
                        mMediaPlayer.setVolume((int) Double.parseDouble(reponseHub.getMessage()));
                        break;
                    case Status.STOP:
                        if (mMediaPlayer.isPlaying()) {
                            mMediaPlayer.stop();
                        }
                        callBackUpdateList(false);
                        break;
                    case Status.PAUSE:
                        if (mMediaPlayer.isPlaying()) {
                            mMediaPlayer.pause();
                        }
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
        } catch (Exception e) {
            e.printStackTrace();
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
        // on test
        playURLVideo(loginDevice.getPath(), false);
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
            if (hubConnection == null) {
                hubConnection = HubConnectionBuilder.create(NetworkUtils.URL_HUB).build();
                mainViewModel = new ViewModelProvider.AndroidViewModelFactory(getApplication()).create(MainViewModel.class);
                mainViewModel.getPlayList(this);
                mainViewModel.lstLiveData.observe(this, this::createRecyclerView);
                new HubConnectionTask(connectionId -> {
                    if (connectionId != null) {
                        process();
                    }
                }).execute(hubConnection);
                onMessage();
            }
            try {
                if (mainViewModel.lstLiveData == null) {
                    mainViewModel.getPlayList(this);
                    mainViewModel.lstLiveData.observe(this, this::createRecyclerView);
                } else {
                    i = Utils.getCurrentPosition(mainViewModel.lstLiveData.getValue());
                    Log.d("vao day 1", "vao day 1" + i);
                    if (i == -1) {
                        i = 0;
                        initAlarm(mainViewModel.lstLiveData.getValue().get(i).getStart(), true);
                    } else if (i == -2) {
                        i = 0;
                        initAlarm(mainViewModel.lstLiveData.getValue().get(i).getStart(), false);
                    } else {
                        playURLVideo(mainViewModel.lstLiveData.getValue().get(i).getPath(), true);
                        if (i + 1 == mainViewModel.lstLiveData.getValue().size()) {
                            i = 0;
                            initAlarm(mainViewModel.lstLiveData.getValue().get(i).getStart(), true);
                        } else {
                            i = i + 1;
                            initAlarm(mainViewModel.lstLiveData.getValue().get(i).getStart(), false);
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
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
            onWatchDog();
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
        try {
            if (!isUpdate) {
                if (count_ping_hub > 1) {
                    if (hubConnection == null) {
                        connectHub();
                    } else {
                        hubConnection.start();
                    }
                }
            }

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
                    Video video = null;
                    i = Utils.getCurrentPosition(mainViewModel.lstLiveData.getValue());
                    if(i >= 0 && i < mainViewModel.lstLiveData.getValue().size()){
                        if (mainViewModel.lstLiveData.getValue().get(i).getPath().startsWith("http")) {
                            if (mMediaPlayer != null) {
                                long time = mMediaPlayer.getLength() - Utils.getTimeBettween(mainViewModel.lstLiveData.getValue().get(i).getStart(), Utils.getTimeCurrent());
                                video = new Video("" + i, "" + time);
                            }
                            request = PingHubRequest.builder().connectionId(hubConnection.getConnectionId()).imei(Utils.getDeviceId(this)).
                                    status(mMediaPlayer.isPlaying() ? "START" : "STOP").startTine(date).video(gson.toJson(video)).build();
                        } else {
                            video = new Video("" + i, "-1");
                            request = PingHubRequest.builder().connectionId(hubConnection.getConnectionId()).imei(Utils.getDeviceId(this)).
                                    status(mMediaPlayer.isPlaying() ? "START" : "STOP").startTine(date).video(gson.toJson(video)).build();
                        }
                    }else{
                        video = new Video("" + i, "-1");
                        request = PingHubRequest.builder().connectionId(hubConnection.getConnectionId()).imei(Utils.getDeviceId(this)).
                                status(mMediaPlayer.isPlaying() ? "START" : "STOP").startTine(date).video(gson.toJson(video)).build();
                    }

                    Log.d("request", new Gson().toJson(request));
                    count_ping_hub = count_ping_hub + 1;
                    sendMessage(gson.toJson(request), Utils.ping_hub);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
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

    private void playURLVideo(String videoURL, boolean isRestart) {
        try {
            try {
                for (int j = 0; j < mainViewModel.lstLiveData.getValue().size(); j++) {
                    mainViewModel.lstLiveData.getValue().get(j).setCheck(false);
                }
                for (int k = 0; k < mainViewModel.lstLiveData.getValue().size(); k++) {
                    if (mainViewModel.lstLiveData.getValue().get(k).getPath().equals(videoURL)) {
                        mainViewModel.lstLiveData.getValue().get(k).setCheck(true);
                        break;
                    }
                }
                if(adapter != null){
                    adapter.notifyDataSetChanged();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (videoURL.startsWith("fm") || videoURL.startsWith("am")) {
                mMediaPlayer.stop();
                isFMAM = true;
                String messageHub = videoURL;
                Log.d(TAG, "FM ==> " + messageHub);
                //check case fm
                writeToDevice(buildWriteMessageNew(Define.FUNC_WRITE_PLAY_TUNNER, messageHub));
            } else if (videoURL.startsWith("rtsp") || videoURL.startsWith("http")) {
                mMediaPlayer.stop();
                Media m = new Media(libvlc, Uri.parse(videoURL));
                mMediaPlayer.setMedia(m);
                mMediaPlayer.play();
                if (isRestart) {
                    mMediaPlayer.setTime(Utils.getTimeBettween(mainViewModel.lstLiveData.getValue().get(i).getStart(), Utils.getTimeCurrent()));
                }
                mMediaPlayer.setVolume(100);
                isPlayVODOrLive = true;
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

    private void processWriteCallBack(String deviceAddress, boolean isSuccess) {
        if (isFMAM) {
            if (isSuccess) {
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
            }
        } else if (isPlayVODOrLive) {
            if (isSuccess) {
                if (deviceAddress.equals(Define.SOURCE_AUDIO_R)) {
                    try {
                        Thread.sleep(Define.delay);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    if (volume != null && !volume.isEmpty() && !volume.equals("null")) {
                        writeToDevice(buildWriteMessage(Define.VOLUME, volume));
                    } else {
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
                String mes = buildWriteMessageNew(Define.FUNC_WRITE_WATCH_DOG, "10");
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
    private void initAlarm(String startTime, boolean isDefault) {
        try {
            Date date = simpleDateFormat2.parse(startTime);
            Calendar rightduration = Calendar.getInstance();
            rightduration.setTime(date);
            firingCal = Calendar.getInstance();
            if (isDefault) {
                firingCal.add(Calendar.DATE, 1);
            }
            firingCal.set(Calendar.HOUR_OF_DAY, rightduration.get(Calendar.HOUR_OF_DAY));
            firingCal.set(Calendar.MINUTE, rightduration.get(Calendar.MINUTE));
            firingCal.set(Calendar.SECOND, rightduration.get(Calendar.SECOND));
            long intendedTime = firingCal.getTimeInMillis();

            registerMyAlarmBroadcast();
            alarmManager.set(AlarmManager.RTC_WAKEUP, intendedTime, myPendingIntent);
        } catch (Exception e) {
        }
    }

    // register alar manager
    private void registerMyAlarmBroadcast() {
        myBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.i(TAG, "BroadcastReceiver::OnReceive()" + mainViewModel.lstLiveData.getValue().get(i).getStart());
                i = Utils.getCurrentPosition(mainViewModel.lstLiveData.getValue());
                mMediaPlayer.stop();
                if (i == -1) {
                    i = 0;
                    initAlarm(mainViewModel.lstLiveData.getValue().get(i).getStart(), true);
                } else if (i == -2) {
                    i = 0;
                    initAlarm(mainViewModel.lstLiveData.getValue().get(i).getStart(), false);
                } else {
                    playURLVideo(mainViewModel.lstLiveData.getValue().get(i).getPath(), false);
                    if (i + 1 == mainViewModel.lstLiveData.getValue().size()) {
                        i = 0;
                        initAlarm(mainViewModel.lstLiveData.getValue().get(i).getStart(), true);
                    } else {
                        i = i + 1;
                        initAlarm(mainViewModel.lstLiveData.getValue().get(i).getStart(), false);
                    }
                }
            }
        };
        registerReceiver(myBroadcastReceiver, new IntentFilter(ConfigUtil.ACTION_SCHEDULED));
        myPendingIntent = PendingIntent.getBroadcast(this, 0, new Intent(ConfigUtil.ACTION_SCHEDULED), 0);
        alarmManager = (AlarmManager) (this.getSystemService(Context.ALARM_SERVICE));
    }

    private void unregisterAlarmBroadcast() {
        alarmManager.cancel(myPendingIntent);
        getBaseContext().unregisterReceiver(myBroadcastReceiver);
    }

    private void setPosVideo() {
        mMediaPlayer.setPosition(mMediaPlayer.getPosition() / mMediaPlayer.getLength());
    }

    private boolean checkPermissions() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            return true;
        }
        return false;
    }

    private void requestPermissions() {
        ActivityCompat.requestPermissions(
                this,
                new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.READ_CONTACTS},
                PERMISSION_ID
        );
    }

    private void checkLocation() {
        if (checkPermissions()) {
            getLocation();
        } else {
            requestPermissions();
        }
    }

    private void getLocation() {
        LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        LocationListener locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                mlocation = location;
            }

            @Override
            public void onStatusChanged(String s, int i, Bundle bundle) {
            }

            @Override
            public void onProviderEnabled(String s) {
            }

            @Override
            public void onProviderDisabled(String s) {
            }
        };
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 100, 100, locationListener);
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 100, 100, locationListener);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case PERMISSION_ID:
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    getLocation();
                }
                break;
        }

    }


}
