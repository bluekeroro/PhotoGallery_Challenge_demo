package com.bluekeroro.android.photogallery;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.v4.app.Fragment;

/**
 * Created by BlueKeroro on 2018/4/8.
 */
public class PhotoPageActivity extends SingleFragmentActivity {
    public static Intent newIntent(Context context, Uri photoPageUri){
        Intent i=new Intent(context,PhotoPageActivity.class);
        i.setData(photoPageUri);
        return i;
    }
    @Override
    protected Fragment createFragment() {
        return PhotoPageFragment.newInstance(getIntent().getData());
    }

    @Override
    public void onBackPressed() {
        if(PhotoPageFragment.mWebView.canGoBack()){
            PhotoPageFragment.mWebView.goBack();
        }else{
            super.onBackPressed();
        }
    }
}
