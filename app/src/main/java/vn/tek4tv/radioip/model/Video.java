package vn.tek4tv.radioip.model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class Video {
    @SerializedName("Index")
    @Expose
    private String index;
    @SerializedName("Time")
    @Expose
    private String time;

    public Video(String index, String time) {
        this.index = index;
        this.time = time;
    }
}
