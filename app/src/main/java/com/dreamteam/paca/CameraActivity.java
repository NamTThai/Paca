package com.dreamteam.paca;

import android.content.Intent;
import android.graphics.Bitmap;
import android.provider.MediaStore;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;


public class CameraActivity extends ActionBarActivity {

    public Button btnTakePhoto;
    public ImageView ingTakenPhoto;
    private static final int CAMERA_REQUEST = 1313;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        btnTakePhoto = (Button) findViewById(R.id.captureButton);
        ingTakenPhoto = (ImageView) findViewById(R.id.MyImageView);

        btnTakePhoto.setOnClickListener(new btnTakePhotoClicker());

    }

    @Override
    protected  void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode,resultCode,data);

        if(requestCode == CAMERA_REQUEST){
            Bitmap thumbnail = (Bitmap) data.getExtras().get("data");
            ingTakenPhoto.setImageBitmap(thumbnail);
        }

    }

    class btnTakePhotoClicker implements Button.OnClickListener {

        @Override
        public void onClick(View v) {

            Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            if(cameraIntent.resolveActivity(getPackageManager()) != null){
                startActivityForResult(cameraIntent, CAMERA_REQUEST);
            }


        }
    }
}
