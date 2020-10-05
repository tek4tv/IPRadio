package vn.tek4tv.radioip.viewmodel;

import android.content.Context;
import android.util.Log;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;


import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import io.reactivex.SingleObserver;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import okhttp3.ResponseBody;
import vn.tek4tv.radioip.model.Playlist;
import vn.tek4tv.radioip.network.NetworkUtils;
import vn.tek4tv.radioip.network.RetrofitClient;
import vn.tek4tv.radioip.utils.Utils;

public class MainViewModel extends ViewModel {

    public MutableLiveData<List<Playlist>> lstLiveData = new MutableLiveData<>();

    public void getPlayList(Context context){
        Log.d("requestt",new Gson().toJson(new Request(Utils.getDeviceId(context))));
        RetrofitClient.getService(NetworkUtils.BASE_URL + "/", "").getPlaylist(new Request(Utils.getDeviceId(context)))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new SingleObserver<ResponseBody>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                    }

                    @Override
                    public void onSuccess(ResponseBody body) {
                        try{
                            Gson gson = new Gson();
                            String jsonOutput = body.string();
                            Type listType = new TypeToken<List<Playlist>>(){}.getType();
                            List<Playlist> posts = gson.fromJson(jsonOutput, listType);
                            lstLiveData.postValue(sortPlayList(posts));
                            Log.d("posts",new Gson().toJson(lstLiveData.getValue()) + "");
                        }catch (Exception e){
                            Log.d("Error", e.toString());
                        }
                    }
                    @Override
                    public void onError(Throwable e) {
                        e.printStackTrace();
                    }
                });
    }
    private List<Playlist> sortPlayList(List<Playlist> list){
        if(list == null){
            return null;
        }
        for(int i = 1 ; i < list.size() ; i++){
            Playlist playlist = list.get(i);
            String dateStr = list.get(i - 1).getStart();
            String duration = list.get(i - 1).getDuration();
            try{
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("HH:mm:ss");
                Date date = simpleDateFormat.parse(dateStr);
                Date dateduration = simpleDateFormat.parse(duration);
                Calendar rightduration  = Calendar.getInstance();
                rightduration.setTime(dateduration);
                Calendar rightNow  = Calendar.getInstance();
                rightNow.setTime(date);
                rightNow.add(Calendar.HOUR_OF_DAY, rightduration.get(Calendar.HOUR_OF_DAY));
                rightNow.add(Calendar.MINUTE, rightduration.get(Calendar.MINUTE));
                rightNow.add(Calendar.SECOND, rightduration.get(Calendar.SECOND));
                String starTime = (rightNow.get(Calendar.HOUR_OF_DAY) < 10 ? ("0" + rightNow.get(Calendar.HOUR_OF_DAY)) : rightNow.get(Calendar.HOUR_OF_DAY)) + ":" + (rightNow.get(Calendar.MINUTE) < 10 ? ("0" + rightNow.get(Calendar.MINUTE)) : rightNow.get(Calendar.MINUTE)) + ":" + (rightNow.get(Calendar.SECOND) < 10 ? ("0" + rightNow.get(Calendar.SECOND)) : rightNow.get(Calendar.SECOND));
                playlist.setStart(starTime);
            }catch (Exception e){
                e.printStackTrace();
            }
        }
        return list;
    }
}
