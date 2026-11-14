# Everything Launcher Proguard / R8 Optimization Rules

# 1. R8 Optimization & Shrinking settings
-optimizationpasses 5
-allowaccessmodification
-dontusemixedcaseclassnames
-skipnonpubliclibraryclasses
-verbose

# 2. Strip debug and verbose logging in release builds to eliminate UI thread overhead on older phones
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
}

# 3. Room Database, DAOs & Entities
-keep class rikazzz.everythinglauncher.data.** { *; }
-keepclassmembers class rikazzz.everythinglauncher.data.** { *; }
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# 4. Kotlin Coroutines & Flow
-dontwarn kotlinx.coroutines.**
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# 5. Material Kolor & Color Utilities
-keep class com.materialkolor.** { *; }
-dontwarn com.materialkolor.**

# 6. AboutLibraries
-keep class com.mikepenz.aboutlibraries.** { *; }
-dontwarn com.mikepenz.aboutlibraries.**

# 7. Media3 Session & Player
-keep class androidx.media3.session.** { *; }
-dontwarn androidx.media3.**

# 8. Keep Android Manifest component classes
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Application
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider

# 9. Keep Generic AndroidX & Compose runtime compatibility
-dontwarn androidx.annotation.**
-dontwarn androidx.core.**
