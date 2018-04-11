package com.bluekeroro.android.photogallery;

import android.net.Uri;

/**
 * Created by BlueKeroro on 2018/4/6.
 */
public class GalleryItem {
    private String mCaption;
    private String mId;
    private String mUrl;
    private String mOwner;
    private String title;
    private String id;
    private String url_s;
    private String owner;
    public void setParameter() {
        mCaption=title;
        mId=id;
        mUrl = url_s;
        mOwner=owner;
    }
    public String getOwner() {
        return mOwner;
    }

    public void setOwner(String owner) {
        mOwner = owner;
    }
    public Uri getPhotoPageUri(){
        return Uri.parse("http://www.flickr.com/photos/")
                .buildUpon()
                .appendPath(mOwner)
                .appendPath(mId)
                .build();
    }
    @Override
    public String toString() {
        return mCaption;
    }

    public String getCaption() {
        return mCaption;
    }

    public void setCaption(String caption) {
        mCaption = caption;
    }

    public String getId() {
        return mId;
    }

    public void setId(String id) {
        mId = id;
    }

    public String getUrl() {
        return mUrl;
    }

    public void setUrl(String url) {
        mUrl = url;
    }
}
