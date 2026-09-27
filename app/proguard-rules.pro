# ProGuard / R8 Comprehensive Rules for Anadolu Ticaret Simülasyonu

# Retain line number info for Crashlytics stack traces
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

-keepattributes *Annotation*
-keepattributes Signature
-keepattributes EnclosingMethod
-keepattributes InnerClasses
-keepattributes RuntimeVisibleAnnotations,RuntimeVisibleParameterAnnotations

# Retain Coroutines and Compose
-keepclasseswithmembers class * {
    @androidx.compose.runtime.Composable <methods>;
}
-keep class androidx.compose.** { *; }

# ==============================================================================
# DATA LAYER, ROOM ENTITIES, SUPABASE DTOs, GAME REGISTRY & DATA MODELS
# ==============================================================================
# Preserve all data classes, entities, DTOs, enums and repositories from obfuscation and stripping
-keep class com.example.data.** { *; }
-keep interface com.example.data.** { *; }
-keep enum com.example.data.** { *; }

-keep class com.example.viewmodel.** { *; }
-keep class com.example.util.** { *; }
-keep class com.example.utils.** { *; }

# Explicit Keep for Room Database and DAOs
-keepclassmembers class * extends androidx.room.RoomDatabase {
    <methods>;
}
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-keep @androidx.room.Database class * { *; }
-keep @androidx.room.TypeConverter class * { *; }
-keep @androidx.room.TypeConverters class * { *; }
-keep class * extends androidx.room.RoomDatabase { *; }
-keep class androidx.room.** { *; }
-dontwarn androidx.room.paging.**

# Keep all classes annotated with @Keep
-keep @androidx.annotation.Keep class * { *; }
-keepclassmembers class * {
    @androidx.annotation.Keep *;
}

# ==============================================================================
# JSON SERIALIZATION (Kotlinx Serialization, Moshi, Gson)
# ==============================================================================
# Kotlinx Serialization
-dontnote kotlinx.serialization.SerializationKt
-keepclassmembers class * {
    *** Companion;
}
-keepclasseswithmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}
-keepclassmembers class * {
    @kotlinx.serialization.SerialName <fields>;
    @kotlinx.serialization.Serializable <fields>;
}
-keep @kotlinx.serialization.Serializable class * { *; }
-keepclassmembers class * implements kotlinx.serialization.KSerializer {
    <methods>;
    <fields>;
}
-keep class kotlinx.serialization.** { *; }

# Moshi JSON
-keep class com.squareup.moshi.** { *; }
-keepclassmembers class * {
    @com.squareup.moshi.Json *;
}
-keep class *JsonAdapter { *; }

# Gson (if used by any third-party or sub-library)
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.google.gson.** { *; }
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
    @com.google.gson.annotations.Expose <fields>;
}

# ==============================================================================
# NETWORKING & HTTP (OkHttp, Retrofit, Ktor, WebSockets)
# ==============================================================================
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-keep class okio.** { *; }

-keepclassmembers,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }

-dontwarn io.ktor.**
-keep class io.ktor.** { *; }

# ==============================================================================
# GOOGLE PLAY SERVICES, PLAY GAMES, IN-APP BILLING, APP UPDATE & ADMOB
# ==============================================================================
-keep class com.google.android.gms.games.** { *; }
-keep interface com.google.android.gms.games.** { *; }
-keep class com.google.android.gms.common.** { *; }
-keep class com.google.android.gms.ads.** { *; }
-keep class com.android.billingclient.api.** { *; }
-keep class com.google.android.play.core.** { *; }
-keep class com.google.android.play.core.appupdate.** { *; }

# ==============================================================================
# SUPABASE & POSTGREST / REALTIME DTOs
# ==============================================================================
-dontwarn io.github.jan.supabase.**
-keep class io.github.jan.supabase.** { *; }

# ==============================================================================
# R8 / PROGUARD AGGRESSIVE OPTIMIZATIONS & DEAD CODE STRIPPING
# ==============================================================================
-optimizationpasses 5
-allowaccessmodification
-dontusemixedcaseclassnames
-dontskipnonpubliclibraryclasses
-mergeinterfacesaggressively
-repackageclasses ''

# OSMDroid map & bitmap cache optimization rules
-keep class org.osmdroid.** { *; }
-dontwarn org.osmdroid.**

# Strip unnecessary logging in release to save CPU cycles and RAM
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
}

