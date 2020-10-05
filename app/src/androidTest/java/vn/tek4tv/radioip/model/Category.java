package vn.tek4tv.radioip.model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

import lombok.Data;

@Data
public class Category implements Serializable {
    @SerializedName("ID")
    @Expose
    private Long id;
    @SerializedName("Name")
    @Expose
    private String name;

}
