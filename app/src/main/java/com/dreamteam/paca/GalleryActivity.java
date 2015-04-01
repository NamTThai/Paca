package com.dreamteam.paca;

import android.app.AlertDialog;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.GridView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;


public class GalleryActivity extends ActionBarActivity {
    public static final String TAG = GalleryActivity.class.getName();

    private final static String mGetPictureAddressesUri = "http://nthai.cs.trincoll.edu/PacaServer/retrieve.php";

    private RequestQueue mRequestQueue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gallery_host);

        mRequestQueue = Volley.newRequestQueue(this);

        StringRequest stringRequest = new StringRequest(Request.Method.GET, mGetPictureAddressesUri,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String s) {
                        fetchImage(s);
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError volleyError) {
                        new AlertDialog.Builder(GalleryActivity.this)
                                .setMessage("Fail to contact server")
                                .create()
                                .show();
                    }
        });
        mRequestQueue.add(stringRequest);

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
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void fetchImage(String s) {
        new AlertDialog.Builder(this)
                .setMessage(s)
                .create()
                .show();

        GridView gridview = (GridView) findViewById(R.id.main_gallery);
        gridview.setAdapter(new ImageAdapter(this));

        gridview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

            }
        });
    }
}
