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

import androidx.inspection.agent.Agent;
import androidx.inspection.agent.AgentFactory;
import androidx.inspection.agent.Connection;

public class WorkManagerAgentFactory implements AgentFactory {

    public WorkManagerAgentFactory() {
        System.out.println("!!!!!!! Ax constructed WorkManagerAgentFactory.WorkManagerAgentFactory");
    }

    @Override
    public Agent createAgent(Connection connection) {
        return new WorkManagerAgent(connection);
    }
}
