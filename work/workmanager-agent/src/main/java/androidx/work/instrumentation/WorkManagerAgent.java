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

package androidx.work.instrumentation;

import android.os.Handler;
import android.os.Looper;
import android.util.JsonReader;

import androidx.instrumentation.Agent;
import androidx.instrumentation.Connection;
import androidx.lifecycle.Observer;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;
import androidx.work.impl.WorkManagerImpl;
import androidx.work.impl.model.WorkSpec;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.StringReader;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class WorkManagerAgent extends Agent implements Observer<List<WorkInfo>> {

    private final WorkManagerImpl mWorkManager;

    private final Map<UUID, WorkInfo> knownWorkers = new HashMap<>();

    private final ExecutorService mService = Executors.newSingleThreadExecutor();

    public WorkManagerAgent(Connection connection) {
        super(connection);
        System.out.println("!!!! Ax Workmanager agent created");
        sendEvent("first_test", "some data 2");
        mWorkManager = WorkManagerImpl.getInstance();
    }

    @Override
    public void onEnable(Agent.EnableResult enableResult) {
        System.out.println("!!! ax worker manager onEable");
        enableResult.enabled();
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                mWorkManager.getAllWorkInfos().observeForever(WorkManagerAgent.this);
            }
        });
    }

    @Override
    public void handleEvent(String eventName, String data) {

    }

    @Override
    public void handleCommand(byte[] rawCommand, int commandId) {
        String text = new String(rawCommand, Charset.forName("UTF-8"));
        try {
            JSONObject object = new JSONObject(text);
            String command = object.getString("command");
            JSONObject data = object.getJSONObject("data");
            String workerId = data.getString("workerId");
            System.out.println("!!!!!!!!! Ax WorkManager parsed 23 :"  + command + ", workerId" + workerId);
            mWorkManager.getProcessor().startWork(workerId);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onChanged(List<WorkInfo> workInfos) {
        for (final WorkInfo info: workInfos) {
            WorkInfo old = knownWorkers.get(info.getId());
            knownWorkers.put(info.getId(), info);
            if (!info.equals(old)) {
                mService.submit(new Runnable() {
                    @Override
                    public void run() {
                        sendWorkerUpdate(info);
                    }
                });
            }
        }
    }

    private void sendWorkerUpdate(WorkInfo info) {
        JSONObject jsonInfo = json(info);
        if (jsonInfo == null) {
            return;
        }
        sendEvent("workers_update", jsonInfo.toString());
    }

    private static JSONObject json(WorkInfo info) {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("id", info.getId());
            jsonObject.put("state", info.getState());
            return jsonObject;
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }
}
