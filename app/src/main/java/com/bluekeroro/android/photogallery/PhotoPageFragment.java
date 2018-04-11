package com.bluekeroro.android.photogallery;

import android.content.Intent;
import android.net.Uri;
import android.nfc.Tag;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;

/**
 * Created by BlueKeroro on 2018/4/8.
 */
public class PhotoPageFragment extends VisibleFragment {
    private static final String TAG="PhotoPageFragment";
    private static final String ARG_URI="photo_page_url";
    private Uri mUri;
    public static WebView mWebView;
    private ProgressBar mProgressBar;
    public static PhotoPageFragment newInstance(Uri uri){
        Bundle args=new Bundle();
        args.putParcelable(ARG_URI,uri);
        PhotoPageFragment fragment=new PhotoPageFragment();
        fragment.setArguments(args);
        return fragment;
    }
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mUri=getArguments().getParcelable(ARG_URI);
        setRetainInstance(true);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v=inflater.inflate(R.layout.fragment_photo_page,container,false);
        mWebView=(WebView)v.findViewById(R.id.fragment_photo_page_web_view);
        mProgressBar=(ProgressBar)v.findViewById(R.id.fragment_photo_page_progress_bar);
        mProgressBar.setMax(100);
        mWebView.getSettings().setJavaScriptEnabled(true);
        mWebView.setWebChromeClient(new WebChromeClient(){
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                //================================
                if(!view.getUrl().substring(0,4).equals("http")){
                    Intent i=new Intent(Intent.ACTION_VIEW,Uri.parse(view.getUrl()));
                    startActivity(i);
                    return ;
                }
                if(newProgress==100){
                    mProgressBar.setVisibility(View.GONE);
                }else{
                    mProgressBar.setVisibility(View.VISIBLE);
                    mProgressBar.setProgress(newProgress);
                }
            }
            @Override
            public void onReceivedTitle(WebView view, String title) {
                AppCompatActivity activity=(AppCompatActivity)getActivity();
                activity.getSupportActionBar().setSubtitle(title);
            }
        });
        mWebView.setWebViewClient(new WebViewClient(){
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return false;
            }
        });
        mWebView.loadUrl(mUri.toString());
        return v;
    }

}
