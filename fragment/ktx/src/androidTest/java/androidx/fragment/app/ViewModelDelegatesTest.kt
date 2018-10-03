package androidx.fragment.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.test.annotation.UiThreadTest
import androidx.test.filters.MediumTest
import androidx.test.rule.ActivityTestRule
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test

@MediumTest
class ViewModelDelegatesTest {
    @get:Rule val activityRule = ActivityTestRule<TestVMActivity>(TestVMActivity::class.java)

    @UiThreadTest
    @Test fun vmInitialization() {
        val activity = activityRule.activity
        assertThat(activity.vm).isNotNull()
        assertThat(activity.factoryVM.prop).isEqualTo("activity")
        val fragment = TestVMFragment()
        activity.supportFragmentManager.commitNow { add(fragment, "tag") }
        assertThat(fragment.vm).isNotNull()
        assertThat(fragment.factoryVM.prop).isEqualTo("fragment")
    }
}

class TestVMFragment : Fragment() {
    val vm: TestViewModel by viewmodelDelegate()
    val factoryVM: TestFactorizedViewModel by viewmodelDelegate(TestVMFactory("fragment"))
}

class TestVMActivity : FragmentActivity() {
    val vm: TestViewModel by viewmodelDelegate()
    val factoryVM: TestFactorizedViewModel by viewmodelDelegate(TestVMFactory("activity"))
}

class TestViewModel : ViewModel()

class TestFactorizedViewModel(val prop: String) : ViewModel()

class TestVMFactory(val prop: String) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass != TestFactorizedViewModel::class.java) {
            throw IllegalArgumentException()
        }
        return TestFactorizedViewModel(prop) as T
    }
}