package com.bluekeroro.android.photogallery;

import android.net.Uri;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by BlueKeroro on 2018/4/6.
 */
public class FlickrFetchr {
    private static final String TAG="FlickrFetchr";
    private static final String API_KEY="00697ba817a55305bfa698a2b54fa177";
    private static final String FETCH_RECENTS_METHOD="flickr.photos.getRecent";
    private static final String SEARCH_METHOD="flickr.photos.search";
    private static final Uri ENDPOINT= Uri.parse("https://api.flickr.com/services/rest/")
            .buildUpon()
            .appendQueryParameter("api_key",API_KEY)
            .appendQueryParameter("format","json")
            .appendQueryParameter("nojsoncallback","1")
            .appendQueryParameter("extras","url_s")
            .build();
    public byte[] getUrlBytes(String urlSpec) throws IOException {
        URL url=new URL(urlSpec);
        HttpURLConnection connection=(HttpURLConnection)url.openConnection();
        try {
            ByteArrayOutputStream out=new ByteArrayOutputStream();
            InputStream in=connection.getInputStream();
            if(connection.getResponseCode()!=HttpURLConnection.HTTP_OK){
                throw new IOException(connection.getResponseMessage()+": with"+urlSpec);
            }
            int bytesRead=0;
            byte[] buffer=new byte[1024];
            while((bytesRead=in.read(buffer))>0){
                out.write(buffer,0,bytesRead);
            }
            out.close();
            return out.toByteArray();
        }finally {
            connection.disconnect();
        }
    }
    public String getUrlString(String urlString)throws IOException{
        return new String(getUrlBytes(urlString));
    }
    private List<GalleryItem> downloadGalleryItems(String url){
        List<GalleryItem> items=new ArrayList<>();
        /*try {
           String jsonString= getUrlString(url);
           JSONObject jsonBody=new JSONObject(jsonString);
            parseItems(items,jsonBody);
            Log.i(TAG,"Received JSON: "+jsonString);
        }catch (IOException ioe){
            Log.e(TAG,"Failed to fetch items",ioe);
        }catch (JSONException ioe){
            Log.e(TAG,"Failed to parse JSON",ioe);
        }
        return items;*/
        //============use Gson===================
        try {
            String jsonString= getUrlString(url);
            parseItemsByGson(items,jsonString);
            Log.i(TAG,"Received JSON: "+jsonString);
        }catch (IOException ioe){
            Log.e(TAG,"Failed to fetch items",ioe);
        }
        return items;
    }
    private void parseItemsByGson(List<GalleryItem> items,String jsonString){
        Gson gson=new Gson();
        JsonObject jsonBody=gson.fromJson(jsonString,JsonObject.class);
        JsonObject photosJsonObject=jsonBody.getAsJsonObject("photos");
        JsonArray photoJsonArray=photosJsonObject.getAsJsonArray("photo");
        for(int i=0;i<photoJsonArray.size();i++){
            JsonElement photoJsonObject=photoJsonArray.get(i);
            GalleryItem item=gson.fromJson(photoJsonObject,GalleryItem.class);
            item.setParameter();
            items.add(item);
        }
    }
    private void parseItems(List<GalleryItem> items,JSONObject jsonBody)
            throws IOException,JSONException{
        JSONObject photosJsonObject=jsonBody.getJSONObject("photos");
        JSONArray photoJsonArray=photosJsonObject.getJSONArray("photo");
        for(int i=0;i<photoJsonArray.length();i++){
            JSONObject photoJsonObject=photoJsonArray.getJSONObject(i);
            GalleryItem item=new GalleryItem();
            item.setId(photoJsonObject.getString("id"));
            item.setCaption(photoJsonObject.getString("title"));
            if(!photoJsonObject.has("url_s")){
                continue;
            }
            item.setUrl(photoJsonObject.getString("url_s"));
            item.setOwner(photoJsonObject.getString("owner"));
            items.add(item);
        }
    }
    private String buildUri(String method,String query,int page){
        Uri.Builder uriBuilder=ENDPOINT.buildUpon()
                .appendQueryParameter("method",method);
        if(method.equals(SEARCH_METHOD)){
            uriBuilder.appendQueryParameter("text",query);
        }
        if(page!=0){
            uriBuilder.appendQueryParameter("page",String.valueOf(page));
        }
        return uriBuilder.build().toString();
    }
    public List<GalleryItem> fetchRecentPhotos(int page){
        String url=buildUri(FETCH_RECENTS_METHOD,null,page);
        return downloadGalleryItems(url);
    }
    public List<GalleryItem> searchPhotos(String query){
        String url=buildUri(SEARCH_METHOD,query,0);
        return downloadGalleryItems(url);
    }
}
