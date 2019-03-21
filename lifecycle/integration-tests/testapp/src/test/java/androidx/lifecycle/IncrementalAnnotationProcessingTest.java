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

package androidx.lifecycle;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.gradle.tooling.GradleConnector;
import org.gradle.tooling.ProjectConnection;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;


@RunWith(JUnit4.class)
public class IncrementalAnnotationProcessingTest {

    private File mProjectDir;
    private File mFooObserver;
    private File mFooAdapter;
    private File mBarAdapter;
    private File mFooProguard;
    private File mBarProguard;
    private ProjectConnection mConnection;

    @Before
    public void setup() throws URISyntaxException {
        mProjectDir = new File(System.getProperty("user.dir") + "/../test-projects/incap");
        File generatedSourceDir = new File(mProjectDir.toPath()
                + "/../../../../../../out/host/gradle/frameworks/support/lifecycle/"
                + "integration-tests/test-projects/incap/build/generated/source/apt/"
                + "debug/androidx/lifecycle/incap");
        File generatedProguardDir = new File(mProjectDir.toPath()
                + "/../../../../../../out/host/gradle/frameworks/support/lifecycle/"
                + "integration-tests/test-projects/incap/build/intermediates/javac/debug/"
                + "compileDebugJavaWithJavac/classes/META-INF/proguard");
        mFooObserver = new File(mProjectDir.toPath()
                + "/src/main/java//androidx/lifecycle/incap/FooObserver.java");
        mFooAdapter = new File(generatedSourceDir + "/FooObserver_LifecycleAdapter.java");
        mBarAdapter = new File(generatedSourceDir + "/BarObserver_LifecycleAdapter.java");
        mFooProguard = new File(generatedProguardDir + "/androidx.lifecycle.incap.FooObserver.pro");
        mBarProguard = new File(generatedProguardDir + "/androidx.lifecycle.incap.BarObserver.pro");

        mConnection = GradleConnector
                .newConnector()
                .useDistribution(new URI("https://services.gradle"
                        + ".org/distributions-snapshots/gradle-5.4-20190328132959+0000-all.zip"))
                .forProjectDirectory(mProjectDir)
                .connect();
    }

    @Test
    public void checkModifyOneSource() throws IOException {
        String search = "FooObserver_Log";
        String replace = "Modified_FooObserver_Log";

        try {
            mConnection.newBuild()
                    .forTasks("clean", "compileDebugJavaWithJavac")
                    .withArguments("--no-build-cache")
                    .run();

            FileTime fooAdapterFirstBuild = Files.getLastModifiedTime(mFooAdapter.toPath());
            FileTime barAdapterFirstBuild = Files.getLastModifiedTime(mBarAdapter.toPath());
            FileTime fooProguardFirstBuild = Files.getLastModifiedTime(mFooProguard.toPath());
            FileTime barProguardFirstBuild = Files.getLastModifiedTime(mBarProguard.toPath());

            searchAndReplace(mFooObserver.toPath(), search, replace);

            mConnection.newBuild()
                    .forTasks("compileDebugJavaWithJavac")
                    .withArguments("--no-build-cache")
                    .run();

            FileTime fooAdapterSecondBuild = Files.getLastModifiedTime(mFooAdapter.toPath());
            FileTime barAdapterSecondBuild = Files.getLastModifiedTime(mBarAdapter.toPath());
            FileTime fooProguardSecondBuild = Files.getLastModifiedTime(mFooProguard.toPath());
            FileTime barProguardSecondBuild = Files.getLastModifiedTime(mBarProguard.toPath());

            // FooObserver is recompiled and its proguard file is regenerated
            assertEquals(-1, fooAdapterFirstBuild.compareTo(fooAdapterSecondBuild));
            assertEquals(-1, fooProguardFirstBuild.compareTo(fooProguardSecondBuild));
            // BarObserver is not recompiled
            assertEquals(0, barAdapterFirstBuild.compareTo(barAdapterSecondBuild));
            assertEquals(0, barProguardFirstBuild.compareTo(barProguardSecondBuild));
        } finally {
            mConnection.close();
            searchAndReplace(mFooObserver.toPath(), replace, search);
        }
    }


    @Test
    public void checkDeleteOneSource() throws IOException {
        String fooObserverContent = new String(Files.readAllBytes(mFooObserver.toPath()));

        try {
            mConnection.newBuild()
                    .forTasks("clean", "compileDebugJavaWithJavac")
                    .withArguments("--no-build-cache")
                    .run();

            FileTime barAdapterFirstBuild = Files.getLastModifiedTime(mBarAdapter.toPath());
            FileTime barProguardFirstBuild = Files.getLastModifiedTime(mBarProguard.toPath());

            mFooObserver.delete();

            mConnection.newBuild()
                    .forTasks("compileDebugJavaWithJavac")
                    .withArguments("--no-build-cache")
                    .run();

            FileTime barAdapterSecondBuild = Files.getLastModifiedTime(mBarAdapter.toPath());
            FileTime barProguardSecondBuild = Files.getLastModifiedTime(mBarProguard.toPath());

            // FooAdapter and FooProguard is deleted since FooObserver is removed
            assertFalse(mFooAdapter.exists());
            assertFalse(mFooProguard.exists());
            // BarObserver is not recompiled
            assertEquals(0, barAdapterFirstBuild.compareTo(barAdapterSecondBuild));
            assertEquals(0, barProguardFirstBuild.compareTo(barProguardSecondBuild));
        } finally {
            mConnection.close();
            Files.write(mFooObserver.toPath(), fooObserverContent.getBytes());
        }
    }

    private void searchAndReplace(Path file, String search, String replace) throws IOException {
        String content = new String(Files.readAllBytes(file));
        String newContent = content.replace(search, replace);
        Files.write(file, newContent.getBytes());
    }
}
