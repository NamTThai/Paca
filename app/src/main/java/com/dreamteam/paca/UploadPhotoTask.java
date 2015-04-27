package com.dreamteam.paca;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.os.AsyncTask;
import android.support.v4.app.NotificationCompat;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class UploadPhotoTask extends AsyncTask<String, Void, Integer> {
    public static final int ID_UPLOADING = 0;
    public static final int ID_FAILED = 1;
    public static final int ID_UPLOADED = 2;

    private static final int RESULT_FAILED = 0;
    private static final int RESULT_UPLOADED = 1;

    public static final String DATABASE_OPERATION_URI = "http://nthai.cs.trincoll.edu/PacaServer/db_operation.php";
    private static final String UPLOAD_URI = "http://nthai.cs.trincoll.edu/PacaServer/upload_photo.php";

    private Notification mNotification;
    private Context mContext;
    private String mFileName;
    private double mLat;
    private double mLng;

    public UploadPhotoTask(Context context, String fileName, double lat, double lng) {
        mContext = context;
        mNotification = new NotificationCompat.Builder(context)
                .setSmallIcon(R.drawable.ic_launcher)
                .setContentTitle("Uploading Picture")
                .setContentText(fileName)
                .build();
        mLat = lat;
        mLng = lng;
    }

    @Override
    protected Integer doInBackground(String... params) {
        HttpURLConnection conn;
        DataOutputStream dos;
        String lineEnd = "\r\n";
        String twoHyphens = "--";
        String boundary = "*****";
        String separator = "&";
        int bytesRead, bytesAvailable, bufferSize;
        byte[] buffer;
        int maxBufferSize = 1 * 1024 * 1024;
        File sourceFile = new File(params[0]);
        String fileName = sourceFile.getName();

        if (!sourceFile.isFile()) {
            return RESULT_FAILED;
        }

        try {
            // open a URL connection to the Servlet
            FileInputStream fileInputStream = new FileInputStream(sourceFile);
            URL url = new URL(UPLOAD_URI);

            // Open a HTTP  connection to  the URL
            conn = (HttpURLConnection) url.openConnection();
            conn.setDoInput(true); // Allow Inputs
            conn.setDoOutput(true); // Allow Outputs
            conn.setUseCaches(false); // Don't use a Cached Copy
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Connection", "Keep-Alive");
            conn.setRequestProperty("ENCTYPE", "multipart/form-data");
            conn.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);
            conn.setRequestProperty("uploaded_file", fileName);

            dos = new DataOutputStream(conn.getOutputStream());

            dos.writeBytes(twoHyphens + boundary + lineEnd);
            dos.writeBytes("Content-Disposition: form-data; name=\"uploaded_file\";filename=" + fileName + "" + lineEnd);
            dos.writeBytes(lineEnd);

            // create a buffer of  maximum size
            bytesAvailable = fileInputStream.available();

            bufferSize = Math.min(bytesAvailable, maxBufferSize);
            buffer = new byte[bufferSize];

            // read file and write it into form...
            bytesRead = fileInputStream.read(buffer, 0, bufferSize);

            while (bytesRead > 0) {
                dos.write(buffer, 0, bufferSize);
                bytesAvailable = fileInputStream.available();
                bufferSize = Math.min(bytesAvailable, maxBufferSize);
                bytesRead = fileInputStream.read(buffer, 0, bufferSize);
            }

            // send multipart form data necesssary after file data...
            dos.writeBytes(lineEnd);
            dos.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);

            fileInputStream.close();
            dos.flush();
            dos.close();

            int serverResponseCode = conn.getResponseCode();
            BufferedReader bf = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            mFileName = bf.readLine();

            if (serverResponseCode == 200 && !mFileName.equals("0")) {
                return RESULT_UPLOADED;
            } else {
                return RESULT_FAILED;
            }
        } catch (IOException e) {
            return RESULT_FAILED;
        }
    }

    @Override
    protected void onProgressUpdate(Void... values) {
        super.onProgressUpdate(values);
        ((NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE))
                .notify(ID_UPLOADING, mNotification);
    }

    @Override
    protected void onPostExecute(Integer resultCode) {
        super.onPostExecute(resultCode);
        NotificationManager notificationManager = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(ID_UPLOADING);
        switch (resultCode) {
            case RESULT_FAILED:
                notifyUploadFailure();
                break;
            case RESULT_UPLOADED:
                RequestQueue requestQueue = Volley.newRequestQueue(mContext);

                StringRequest stringRequest = new StringRequest(Request.Method.POST, DATABASE_OPERATION_URI,
                        new Response.Listener<String>() {
                            @Override
                            public void onResponse(String response) {
                                notifyUploadSuccess();
                            }
                        },
                        new Response.ErrorListener() {
                            @Override
                            public void onErrorResponse(VolleyError error) {
                                notifyUploadFailure();
                            }
                        }) {
                    @Override
                    protected Map<String, String> getParams() throws AuthFailureError {
                        Map<String, String> postParams = new HashMap<>();
                        Date date = new Date();
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
                        String formattedDate = sdf.format(date);
                        postParams.put("request_code", "0");
                        postParams.put("file_name", mFileName);
                        postParams.put("lat", Double.toString(mLat));
                        postParams.put("lng", Double.toString(mLng));
                        postParams.put("timestamp", formattedDate);
                        return postParams;
                    }

                    @Override
                    public Map<String, String> getHeaders() throws AuthFailureError {
                        Map<String,String> params = new HashMap<>();
                        params.put("Content-Type", "application/x-www-form-urlencoded");
                        return params;
                    }
                };
                requestQueue.add(stringRequest);
                break;
        }
    }

    private void notifyUploadFailure() {
        NotificationManager notificationManager = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
        mNotification = new NotificationCompat.Builder(mContext)
                .setSmallIcon(R.drawable.ic_launcher)
                .setContentTitle("Uploading Picture")
                .setContentText("Upload process failed. Please try again.")
                .build();
        notificationManager.notify(ID_FAILED, mNotification);
    }

    private void notifyUploadSuccess() {
        NotificationManager notificationManager = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
        mNotification = new NotificationCompat.Builder(mContext)
                .setSmallIcon(R.drawable.ic_launcher)
                .setContentTitle("Uploading Picture")
                .setContentText("Upload success!")
                .build();
        notificationManager.notify(ID_UPLOADED, mNotification);
    }
}
