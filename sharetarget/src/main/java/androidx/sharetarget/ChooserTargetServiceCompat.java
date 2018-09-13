/*
 * Copyright (C) 2018 The Android Open Source Project
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

package androidx.sharetarget;

import android.content.ComponentName;
import android.content.Context;
import android.content.IntentFilter;
import android.os.Bundle;
import android.service.chooser.ChooserTarget;
import android.service.chooser.ChooserTargetService;

import androidx.core.content.pm.ShortcutInfoCompat;
import androidx.core.content.pm.ShortcutInfoCompatSaver;
import androidx.core.graphics.drawable.IconCompat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Provides direct share items to the system, by cross checking dynamic shortcuts from
 * ShortcutManagerCompat and share target definitions from a Xml resource. Used for backward
 * compatibility to push share targets to shortcut manager on older SDKs.
 */
public class ChooserTargetServiceCompat extends ChooserTargetService {
    @Override
    public List<ChooserTarget> onGetChooserTargets(ComponentName targetActivityName,
            IntentFilter matchedFilter) {
        try {
            Context context = getApplicationContext();
            ShortcutInfoCompatSaver shortcutSaver = ShortcutInfoCompatSaver.getInstance(context);

            List<ShortcutInfoCompat> shortcuts = shortcutSaver.getShortcuts();
            List<ShareTargetCompat> targets = ShareTargetXmlParser.getShareTargets(context);

            ArrayList<ChooserTarget> chooserTargets = new ArrayList<>();
            for (ShareTargetCompat target : targets) {
                // TODO: match other fileds such as data URI and category
                boolean matchedDataType = false;
                for (ShareTargetCompat.TargetData data : target.mTargetData) {
                    if (matchedFilter.hasDataType(data.mMimeType)) {
                        matchedDataType = true;
                        break;
                    }
                }
                if (!matchedDataType) {
                    continue;
                }
                for (ShortcutInfoCompat shortcut : shortcuts) {
                    if (!shortcut.getCategories().containsAll(Arrays.asList(target.mCategories))) {
                        continue;
                    }

                    // Category and Data matched, add as a ChooserTarget
                    IconCompat icon = shortcutSaver.getShortcutIcon(shortcut.getId());
                    Bundle extras = new Bundle();
                    extras.putCharSequence("EXTRA_SHORTCUT_ID", shortcut.getId());

                    chooserTargets.add(new ChooserTarget(
                            // The name of this target.
                            shortcut.getShortLabel(),
                            // The icon to represent this target.
                            icon != null ? icon.toIcon() : null,
                            // The ranking score for this target (0.0-1.0); the system will omit
                            // items
                            // with low scores when there are too many Direct Share items.
                            0.5f,
                            // The name of the component to be launched if this target is chosen.
                            new ComponentName(context.getPackageName(), target.mTargetClass),
                            // The extra values here will be merged into the Intent when this
                            // target is
                            // chosen.
                            extras));
                }
            }
            return chooserTargets;
        } catch (Exception e) {
            throw new RuntimeException("Failed to get chooser targets", e);
        }
    }
}
