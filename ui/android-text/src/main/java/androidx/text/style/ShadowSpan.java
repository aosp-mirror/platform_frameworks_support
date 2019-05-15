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

package androidx.text.style;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;

import android.text.TextPaint;
import android.text.style.CharacterStyle;

import androidx.annotation.ColorInt;
import androidx.annotation.RestrictTo;

/**
 * A span which applies a shadow effect to the covered text.
 * @hide
 */
@RestrictTo(LIBRARY_GROUP)
public class ShadowSpan extends CharacterStyle {
    private final int mColor;
    private final float mOffsetX;
    private final float mOffsetY;
    private final float mRadius;

    public ShadowSpan(@ColorInt int color, float offsetX, float offsetY, float radius) {
        mColor = color;
        mOffsetX = offsetX;
        mOffsetY = offsetY;
        mRadius = radius;
    }

    @Override
    public void updateDrawState(TextPaint tp) {
        tp.setShadowLayer(mRadius, mOffsetX, mOffsetY, mColor);
    }

    public int getColor() {
        return mColor;
    }

    public float getOffsetX() {
        return mOffsetX;
    }

    public float getOffsetY() {
        return mOffsetY;
    }

    public float getRadius() {
        return mRadius;
    }
}
