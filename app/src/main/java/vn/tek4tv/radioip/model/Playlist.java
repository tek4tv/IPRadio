package vn.tek4tv.radioip.model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

import lombok.Data;

@Data
public class Playlist implements Serializable {
    @SerializedName("Index")
    @Expose
    private String index;
    @SerializedName("Name")
    @Expose
    private String name;
    @SerializedName("ID")
    @Expose
    private Long id;
    @SerializedName("Duration")
    @Expose
    private String duration;
    @SerializedName("Path")
    @Expose
    private String path;
    @SerializedName("Start")
    @Expose
    private String start;
    @SerializedName("End")
    @Expose
    private String end;
    @SerializedName("Edit")
    @Expose
    private boolean edit;
    @SerializedName("Category")
    @Expose
    private Category category;

    private boolean isCheck = false;

}
