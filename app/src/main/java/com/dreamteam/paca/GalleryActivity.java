package com.dreamteam.paca;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.util.LruCache;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ListView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;


public class GalleryActivity extends ActionBarActivity {
    public static final String TAG = GalleryActivity.class.getName();
    private static final int CAMERA_REQUEST = 1313;

    private final static String mGetPictureAddressesUri = "http://nthai.cs.trincoll.edu/PacaServer/retrieve.php";

    private RequestQueue mRequestQueue;
    private ImageLoader mImageLoader;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gallery_host);

        mRequestQueue = getRequestQueue();
        mImageLoader = new ImageLoader(mRequestQueue,
                new ImageLoader.ImageCache() {
                    private final LruCache<String, Bitmap>
                            cache = new LruCache<>(20);

                    @Override
                    public Bitmap getBitmap(String url) {
                        return cache.get(url);
                    }

                    @Override
                    public void putBitmap(String url, Bitmap bitmap) {
                        cache.put(url, bitmap);
                    }
                });

        JsonArrayRequest jsonArrayRequest = new JsonArrayRequest(Request.Method.GET, mGetPictureAddressesUri,
                new Response.Listener<JSONArray>() {
                    @Override
                    public void onResponse(JSONArray response) {
                        fetchImage(response);
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        new AlertDialog.Builder(GalleryActivity.this)
                                .setMessage("Fail to contact server")
                                .create()
                                .show();
                    }
        });
        jsonArrayRequest.setTag(TAG);
        mRequestQueue.add(jsonArrayRequest);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_gallery_host, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch (id) {
            case R.id.action_upload:
                new AlertDialog.Builder(this)
                        .setMessage("TODO: upload function not yet implemented")
                        .create()
                        .show();
                return true;
            case R.id.action_OpenCamera:
                Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                if(cameraIntent.resolveActivity(getPackageManager()) != null){
                    startActivityForResult(cameraIntent, CAMERA_REQUEST);
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mRequestQueue != null) {
            mRequestQueue.cancelAll(TAG);
        }
    }

    public RequestQueue getRequestQueue() {
        if (mRequestQueue == null) {
            mRequestQueue = Volley.newRequestQueue(this);
        }
        return mRequestQueue;
    }

    public ImageLoader getImageLoader() {
        return mImageLoader;
    }

    private void fetchImage(JSONArray array) {
        ArrayList<String> initialAddresses = new ArrayList<>();
        try {
            for (int i = 0; i < array.length(); i++) {
                initialAddresses.add(array.getString(i));
            }
        } catch (JSONException e) {
            Log.e(TAG, JSONException.class.getName(), e);
        }

        ListView imageStream = (ListView) findViewById(R.id.main_gallery);
        imageStream.setAdapter(new ImageAdapter(this, initialAddresses));
    }
}
