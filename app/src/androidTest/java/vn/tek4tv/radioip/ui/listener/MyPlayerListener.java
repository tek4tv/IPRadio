package vn.tek4tv.radioip.ui.listener;

import android.util.Log;

import org.videolan.libvlc.MediaPlayer;

import java.lang.ref.WeakReference;

import vn.tek4tv.radioip.ui.MainActivity;

public class MyPlayerListener implements MediaPlayer.EventListener {

    private static String TAG = "PlayerListener";
    private WeakReference<MainActivity> mOwner;
    private OnListennerEndVideo onListennerEndVideo;

    public MyPlayerListener(MainActivity owner, OnListennerEndVideo onListennerEndVideo) {
        mOwner = new WeakReference<MainActivity>(owner);
        this.onListennerEndVideo = onListennerEndVideo;
    }

    @Override
    public void onEvent(MediaPlayer.Event event) {
        MainActivity player = mOwner.get();

        switch(event.type) {
            case MediaPlayer.Event.EndReached:
                Log.d(TAG, "MediaPlayerEndReached");
                onListennerEndVideo.onEndAudio();
                break;
            case MediaPlayer.Event.Playing:
            case MediaPlayer.Event.Paused:
            case MediaPlayer.Event.Stopped:
            default:
                break;
        }
    }
}
