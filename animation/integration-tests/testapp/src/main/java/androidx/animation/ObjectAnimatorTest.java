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

package androidx.animation;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertTrue;

import android.graphics.Color;
import android.graphics.Path;
import android.graphics.PointF;

import androidx.test.annotation.UiThreadTest;
import androidx.test.filters.MediumTest;
import androidx.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

@MediumTest
@RunWith(AndroidJUnit4.class)
public class ObjectAnimatorTest {
    private static final float LINE1_START = -32f;
    private static final float LINE1_END = -2f;
    private static final float LINE1_Y = 0f;
    private static final float LINE2_START = 2f;
    private static final float LINE2_END = 12f;

    private float mPreviousDurationScale = 1.0f;
    private static final float EPSILON = 0.001f;

    @Before
    public void setup() {
        mPreviousDurationScale = ValueAnimator.getDurationScale();
        ValueAnimator.setDurationScale(1.0f);
    }

    @After
    public void tearDown() {
        ValueAnimator.setDurationScale(mPreviousDurationScale);
    }

    @ClassRule
    public static AnimationTestRule sTestAnimationHandler = new AnimationTestRule();

    class AnimObject {
        public int x = 0;
        public float y = 0;
        public Integer color = null;

        public float posX = 0f;
        public float posY = 0f;

        public void setX(int val) {
            x = val;
        }

        public void setY(float val) {
            y = val;
        }

        public float getY() {
            return y;
        }

        public void setColor(Integer colorVal) {
            color = colorVal;
        }
    }

    @UiThreadTest
    @Test
    public void testDuration() {
        final long duration = 2000;
        AnimObject obj = new AnimObject();
        ObjectAnimator objectAnimator = ObjectAnimator.ofInt(obj, "x", 1, 5);
        objectAnimator.setDuration(duration);

        objectAnimator.start();
        assertEquals(1, obj.x);
        assertTrue(objectAnimator.isRunning());

        sTestAnimationHandler.advanceTimeBy(1999);
        assertTrue(objectAnimator.isRunning());

        sTestAnimationHandler.advanceTimeBy(1);
        assertFalse(objectAnimator.isRunning());
    }

    @UiThreadTest
    @Test
    public void testOfFloat() {

        AnimObject object = new AnimObject();
        String property = "y";
        ObjectAnimator objAnimator = ObjectAnimator.ofFloat(object, property, 0f, 100f);
        objAnimator.setInterpolator(null);
        assertTrue(objAnimator != null);
        objAnimator.setDuration(100);
        objAnimator.setInterpolator(null);

        objAnimator.start();
        assertEquals(0f, object.y);

        for (long i = 1; i < objAnimator.getDuration(); i++) {
            // Advance time by 1ms at a time
            sTestAnimationHandler.advanceTimeBy(1);
            assertEquals((float) i, object.y, EPSILON);
        }
    }

    @UiThreadTest
    @Test
    public void testOfFloatBase() {
        AnimObject object = new AnimObject();
        String property = "y";
        ObjectAnimator animator = ObjectAnimator.ofFloat(object, property, 0f, 1f);
        ObjectAnimator objAnimator = new ObjectAnimator();
        objAnimator.setTarget(object);
        objAnimator.setPropertyName(property);
        assertEquals(animator.getTarget(), objAnimator.getTarget());
        assertEquals(animator.getPropertyName(), objAnimator.getPropertyName());
    }

    @UiThreadTest
    @Test
    public void testOfInt() {
        AnimObject object = new AnimObject();
        String property = "x";

        final ObjectAnimator intAnimator = ObjectAnimator.ofInt(object, property, 100, 0);

        intAnimator.setDuration(100);
        intAnimator.setInterpolator(null);

        intAnimator.start();

        for (long i = 0; i <= intAnimator.getDuration(); i++) {
            // Advance time by 1ms at a time
            assertEquals((int) i, 100 - object.x);
            sTestAnimationHandler.advanceTimeBy(1);
        }
    }

    @UiThreadTest
    @Test
    public void testOfObject() {
        AnimObject object = new AnimObject();
        String property = "color";
        int startColor = 0xFFFF8080;
        int endColor = 0xFF8080FF;

        Object[] values = {new Integer(startColor), new Integer(endColor)};
        ArgbEvaluator evaluator = new ArgbEvaluator();
        final ObjectAnimator colorAnimator = ObjectAnimator.ofObject(object, property,
                evaluator, values);

        colorAnimator.setDuration(100);
        colorAnimator.setInterpolator(null);
        colorAnimator.start();

        for (int i = 0; i <= 100; i++) {

            int interpolatedColor = (int) evaluator.evaluate(i / 100f, startColor, endColor);
            assertEquals(interpolatedColor, (int) object.color);

            // Check that channel is interpolated separately.
            assertEquals(0xFF, Color.alpha(object.color));
            assertEquals(0x80, Color.green(object.color));
            sTestAnimationHandler.advanceTimeBy(1);
        }

    }

    @UiThreadTest
    @Test
    public void testOfPropertyValuesHolder() {
        AnimObject object = new AnimObject();
        String propertyName = "x";
        int startValue = 200;
        int endValue = 0;
        int[] values = {startValue, endValue};
        PropertyValuesHolder propertyValuesHolder = PropertyValuesHolder.ofInt(
                propertyName, values);
        final ObjectAnimator intAnimator = ObjectAnimator.ofPropertyValuesHolder(object,
                propertyValuesHolder);

        intAnimator.setDuration(200);
        intAnimator.setInterpolator(null);
        intAnimator.start();

        for (int i = 0; i <= 200; i++) {
            assertEquals(i, 200 - object.x);
            sTestAnimationHandler.advanceTimeBy(1);
        }

        assertFalse(intAnimator.isStarted());
    }

    @UiThreadTest
    @Test
    public void testOfArgb() {
        AnimObject object = new AnimObject();
        String property = "color";
        int start = 0xffff0000;
        int end = 0xff0000ff;

        final ObjectAnimator animator = ObjectAnimator.ofArgb(object, property, start, end);
        animator.setDuration(200);
        animator.setInterpolator(null);
        animator.start();

        ArgbEvaluator evaluator = new ArgbEvaluator();
        for (int i = 0; i <= 200; i++) {

            int interpolatedColor = (int) evaluator.evaluate(i / 200f, start, end);
            assertEquals(interpolatedColor, (int) object.color);

            // Check that channel is interpolated separately.
            assertEquals(0xFF, Color.alpha(object.color));
            sTestAnimationHandler.advanceTimeBy(1);
        }
    }

    @UiThreadTest
    @Test
    public void testNullObject() {
        final ObjectAnimator anim = ObjectAnimator.ofFloat(null, "dummyValue", 0f, 1f);
        anim.setDuration(300);
        anim.start();

        // Check that the animation didn't fail to start due to null target
        assertTrue(anim.isStarted());
        sTestAnimationHandler.advanceTimeBy(299);
        assertTrue(anim.isStarted());

        sTestAnimationHandler.advanceTimeBy(1);
        assertFalse(anim.isStarted());
    }

    @UiThreadTest
    @Test
    public void testGetPropertyName() {
        Object object = new Object();
        String propertyName = "backgroundColor";
        int startColor = Color.RED;
        int endColor = Color.BLUE;
        Object[] values = {new Integer(startColor), new Integer(endColor)};
        ArgbEvaluator evaluator = new ArgbEvaluator();
        ObjectAnimator colorAnimator = ObjectAnimator.ofObject(object, propertyName,
                evaluator, values);
        String actualPropertyName = colorAnimator.getPropertyName();
        assertEquals(propertyName, actualPropertyName);
    }

    @UiThreadTest
    @Test
    public void testSetFloatValues() {
        AnimObject object = new AnimObject();
        String property = "y";
        float startY = 0f;
        float endY = 2000f;
        float[] values = {startY, endY};
        ObjectAnimator objAnimator = new ObjectAnimator();
        objAnimator.setTarget(object);
        objAnimator.setPropertyName(property);
        objAnimator.setFloatValues(values);
        objAnimator.setDuration(2000);
        objAnimator.setInterpolator(new AccelerateInterpolator());
        objAnimator.start();

        for (int i = 0; i <= 2000; i += 100) {
            float fraction = i / 2000f;
            fraction = objAnimator.getInterpolator().getInterpolation(fraction);
            assertEquals(fraction * 2000, object.y);
            sTestAnimationHandler.advanceTimeBy(100);
        }

        assertFalse(objAnimator.isStarted());
    }

    @UiThreadTest
    @Test
    public void testGetTarget() {
        Object object = new Object();
        String propertyName = "backgroundColor";
        int startColor = Color.RED;
        int endColor = Color.BLUE;
        Object[] values = {new Integer(startColor), new Integer(endColor)};
        ArgbEvaluator evaluator = new ArgbEvaluator();
        ObjectAnimator colorAnimator = ObjectAnimator.ofObject(object, propertyName,
                evaluator, values);
        Object target = colorAnimator.getTarget();
        assertEquals(object, target);
    }

    @Test
    public void testClone() {
        Object object = new Object();
        String property = "y";
        float startY = 0f;
        float endY = 1000f;
        long duration = 200;
        TimeInterpolator interpolator = new AccelerateInterpolator();
        ObjectAnimator objAnimator = ObjectAnimator.ofFloat(object, property, startY, endY);
        objAnimator.setDuration(duration);
        objAnimator.setRepeatCount(ValueAnimator.INFINITE);
        objAnimator.setInterpolator(interpolator);
        objAnimator.setRepeatMode(ValueAnimator.REVERSE);
        ObjectAnimator cloneAnimator = objAnimator.clone();

        assertEquals(duration, cloneAnimator.getDuration());
        assertEquals(ValueAnimator.INFINITE, cloneAnimator.getRepeatCount());
        assertEquals(ValueAnimator.REVERSE, cloneAnimator.getRepeatMode());
        assertEquals(object, cloneAnimator.getTarget());
        assertEquals(property, cloneAnimator.getPropertyName());
        assertEquals(interpolator, cloneAnimator.getInterpolator());
    }

    @UiThreadTest
    @Test
    public void testOfFloat_Path() {
        // Test for ObjectAnimator.ofFloat(Object, String, String, Path)
        // Create a path that contains two disconnected line segments. Check that the animated
        // property x and property y always stay on the line segments.
        Path path = new Path();
        path.moveTo(LINE1_START, LINE1_Y);
        path.lineTo(LINE1_END, LINE1_Y);
        path.moveTo(LINE2_START, LINE2_START);
        path.lineTo(LINE2_END, LINE2_END);
        final double totalLength = (LINE1_END - LINE1_START) + Math.sqrt(
                (LINE2_END - LINE2_START) * (LINE2_END - LINE2_START)
                        + (LINE2_END - LINE2_START) * (LINE2_END - LINE2_START));
        final double firstSegEndFraction = (LINE1_END - LINE1_START) / totalLength;
        final float delta = 0.01f;

        class Object2D {
            public float positionX = 0;
            public float positionY = 0;
            public void setPositionX(float val) {
                positionX = val;
            }
            public void setPositionY(float val) {
                positionY = val;
            }
        }

        Object2D target = new Object2D();
        final ObjectAnimator anim = ObjectAnimator.ofFloat(target, "positionX", "positionY", path);
        anim.setDuration(200);
        // Linear interpolator
        anim.setInterpolator(null);

        anim.start();

        for (int i = 0; i < 200; i++) {
            float fraction = i / 200f;
            if (fraction <= firstSegEndFraction) {
                float x = (float) (LINE1_START + fraction
                        / firstSegEndFraction * (LINE1_END - LINE1_START));
                assertEquals(x, target.positionX, EPSILON);
                assertEquals(LINE1_Y, target.positionY, EPSILON);
            } else {
                float x = (float) (LINE2_START + (fraction - firstSegEndFraction)
                        / (1 - firstSegEndFraction) * (LINE2_END - LINE2_START));
                assertEquals(x, target.positionX, EPSILON);
                assertEquals(x, target.positionY, EPSILON);
            }
            sTestAnimationHandler.advanceTimeBy(1);
        }
    }

    @UiThreadTest
    @Test
    public void testOfInt_Path() {
        // Test for ObjectAnimator.ofInt(Object, String, String, Path)
        Path path = new Path();
        path.moveTo(100, -100);
        path.lineTo(0, 0);
        path.lineTo(100, 100);

        class Object2D {
            public int positionX = 0;
            public int positionY = 0;
            public void setPositionX(int val) {
                positionX = val;
            }
            public void setPositionY(int val) {
                positionY = val;
            }
        }

        Object2D target = new Object2D();
        final ObjectAnimator anim = ObjectAnimator.ofInt(target, "positionX", "positionY", path);
        anim.setDuration(200);
        // Linear interpolator
        anim.setInterpolator(null);

        anim.start();

        for (int i = 0; i < 200; i++) {
            if (i <= 100) {
                assertEquals(100 - i, target.positionX);
                assertEquals(i - 100, target.positionY);
            } else {
                assertEquals(i - 100, target.positionX);
                assertEquals(i - 100, target.positionY);
            }
            sTestAnimationHandler.advanceTimeBy(1);
        }

        assertFalse(anim.isStarted());
    }

    @UiThreadTest
    @Test
    public void testOfMultiFloat_Path() {
        // Test for ObjectAnimator.ofMultiFloat(Object, String, Path);
        Path path = new Path();
        path.moveTo(100, -100);
        path.lineTo(0, 0);
        path.lineTo(100, 100);

        class PositionF {
            public float x = 0f;
            public float y = 0f;

            public void setPosition(float posX, float posY) {
                x = posX;
                y = posY;
            }
        }

        PositionF target = new PositionF();
        final ObjectAnimator anim = ObjectAnimator.ofMultiFloat(target, "position", path);
        // Linear interpolator
        anim.setInterpolator(null);
        anim.setDuration(200);

        anim.start();

        for (int i = 0; i < 200; i++) {
            if (i <= 100) {
                assertEquals(100f - i, target.x, EPSILON);
                assertEquals(i - 100f, target.y, EPSILON);
            } else {
                assertEquals(i - 100f, target.x, EPSILON);
                assertEquals(i - 100f, target.y, EPSILON);
            }
            sTestAnimationHandler.advanceTimeBy(1);
        }

        assertFalse(anim.isStarted());
    }

    @UiThreadTest
    @Test
    public void testOfMultiFloat() {
        // Test for ObjectAnimator.ofMultiFloat(Object, String, float[][]);
        final float[][] data = new float[10][];
        for (int i = 0; i < data.length; i++) {
            data[i] = new float[3];
            data[i][0] = i;
            data[i][1] = i * 2;
            data[i][2] = 0f;
        }

        class Position3F {
            public float x, y, z;
            public void setPosition(float posX, float posY, float posZ) {
                x = posX;
                y = posY;
                z = posZ;
            }
        }

        Position3F target = new Position3F();
        final ObjectAnimator anim = ObjectAnimator.ofMultiFloat(target, "position", data);
        anim.setInterpolator(null);
        anim.setDuration(20);

        anim.start();

        for (int i = 0; i <= 20; i++) {
            float expectedX = i / 20f * (data.length - 1);

            assertEquals(expectedX, target.x, EPSILON);
            assertEquals(expectedX * 2, target.y, EPSILON);
            assertEquals(0f, target.z, 0.0f);

            sTestAnimationHandler.advanceTimeBy(1);
        }
    }

    @UiThreadTest
    @Test
    public void testOfMultiInt_Path() {
        // Test for ObjectAnimator.ofMultiInt(Object, String, Path);
        Path path = new Path();
        path.moveTo(100, -100);
        path.lineTo(0, 0);
        path.lineTo(100, 100);

        class Position {
            public int x = 0;
            public int y = 0;

            public void setPosition(int posX, int posY) {
                x = posX;
                y = posY;
            }
        }

        Position target = new Position();
        final ObjectAnimator anim = ObjectAnimator.ofMultiInt(target, "position", path);
        // Linear interpolator
        anim.setInterpolator(null);
        anim.setDuration(200);

        anim.start();

        for (int i = 0; i < 200; i++) {
            if (i <= 100) {
                assertEquals(100 - i, target.x);
                assertEquals(i - 100, target.y);
            } else {
                assertEquals(i - 100, target.x);
                assertEquals(i - 100, target.y);
            }
            sTestAnimationHandler.advanceTimeBy(1);
        }

        assertFalse(anim.isStarted());
    }

    @UiThreadTest
    @Test
    public void testOfMultiInt() {
        // Test for ObjectAnimator.ofMultiFloat(Object, String, int[][]);
        final int[][] data = new int[10][];
        for (int i = 0; i < data.length; i++) {
            data[i] = new int[3];
            data[i][0] = i;
            data[i][1] = i * 2;
            data[i][2] = 0;
        }

        class Position {
            public int x, y, z;
            public void setPosition(int posX, int posY, int posZ) {
                x = posX;
                y = posY;
                z = posZ;
            }
        }

        Position target = new Position();
        final ObjectAnimator anim = ObjectAnimator.ofMultiInt(target, "position", data);
        anim.setInterpolator(null);
        anim.setDuration(data.length - 1);

        anim.start();

        for (int i = 0; i <= data.length - 1; i++) {
            int expectedX = i;

            assertEquals(expectedX, target.x);
            assertEquals(expectedX * 2, target.y);
            assertEquals(0, target.z);

            sTestAnimationHandler.advanceTimeBy(1);
        }
    }

    @UiThreadTest
    @Test
    public void testOfObject_Converter() {
        // Test for ObjectAnimator.ofObject(Object, String, TypeConverter<T, V>, Path)
        // Create a path that contains two disconnected line segments. Check that the animated
        // property x and property y always stay on the line segments.
        Path path = new Path();
        path.moveTo(LINE1_START, -LINE1_START);
        path.lineTo(LINE1_END, -LINE1_END);
        path.moveTo(LINE2_START, LINE2_START);
        path.lineTo(LINE2_END, LINE2_END);

        Object target1 = new Object() {
            public void setDistance(float distance) {
            }
        };
        Object target2 = new Object() {
            public void setPosition(PointF pos) {
            }
        };
        TypeConverter<PointF, Float> converter = new TypeConverter<PointF, Float>(
                PointF.class, Float.class) {
            @Override
            public Float convert(PointF value) {
                return (float) Math.sqrt(value.x * value.x + value.y * value.y);
            }
        };

        // Create two animators. One use a converter that converts the point to distance to origin.
        // The other one does not have a type converter.
        final ObjectAnimator anim1 = ObjectAnimator.ofObject(target1, "distance", converter, path);
        anim1.setDuration(100);
        anim1.setInterpolator(null);

        final ObjectAnimator anim2 = ObjectAnimator.ofObject(target2, "position", null, path);
        anim2.setDuration(100);
        anim2.setInterpolator(null);

        anim1.start();
        anim2.start();

        for (int i = 0; i <= 100; i += 10) {
            float distance = (float) anim1.getAnimatedValue();
            PointF position = (PointF) anim2.getAnimatedValue();
            assertEquals(distance, Math.sqrt(position.x * position.x + position.y * position.y),
                    EPSILON);
            sTestAnimationHandler.advanceTimeBy(10);
        }
    }

    @UiThreadTest
    @Test
    public void testIsStarted() {
        AnimObject object = new AnimObject();
        Interpolator interpolator = new AccelerateInterpolator();
        ObjectAnimator objAnimator = ObjectAnimator.ofFloat(object, "y", 0f, 100f);
        objAnimator.setDuration(200);
        objAnimator.setInterpolator(interpolator);

        assertFalse(objAnimator.isStarted());

        objAnimator.start();
        assertTrue(objAnimator.isStarted());

        sTestAnimationHandler.advanceTimeBy(199);
        assertTrue(objAnimator.isStarted());

        sTestAnimationHandler.advanceTimeBy(1);
        assertFalse(objAnimator.isStarted());
    }

    @UiThreadTest
    @Test
    public void testSetStartEndValues() {
        final float startValue = 100, endValue = 500;
        AnimObject target = new AnimObject();
        final ObjectAnimator anim1 = ObjectAnimator.ofFloat(target, "y", 0);
        target.setY(startValue);
        anim1.setupStartValues();
        target.setY(endValue);
        anim1.setupEndValues();

        anim1.start();
        assertEquals(startValue, target.y, EPSILON);
        sTestAnimationHandler.advanceTimeBy(anim1.getDuration());
        assertEquals(endValue, target.y, EPSILON);
    }
}
