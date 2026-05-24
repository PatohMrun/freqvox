# ============================================================================
# Freqvox release / R8 keep rules
# ============================================================================
# Strategy: shrink unused code (keeps APK small) but DO NOT obfuscate, so that
# any future crash logcat lines remain readable. If you ever ship symbol upload
# (Crashlytics / Play Console mapping.txt), you can flip -dontobfuscate off.
# ============================================================================

-dontobfuscate
-keepattributes Signature, *Annotation*, InnerClasses, EnclosingMethod
-keepattributes SourceFile, LineNumberTable
-renamesourcefileattribute SourceFile

# --- Kotlin runtime / coroutines --------------------------------------------
-keep class kotlin.Metadata { *; }
-keep class kotlin.reflect.** { *; }
-dontwarn kotlin.reflect.**
-dontwarn org.jetbrains.annotations.**
-dontwarn kotlinx.coroutines.debug.**

# --- Hilt / Dagger 2.51 -----------------------------------------------------
-keep class dagger.hilt.** { *; }
-keep,allowobfuscation @interface dagger.hilt.* { *; }
-keep @dagger.hilt.android.HiltAndroidApp class * { *; }
-keep @dagger.hilt.InstallIn class * { *; }
-keep @dagger.Module class * { *; }
-keep class com.freqvox.di.** { *; }
-keep class com.freqvox.FreqvoxApplication { *; }
-keepclasseswithmembers class * {
    @dagger.hilt.android.* <init>(...);
}
-keepclasseswithmembers class * {
    @javax.inject.* <init>(...);
}

# --- Room 2.6.1 -------------------------------------------------------------
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao class * { *; }
-keep @androidx.room.TypeConverter class * { *; }
-keep class com.freqvox.data.local.** { *; }
-keepclassmembers class com.freqvox.data.local.entities.** { *; }
-dontwarn androidx.room.paging.**

# --- Media3 / ExoPlayer 1.5.1 ----------------------------------------------
-keep class androidx.media3.** { *; }
-keep interface androidx.media3.** { *; }
-keep class com.freqvox.playback.** { *; }
-keepclassmembers class com.freqvox.playback.** { *; }
-dontwarn androidx.media3.**

# --- Coil 2.7 ---------------------------------------------------------------
-dontwarn coil.**
-keep class coil.** { *; }

# --- Compose runtime / foundation safety net --------------------------------
-keep class androidx.compose.runtime.** { *; }
-keep class androidx.compose.foundation.lazy.** { *; }
-keepclasseswithmembers class * {
    @androidx.compose.runtime.Composable <methods>;
}

# --- Calvin-LL Reorderable 3.1.0 -------------------------------------------
-keep class sh.calvin.reorderable.** { *; }
-keepclassmembers class sh.calvin.reorderable.** { *; }
-dontwarn sh.calvin.reorderable.**

# --- QueueItem & DTOs used by Media3 metadata ------------------------------
-keep class com.freqvox.playback.QueueItem { *; }
-keepclassmembers class com.freqvox.playback.QueueItem { *; }

# --- Kapt-generated factories (Hilt + Room) --------------------------------
-keep class **_Impl { *; }
-keep class **_HiltModules** { *; }
-keep class **_HiltModules$* { *; }
-keep class **_GeneratedInjector { *; }
-keep class **_Factory { *; }
-keep class **_MembersInjector { *; }
-keep class **Hilt_* { *; }
