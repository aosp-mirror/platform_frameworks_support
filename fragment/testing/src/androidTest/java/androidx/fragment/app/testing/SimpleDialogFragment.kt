package androidx.fragment.app.testing

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.fragment.testing.test.R
import androidx.lifecycle.Lifecycle.State.CREATED
import androidx.lifecycle.Lifecycle.State.DESTROYED
import androidx.lifecycle.Lifecycle.State.INITIALIZED
import androidx.lifecycle.Lifecycle.State.RESUMED
import androidx.lifecycle.Lifecycle.State.STARTED

/**
 * A dialog fragment with a button to demonstrate unit testing.
 */
class SimpleDialogFragment : DialogFragment() {

    var mCurrentState = INITIALIZED
        private set

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mCurrentState = CREATED
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_simple_dialog, container, false)

    override fun onStart() {
        super.onStart()
        mCurrentState = STARTED
    }

    override fun onResume() {
        super.onResume()
        mCurrentState = RESUMED
    }

    override fun onPause() {
        super.onPause()
        mCurrentState = STARTED
    }

    override fun onStop() {
        super.onStop()
        mCurrentState = CREATED
    }

    override fun onDestroy() {
        super.onDestroy()
        mCurrentState = DESTROYED
    }
}