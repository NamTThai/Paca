package com.dreamteam.paca;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextSwitcher;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.NetworkImageView;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import butterknife.ButterKnife;
import butterknife.InjectView;

public class FeedAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements View.OnClickListener {
    private static final int VIEW_TYPE_DEFAULT = 1;
    private static final int VIEW_TYPE_LOADER = 2;

    private static final String ADDRESS_URI = "http://nthai.cs.trincoll.edu/Pictures/";

    private static final DecelerateInterpolator DECCELERATE_INTERPOLATOR = new DecelerateInterpolator();
    private static final AccelerateInterpolator ACCELERATE_INTERPOLATOR = new AccelerateInterpolator();
    private static final OvershootInterpolator OVERSHOOT_INTERPOLATOR = new OvershootInterpolator(4);

    private static final int ANIMATED_ITEMS_COUNT = 2;

    private Context mContext;
    private ArrayList<JSONObject> feedItems;
    private int lastAnimatedPosition = -1;
    private int itemsCount = 0;
    private boolean animateItems = false;

    private final Map<RecyclerView.ViewHolder, AnimatorSet> likeAnimations = new HashMap<>();
    private final ArrayList<Integer> likedPositions = new ArrayList<>();

    private OnFeedItemClickListener onFeedItemClickListener;

    private boolean showLoadingView = false;
    private int loadingViewSize = Utils.dpToPx(200);

    public FeedAdapter(Context context) {
        new FeedAdapter(context, new ArrayList<JSONObject>());
    }

    public FeedAdapter(Context context, ArrayList<JSONObject> items) {
        this.mContext = context;
        feedItems = items;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        final View view = LayoutInflater.from(mContext).inflate(R.layout.item_feed, parent, false);
        final CellFeedViewHolder cellFeedViewHolder = new CellFeedViewHolder(view);
        if (viewType == VIEW_TYPE_DEFAULT) {
            cellFeedViewHolder.btnDisLike.setOnClickListener(this);
            //cellFeedViewHolder.btnMore.setOnClickListener(this);
            //cellFeedViewHolder.ivFeedCenter.setOnClickListener(this);
            cellFeedViewHolder.btnLike.setOnClickListener(this);
        } else if (viewType == VIEW_TYPE_LOADER) {
            View bgView = new View(mContext);
            bgView.setLayoutParams(new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
            ));
            bgView.setBackgroundColor(0x77ffffff);
            cellFeedViewHolder.vImageRoot.addView(bgView);
            cellFeedViewHolder.vProgressBg = bgView;

            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(loadingViewSize, loadingViewSize);
            params.gravity = Gravity.CENTER;
            SendingProgressView sendingProgressView = new SendingProgressView(mContext);
            sendingProgressView.setLayoutParams(params);
            cellFeedViewHolder.vImageRoot.addView(sendingProgressView);
            cellFeedViewHolder.vSendingProgress = sendingProgressView;
        }

        return cellFeedViewHolder;
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    private void runEnterAnimation(View view, int position) {
        if (!animateItems || position >= ANIMATED_ITEMS_COUNT - 1) {
            return;
        }

        if (position > lastAnimatedPosition) {
            lastAnimatedPosition = position;
            view.setTranslationY(Utils.getScreenHeight(mContext));
            view.animate()
                    .translationY(0)
                    .setInterpolator(new DecelerateInterpolator(3.f))
                    .setDuration(700)
                    .start();
        }
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int position) {
        runEnterAnimation(viewHolder.itemView, position);
        final CellFeedViewHolder holder = (CellFeedViewHolder) viewHolder;
        if (getItemViewType(position) == VIEW_TYPE_DEFAULT) {
            bindDefaultFeedItem(position, holder);
        } else if (getItemViewType(position) == VIEW_TYPE_LOADER) {
            bindLoadingFeedItem(position, holder);
        }
    }

    private void bindDefaultFeedItem(int position, CellFeedViewHolder holder) {
        position = position % feedItems.size();
        String uri;
        try {
            uri = ADDRESS_URI + feedItems.get(position).getString("address");
        } catch (JSONException e) {
            uri = GalleryActivity.TOKEN_IMAGE_ADDESS;
        }
        holder.ivFeedCenter.setImageUrl(uri, ((GalleryActivity) mContext).getImageLoader());

        updateLikesCounter(holder);
        updateHeartButton(holder, false);

        holder.btnDisLike.setTag(holder);
        //holder.btnMore.setTag(position);
        holder.ivFeedCenter.setTag(holder);
        holder.btnLike.setTag(holder);

        if (likeAnimations.containsKey(holder)) {
           likeAnimations.get(holder).cancel();
        }
        resetLikeAnimationState(holder);
    }

    private void bindLoadingFeedItem(int position, final CellFeedViewHolder holder) {
        holder.ivFeedCenter.setImageResource(R.drawable.img_feed_center_1);
        //holder.ivFeedBottom.setImageResource(R.drawable.img_feed_bottom_1);
        holder.vSendingProgress.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                holder.vSendingProgress.getViewTreeObserver().removeOnPreDrawListener(this);
                holder.vSendingProgress.simulateProgress();
                return true;
            }
        });
        holder.vSendingProgress.setOnLoadingFinishedListener(new SendingProgressView.OnLoadingFinishedListener() {
            @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
            @Override
            public void onLoadingFinished() {
                holder.vSendingProgress.animate().scaleY(0).scaleX(0).setDuration(200).setStartDelay(100);
                holder.vProgressBg.animate().alpha(0.f).setDuration(200).setStartDelay(100)
                        .setListener(new AnimatorListenerAdapter() {
                            @Override
                            public void onAnimationEnd(Animator animation) {
                                holder.vSendingProgress.setScaleX(1);
                                holder.vSendingProgress.setScaleY(1);
                                holder.vProgressBg.setAlpha(1);
                                showLoadingView = false;
                                notifyItemChanged(0);
                            }
                        })
                        .start();
            }
        });
    }

    @Override
    public int getItemViewType(int position) {
        if (showLoadingView && position == 0) {
            return VIEW_TYPE_LOADER;
        } else {
            return VIEW_TYPE_DEFAULT;
        }
    }

    @Override
    public int getItemCount() {
        return itemsCount;
    }

    public ArrayList<JSONObject> getFeedItems() {
        return feedItems;
    }

    public void setFeedItems(ArrayList<JSONObject> feedItems) {
        this.feedItems = feedItems;
    }

    private void updateLikesCounter(CellFeedViewHolder holder) {
        int currentLikesCount;
        try {
            currentLikesCount = feedItems.get(holder.getPosition()).getInt("likes");
        } catch (JSONException e) {
            currentLikesCount = 0;
        }

        String likesCountText = mContext.getResources().getQuantityString(
                R.plurals.likes_count, currentLikesCount, currentLikesCount
        );

        holder.tsLikesCounter.setCurrentText(likesCountText);
    }

    private void updateLikesCounter(CellFeedViewHolder holder, boolean animated, boolean like) {
        int currentLikesCount;
        try {
            currentLikesCount = feedItems.get(holder.getPosition()).getInt("likes");
        } catch (JSONException e) {
            currentLikesCount = 0;
        }

        currentLikesCount += like ? 1 : -1;

        String likesCountText = mContext.getResources().getQuantityString(
                R.plurals.likes_count, currentLikesCount, currentLikesCount
        );

        if (animated) {
            holder.tsLikesCounter.setText(likesCountText);
        } else {
            holder.tsLikesCounter.setCurrentText(likesCountText);
        }

        RequestQueue requestQueue = Volley.newRequestQueue(mContext);
        final JSONObject pictureObject = feedItems.get(holder.getPosition());
        final String likes = Integer.toString(currentLikesCount);
        StringRequest stringRequest = new StringRequest(Request.Method.POST, UploadPhotoTask.DATABASE_OPERATION_URI,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Log.d(GalleryActivity.TAG, "success: " + response);
                        //notifyUploadSuccess();
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.d(GalleryActivity.TAG, "failed: " + error.getMessage());
                        //notifyUploadFailure();
                    }
                }) {
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                Map<String, String> postParams = new HashMap<>();
                postParams.put("request_code", "1");
                postParams.put("id", pictureObject.optString("id", "0"));
                postParams.put("likes", likes);
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
    }

    private void updateHeartButton(final CellFeedViewHolder holder, boolean animated) {
        if (animated) {
            if (!likeAnimations.containsKey(holder)) {
                AnimatorSet animatorSet = new AnimatorSet();
                likeAnimations.put(holder, animatorSet);

                ObjectAnimator rotationAnim = ObjectAnimator.ofFloat(holder.btnLike, "rotation", 0f, 360f);
                rotationAnim.setDuration(300);
                rotationAnim.setInterpolator(ACCELERATE_INTERPOLATOR);

                ObjectAnimator bounceAnimX = ObjectAnimator.ofFloat(holder.btnLike, "scaleX", 0.2f, 1f);
                bounceAnimX.setDuration(300);
                bounceAnimX.setInterpolator(OVERSHOOT_INTERPOLATOR);

                ObjectAnimator bounceAnimY = ObjectAnimator.ofFloat(holder.btnLike, "scaleY", 0.2f, 1f);
                bounceAnimY.setDuration(300);
                bounceAnimY.setInterpolator(OVERSHOOT_INTERPOLATOR);
                bounceAnimY.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationStart(Animator animation) {
                        holder.btnLike.setImageResource(R.drawable.ic_heart_red);
                    }
                });

                animatorSet.play(rotationAnim);
                animatorSet.play(bounceAnimX).with(bounceAnimY).after(rotationAnim);

                animatorSet.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        resetLikeAnimationState(holder);
                    }
                });

                animatorSet.start();
            }
        } else {
            if (likedPositions.contains(holder.getPosition())) {
                holder.btnLike.setImageResource(R.drawable.ic_heart_red);
            } else {
                holder.btnLike.setImageResource(R.drawable.ic_heart_outline_grey);
            }
        }
    }

    @Override
    public void onClick(View view) {
        final int viewId = view.getId();
        switch (viewId) {
            case R.id.btn_dislike:
                CellFeedViewHolder holder = (CellFeedViewHolder) view.getTag();
                if (!likedPositions.contains(holder.getPosition())) {
                    likedPositions.add(holder.getPosition());
                    updateLikesCounter(holder, true, false);
                    //updateHeartButton(holder, true);
                }
                break;
            case R.id.btn_like:
                holder = (CellFeedViewHolder) view.getTag();
                if (!likedPositions.contains(holder.getPosition())) {
                    likedPositions.add(holder.getPosition());
                    updateLikesCounter(holder, true, true);
                    updateHeartButton(holder, true);
                }
                break;
        }
    }

    /*private void animatePhotoLike(final CellFeedViewHolder holder) {
        if (!likeAnimations.containsKey(holder)) {
            holder.vBgLike.setVisibility(View.VISIBLE);
            holder.ivLike.setVisibility(View.VISIBLE);

            holder.vBgLike.setScaleY(0.1f);
            holder.vBgLike.setScaleX(0.1f);
            holder.vBgLike.setAlpha(1f);
            holder.ivLike.setScaleY(0.1f);
            holder.ivLike.setScaleX(0.1f);

            AnimatorSet animatorSet = new AnimatorSet();
            likeAnimations.put(holder, animatorSet);

            ObjectAnimator bgScaleYAnim = ObjectAnimator.ofFloat(holder.vBgLike, "scaleY", 0.1f, 1f);
            bgScaleYAnim.setDuration(200);
            bgScaleYAnim.setInterpolator(DECCELERATE_INTERPOLATOR);
            ObjectAnimator bgScaleXAnim = ObjectAnimator.ofFloat(holder.vBgLike, "scaleX", 0.1f, 1f);
            bgScaleXAnim.setDuration(200);
            bgScaleXAnim.setInterpolator(DECCELERATE_INTERPOLATOR);
            ObjectAnimator bgAlphaAnim = ObjectAnimator.ofFloat(holder.vBgLike, "alpha", 1f, 0f);
            bgAlphaAnim.setDuration(200);
            bgAlphaAnim.setStartDelay(150);
            bgAlphaAnim.setInterpolator(DECCELERATE_INTERPOLATOR);

            ObjectAnimator imgScaleUpYAnim = ObjectAnimator.ofFloat(holder.ivLike, "scaleY", 0.1f, 1f);
            imgScaleUpYAnim.setDuration(300);
            imgScaleUpYAnim.setInterpolator(DECCELERATE_INTERPOLATOR);
            ObjectAnimator imgScaleUpXAnim = ObjectAnimator.ofFloat(holder.ivLike, "scaleX", 0.1f, 1f);
            imgScaleUpXAnim.setDuration(300);
            imgScaleUpXAnim.setInterpolator(DECCELERATE_INTERPOLATOR);

            ObjectAnimator imgScaleDownYAnim = ObjectAnimator.ofFloat(holder.ivLike, "scaleY", 1f, 0f);
            imgScaleDownYAnim.setDuration(300);
            imgScaleDownYAnim.setInterpolator(ACCELERATE_INTERPOLATOR);
            ObjectAnimator imgScaleDownXAnim = ObjectAnimator.ofFloat(holder.ivLike, "scaleX", 1f, 0f);
            imgScaleDownXAnim.setDuration(300);
            imgScaleDownXAnim.setInterpolator(ACCELERATE_INTERPOLATOR);

            animatorSet.playTogether(bgScaleYAnim, bgScaleXAnim, bgAlphaAnim, imgScaleUpYAnim, imgScaleUpXAnim);
            animatorSet.play(imgScaleDownYAnim).with(imgScaleDownXAnim).after(imgScaleUpYAnim);

            animatorSet.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    resetLikeAnimationState(holder);
                }
            });
            animatorSet.start();
        }
    }*/

    private void resetLikeAnimationState(CellFeedViewHolder holder) {
        likeAnimations.remove(holder);
        holder.vBgLike.setVisibility(View.GONE);
        holder.ivLike.setVisibility(View.GONE);
    }

    public void updateItems(boolean animated) {
        //TODO
        //set item count here
        itemsCount = 10;
        animateItems = animated;
        notifyDataSetChanged();
    }

    public void setOnFeedItemClickListener(OnFeedItemClickListener onFeedItemClickListener) {
        this.onFeedItemClickListener = onFeedItemClickListener;
    }

    public void showLoadingView() {
        showLoadingView = true;
        notifyItemChanged(0);
    }

    public static class CellFeedViewHolder extends RecyclerView.ViewHolder {
        @InjectView(R.id.ivFeedCenter)
        NetworkImageView ivFeedCenter;
        @InjectView(R.id.btn_dislike)
        ImageButton btnDisLike;
        @InjectView(R.id.btn_like)
        ImageButton btnLike;
        @InjectView(R.id.vBgLike)
        View vBgLike;
        @InjectView(R.id.ivLike)
        ImageView ivLike;
        @InjectView(R.id.tsLikesCounter)
        TextSwitcher tsLikesCounter;
        @InjectView(R.id.vImageRoot)
        FrameLayout vImageRoot;

        SendingProgressView vSendingProgress;
        View vProgressBg;

        public CellFeedViewHolder(View view) {
            super(view);
            ButterKnife.inject(this, view);
        }

    }

    public interface OnFeedItemClickListener {
        public void onCommentsClick(View v, int position);

        public void onMoreClick(View v, int position);
    }
}
