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
import android.content.IntentFilter;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.graphics.drawable.Icon;
import android.os.Bundle;
import android.service.chooser.ChooserTarget;
import android.service.chooser.ChooserTargetService;

import androidx.core.app.Person;

import java.util.ArrayList;
import java.util.List;

/**
 * Provides the Direct Share items to the system.
 */
public class ChooserTargetServiceCompat extends ChooserTargetService {

    @Override
    public List<ChooserTarget> onGetChooserTargets(ComponentName targetActivityName,
                                                   IntentFilter matchedFilter) {

        ShortcutManager shortcutManager = getSystemService(ShortcutManager.class);
        List<ShortcutInfo> shortcuts = shortcutManager.getDynamicShortcuts();
        ArrayList<ChooserTarget> targets = new ArrayList<>();
        for (ShortcutInfo shortcut : shortcuts) {
            if (ShortcutInfoCompat.getLongLived(shortcut)) {
                Bundle extras = new Bundle();
                Person[] persons = ShortcutInfoCompat.getPersons(shortcut);
                if (persons != null) {
                    extras.putBundle("EXTRA_PERSON", persons[0].toBundle());
                }
                targets.add(new ChooserTarget(
                        // The name of this target.
                        shortcut.getShortLabel(),
                        // The icon to represent this target.
                        null,//Icon.createWithResource(this, ),
                        // The ranking score for this target (0.0-1.0); the system will omit items with
                        // low scores when there are too many Direct Share items.
                        0.5f,
                        // The name of the component to be launched if this target is chosen.
                        shortcut.getActivity(),
                        // The extra values here will be merged into the Intent when this target is
                        // chosen.
                        extras));
            }
        }
        return targets;
    }

}
