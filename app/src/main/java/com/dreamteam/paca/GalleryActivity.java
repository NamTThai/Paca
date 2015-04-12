package com.dreamteam.paca;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.graphics.Bitmap;
import android.location.Location;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.Settings;
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
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;


public class GalleryActivity extends ActionBarActivity implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {
    public static final String TAG = GalleryActivity.class.getName();
    private static final int CAMERA_REQUEST = 1313;

    private static final String GET_PICTURE_ADDRESS_URI = "http://nthai.cs.trincoll.edu/PacaServer/retrieve.php";
    private static final String LONGITUDE = "longitude";
    private static final String LATITUDE = "latitude";

    private static final String RESOLVING_ERROR = "Resolving error";
    private static final int REQUEST_RESOLVE_ERROR = 1001;

    private RequestQueue mRequestQueue;
    private ImageLoader mImageLoader;

    private GoogleApiClient mGoogleApiClient;
    private Location mLocation;
    private boolean mResolvingError;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gallery_host);

        this.getSupportActionBar().setElevation(0);

        mRequestQueue = getRequestQueue();
        mImageLoader = getImageLoader();
        mGoogleApiClient = getGoogleApiClient();

        if (savedInstanceState != null) {
            mResolvingError = savedInstanceState.getBoolean(RESOLVING_ERROR, false);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(RESOLVING_ERROR, mResolvingError);
    }

    @Override
    public void onConnected(Bundle bundle) {
        mLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);

        JsonArrayRequest jsonArrayRequest = new JsonArrayRequest(Request.Method.GET, GET_PICTURE_ADDRESS_URI,
                new Response.Listener<JSONArray>() {
                    @Override
                    public void onResponse(JSONArray response) {
                        fetchImage(response);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        new AlertDialog.Builder(GalleryActivity.this)
                                .setMessage(R.string.cant_contact_server)
                                .create()
                                .show();
                    }
                });
        jsonArrayRequest.setTag(TAG);
        getRequestQueue().add(jsonArrayRequest);
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        if (!mResolvingError) {
            if (connectionResult.hasResolution()) {
                try {
                    connectionResult.startResolutionForResult(this, REQUEST_RESOLVE_ERROR);
                    mResolvingError = true;
                } catch (IntentSender.SendIntentException e) {
                    mGoogleApiClient.connect();
                }
            } else {
                Dialog dialog = GooglePlayServicesUtil
                        .getErrorDialog(connectionResult.getErrorCode(),
                                this, REQUEST_RESOLVE_ERROR);
                dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        mResolvingError = false;
                    }
                });
                dialog.show();
                mResolvingError = true;
            }
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        showSettingsAlert();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (!mResolvingError) {
            getGoogleApiClient().connect();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        getGoogleApiClient().disconnect();
        if (getRequestQueue() != null) {
            getRequestQueue().cancelAll(TAG);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (resultCode) {
            case REQUEST_RESOLVE_ERROR:
                mResolvingError = false;
                break;
            case RESULT_OK:
                if (!getGoogleApiClient().isConnecting() &&
                        !getGoogleApiClient().isConnected()) {
                    getGoogleApiClient().connect();
                }
                break;
        }
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
                if (cameraIntent.resolveActivity(getPackageManager()) != null) {
                    startActivityForResult(cameraIntent, CAMERA_REQUEST);
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public RequestQueue getRequestQueue() {
        if (mRequestQueue == null) {
            mRequestQueue = Volley.newRequestQueue(this);
        }
        return mRequestQueue;
    }

    public GoogleApiClient getGoogleApiClient() {
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }
        return mGoogleApiClient;
    }

    public ImageLoader getImageLoader() {
        if (mImageLoader == null) {
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
        }
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

    public void showSettingsAlert() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.enable_gps)
                .setMessage(R.string.enable_gps_message)
                .setPositiveButton(R.string.button_positive, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                        startActivity(intent);
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
