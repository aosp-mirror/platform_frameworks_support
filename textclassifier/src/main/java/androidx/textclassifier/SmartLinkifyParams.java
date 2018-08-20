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

import android.text.Spannable;
import android.text.style.ClickableSpan;
import android.util.ArrayMap;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.core.os.LocaleListCompat;
import androidx.core.util.Function;
import androidx.core.util.Preconditions;
import androidx.textclassifier.TextClassifier.EntityType;
import androidx.textclassifier.TextLinks.TextLink;
import androidx.textclassifier.TextLinks.TextLinkSpan;
import androidx.textclassifier.TextLinks.TextLinkSpanData;

import java.util.Collections;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Used to specify how to generate and apply links when using SmartLinkify APIs.
 */
public final class SmartLinkifyParams {

    @TextLinks.ApplyStrategy
    private final int mApplyStrategy;
    private final Function<TextLinkSpanData, TextLinkSpan> mSpanFactory;
    @Nullable private final TextClassifier.EntityConfig mEntityConfig;
    @Nullable private final LocaleListCompat mDefaultLocales;
    @Nullable private final Long mReferenceTime;
    private final Map<Pattern, String> mPatternsMap;
    private MatchMaker mMatchMaker;

    SmartLinkifyParams(
            @TextLinks.ApplyStrategy int applyStrategy,
            Function<TextLinkSpanData, TextLinkSpan> spanFactory,
            @Nullable TextClassifier.EntityConfig entityConfig,
            @Nullable LocaleListCompat defaultLocales,
            @Nullable Long referenceTime,
            Map<Pattern, String> patternsMap,
            MatchMaker matchMaker) {
        mApplyStrategy = applyStrategy;
        mSpanFactory = spanFactory;
        mEntityConfig = entityConfig;
        mDefaultLocales = defaultLocales;
        mReferenceTime = referenceTime;
        mPatternsMap = Collections.unmodifiableMap(patternsMap);
        mMatchMaker = matchMaker;
    }

    /**
     * Returns the entity config used to determine what entity types to generate.
     *
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @Nullable
    TextClassifier.EntityConfig getEntityConfig() {
        return mEntityConfig;
    }

    /**
     * Returns an ordered list of locale preferences that can be used to disambiguate
     * the provided text.
     *
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @Nullable
    LocaleListCompat getDefaultLocales() {
        return mDefaultLocales;
    }

    /**
     * Returns mappings of regex pattern to entity type.
     *
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    Map<Pattern, String> getPatterns() {
        return mPatternsMap;
    }

    /** @hide */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    MatchMaker getMatchMaker() {
        return mMatchMaker;
    }

    /**
     * Annotates the given text with the generated links. It will fail if the provided text doesn't
     * match the original text used to crete the TextLinks.
     *
     * @param text the text to apply the links to. Must match the original text
     * @param textLinks the links to apply to the text
     *
     * @return a status code indicating whether or not the links were successfully applied
     *
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @TextLinks.Status
    int apply(@NonNull Spannable text,
            @NonNull TextLinks textLinks,
            @NonNull TextClassifier textClassifier) {
        Preconditions.checkNotNull(text);
        Preconditions.checkNotNull(textLinks);
        Preconditions.checkNotNull(textClassifier);

        if (!canApply(text, textLinks)) {
            return TextLinks.STATUS_DIFFERENT_TEXT;
        }
        if (textLinks.getLinks().isEmpty()) {
            return TextLinks.STATUS_NO_LINKS_FOUND;
        }

        int applyCount = 0;
        for (TextLink link : textLinks.getLinks()) {
            TextLinkSpanData textLinkSpanData = new TextLinkSpanData(
                    link, textClassifier, mMatchMaker, mReferenceTime);
            final TextLinkSpan span = mSpanFactory.apply(textLinkSpanData);
            if (span != null) {
                final ClickableSpan[] existingSpans = text.getSpans(
                        link.getStart(), link.getEnd(), ClickableSpan.class);
                if (existingSpans.length > 0) {
                    if (mApplyStrategy == TextLinks.APPLY_STRATEGY_REPLACE) {
                        for (ClickableSpan existingSpan : existingSpans) {
                            text.removeSpan(existingSpan);
                        }
                        text.setSpan(span, link.getStart(), link.getEnd(),
                                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        applyCount++;
                    }
                } else {
                    text.setSpan(span, link.getStart(), link.getEnd(),
                            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    applyCount++;
                }
            }
        }
        if (applyCount == 0) {
            return TextLinks.STATUS_NO_LINKS_APPLIED;
        }
        return TextLinks.STATUS_LINKS_APPLIED;
    }

    /**
     * Returns true if it is possible to apply the specified textLinks to the specified text.
     * Otherwise, returns false.
     *
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    boolean canApply(@NonNull Spannable text, @NonNull TextLinks textLinks) {
        return text.toString().startsWith(textLinks.getText().toString());
    }

    /**
     * A builder for building SmartLinkifyParams.
     */
    public static final class Builder {

        private static final Function<TextLinkSpanData, TextLinkSpan> DEFAULT_SPAN_FACTORY =
                new Function<TextLinkSpanData, TextLinkSpan>() {
                    @Override
                    public TextLinkSpan apply(TextLinkSpanData data) {
                        return new TextLinkSpan(data);
                    }
                };

        @TextLinks.ApplyStrategy
        private int mApplyStrategy = TextLinks.APPLY_STRATEGY_IGNORE;
        private Function<TextLinkSpanData, TextLinkSpan> mSpanFactory = DEFAULT_SPAN_FACTORY;
        @Nullable private TextClassifier.EntityConfig mEntityConfig;
        @Nullable private LocaleListCompat mDefaultLocales;
        @Nullable private Long mReferenceTime;
        private final Map<Pattern, String> mPatternsMap = new ArrayMap<>();
        private MatchMaker mMatchMaker = MatchMaker.NO_OP;

        /**
         * Sets the apply strategy used to determine how to apply links to text.
         *      e.g {@link TextLinks#APPLY_STRATEGY_IGNORE}
         *
         * @return this builder
         */
        public Builder setApplyStrategy(@TextLinks.ApplyStrategy int applyStrategy) {
            mApplyStrategy = checkApplyStrategy(applyStrategy);
            return this;
        }

        /**
         * Sets a custom span factory for converting TextLinks to TextLinkSpans.
         * Set to {@code null} to use the default span factory.
         *
         * <strong>NOTE: </strong> the span factory must never return {@code null}.
         *
         * @return this builder
         */
        public Builder setSpanFactory(
                @Nullable Function<TextLinkSpanData, TextLinkSpan> spanFactory) {
            mSpanFactory = spanFactory == null ? DEFAULT_SPAN_FACTORY : spanFactory;
            return this;
        }

        /**
         * Sets the entity configuration to use. This determines what types of entities the
         * TextClassifier will look for.
         * Set to {@code null} for the default entity config and the TextClassifier will
         * automatically determine what links to generate.
         *
         * @return this builder
         */
        @NonNull
        public Builder setEntityConfig(@Nullable TextClassifier.EntityConfig entityConfig) {
            mEntityConfig = entityConfig;
            return this;
        }

        /**
         * @param defaultLocales ordered list of locale preferences that may be used to
         *                       disambiguate the provided text. If no locale preferences exist,
         *                       set this to null or an empty locale list.
         * @return this builder
         */
        @NonNull
        public Builder setDefaultLocales(@Nullable LocaleListCompat defaultLocales) {
            mDefaultLocales = defaultLocales;
            return this;
        }

        /**
         * @param referenceTime reference time based on which relative dates (e.g. "tomorrow")
         *      should be interpreted. This should usually be the time when the text was
         *      originally composed and should be milliseconds from the epoch of
         *      1970-01-01T00:00:00Z(UTC timezone). For example, if there is a message saying
         *      "see you 10 days later", and the message was composed yesterday, text classifier
         *      will then realize it is indeed means 9 days later from now and generate a link
         *      accordingly. If no reference time is set, now is used.
         *
         * @return this builder
         */
        @NonNull
        public Builder setReferenceTime(@Nullable Long referenceTime) {
            mReferenceTime = referenceTime;
            return this;
        }

        /**
         * Maps a regex pattern to an entity type.
         * This indicates that sections of the text that match the specified pattern should be
         * treated as the specified entity type.
         *
         * @param pattern the regex pattern
         * @param entityType the entity type that the matching subsequence should be treated as
         *
         * @return this builder
         */
        @NonNull
        public Builder addPattern(
                @NonNull Pattern pattern, @NonNull @EntityType String entityType) {
            mPatternsMap.put(
                    Preconditions.checkNotNull(pattern), Preconditions.checkNotNull(entityType));
            return this;
        }

        /**
         * Sets the matchmaker which returns a list of ordered actions for a specified entity type.
         *
         * <ul>
         *  <li>Use to include custom actions when handling links.
         *  <li>Use with {@link #addPattern(Pattern, String)} to customize generating and handling
         *      of links based on regex patterns.
         * </ul>
         *
         * <p>e.g.
         * <pre>{@code
         *      new SmartLinkifyParams.Builder()
         *          .addPattern(myDatePattern, TextClassifier.TYPE_DATE)
         *          .addPattern(customPattern1, "com.myapp.entitytype.custom1")
         *          .setMatchMaker((entityType, text) -> {
         *              if (isMyCustomEntityType(entityType)) {
         *                  return createActions(entityType, text);
         *              }
         *              return Collections.emptyList();
         *          })
         *          .build();
         * }</pre>
         *
         * @param matchMaker the actions generator
         *
         * @return this builder
         */
        @NonNull
        public Builder setMatchMaker(@Nullable MatchMaker matchMaker) {
            mMatchMaker = matchMaker == null ? MatchMaker.NO_OP : matchMaker;
            return this;
        }

        /**
         * Builds and returns a SmartLinkifyParams object.
         */
        public SmartLinkifyParams build() {
            return new SmartLinkifyParams(
                    mApplyStrategy, mSpanFactory, mEntityConfig, mDefaultLocales, mReferenceTime,
                    mPatternsMap, mMatchMaker);
        }

        /** @throws IllegalArgumentException if the value is invalid */
        @TextLinks.ApplyStrategy
        private static int checkApplyStrategy(int applyStrategy) {
            if (applyStrategy != TextLinks.APPLY_STRATEGY_IGNORE
                    && applyStrategy != TextLinks.APPLY_STRATEGY_REPLACE) {
                throw new IllegalArgumentException(
                        "Invalid apply strategy. "
                                + "See SmartLinkifyParams.ApplyStrategy for options.");
            }
            return applyStrategy;
        }
    }
}
