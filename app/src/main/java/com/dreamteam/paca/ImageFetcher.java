package com.dreamteam.paca;

import android.app.AlertDialog;
import android.os.AsyncTask;
import android.util.Log;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;

public class ImageFetcher extends AsyncTask<Void, Void, String> {
    public final static String TAG = ImageFetcher.class.getName();
    private final static String mUri = "http://nthai.cs.trincoll.edu/PacaServer/retrieve.php";
    private GalleryActivity mGalleryActivity;

    public ImageFetcher(GalleryActivity galleryActivity) {
        mGalleryActivity = galleryActivity;
    }

    @Override
    protected String doInBackground(Void... params) {
        try {
            HttpClient httpClient = new DefaultHttpClient();

            HttpGet getRequest = new HttpGet();
            URI retrievePhpScript = new URI(mUri);
            getRequest.setURI(retrievePhpScript);
            HttpResponse response = httpClient.execute(getRequest);
            BufferedReader responseStream = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
            StringBuilder result = new StringBuilder();
            String line = responseStream.readLine();
            while (line != null) {
                result.append(line);
                line = responseStream.readLine();
            }
            responseStream.close();
            return result.toString();
        } catch (URISyntaxException e) {
            Log.e(TAG, URISyntaxException.class.getName(), e);
            return null;
        } catch (IOException e) {
            Log.e(TAG, IOException.class.getName(), e);
            return null;
        }
    }

    @Override
    protected void onPostExecute(final String s) {
        super.onPostExecute(s);
        Log.d(TAG, s);
        mGalleryActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                new AlertDialog.Builder(mGalleryActivity)
                        .setMessage(s)
                        .create()
                        .show();
            }
        });
    }
}
