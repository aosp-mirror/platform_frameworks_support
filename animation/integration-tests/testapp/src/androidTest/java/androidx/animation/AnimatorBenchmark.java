package androidx.animation;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import androidx.benchmark.BenchmarkRule;
import androidx.benchmark.BenchmarkState;
import androidx.test.annotation.UiThreadTest;
import androidx.test.filters.LargeTest;
import androidx.test.runner.AndroidJUnit4;

import static junit.framework.TestCase.assertTrue;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class AnimatorBenchmark {
    @Rule
    public BenchmarkRule mRule = new BenchmarkRule();

    @ClassRule
    public static AnimationTestRule animationRule = new AnimationTestRule();

    /**
     * Tests that ObjectAnimator created using ObjectAnimator.ofFloat animates and interpolates
     * correctly between the start and end values defined in the method.
     */
    @UiThreadTest
    @Test
    public void testOfFloat() {
        class AnimObject {
            public float x = 1f;
            public void setX(float x) {
                this.x = x;
            }

            public float getX() {
                return x;
            }

        }
        AnimObject obj = new AnimObject();
        ObjectAnimator animator = ObjectAnimator.ofFloat(obj, "x", 0f, 20f, 100f);
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.start();

        BenchmarkState state = mRule.getState();
        int iter = 0;
        while (state.keepRunning()) {
            animator.animateValue(iter / 100f);
            iter = (iter + 1) % 100;
        }
    }

}
