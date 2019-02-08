/*
 * Copyright 2019 The Android Open Source Project
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
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.media2.UriMediaItem;
import androidx.media2.widget.VideoView;
import androidx.media2.widget.VideoViewFullScreenActivity;

/**
 * Test application for VideoView/MediaControlView
 */
public class VideoPlayerFullScreenActivity extends VideoViewFullScreenActivity {
    VideoView mVideoView;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mVideoView = getVideoView();

        Intent intent = getIntent();
        Uri videoUri;
        if (intent == null || (videoUri = intent.getData()) == null || !videoUri.isAbsolute()) {
            showErrorDialog("Invalid intent");
        } else {
            UriMediaItem mediaItem = new UriMediaItem.Builder(this, videoUri).build();
            mVideoView.setMediaItem(mediaItem);
        }
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
}
