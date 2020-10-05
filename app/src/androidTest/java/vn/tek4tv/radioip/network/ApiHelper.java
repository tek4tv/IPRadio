package vn.tek4tv.radioip.network;


import io.reactivex.Single;
import okhttp3.ResponseBody;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface ApiHelper {
    @POST("api/device/imei")
    Single<ResponseBody> getPlaylist(@Body Request request);
}
