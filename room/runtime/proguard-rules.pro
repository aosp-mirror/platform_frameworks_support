-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**
-keepclassmembers,allowshrinking,allowobfuscation class * {
    @androidx.room.TypeConverter <methods>;
}
