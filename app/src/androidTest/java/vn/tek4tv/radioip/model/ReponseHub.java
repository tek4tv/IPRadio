package vn.tek4tv.radioip.model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class ReponseHub implements Serializable {
    @SerializedName("IMEI")
    @Expose
    private String imei;
    @SerializedName("Message")
    @Expose
    private String message;
    @SerializedName("Volume")
    @Expose
    private String volume;

}
