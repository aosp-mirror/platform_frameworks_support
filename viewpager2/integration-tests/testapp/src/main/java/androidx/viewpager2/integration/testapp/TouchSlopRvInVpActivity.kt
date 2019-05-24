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

package androidx.viewpager2.integration.testapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.Switch
import android.widget.TextView
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.VERTICAL
import androidx.viewpager2.widget.ViewPager2

class TouchSlopRvInVpActivity : FragmentActivity() {
    companion object {
        val colors = listOf(
            R.color.red_300, R.color.red_500,
            R.color.blue_300, R.color.blue_500,
            R.color.green_300, R.color.green_500,
            R.color.yellow_300, R.color.yellow_500,
            R.color.magnolia_100, R.color.purple_500
        )

        fun matchParent(): ViewGroup.LayoutParams {
            return ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT)
        }
    }

    private lateinit var vm: TouchSlopViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_touch_slop)
        vm = ViewModelProviders.of(this).get(TouchSlopViewModel::class.java)

        val viewPager: ViewPager2 = findViewById(R.id.view_pager)
        viewPager.adapter = VpWithRvsAdapter(vm)

        val parentSwitch: Switch = findViewById(R.id.use_paging_slop_parent)
        parentSwitch.isChecked = true // VP default is TOUCH_SLOP_PAGING
        TouchSlopController(viewPager, parentSwitch).setUp()

        vm.usePagingTouchSlopInChildren.value = false
        val childrenSwitch: Switch = findViewById(R.id.use_paging_slop_children)
        childrenSwitch.setOnCheckedChangeListener { _, isChecked ->
            vm.usePagingTouchSlopInChildren.value = isChecked
            viewPager.adapter!!.notifyDataSetChanged()
        }
    }

    class TouchSlopViewModel : ViewModel() {
        val usePagingTouchSlopInChildren = MutableLiveData<Boolean>()
    }

    private class VpWithRvsAdapter(val vm: TouchSlopViewModel) :
        RecyclerView.Adapter<VpViewHolder>() {

        override fun getItemCount(): Int {
            return 10
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VpViewHolder {
            val rv = RecyclerView(parent.context)
            rv.layoutParams = matchParent()
            rv.layoutManager = LinearLayoutManager(rv.context, VERTICAL, false)
            rv.adapter = RvAdapter()
            return VpViewHolder(rv)
        }

        override fun onBindViewHolder(holder: VpViewHolder, position: Int) {
            holder.recyclerView.setBackgroundResource(colors[position])
            holder.recyclerView.setScrollingTouchSlop(
                when (vm.usePagingTouchSlopInChildren.value!!) {
                    true -> RecyclerView.TOUCH_SLOP_PAGING
                    false -> RecyclerView.TOUCH_SLOP_DEFAULT
                }
            )
        }
    }

    private class VpViewHolder(val recyclerView: RecyclerView) :
        RecyclerView.ViewHolder(recyclerView)

    private class RvAdapter : RecyclerView.Adapter<RvViewHolder>() {
        override fun getItemCount(): Int {
            return 50
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RvViewHolder {
            val inflater = LayoutInflater.from(parent.context)
            val view = inflater.inflate(android.R.layout.simple_list_item_1, parent, false)
            return RvViewHolder(view)
        }

        override fun onBindViewHolder(holder: RvViewHolder, position: Int) {
            holder.textView.text = "Position $position"
        }
    }

    private class RvViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textView: TextView = itemView.findViewById(android.R.id.text1)
    }
}
