package androidx.ads.identifier.provider;

/**
 * The advertising ID API service.
 * The advertising ID is a resettable identifier used for ads purpose.
 * @hide
 */
interface IAdvertisingIdService {
    String getId() = 0;
    boolean isLimitAdTrackingEnabled() = 1;
}
