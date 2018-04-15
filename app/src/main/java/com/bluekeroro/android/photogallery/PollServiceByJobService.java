package com.bluekeroro.android.photogallery;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.Intent;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Build;
import android.util.Log;

import java.util.List;

/**
 * Created by BlueKeroro on 2018/4/10.
 */
@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class PollServiceByJobService extends JobService {
    private PollTask mCurrentTask;
    public static final String ACTION_SHOW_NOTIFICATION=
            "com.bluekeroro.android.photogallery.SHOW_NOTIFICATION";
    public static final String PERM_PRIVATE=
            "com.bluekeroro.android.photogallery.PRIVATE";
    public static final String REQUEST_CODE="REQUEST_CODE";
    public static final String NOTIFICATION="NOTIFICATION";
    private static final String TAG="PollServiceByJobService";

    @Override
    public boolean onStartJob(JobParameters params) {
        mCurrentTask=new PollTask();
        mCurrentTask.execute(params);
        return false;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        if(mCurrentTask!=null){
            mCurrentTask.cancel(true);
        }
        return true;
    }
    private class PollTask extends AsyncTask<JobParameters,Void,Void>{
        @Override
        protected Void doInBackground(JobParameters... params) {
            JobParameters jobParams=params[0];
            String query=QueryPreferences.getStoredQuery(PollServiceByJobService.this);
            String lastResultId=QueryPreferences.getLastResultId(PollServiceByJobService.this);
            List<GalleryItem> items;
            if(query==null){
                items=new FlickrFetchr().fetchRecentPhotos(0);
            }else{
                items=new FlickrFetchr().searchPhotos(query);
            }
            if(items.size()==0){
                return null;
            }
            String resultId=items.get(0).getId();
            if(resultId.equals(lastResultId)){
                Log.i(TAG,"Got an old result: "+resultId);
            }else{
                Log.i(TAG,"Got a new result: "+resultId);
                Resources resources=getResources();
                Intent i=PhotoGalleryActivity.newIntent(PollServiceByJobService.this);
                PendingIntent pi=PendingIntent.getActivity(PollServiceByJobService.this,0,i,0);
                Notification notification=new Notification.Builder(PollServiceByJobService.this)
                        .setTicker(resources.getString(R.string.new_pictures_title))
                        .setSmallIcon(android.R.drawable.ic_menu_report_image)
                        .setContentTitle(resources.getString(R.string.new_pictures_title))
                        .setContentText(resources.getString(R.string.new_pictures_text))
                        .setContentIntent(pi)
                        .setAutoCancel(true)
                        .build();
                showBackgroundNotification(0,notification);
            }
            QueryPreferences.setLastResultId(PollServiceByJobService.this,resultId);
            jobFinished(jobParams,false);
            return null;
        }
    }

    private void showBackgroundNotification(int requestCode,Notification notification){
        Intent i=new Intent(ACTION_SHOW_NOTIFICATION);
        i.putExtra(REQUEST_CODE,requestCode);
        i.putExtra(NOTIFICATION,notification);
        sendOrderedBroadcast(i,PERM_PRIVATE,null,null, Activity.RESULT_OK,null,null);
    }
}

