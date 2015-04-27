package com.dreamteam.paca;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.graphics.Bitmap;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.support.v4.util.LruCache;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageButton;
import android.widget.Toast;

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
import org.json.JSONObject;

import java.util.ArrayList;

import butterknife.InjectView;


public class GalleryActivity extends BaseActivity implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,FeedAdapter.OnFeedItemClickListener,
        FeedContextMenu.OnFeedContextMenuItemClickListener {
    public static final String TAG = GalleryActivity.class.getName();
    private static final int REQUEST_TAKE_PHOTO = 1;

    private static final int ANIM_DURATION_TOOLBAR = 300;
    private static final int ANIM_DURATION_FAB = 400;

    private static final String GET_PICTURE_ADDRESS_URI =
            "http://nthai.cs.trincoll.edu/PacaServer/retrieve.php?lat=%1$s&lng=%2$s";
    private static final String TOKEN_IMAGE_ADDESS =
            "https://thisgreenearth.files.wordpress.com/2011/04/alpaca.jpg";

    private static final String RESOLVING_ERROR = "Resolving error";
    private static final int REQUEST_RESOLVE_ERROR = 1001;

    private RequestQueue mRequestQueue;
    private ImageLoader mImageLoader;

    private GoogleApiClient mGoogleApiClient;
    private Location mLocation;
    private boolean mResolvingError;

    public static final String ACTION_SHOW_LOADING_ITEM = "action_show_loading_item";

    @InjectView(R.id.refresh)
    SwipeRefreshLayout mSwipeRefreshLayout;
    @InjectView(R.id.image_feed)
    RecyclerView rvFeed;
    @InjectView(R.id.take_photos)
    ImageButton buttonTakePhotos;

    private FeedAdapter feedAdapter;

    private boolean pendingIntroAnimation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gallery);

        getSupportActionBar().setElevation(0);

        mRequestQueue = getRequestQueue();
        mImageLoader = getImageLoader();
        mGoogleApiClient = getGoogleApiClient();

        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                getGoogleApiClient().reconnect();
            }
        });
        mSwipeRefreshLayout.setRefreshing(true);

        if (savedInstanceState == null) {
            pendingIntroAnimation = true;
        } else {
            mResolvingError = savedInstanceState.getBoolean(RESOLVING_ERROR, false);
            feedAdapter.updateItems(false);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        switch (intent.getAction()) {
            case ACTION_SHOW_LOADING_ITEM:
                showFeedLoadingItemDelayed();
                break;
        }
    }

    private void showFeedLoadingItemDelayed() {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                rvFeed.smoothScrollToPosition(0);
                feedAdapter.showLoadingView();
            }
        }, 500);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(RESOLVING_ERROR, mResolvingError);
    }

    @Override
    public void onConnected(Bundle bundle) {
        mLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        String getPictureAddressUri = String.format(GET_PICTURE_ADDRESS_URI,
                mLocation.getLatitude(), mLocation.getLongitude());

        JsonArrayRequest jsonArrayRequest = new JsonArrayRequest(Request.Method.GET, getPictureAddressUri,
                new Response.Listener<JSONArray>() {
                    @Override
                    public void onResponse(JSONArray response) {
                        setupFeed(response);
                        mSwipeRefreshLayout.setRefreshing(false);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        setupFeed(null);
                        Log.d(TAG, error.getMessage());
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
        switch (requestCode) {
            case REQUEST_RESOLVE_ERROR:
                mResolvingError = false;
                if (resultCode == RESULT_OK) {
                    if (!getGoogleApiClient().isConnecting() &&
                            !getGoogleApiClient().isConnected()) {
                        getGoogleApiClient().connect();
                    }
                }
                break;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        if (pendingIntroAnimation) {
            pendingIntroAnimation = false;
            startIntroAnimation();
        }
        return true;
    }

    @TargetApi(14)
    private void startIntroAnimation() {
        buttonTakePhotos.setTranslationY(2 * getResources().getDimensionPixelOffset(R.dimen.btn_fab_size));

        int actionbarSize = Utils.dpToPx(56);
        getToolbar().setTranslationY(-actionbarSize);
        getIvLogo().setTranslationY(-actionbarSize);

        getToolbar().animate()
                .translationY(0)
                .setDuration(ANIM_DURATION_TOOLBAR)
                .setStartDelay(300);
        getIvLogo().animate()
                .translationY(0)
                .setDuration(ANIM_DURATION_TOOLBAR)
                .setStartDelay(400)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        startContentAnimation();
                    }})
                .start();
    }

    @TargetApi(14)
    private void startContentAnimation() {
        buttonTakePhotos.animate()
                .translationY(0)
                .setInterpolator(new OvershootInterpolator(1.f))
                .setStartDelay(300)
                .setDuration(ANIM_DURATION_FAB)
                .start();
        getFeedAdapter().updateItems(true);
    }

    public FeedAdapter getFeedAdapter() {
        if (feedAdapter == null) {
            feedAdapter = new FeedAdapter(this);
        }
        return feedAdapter;
    }

    private void setupFeed(JSONArray response) {
        final LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this) {
            @Override
            protected int getExtraLayoutSpace(RecyclerView.State state) {
                return 300;
            }
        };
        rvFeed.setLayoutManager(linearLayoutManager);
        feedAdapter = (FeedAdapter) rvFeed.getAdapter();

        ArrayList<String> feedItems = new ArrayList<>();
        if (response != null) {
            for (int i = 0; i < response.length(); i++) {
                try {
                    JSONObject pictureObject = response.getJSONObject(i);
                    feedItems.add(pictureObject.getString("address"));
                } catch (JSONException | NullPointerException e) {
                    feedItems.add(TOKEN_IMAGE_ADDESS);
                }
            }
        } else {
            feedItems.add(TOKEN_IMAGE_ADDESS);
        }
        
        if (feedAdapter == null) {
            feedAdapter = new FeedAdapter(this, feedItems);
            feedAdapter.setOnFeedItemClickListener(this);
            rvFeed.setAdapter(feedAdapter);
            rvFeed.setOnScrollListener(new RecyclerView.OnScrollListener() {
                @Override
                public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                    FeedContextMenuManager.getInstance().onScrolled(recyclerView, dx, dy);
                    int visibleItemCount = linearLayoutManager.getChildCount();
                    int totalItemCount = linearLayoutManager.getItemCount();
                    int pastVisiblesItems = linearLayoutManager.findFirstVisibleItemPosition();
                    if ( (visibleItemCount+pastVisiblesItems) >= totalItemCount) {
                        Toast.makeText(GalleryActivity.this, "There are no more images to show", Toast.LENGTH_LONG).show();
                    }
                }
            });
        } else {
            feedAdapter.setFeedItems(feedItems);
            feedAdapter.notifyDataSetChanged();
            //rvFeed.notify();
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

    @Override
    public void onCommentsClick(View v, int position) {
        final Intent intent = new Intent(this, CommentsActivity.class);
        int[] startingLocation = new int[2];
        v.getLocationOnScreen(startingLocation);
        intent.putExtra(CommentsActivity.ARG_DRAWING_START_LOCATION, startingLocation[1]);
        startActivity(intent);
        overridePendingTransition(0, 0);
    }

    @Override
    public void onMoreClick(View v, int itemPosition) {
        FeedContextMenuManager.getInstance().toggleContextMenuFromView(v, itemPosition, this);
    }

    @Override
    public void onReportClick(int feedItem) {
        FeedContextMenuManager.getInstance().hideContextMenu();
    }

    @Override
    public void onSharePhotoClick(int feedItem) {
        FeedContextMenuManager.getInstance().hideContextMenu();
    }

    @Override
    public void onCopyShareUrlClick(int feedItem) {
        FeedContextMenuManager.getInstance().hideContextMenu();
    }

    @Override
    public void onCancelClick(int feedItem) {
        FeedContextMenuManager.getInstance().hideContextMenu();
    }

    public void onTakePhotoClick(View view) {
        int[] startingLocation = new int[2];
        buttonTakePhotos.getLocationOnScreen(startingLocation);
        startingLocation[0] += buttonTakePhotos.getWidth() / 2;

        Intent intent = new Intent(this, TakePhotoActivity.class);
        intent.putExtra(TakePhotoActivity.ARG_REVEAL_START_LOCATION, startingLocation);
        if (mLocation != null) {
            intent.putExtra(TakePhotoActivity.ARG_LAT, mLocation.getLatitude());
            intent.putExtra(TakePhotoActivity.ARG_LNG, mLocation.getLongitude());
        }
        startActivity(intent);
        overridePendingTransition(0, 0);
    }
}
