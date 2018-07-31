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

package androidx.textclassifier;

import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.UserManager;
import android.provider.Browser;
import android.provider.ContactsContract;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.URLSpan;
import android.text.util.Linkify;
import android.util.Patterns;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.VisibleForTesting;
import androidx.annotation.WorkerThread;
import androidx.core.app.RemoteActionCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.IconCompat;
import androidx.core.text.util.LinkifyCompat;
import androidx.core.util.Preconditions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Provides limited text classifier feature by using the legacy {@link LinkifyCompat} API.
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
final class LegacyTextClassifier extends TextClassifier {
    private static final List<String> DEFAULT_ENTITY_TYPES = Collections.unmodifiableList(
            Arrays.asList(TextClassifier.TYPE_URL,
                    TextClassifier.TYPE_EMAIL,
                    TextClassifier.TYPE_PHONE));

    private static final int NOT_LINKIFY = 0;

    public static LegacyTextClassifier sInstance;

    private final MatchMaker mMatchMaker;

    @VisibleForTesting()
    LegacyTextClassifier(MatchMaker matchMaker) {
        super(SessionStrategy.NO_OP);
        mMatchMaker = Preconditions.checkNotNull(matchMaker);
    }

    public static LegacyTextClassifier of(Context context) {
        if (sInstance == null) {
            sInstance = new LegacyTextClassifier(
                    new MatchMakerImpl(context.getApplicationContext()));
        }
        return sInstance;
    }

    @WorkerThread
    @Override
    @NonNull
    /** @inheritDoc */
    public TextClassification classifyText(@NonNull TextClassification.Request request) {
        final String requestText = request.getText().toString();
        if (requestText.matches(Patterns.WEB_URL.pattern())) {
            return createTextClassification(requestText, TextClassifier.TYPE_URL);
        } else if (requestText.matches(Patterns.EMAIL_ADDRESS.pattern())) {
            return createTextClassification(requestText, TextClassifier.TYPE_EMAIL);
        } else if (requestText.matches(Patterns.PHONE.pattern())) {
            return createTextClassification(requestText, TextClassifier.TYPE_PHONE);
        } else {
            return TextClassification.EMPTY;
        }
    }

    private TextClassification createTextClassification(
            String text, @EntityType String entityType) {
        final TextClassification.Builder builder = new TextClassification.Builder()
                .setText(text)
                .setEntityType(entityType, 1f);
        for (RemoteActionCompat action : mMatchMaker.getActions(entityType, text)) {
            builder.addAction(action);
        }
        return builder.build();
    }

    @WorkerThread
    @Override
    @NonNull
    /** @inheritDoc */
    public TextLinks generateLinks(@NonNull TextLinks.Request request) {
        final Collection<String> entityTypes = request.getEntityConfig()
                .resolveEntityTypes(DEFAULT_ENTITY_TYPES);
        final String requestText = request.getText().toString();
        final TextLinks.Builder builder = new TextLinks.Builder(requestText);
        for (String entityType : entityTypes) {
            addLinks(builder, requestText, entityType);
        }
        return builder.build();
    }

    private static void addLinks(
            TextLinks.Builder builder, String string, @EntityType String entityType) {
        final int linkifyMask = entityTypeToLinkifyMask(entityType);
        if (linkifyMask == NOT_LINKIFY) {
            return;
        }

        final Spannable spannable = new SpannableString(string);
        if (LinkifyCompat.addLinks(spannable, linkifyMask)) {
            final URLSpan[] spans = spannable.getSpans(0, spannable.length(), URLSpan.class);
            for (URLSpan urlSpan : spans) {
                builder.addLink(
                        spannable.getSpanStart(urlSpan),
                        spannable.getSpanEnd(urlSpan),
                        Collections.singletonMap(entityType, 1.0f),
                        urlSpan);
            }
        }
    }

    @LinkifyCompat.LinkifyMask
    private static int entityTypeToLinkifyMask(@EntityType String entityType) {
        switch (entityType) {
            case TextClassifier.TYPE_URL:
                return Linkify.WEB_URLS;
            case TextClassifier.TYPE_PHONE:
                return Linkify.PHONE_NUMBERS;
            case TextClassifier.TYPE_EMAIL:
                return Linkify.EMAIL_ADDRESSES;
            default:
                // NOTE: Do not support MAP_ADDRESSES. Legacy version does not work well.
                return NOT_LINKIFY;
        }
    }

    /**
     * Returns actions for a specified entity type.
     */
    @VisibleForTesting()
    interface MatchMaker {
        /**
         * Returns an ordered list of actions for the specified entityType. Clients should expect
         * that the actions will be ordered based on how important the matchmaker thinks the action
         * is to the current task.
         */
        List<RemoteActionCompat> getActions(@EntityType String entityType, String text);
    }

    /**
     * Default MatchMaker implementation for the LegacyTextClassifier.
     */
    // TODO: Write unit tests for MatchMakerImpl.
    // Will involve faking/mocking out system internals such as context, package manager, etc.
    private static final class MatchMakerImpl implements MatchMaker {

        // RemoteAction requires that there be an icon.
        // Use this when no icon is required. Use with RemoteAction.setShouldShowIcon(false).
        private static final IconCompat NO_ICON = IconCompat.createWithData(new byte[0], 0, 0);
        private final Context mContext;

        MatchMakerImpl(Context context) {
            mContext = Preconditions.checkNotNull(context);
        }

        @Override
        public List<RemoteActionCompat> getActions(String entityType, String text) {
            switch (entityType) {
                case TextClassifier.TYPE_URL:
                    return createForUrl(text);
                case TextClassifier.TYPE_EMAIL:
                    return createForEmail(text);
                case TextClassifier.TYPE_PHONE:
                    return createForPhone(text);
                default:
                    return Collections.emptyList();
            }
        }

        private List<RemoteActionCompat> createForUrl(String text) {
            if (Uri.parse(text).getScheme() == null) {
                text = "http://" + text;
            }
            final RemoteActionCompat browserAction = createRemoteAction(
                    new Intent(Intent.ACTION_VIEW, Uri.parse(text))
                            .putExtra(Browser.EXTRA_APPLICATION_ID, mContext.getPackageName()),
                    "Url",
                    "Url description",
                    0);
            if (browserAction != null) {
                return Collections.unmodifiableList(Arrays.asList(browserAction));
            }
            return Collections.emptyList();
        }

        private List<RemoteActionCompat> createForEmail(String text) {
            final List<RemoteActionCompat> actions = new ArrayList<>();
            final RemoteActionCompat emailAction = createRemoteAction(
                    new Intent(Intent.ACTION_SENDTO)
                            .setData(Uri.parse(String.format("mailto:%s", text))),
                    "Email",
                    "Email description",
                    0);
            if (emailAction != null) {
                actions.add(emailAction);
            }
            final RemoteActionCompat contactsAction = createRemoteAction(
                    new Intent(Intent.ACTION_INSERT_OR_EDIT)
                            .setType(ContactsContract.Contacts.CONTENT_ITEM_TYPE)
                            .putExtra(ContactsContract.Intents.Insert.EMAIL, text),
                    "Add",
                    "Add description",
                    0);
            if (contactsAction != null) {
                actions.add(contactsAction);
            }
            return immutableList(actions);
        }

        private List<RemoteActionCompat> createForPhone(String text) {
            final List<RemoteActionCompat> actions = new ArrayList<>();
            final Object userManager = mContext.getSystemService(Context.USER_SERVICE);
            final Bundle userRestrictions = userManager instanceof UserManager
                    ? ((UserManager) userManager).getUserRestrictions() : new Bundle();
            if (!userRestrictions.getBoolean(UserManager.DISALLOW_OUTGOING_CALLS, false)) {
                final RemoteActionCompat dialAction = createRemoteAction(
                        new Intent(Intent.ACTION_DIAL)
                                .setData(Uri.parse(String.format("tel:%s", text))),
                        "Dial",
                        "Dial description",
                        0);
                if (dialAction != null) {
                    actions.add(dialAction);
                }
            }
            final RemoteActionCompat contactsAction = createRemoteAction(
                    new Intent(Intent.ACTION_INSERT_OR_EDIT)
                            .setType(ContactsContract.Contacts.CONTENT_ITEM_TYPE)
                            .putExtra(ContactsContract.Intents.Insert.PHONE, text),
                    "Add",
                    "Add description",
                    text.hashCode());
            if (contactsAction != null) {
                actions.add(contactsAction);
            }
            if (!userRestrictions.getBoolean(UserManager.DISALLOW_SMS, false)) {
                final RemoteActionCompat smsAction = createRemoteAction(
                        new Intent(Intent.ACTION_SENDTO)
                                .setData(Uri.parse(String.format("smsto:%s", text))),
                        "Message",
                        "Message description",
                        0);
                if (smsAction != null) {
                    actions.add(smsAction);
                }
            }
            if (!actions.isEmpty()) {
                return Collections.unmodifiableList(actions);
            }
            return Collections.emptyList();
        }

        private List<RemoteActionCompat> immutableList(List<RemoteActionCompat> actions) {
            if (!actions.isEmpty()) {
                return Collections.unmodifiableList(actions);
            }
            return Collections.emptyList();
        }

        @Nullable
        private RemoteActionCompat createRemoteAction(
                Intent mIntent, String title, String description, int requestCode) {
            final PackageManager pm = mContext.getPackageManager();
            final ResolveInfo resolveInfo = pm.resolveActivity(mIntent, 0);
            final String packageName = resolveInfo != null && resolveInfo.activityInfo != null
                    ? resolveInfo.activityInfo.packageName : null;
            IconCompat icon = null;
            boolean shouldShowIcon = false;
            if (packageName != null && !"android".equals(packageName)) {
                // There is a default activity handling the intent.
                mIntent.setComponent(new ComponentName(packageName, resolveInfo.activityInfo.name));
                if (resolveInfo.activityInfo.getIconResource() != 0) {
                    try {
                        icon = IconCompat.createWithResource(
                                pm.getResourcesForApplication(packageName), packageName,
                                resolveInfo.activityInfo.getIconResource());
                        shouldShowIcon = true;
                    } catch (PackageManager.NameNotFoundException e) {
                        // TODO: log.
                    }
                }
            }
            if (icon == null) {
                icon = NO_ICON;
            }
            final PendingIntent pendingIntent = createPendingIntent(mIntent, requestCode);
            if (pendingIntent == null) {
                return null;
            }
            final RemoteActionCompat action =
                    new RemoteActionCompat(icon, title, description, pendingIntent);
            action.setShouldShowIcon(shouldShowIcon);
            return action;
        }

        @Nullable
        private PendingIntent createPendingIntent(Intent intent, int requestCode) {
            final ResolveInfo activityRI = mContext.getPackageManager().resolveActivity(intent, 0);
            final int flags = PendingIntent.FLAG_UPDATE_CURRENT;
            if (activityRI != null) {
                if (mContext.getPackageName().equals(activityRI.activityInfo.packageName)) {
                    return PendingIntent.getActivity(mContext, requestCode, intent, flags);
                }
                final boolean exported = activityRI.activityInfo.exported;
                if (exported && hasPermission(activityRI.activityInfo.permission)) {
                    return PendingIntent.getActivity(mContext, requestCode, intent, flags);
                }
            }

            final ResolveInfo serviceRI = mContext.getPackageManager().resolveService(intent, 0);
            if (serviceRI != null) {
                if (mContext.getPackageName().equals(serviceRI.serviceInfo.packageName)) {
                    return PendingIntent.getService(mContext, requestCode, intent, flags);
                }
                final boolean exported = serviceRI.serviceInfo.exported;
                if (exported && hasPermission(serviceRI.serviceInfo.permission)) {
                    return PendingIntent.getService(mContext, requestCode, intent, flags);
                }
            }
            return null;
        }

        private boolean hasPermission(String permission) {
            return permission == null
                    || ContextCompat.checkSelfPermission(mContext, permission)
                            == PackageManager.PERMISSION_GRANTED;
        }
    }
}
