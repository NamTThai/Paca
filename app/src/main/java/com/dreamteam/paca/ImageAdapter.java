package com.dreamteam.paca;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import com.android.volley.toolbox.NetworkImageView;

import java.util.ArrayList;

public class ImageAdapter extends BaseAdapter {
    private final static String mAddressesUri = "http://nthai.cs.trincoll.edu/Pictures/";
    private Context mContext;
    private ArrayList<String> mInitialAddresses;

    public ImageAdapter(Context c, ArrayList<String> initial) {
        mContext = c;
        mInitialAddresses = initial;
    }

    @Override
    public int getCount() {
        return mInitialAddresses.size();
    }

    @Override
    public Object getItem(int position) {
        return mInitialAddresses.get(position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        NetworkImageView networkImageView;
        if (convertView == null) {
            networkImageView = new NetworkImageView(mContext);
        } else {
            networkImageView = (NetworkImageView) convertView;
        }


        if (mInitialAddresses.size() <= position) {
            return networkImageView;
        }

        String imageUrl = mAddressesUri + mInitialAddresses.get(position);

        networkImageView.setImageUrl(imageUrl, ((GalleryActivity) mContext).getImageLoader());
        return networkImageView;
    }
}
