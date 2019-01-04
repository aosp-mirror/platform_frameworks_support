/*
 * Copyright 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.androidx.media;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.media2.MediaController;
import androidx.media2.SessionToken;
import androidx.media2.UriMediaItem;
import androidx.media2.widget.MediaControlView;
import androidx.media2.widget.VideoView;

import java.util.concurrent.Executor;

/**
 * Test application for VideoView/MediaControlView
 */
public class VideoPlayerActivity extends FragmentActivity {
    public static final String LOOPING_EXTRA_NAME =
            "com.example.androidx.media.VideoPlayerActivity.IsLooping";
    public static final String MEDIA_TYPE_ADVERTISEMENT =
            "com.example.androidx.media.VideoPlayerActivity.MediaTypeAdvertisement";
    private static final String TAG = "VideoPlayerActivity";

    private MyVideoView mVideoView = null;
    private float mSpeed = 1.0f;

    private MediaControlView mMediaControlView = null;
    private MediaController mMediaController = null;

    private int mPrevWidth;
    private int mPrevHeight;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Remove title bar
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        setContentView(R.layout.activity_video_player);

        mVideoView = findViewById(R.id.video_view);

        CheckBox useTextureView = findViewById(R.id.use_textureview_checkbox);
        useTextureView.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    changeViewType(VideoView.VIEW_TYPE_TEXTUREVIEW);
                } else {
                    changeViewType(VideoView.VIEW_TYPE_SURFACEVIEW);
                }
            }
        });

        String errorString = null;
        Intent intent = getIntent();
        Uri videoUri;
        if (intent == null || (videoUri = intent.getData()) == null || !videoUri.isAbsolute()) {
            errorString = "Invalid intent";
        } else {
            UriMediaItem mediaItem = new UriMediaItem.Builder(this, videoUri).build();
            mVideoView.setMediaItem(mediaItem);

            mMediaControlView = new MediaControlView(this);
            mVideoView.setMediaControlView(mMediaControlView, 2000);
            mMediaControlView.setOnFullScreenListener(new FullScreenListener());
            SessionToken token = mVideoView.getSessionToken();

            Executor executor = ContextCompat.getMainExecutor(this);
            mMediaController = new MediaController(
                    this, token, executor, new ControllerCallback());
        }
        if (errorString != null) {
            showErrorDialog(errorString);
        }
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy");
        super.onDestroy();
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (ev.getAction() == MotionEvent.ACTION_DOWN) {
            int screenWidth = getResources().getDisplayMetrics().widthPixels;
            mSpeed = mMediaController.getPlaybackSpeed();
            if (ev.getRawX() < (screenWidth / 2.0f)) {
                mSpeed -= 0.1f;
            } else {
                mSpeed += 0.1f;
            }
            mMediaController.setPlaybackSpeed(mSpeed);
            Toast.makeText(this, "speed rate: " + String.format("%.2f", mSpeed), Toast.LENGTH_SHORT)
                    .show();
        }
        return super.onTouchEvent(ev);
    }

    private void showErrorDialog(String errorMessage) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Playback error")
                .setMessage(errorMessage)
                .setCancelable(false)
                .setPositiveButton("OK",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                finish();
                            }
                        }).show();
    }

    class ControllerCallback extends MediaController.ControllerCallback {
        @Override
        public void onPlaybackSpeedChanged(
                @NonNull MediaController controller, float speed) {
            mSpeed = speed;
        }
    }

    private class FullScreenListener
            implements MediaControlView.OnFullScreenListener {
        @Override
        public void onFullScreen(View view, boolean fullScreen) {
            // TODO: Remove bottom controls after adding back button functionality.
            if (mPrevHeight == 0 && mPrevWidth == 0) {
                ViewGroup.LayoutParams params = mVideoView.getLayoutParams();
                mPrevWidth = params.width;
                mPrevHeight = params.height;
            }

            if (fullScreen) {
                // Remove notification bar
                getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                        WindowManager.LayoutParams.FLAG_FULLSCREEN);

                ViewGroup.LayoutParams params = mVideoView.getLayoutParams();
                params.width = ViewGroup.LayoutParams.MATCH_PARENT;
                params.height = ViewGroup.LayoutParams.MATCH_PARENT;
                mVideoView.setLayoutParams(params);
            } else {
                // Restore notification bar
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

                ViewGroup.LayoutParams params = mVideoView.getLayoutParams();
                params.width = mPrevWidth;
                params.height = mPrevHeight;
                mVideoView.setLayoutParams(params);
            }
            mVideoView.requestLayout();
            mVideoView.invalidate();
        }
    }

    /**
     * Extension of the stock android video view used to hook and override
     * keypress behavior.  Mainly used to make sure that certain keystrokes
     * don't automatically bring up the andriod MediaController widget (which
     * then steals focus)
     *
     * @author johngro@google.com (John Grossman)
     */
    public static class MyVideoView extends VideoView {
        private float mDX;
        private float mDY;

        public MyVideoView(Context context) {
            super(context);
        }

        public MyVideoView(Context context, AttributeSet attrs) {
            super(context, attrs);
        }

        public MyVideoView(Context context, AttributeSet attrs, int defStyle) {
            super(context, attrs, defStyle);
        }

        @Override
        public boolean onTouchEvent(MotionEvent ev) {
            switch (ev.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    mDX = ev.getRawX() - getX();
                    mDY = ev.getRawY() - getY();
                    super.onTouchEvent(ev);
                    return true;
                case MotionEvent.ACTION_MOVE:
                    animate()
                            .x(ev.getRawX() - mDX)
                            .y(ev.getRawY() - mDY)
                            .setDuration(0)
                            .start();
                    super.onTouchEvent(ev);
                    return true;
            }
            return super.onTouchEvent(ev);
        }
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    void changeViewType(@VideoView.ViewType int viewType) {
        mVideoView.setViewType(viewType);
        String type = getViewTypeString(viewType);
        Toast.makeText(this, "switching to " + type, Toast.LENGTH_SHORT).show();
        setTitle(type);
    }

    private String getViewTypeString(@VideoView.ViewType int viewType) {
        if (viewType == VideoView.VIEW_TYPE_SURFACEVIEW) {
            return "SurfaceView";
        } else if (viewType == VideoView.VIEW_TYPE_TEXTUREVIEW) {
            return "TextureView";
        }
        return "Unknown";
    }

}
