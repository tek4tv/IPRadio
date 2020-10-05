package vn.tek4tv.radioip.ui.listener;


import org.videolan.libvlc.MediaPlayer;

import java.lang.ref.WeakReference;

import vn.tek4tv.radioip.ui.MainActivity;

public class MyPlayerListener implements MediaPlayer.EventListener {
    private WeakReference<MainActivity> mOwner;
    private OnListennerEndVideo onListennerEndVideo;

    public MyPlayerListener(MainActivity owner, OnListennerEndVideo onListennerEndVideo) {
        mOwner = new WeakReference<>(owner);
        this.onListennerEndVideo = onListennerEndVideo;
    }

    @Override
    public void onEvent(MediaPlayer.Event event) {
        switch(event.type) {
            case MediaPlayer.Event.EndReached:
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
