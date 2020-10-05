package vn.tek4tv.radioip.network;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;
import timber.log.BuildConfig;

public class RetrofitClient {
    private static Retrofit retrofit = null;
    private static OkHttpClient okHttpClient = null;

    public static void setRetrofit(Retrofit retrofit) {
        RetrofitClient.retrofit = retrofit;
    }

    public static void setOkHttpClient(OkHttpClient okHttpClient) {
        RetrofitClient.okHttpClient = okHttpClient;
    }

    public static OkHttpClient getOkHttpClient(String token) {
        if (okHttpClient == null) {
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.level(HttpLoggingInterceptor.Level.BODY);
            if (BuildConfig.DEBUG) {
                okHttpClient = new OkHttpClient.Builder()
                        .connectTimeout(90000, TimeUnit.SECONDS)
                        .addInterceptor(new HydroBankInterceptor(token))
                        .addInterceptor(logging)
                        .readTimeout(90, TimeUnit.SECONDS)
                        .writeTimeout(90, TimeUnit.SECONDS)
                        .build();
            } else {
                okHttpClient = new OkHttpClient.Builder()
                        .connectTimeout(90000, TimeUnit.SECONDS)
                        .addInterceptor(new HydroBankInterceptor(token))
                        .readTimeout(90, TimeUnit.SECONDS)
                        .writeTimeout(90, TimeUnit.SECONDS)
                        .build();
            }

        }

        return okHttpClient;
    }

    public static ApiHelper getService(String baseUrl, String token) {
        if (retrofit == null) {
            retrofit = new Retrofit.Builder()
                    .baseUrl(baseUrl)
                    .client(getOkHttpClient(token))
                    .addConverterFactory(GsonConverterFactory.create())
                    .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                    .build();
        }
        return retrofit.create(ApiHelper.class);
    }

    private static class HydroBankInterceptor implements Interceptor {
        String token;

        public HydroBankInterceptor(String token) {
            this.token = token;
        }

        @Override
        public Response intercept(Chain chain) throws IOException {
            Request request = chain.request();
            Request authenticatedRequest = request.newBuilder()
                    .addHeader("Authorization", "Bearer " + token)
                    .build();
            return chain.proceed(authenticatedRequest);
        }
    }

//    public static class NetworkConnectionInterceptor implements Interceptor {
//        @NotNull
//        @Override
//        public Response intercept(@NotNull Chain chain) throws IOException {
//            if (!InternetConnection.checkConnection(BBTemplateApplication.getAppContext())) {
//                throw new NoConnectivityException();
//                // Throwing our custom exception 'NoConnectivityException'
//            }
//
//            Request.Builder builder = chain.request().newBuilder();
//            return chain.proceed(builder.build());
//        }
//    }

    public static class NoConnectivityException extends IOException {

        @Override
        public String getMessage() {
            return "No Internet Connection";
            // You can send any message whatever you want from here.
        }
    }
}
