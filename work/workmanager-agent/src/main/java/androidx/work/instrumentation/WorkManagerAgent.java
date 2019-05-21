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

import androidx.inspection.agent.Agent;
import androidx.inspection.agent.CommandHandler;
import androidx.inspection.agent.Connection;
import androidx.inspection.workmanager.protobuf.InvalidProtocolBufferException;
import androidx.lifecycle.Observer;
import androidx.work.WorkInfo;
import androidx.work.impl.WorkManagerImpl;
import androidx.work.inspector.proto.WorkerCommand;
import androidx.work.inspector.proto.WorkerCommand.StartWorker;
import androidx.work.inspector.proto.WorkerEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class WorkManagerAgent extends Agent implements Observer<List<WorkInfo>>, CommandHandler {

    private final WorkManagerImpl mWorkManager;

    private final Map<UUID, WorkInfo> knownWorkers = new HashMap<>();

    private final ExecutorService mService = Executors.newSingleThreadExecutor();

    private final Connection mConnection;

    public WorkManagerAgent(Connection connection) {
        System.out.println("!!!! Ax Workmanager agent created");
        mWorkManager = WorkManagerImpl.getInstance();
        mConnection = connection;
        mConnection.setCommandHandler(this);
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
        WorkerEvent.StateUpdated event = WorkerEvent.StateUpdated.newBuilder()
                .setWorkerId(info.getId().toString())
                .setStatus(info.getState().toString()).build();
        mConnection.sendEvent(WorkerEvent.newBuilder().setStateUpdated(event).build().toByteArray());
    }


    private void onHandleCommand(WorkerCommand command, RawResponse response) {
        switch (command.getUnionCase()) {
            case START_WORKER:
                startWorker(command.getStartWorker());
            default:
                // TODO: shouldn't be a failure response
                throw new IllegalStateException("Unknown commands");
        }
    }

    private void startWorker(StartWorker command) {
        mWorkManager.getProcessor().startWork(command.getWorkerId());
    }

    @Override
    public void onCommand(byte[] rawCommand, RawResponse response) {
        WorkerCommand workerCommand;
        try {
            workerCommand = WorkerCommand.parseFrom(rawCommand);
        } catch (InvalidProtocolBufferException e) {
            // TODO: shouldn't be a failure response
            throw new IllegalStateException("Unknown commands");
        }
        onHandleCommand(workerCommand, response);
    }
}
