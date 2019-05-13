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

package androidx.work.tracing

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.os.Process
import android.util.JsonWriter
import android.util.Log
import androidx.work.Logger
import androidx.work.impl.WorkerWrapper
import androidx.work.impl.utils.CancelWorkRunnable
import androidx.work.impl.utils.EnqueueRunnable
import androidx.work.impl.utils.SerialExecutor
import androidx.work.impl.utils.StartWorkRunnable
import androidx.work.impl.utils.StopWorkRunnable
import java.io.OutputStreamWriter
import java.util.concurrent.Executor

class TracingExecutor(private val context: Context, private val executor: Executor) : Executor {

    companion object {
        private val TAG = Logger.tagWithPrefix("TracingExecutor")
    }

    private val events = mutableListOf<Traceable>()

    override fun execute(command: Runnable) {
        val trace = Runnable {
            val start = System.currentTimeMillis()
            command.run()
            val end = System.currentTimeMillis()
            val task = command as? SerialExecutor.Task

            when (val runnable = task?.runnable ?: command) {
                is WorkerWrapper -> {
                    val name = runnable.description
                    val pid = Process.myPid()
                    val tid = Process.myTid()
                    durationEvents(name, start, end, pid, tid)
                }

                is StartWorkRunnable -> {
                    val name = "startWork"
                    val pid = Process.myPid()
                    val tid = Process.myTid()
                    durationEvents(name, start, end, pid, tid)
                }

                is StopWorkRunnable -> {
                    val name = "stopWork"
                    val pid = Process.myPid()
                    val tid = Process.myTid()
                    durationEvents(name, start, end, pid, tid)
                }

                is CancelWorkRunnable -> {
                    val name = "cancelWork"
                    val pid = Process.myPid()
                    val tid = Process.myTid()
                    durationEvents(name, start, end, pid, tid)
                }

                is EnqueueRunnable -> {
                    val name = "enqueueWork"
                    val pid = Process.myPid()
                    val tid = Process.myTid()
                    durationEvents(name, start, end, pid, tid)
                }
            }

            val commandName = task?.runnable?.javaClass?.name ?: command.javaClass.name
            if (commandName.contains("Worker$1")) {
                val name = "Worker"
                val pid = Process.myPid()
                val tid = Process.myTid()
                durationEvents(name, start, end, pid, tid)
            }

            if (commandName.contains("ListenableWorker")) {
                val name = "ListenableWorker"
                val pid = Process.myPid()
                val tid = Process.myTid()
                durationEvents(name, start, end, pid, tid)
            }
        }
        executor.execute(trace)
    }

    fun writeEvents() {
        executor.execute {
            val file = context.openFileOutput("traces.json", MODE_PRIVATE)
            file.use {
                val writer = JsonWriter(OutputStreamWriter(it))
                writer.setIndent("  ") // set indentation to 2 spaces

                writer.beginArray()
                for (event in events) {
                    when (event) {
                        is InstantEvent -> event.write(writer)
                        is DurationEvent -> event.write(writer)
                    }
                }
                writer.endArray()
                writer.flush()
            }
            Log.d(TAG, "Completed writing events.")
        }
    }

    private fun durationEvents(name: String, start: Long, end: Long, pid: Int, tid: Int) {
        val beginEvent = DurationEvent(
            name = name,
            pid = pid,
            tid = tid,
            timestamp = start,
            eventType = DurationEventType.Begin
        )

        val endEvent = DurationEvent(
            name = name,
            pid = pid,
            tid = tid,
            timestamp = end,
            eventType = DurationEventType.End
        )
        events.add(beginEvent)
        events.add(endEvent)
    }
}
