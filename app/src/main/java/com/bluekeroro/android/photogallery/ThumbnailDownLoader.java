package com.bluekeroro.android.photogallery;
import android.content.pm.ProviderInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.support.v4.util.LruCache;
import android.util.Log;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.LogRecord;


/**
 * Created by BlueKeroro on 2018/4/6.
 */
public class ThumbnailDownLoader<T> extends HandlerThread {
    private static final String TAG="ThumbnailDownLoader";
    private Boolean mHasQuit=false;
    private static final int MESSAGE_DOWNLOAD=0;
    private static final int PRE_DOWNLOAD=1;
    private Handler mRequestHandler;
    private ConcurrentMap<T,String> mRequestMap=new ConcurrentHashMap<>();
    private ConcurrentMap<String,String> mPreDoladMap=new ConcurrentHashMap<>();
    private static final String PRE_LOAD_MAP_FLAG="pre_load_map_flag";
    private Handler mResponseHandler;
    private ThumbnailDownLoadListener<T> mThumbnailDownLoadListener;
    private LruCache<String,Bitmap> mLruCache;
    private List<String> mUrlStringList;
    public interface ThumbnailDownLoadListener<T>{
        void onThumbnailDownLoaded(T target,Bitmap bitmap);
    }
    public void setThumbnailDownLoadListener(ThumbnailDownLoadListener<T> listener) {
        mThumbnailDownLoadListener=listener;
    }

    public ThumbnailDownLoader (Handler responseHandler) {
        super(TAG);
        mResponseHandler=responseHandler;
        long maxMemory=Runtime.getRuntime().maxMemory();
        mLruCache=new LruCache<String,Bitmap>((int)(maxMemory/8)){
            @Override
            protected int sizeOf(String key, Bitmap value) {
                return value.getByteCount();
            }
        };
    }

    @Override
    public boolean quit() {
        mHasQuit=true;
        return super.quit();
    }
    public void queueThumbnail(T target,String url,List<String> urlStringList){
        Log.i(TAG,"Got a URL: "+url);
        mUrlStringList=urlStringList;
        if(url==null){
            mRequestMap.remove(target);
        }else{
            mRequestMap.put(target,url);
            mPreDoladMap.put(url,PRE_LOAD_MAP_FLAG);
            mRequestHandler.obtainMessage(MESSAGE_DOWNLOAD,target).sendToTarget();
        }
        if(mUrlStringList!=null){
            for(int i=0;i<mUrlStringList.size();i++){
                if(mUrlStringList.get(i)!=null&&mPreDoladMap.get(mUrlStringList.get(i))==null){
                    mPreDoladMap.put(mUrlStringList.get(i),PRE_LOAD_MAP_FLAG);
                    mRequestHandler.obtainMessage(PRE_DOWNLOAD,mUrlStringList.get(i)).sendToTarget();
                }
            }
        }
    }

    @Override
    protected void onLooperPrepared() {
        mRequestHandler=new Handler() {
            @Override
            public void handleMessage(Message msg) {
                if(msg.what==MESSAGE_DOWNLOAD){
                    T target=(T)msg.obj;
                    Log.i(TAG,"Got a request for URL: "+mRequestMap.get(target));
                    handleRequest(target);
                }else if(msg.what==PRE_DOWNLOAD){
                    String mUrl=(String)msg.obj;
                    handleRequestPre(mUrl);
                }
            }
        };
    }
    private void handleRequestPre(String url){
        try {
            if(url==null){
                return ;
            }
            if(mLruCache.get(url)==null){
                byte[] bitmapBytes=new FlickrFetchr().getUrlBytes(url);
                Bitmap bitmap= BitmapFactory.decodeByteArray(bitmapBytes,0,bitmapBytes.length);
                mLruCache.put(url,bitmap);
            }
        }catch (IOException ioe){
            Log.e(TAG,"Error downloading image",ioe);
        }
    }
    private void handleRequest(final T target){
        try {
            final String url=mRequestMap.get(target);
            if(url==null){
                return ;
            }
            byte[] bitmapBytes;
            final Bitmap bitmap;
            if(mLruCache.get(url)==null){
                bitmapBytes=new FlickrFetchr().getUrlBytes(url);
                bitmap= BitmapFactory.decodeByteArray(bitmapBytes,0,bitmapBytes.length);
                mLruCache.put(url,bitmap);
                Log.i(TAG,"Bitmap created");
            }else{
                bitmap=mLruCache.get(url);
            }
            mResponseHandler.post(new Runnable() {
                @Override
                public void run() {
                    if(mRequestMap.get(target)!=url||mHasQuit){
                        return ;
                    }
                    mRequestMap.remove(target);
                    mThumbnailDownLoadListener.onThumbnailDownLoaded(target,bitmap);
                }
            });
        }catch (IOException ioe){
            Log.e(TAG,"Error downloading image",ioe);
        }
    }
    public void clearQueue(){
        mRequestHandler.removeMessages(MESSAGE_DOWNLOAD);
    }
}
