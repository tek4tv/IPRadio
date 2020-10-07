package vn.tek4tv.radioip.model;

import com.google.gson.annotations.SerializedName;

public class Detail {
    @SerializedName("url")
    private String url;
    @SerializedName("urlwv")
    private String urlwv;
    @SerializedName("url_favorites")
    private String url_favorites;
    @SerializedName("urlwv_header")
    private String urlwv_header;
    @SerializedName("height")
    private int height;
    @SerializedName("isLive")
    private boolean isLive;

    @SerializedName("type")
    private int type;
    public Detail() {
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public boolean isLive() {
        return isLive;
    }

    public void setLive(boolean live) {
        isLive = live;
    }

    public String getUrlwv_header() {
        return urlwv_header;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public void setUrlwv_header(String urlwv_header) {
        this.urlwv_header = urlwv_header;
    }

    public String getUrl_favorites() {
        return url_favorites;
    }

    public void setUrl_favorites(String url_favorites) {
        this.url_favorites = url_favorites;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getUrlwv() {
        return urlwv;
    }

    public void setUrlwv(String urlwv) {
        this.urlwv = urlwv;
    }
}
