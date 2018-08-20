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

package androidx.paging;

import androidx.arch.core.util.Function;

public class Transform {
    public boolean shouldRemain(Object item) {
        return true;
    }

    public Object map(Object item) {
        return item;
    }

    public static class Filter<T> extends Transform {
        final Function<T, Boolean> mFilter;

        public Filter(Function<T, Boolean> filter) {
            mFilter = filter;
        }

        @SuppressWarnings("unchecked")
        @Override
        public boolean shouldRemain(Object item) {
            return mFilter.apply((T) item);
        }
    }

    public static class Mapper<I, O> extends Transform {
        final Function<I, O> mMapper;

        public Mapper(Function<I, O> mapper) {
            mMapper = mapper;
        }

        @SuppressWarnings("unchecked")
        @Override
        public Object map(Object item) {
            return mMapper.apply((I) item);
        }
    }
}
