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

package com.example.android.livedataplusplus

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProviders
import com.example.android.livedataplusplus.lib.LifecycleBoundDispatcher
import com.example.android.livedataplusplus.lib.lifecycleBoundContext
import com.example.android.livedataplusplus.lib.liveData
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    val pausingDispatcher = lifecycleBoundContext(this)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        ViewModelProviders.of(this)[MainViewModel::class.java].myLiveData.observe(this, Observer {
            cLog("live data value $it")

        })
//        GlobalScope.launch(pausingDispatcher) {
//            try {
//                addMessage("hello")
//                delay(10000)
//                addMessage("world")
//            } finally {
//                Log.d("CORO", "running finally block")
//            }
//
//        }
    }

    fun addMessage(msg  : String) {
        cLog("adding msg $msg")
        message.setText("${message.text}\n$msg")
    }
}


class MainViewModel : ViewModel() {
    val myLiveData = liveData<Int> {
        var num = 0
        while(waitUntilActive()) {
            cLog("CORO running loop")
            yield(num++)
            cLog("CORO yield completed")
            delay(1000)
            cLog("CORO delay completed")
        }
    }
}

fun cLog(msg : String) {
    Log.d("CORO", "${Thread.currentThread().name}-${System.currentTimeMillis()}: $msg")
}