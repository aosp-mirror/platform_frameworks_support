package androidx.textclassifier;

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;

/**
 * Created by tonymak on 8/6/18.
 */

interface SessionStrategy {
    @NonNull
    TextSelection suggestSelection(@NonNull TextSelection.Request request);

    @NonNull
    TextClassification classifyText(@NonNull TextClassification.Request request);

    @NonNull
    TextLinks generateLinks(@NonNull TextLinks.Request request);

    int getMaxGenerateLinksTextLength();

    void reportSelectionEvent(@NonNull SelectionEvent event);
}
