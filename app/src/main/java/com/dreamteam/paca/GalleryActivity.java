package com.dreamteam.paca;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.Settings;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ListView;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;


public class GalleryActivity extends ActionBarActivity implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {
    public static final String TAG = GalleryActivity.class.getName();
    private static final int CAMERA_REQUEST = 1313;

    private static final String GET_PICTURE_ADDRESS_URI = "http://nthai.cs.trincoll.edu/PacaServer/retrieve.php";
    private static final String LONGITUDE = "longitude";
    private static final String LATITUDE = "latitude";

    private RequestQueue mRequestQueue;
    private ImageLoader mImageLoader;

    private GoogleApiClient mGoogleApiClient;
    private Location mLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gallery_host);

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();

        /*
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

        JsonArrayRequest jsonArrayRequest = new JsonArrayRequest(Request.Method.GET, GET_PICTURE_ADDRESS_URI,
                new Response.Listener<JSONArray>() {
                    @Override
                    public void onResponse(JSONArray response) {
                        fetchImage(response);
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                new AlertDialog.Builder(GalleryActivity.this)
                        .setMessage(R.string.cant_contact_server)
                        .create()
                        .show();
            }
        });
        jsonArrayRequest.setTag(TAG);
        mRequestQueue.add(jsonArrayRequest);*/

        //fetchLocationToSendToServer();
    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.d(TAG, "Connected");
        mLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        new AlertDialog.Builder(this)
                .setMessage(Double.toString(mLocation.getLatitude()) + " " + Double.toString(mLocation.getLongitude()))
                .create()
                .show();
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.d(TAG, "Connection failed");
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.d(TAG, "Connection suspended");
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
            case R.id.action_open_camera:
                Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                if (cameraIntent.resolveActivity(getPackageManager()) != null) {
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

    public JSONObject fetchLocationToSendToServer() {
        JSONObject request = new JSONObject();

        try {
            if (mLocation != null) {
                request.put(LONGITUDE, mLocation.getLongitude());
                request.put(LATITUDE, mLocation.getLatitude());
            }
        } catch (JSONException e) {
            showSettingsAlert(this);
        }

        return request;
    }

    public void showSettingsAlert(final Context context) {
        new AlertDialog.Builder(context)
                .setTitle(R.string.enable_gps)
                .setMessage(R.string.enable_gps_message)
                .setPositiveButton(R.string.button_positive, new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                        context.startActivity(intent);
                    }
                })
                .setNegativeButton(R.string.button_negative, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                })
                .show();
    }
}
