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

package androidx.ads.identifier.testapp;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.View;
import android.widget.TextView;

import androidx.ads.identifier.AdvertisingIdClient;
import androidx.ads.identifier.AdvertisingIdInfo;

import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Simple activity as an ads identifier developer.
 */
public class AdsIdentifierActivity extends Activity {

    private static final String GET_AD_ID_ACTION = "androidx.ads.identifier.provider.GET_AD_ID";
    private static final String HIGH_PRIORITY_PERMISSION =
            "androidx.ads.identifier.provider.HIGH_PRIORITY";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ads_identifier);
    }

    /** Gets Ad ID. */
    public void getId(View view) {
        TextView textView = findViewById(R.id.text);
        AsyncTask.execute(() -> {
            AdvertisingIdInfo adInfo;
            try {
                adInfo = AdvertisingIdClient.getAdvertisingIdInfo(getApplicationContext());
            } catch (final Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> textView.setText(e.toString()));
                return;
            }
            runOnUiThread(() -> textView.setText(adInfo.toString()));
        });
    }

    /** Checks is provider available. */
    public void isProviderAvailable(View view) {
        TextView textView = findViewById(R.id.text);
        boolean isAvailable = AdvertisingIdClient.isAdvertisingIdProviderAvailable(this);
        textView.setText(String.valueOf(isAvailable));
    }

    /** Lists all the providers. */
    public void listProvider(View view) {
        TextView textView = findViewById(R.id.text);
        textView.setText("Services:\n");

        Intent intent = new Intent(GET_AD_ID_ACTION);
        List<ResolveInfo> resolveInfos = getPackageManager().queryIntentServices(intent, 0);
        if (resolveInfos != null && !resolveInfos.isEmpty()) {
            for (ResolveInfo resolveInfo : resolveInfos) {
                String packageName = resolveInfo.serviceInfo.packageName;
                PackageInfo packageInfo;
                try {
                    packageInfo = getPackageManager().getPackageInfo(packageName,
                            PackageManager.GET_PERMISSIONS);
                } catch (PackageManager.NameNotFoundException e) {
                    continue;
                }
                show(textView, packageInfo);
            }
        }
    }

    private void show(TextView textView, PackageInfo packageInfo) {
        textView.append(String.format(Locale.US, "%s\nFLAG_SYSTEM:%d\n",
                packageInfo.packageName,
                packageInfo.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM));
        textView.append(String.format(Locale.US, "isRequestHighPriority:%s\n",
                isRequestHighPriority(packageInfo.requestedPermissions)));
        textView.append(String.format(Locale.US, "firstInstallTime:%s\n",
                DateFormat.format("yyyy-MM-dd HH:mm:ss", new Date(packageInfo.firstInstallTime))));
        textView.append("\n");
    }

    private static boolean isRequestHighPriority(String[] array) {
        if (array == null) {
            return false;
        }
        for (String permission : array) {
            if (HIGH_PRIORITY_PERMISSION.equals(permission)) {
                return true;
            }
        }
        return false;
    }
}
