package com.bluekeroro.android.photogallery;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.util.LruCache;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by BlueKeroro on 2018/4/5.
 */
public class PhotoGalleryFragment extends VisibleFragment {
    private RecyclerView mPhotoRecyclerView;
    private TextView mTextView;
    private List<GalleryItem> mItems=new ArrayList<>();
    public static PhotoGalleryFragment newInstance(){
        return new PhotoGalleryFragment();
    }
    private static final String TAG="PhotoGalleryFragment";
    private static final String ITEMPOSITION="ItemPosition";
    private int page;
    private int position;
    private static final int JOB_ID=1;
    private ThumbnailDownLoader<PhotoHolder> mThumbnailDownLoader;
    private PhotoAdapter mPhotoAdapter;
    private boolean hasBeenScheduled;
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        setHasOptionsMenu(true);
        updateItem(0);
        Handler responseHandler=new Handler();
        mThumbnailDownLoader=new ThumbnailDownLoader<>(responseHandler);
        mThumbnailDownLoader.setThumbnailDownLoadListener(new
                ThumbnailDownLoader.ThumbnailDownLoadListener<PhotoHolder>(){
                    @Override
                    public void onThumbnailDownLoaded(PhotoHolder photoHolder, Bitmap bitmap) {
                        Drawable drawable=new BitmapDrawable(getResources(),bitmap);
                        photoHolder.bindGalleryDrawable(drawable);
                    }
                });
        mThumbnailDownLoader.start();
        mThumbnailDownLoader.getLooper();
        page=0;
        position=0;
        if(savedInstanceState!=null){
            position=savedInstanceState.getInt(ITEMPOSITION);
        }
        hasBeenScheduled=false;
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.LOLLIPOP){
            JobScheduler scheduler=(JobScheduler)getActivity()
                    .getSystemService(Context.JOB_SCHEDULER_SERVICE);
            for(JobInfo jobInfo:scheduler.getAllPendingJobs()){
                if(jobInfo.getId()==JOB_ID){
                    hasBeenScheduled=true;
                }
            }
        }
        Log.i(TAG,"Background thread started");
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        GridLayoutManager layoutManager=(GridLayoutManager)mPhotoRecyclerView.getLayoutManager();
        position=layoutManager.findFirstVisibleItemPosition();
        outState.putInt(ITEMPOSITION,position);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState){
        View v=inflater.inflate(R.layout.fragment_photo_gallery,container,false);
        mPhotoRecyclerView=(RecyclerView)v.findViewById(R.id.fragment_photo_recycler_view);
        mTextView=(TextView)v.findViewById(R.id.fragment_photo_loading);
        mTextView.setVisibility(View.INVISIBLE);
        ViewTreeObserver observer=mPhotoRecyclerView.getViewTreeObserver();
        observer.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                mPhotoRecyclerView.setLayoutManager(new GridLayoutManager(getActivity(),mPhotoRecyclerView.getWidth()/200));
                mPhotoRecyclerView.scrollToPosition(position);
                mPhotoRecyclerView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
        });
        setupAdapter();
        mPhotoRecyclerView.setOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
            }

            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                if(!recyclerView.canScrollVertically(1)&&QueryPreferences.getStoredQuery(getActivity())==null){
                    Log.i("onScrollStateChanged","onScrollStateChanged"+"bottom");
                    page++;
                    updateItem(page);
                }
            }
        });
        return v;
    }
    private class FetchItemsTask extends AsyncTask<Void,Void,List<GalleryItem>>{
        private String mQuery;
        private int mPage;
        public FetchItemsTask(String query,int page){
            mQuery=query;
            mPage=page;
        }
        @Override
        protected List<GalleryItem> doInBackground(Void... params) {
            if(mQuery==null){
                return new FlickrFetchr().fetchRecentPhotos(mPage);
            }else{
                return new FlickrFetchr().searchPhotos(mQuery);
            }
        }
        @Override
        protected void onPostExecute(List<GalleryItem> galleryItems) {
            if(mTextView!=null){
                mTextView.setVisibility(View.INVISIBLE);
            }
            if(page!=0){
                mItems.addAll(galleryItems);
                GridLayoutManager layoutManager=(GridLayoutManager)mPhotoRecyclerView.getLayoutManager();
                mPhotoAdapter.notifyItemRangeInserted(layoutManager.findLastVisibleItemPosition()+1,galleryItems.size());
                return ;
            }

            mItems=galleryItems;
            setupAdapter();
        }
    }
    private class PhotoHolder extends RecyclerView.ViewHolder implements View.OnClickListener{
        private ImageView mItemImageView;
        private GalleryItem mGalleryItem;
        public PhotoHolder(View itemView) {
            super(itemView);
            mItemImageView=(ImageView)itemView.findViewById(R.id.fragment_photo_gallery_image_view);
            mItemImageView.setOnClickListener(this);
        }
        public void bindGalleryDrawable(Drawable drawable){
            mItemImageView.setImageDrawable(drawable);

        }
        public void bindGalleryItem(GalleryItem galleryItem){
            mGalleryItem=galleryItem;
        }
        @Override
        public void onClick(View v) {
            //Intent i=new Intent(Intent.ACTION_VIEW,mGalleryItem.getPhotoPageUri());
            Intent i=PhotoPageActivity.newIntent(getActivity(),mGalleryItem.getPhotoPageUri());
            startActivity(i);
        }
    }
    private class PhotoAdapter extends RecyclerView.Adapter<PhotoHolder>{
        private List<GalleryItem> mGalleryItems;
        public PhotoAdapter(List<GalleryItem> galleryItems){
            mGalleryItems=galleryItems;
        }

        @Override
        public PhotoHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater inflater=LayoutInflater.from(getActivity());
            View view=inflater.inflate(R.layout.gallery_item,parent,false);
            return new PhotoHolder(view);
        }
        @Override
        public void onBindViewHolder(PhotoHolder holder, int position) {
            GalleryItem galleryItem=mGalleryItems.get(position);
            holder.bindGalleryItem(galleryItem);
            Drawable placeholder=getResources().getDrawable(R.drawable.bill_up_close);
            holder.bindGalleryDrawable(placeholder);
            List<String> mPreUrl=new ArrayList<>();
            for(int i=1;i<21;i++){
                if(position+i<mGalleryItems.size()){
                    mPreUrl.add(mGalleryItems.get(position+i).getUrl());
                }
                if(position-i>=0){
                    mPreUrl.add(mGalleryItems.get(position-i).getUrl());
                }
            }
            mThumbnailDownLoader.queueThumbnail(holder,galleryItem.getUrl(),mPreUrl);
        }
        @Override
        public int getItemCount() {
            return mGalleryItems.size();
        }
    }
    private void setupAdapter(){
        if(isAdded()){
            mPhotoRecyclerView.setAdapter(mPhotoAdapter=new PhotoAdapter(mItems));
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mThumbnailDownLoader.quit();
        Log.i(TAG,"Background thread destroyed");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mThumbnailDownLoader.clearQueue();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.fragment_photo_gallery,menu);
        MenuItem searchItem=menu.findItem(R.id.menu_item_search);
        final SearchView searchView=(SearchView)searchItem.getActionView();
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String s) {
                Log.d(TAG,"QueryTextSubmit: "+s);
                QueryPreferences.setStoredQuery(getActivity(),s);
                page=0;
                updateItem(0);
                searchView.onActionViewCollapsed();
                return true;
            }

            @Override
            public boolean onQueryTextChange(String s) {
                Log.d(TAG,"QueryTextChange: "+s);
                return false;
            }
        });
        searchView.setOnSearchClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String query=QueryPreferences.getStoredQuery(getActivity());
                searchView.setQuery(query,false);
            }
        });
        MenuItem toggleItem=menu.findItem(R.id.menu_item_toggle_polling);
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.LOLLIPOP){
            if(hasBeenScheduled){
                toggleItem.setTitle(R.string.stop_polling);
            }else{
                toggleItem.setTitle(R.string.start_polling);
            }
        }else if(PollService.isServiceAlarmOn(getActivity())){
            toggleItem.setTitle(R.string.stop_polling);
        }else{
            toggleItem.setTitle(R.string.start_polling);
        }
    }
    private void updateItem(int page){
        if(mTextView!=null){
            mTextView.setVisibility(View.VISIBLE);
        }
        String query=QueryPreferences.getStoredQuery(getActivity());
        new FetchItemsTask(query,page).execute();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()){
            case R.id.menu_item_clear:
                QueryPreferences.setStoredQuery(getActivity(),null);
                page=0;
                updateItem(0);
                return true;
            case R.id.menu_item_toggle_polling:
                if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.LOLLIPOP){
                    JobScheduler scheduler=(JobScheduler)getActivity()
                            .getSystemService(Context.JOB_SCHEDULER_SERVICE);
                    for(JobInfo jobInfo:scheduler.getAllPendingJobs()){
                        if(jobInfo.getId()==JOB_ID){
                            scheduler.cancel(JOB_ID);
                            hasBeenScheduled=false;
                            getActivity().invalidateOptionsMenu();
                            return true;
                        }
                    }
                    JobInfo jobInfo=new JobInfo.Builder(JOB_ID,new ComponentName(getActivity(),PollServiceByJobService.class))
                            .setRequiredNetworkType(JobInfo.NETWORK_TYPE_UNMETERED)
                            .setPeriodic(1000*60)
                            .setPersisted(true)
                            .build();
                    scheduler.schedule(jobInfo);
                    hasBeenScheduled=true;
                }else{
                    boolean shouldStarAlarm=!PollService.isServiceAlarmOn(getActivity());
                    PollService.setServiceAlarm(getActivity(),shouldStarAlarm);
                }
                getActivity().invalidateOptionsMenu();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
