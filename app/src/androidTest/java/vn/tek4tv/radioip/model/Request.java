package vn.tek4tv.radioip.model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class Request {
    @SerializedName("IMEI")
    @Expose
    private String imei;

    public Request(String imei) {
        this.imei = imei;
    }
}
