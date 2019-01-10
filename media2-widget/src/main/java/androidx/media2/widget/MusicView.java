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

package androidx.media2.widget;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;

class MusicView extends ViewGroup {
    private MusicViewType mType = MusicViewType.EMBEDDED;
    private View mFullLandscapeView;
    private View mFullPortraitView;
    private View mEmbeddedView;

    MusicView(@NonNull Context context) {
        super(context);

        inflateLayout();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (!(MeasureSpec.getMode(widthMeasureSpec) == MeasureSpec.EXACTLY
                && MeasureSpec.getMode(heightMeasureSpec) == MeasureSpec.EXACTLY)) {
            throw new AssertionError("MusicView should be measured in MeasureSpec.EXACTLY");
        }

        final int width = MeasureSpec.getSize(widthMeasureSpec);
        final int height = MeasureSpec.getSize(heightMeasureSpec);

        if (width > height) {
            mType = MusicViewType.FULL_LANDSCAPE;
            mFullLandscapeView.measure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.AT_MOST),
                    MeasureSpec.makeMeasureSpec(height, MeasureSpec.AT_MOST));
            if (hasTooSmallMeasuredState(mFullLandscapeView)) {
                mType = MusicViewType.EMBEDDED;
            }
        } else {
            mType = MusicViewType.FULL_PORTRAIT;
            mFullPortraitView.measure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.AT_MOST),
                    MeasureSpec.makeMeasureSpec(height, MeasureSpec.UNSPECIFIED));
            if (hasTooSmallMeasuredState(mFullPortraitView)
                    || mFullPortraitView.getMeasuredHeight() > height) {
                mType = MusicViewType.EMBEDDED;
            }
        }

        if (mType == MusicViewType.EMBEDDED) {
            mEmbeddedView.measure(MeasureSpec.makeMeasureSpec(width / 2, MeasureSpec.AT_MOST),
                    MeasureSpec.makeMeasureSpec(height / 2, MeasureSpec.AT_MOST));
        }

        setMeasuredDimension(width, height);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        View view;
        if (mType == MusicViewType.FULL_LANDSCAPE) {
            view = mFullLandscapeView;
        } else if (mType == MusicViewType.FULL_PORTRAIT) {
            view = mFullPortraitView;
        } else {
            view = mEmbeddedView;
        }

        final int count = getChildCount();
        for (int i = 0; i < count; i++) {
            View child = getChildAt(i);
            if (child == view) {
                child.setVisibility(View.VISIBLE);
            } else {
                child.setVisibility(View.INVISIBLE);
            }
        }

        final int parentWidth = right - left;
        final int parentHeight = bottom - top;

        final int width = view.getMeasuredWidth();
        final int height = view.getMeasuredHeight();

        final int childLeft = (parentWidth - width) / 2;
        final int childTop = (parentHeight - height) / 2;

        view.layout(childLeft, childTop, childLeft + width, childTop + height);
    }

    void setAlbumDrawable(Drawable album) {
        int count = getChildCount();
        for (int i = 0; i < count; i++) {
            ImageView albumView = getChildAt(i).findViewById(R.id.album);
            if (albumView != null) {
                albumView.setImageDrawable(album);
            }
        }
    }

    void setTitleText(String title) {
        int count = getChildCount();
        for (int i = 0; i < count; i++) {
            TextView titleView = getChildAt(i).findViewById(R.id.title);
            if (titleView != null) {
                titleView.setText(title);
            }
        }
    }

    void setArtistText(String artist) {
        int count = getChildCount();
        for (int i = 0; i < count; i++) {
            TextView artistView = getChildAt(i).findViewById(R.id.artist);
            if (artistView != null) {
                artistView.setText(artist);
            }
        }
    }

    private void inflateLayout() {
        LayoutInflater inflater = (LayoutInflater) getContext()
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        mFullLandscapeView = inflater.inflate(R.layout.full_landscape_music, null);
        mFullPortraitView = inflater.inflate(R.layout.full_portrait_music, null);
        mEmbeddedView = inflater.inflate(R.layout.embedded_music, null);

        addView(mFullLandscapeView);
        addView(mFullPortraitView);
        addView(mEmbeddedView);
    }

    private static boolean hasTooSmallMeasuredState(@NonNull View view) {
        return ((view.getMeasuredWidthAndState() & MEASURED_STATE_TOO_SMALL)
                | (view.getMeasuredHeightAndState() & MEASURED_STATE_TOO_SMALL)) != 0;
    }

    private enum MusicViewType { FULL_LANDSCAPE, FULL_PORTRAIT, EMBEDDED }
}
