package com.dreamteam.paca;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.os.AsyncTask;
import android.support.v4.app.NotificationCompat;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

public class UploadPhotoTask extends AsyncTask<String, Void, Integer> {
    public static final int ID_UPLOADING = 0;
    public static final int ID_FAILED = 1;
    public static final int ID_UPLOADED = 2;

    private static final int RESULT_FAILED = 0;
    private static final int RESULT_UPLOADED = 1;

    private static final String UPLOAD_URI = "http://nthai.cs.trincoll.edu/PacaServer/upload_photo.php";

    private Notification mNotification;
    private Context mContext;

    public UploadPhotoTask(Context context, String fileName) {
        mContext = context;
        mNotification = new NotificationCompat.Builder(context)
                .setSmallIcon(R.drawable.ic_launcher)
                .setContentTitle("Uploading Picture")
                .setContentText(fileName)
                .build();
    }

    @Override
    protected Integer doInBackground(String... params) {
        HttpURLConnection conn;
        DataOutputStream dos;
        String lineEnd = "\r\n";
        String twoHyphens = "--";
        String boundary = "*****";
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
            dos.writeBytes("Content-Disposition: form-data; name=\"uploaded_file\";filename="+ fileName + "" + lineEnd);
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

            if(serverResponseCode == 200){
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
                mNotification = new NotificationCompat.Builder(mContext)
                        .setSmallIcon(R.drawable.ic_launcher)
                        .setContentTitle("Uploading Picture")
                        .setContentText("Upload process failed. Please try again.")
                        .build();
                notificationManager.notify(ID_FAILED, mNotification);
                break;
            case RESULT_UPLOADED:
                mNotification = new NotificationCompat.Builder(mContext)
                        .setSmallIcon(R.drawable.ic_launcher)
                        .setContentTitle("Uploading Picture")
                        .setContentText("Upload success!")
                        .build();
                notificationManager.notify(ID_UPLOADED, mNotification);
                break;
        }
    }
}
