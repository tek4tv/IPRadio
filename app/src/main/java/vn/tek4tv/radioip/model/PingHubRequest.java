package vn.tek4tv.radioip.model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PingHubRequest {
    @SerializedName("ConnectionId")
    @Expose
    private String connectionId;
    @SerializedName("IMEI")
    @Expose
    private String imei;
    @SerializedName("StartTime")
    @Expose
    private String startTine;
    @SerializedName("Status")
    @Expose
    private String status;
    @SerializedName("Volume")
    @Expose
    private String volume;
    @SerializedName("Message")
    @Expose
    private String message;
    @SerializedName("Video")
    @Expose
    private String video;
}
