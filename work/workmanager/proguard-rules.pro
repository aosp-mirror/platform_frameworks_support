-keep class * extends androidx.work.Worker
-keep class * extends androidx.work.InputMerger
# Keep all constructors on ListenableWorker, Worker (also marked with @Keep)
-keep public class * extends androidx.work.ListenableWorker {
    public <init>(...);
}
# We need to keep WorkerParameters for the ListenableWorker constructor
-keep class androidx.work.WorkerParameters
# We reflectively try and instantiate FirebaseJobScheduler when we find a Firebase dependency
# on the classpath.
-keep class androidx.work.impl.background.firebase.FirebaseJobScheduler
# Keep WorkManager app components.
-keep class androidx.work.impl.WorkManagerInitializer
-keep class androidx.work.impl.background.systemalarm.SystemAlarmService
-keep class androidx.work.impl.background.systemjob.SystemJobService
-keep class androidx.work.impl.utils.ForceStopRunnable$BroadcastReceiver
-keep class androidx.work.impl.background.systemalarm.ConstraintProxy$BatteryChargingProxy
-keep class androidx.work.impl.background.systemalarm.ConstraintProxy$BatteryNotLowProxy
-keep class androidx.work.impl.background.systemalarm.ConstraintProxy$StorageNotLowProxy
-keep class androidx.work.impl.background.systemalarm.ConstraintProxy$NetworkStateProxy
-keep class androidx.work.impl.background.systemalarm.RescheduleReceiver
-keep class androidx.work.impl.background.systemalarm.ConstraintProxyUpdateReceiver
