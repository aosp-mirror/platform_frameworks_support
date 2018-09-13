/**
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

package androidx.core.content.pm;

import android.content.ComponentName;
import android.content.Context;
import android.content.IntentFilter;
import android.graphics.drawable.Icon;
import android.os.Bundle;
import android.service.chooser.ChooserTarget;
import android.service.chooser.ChooserTargetService;

import androidx.core.app.Person;
import androidx.core.graphics.drawable.IconCompat;

import java.util.ArrayList;
import java.util.List;

/**
 * Provides the Direct Share items to the system, by cross checking Dynamic shortcuts from
 * ShortcutManager and share-targets defined in the App's manifest file.
 */
// TODO: Choose a better name, since this is not a compatibility version for ChooserTargetService
public class ChooserTargetServiceCompat extends ChooserTargetService {
    @Override
    // TODO: CRITICAL All the access to shortcuts must be thread safe
    public List<ChooserTarget> onGetChooserTargets(ComponentName targetActivityName,
            IntentFilter matchedFilter) {
        System.out.println("METT: onGetChooserTargets targetActivityName="
                + targetActivityName.flattenToString()
                + " matchedFilter=" + matchedFilter.toString());

        Context context = getApplicationContext();
        ShortcutInfoCompatSaver shortcutSaver = ShortcutInfoCompatSaver.getInstance(context);
        List<ShortcutInfoCompat> shortcuts = shortcutSaver.getShortcuts();
        List<ShareTargetCompat> targets = XmlShareTargetParser.parseShareTargets(context);

        ArrayList<ChooserTarget> chooserTargets = new ArrayList<>();
        for (ShareTargetCompat target : targets) {
            if (!matchedFilter.hasAction(target.mIntent.getAction())) {
                //    continue;
            }
            for (ShortcutInfoCompat shortcut : shortcuts) {
                if (!shortcut.mCategories.contains(target.mCategory)) {
                    continue;
                }
                // Intent and Category matched, add as a ChooserTarget
                System.out.println("METT: adding ChooserTarget, label=" + shortcut.getShortLabel()
                        + " component=" + target.mIntent.getComponent());

                Bundle extras = new Bundle();
                if (shortcut.mPersons.length > 0) {
                    extras.putBundle("EXTRA_PERSON", shortcut.mPersons[0].toBundle());
                }

                chooserTargets.add(new ChooserTarget(
                        // The name of this target.
                        shortcut.getShortLabel(),
                        // The icon to represent this target.
                        getIcon(shortcut, shortcutSaver),
                        // The ranking score for this target (0.0-1.0); the system will omit items
                        // with low scores when there are too many Direct Share items.
                        0.5f,
                        // The name of the component to be launched if this target is chosen.
                        target.mIntent.getComponent(),
                        // The extra values here will be merged into the Intent when this
                        // target is
                        // chosen.
                        extras));
            }
        }
        return chooserTargets;
    }

    private Icon getIcon(ShortcutInfoCompat shortcut, ShortcutInfoCompatSaver shortcutSaver) {
        IconCompat icon = shortcut.mIcon;
        if (icon == null) {
            icon = shortcutSaver.getShortcutIcon(shortcut.getId());
        }
        if (icon != null) {
            return icon.toIcon();
        }
        return null;
    }
}
