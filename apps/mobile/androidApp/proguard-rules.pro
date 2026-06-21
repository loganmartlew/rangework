# --- kotlinx.serialization ---
# Keep @Serializable classes and their serializers
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep all serializable models in the shared module
-keep,includedescriptorclasses class com.loganmartlew.rangework.shared.**$$serializer { *; }
-keepclassmembers class com.loganmartlew.rangework.shared.** {
    *** Companion;
}
-keepclasseswithmembers class com.loganmartlew.rangework.shared.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# --- Supabase SDK (reflection-heavy plugin system) ---
-keep class io.github.jan.supabase.** { *; }
-keepclassmembers class io.github.jan.supabase.** { *; }
-dontwarn io.github.jan.supabase.**

# --- Ktor + OkHttp ---
-keep class io.ktor.** { *; }
-keepclassmembers class io.ktor.** { *; }
-dontwarn io.ktor.**
-dontwarn okhttp3.**
-dontwarn okio.**

# --- Enum entries (needed for serializable enums like ClubCategory) ---
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
    <fields>;
}

# --- Kotlin metadata (needed by reflection-based libraries) ---
-keep class kotlin.Metadata { *; }
-keepattributes *Annotation*

# --- Sentry ---
-keep class io.sentry.** { *; }
-dontwarn io.sentry.**

# --- Google Identity / Credentials ---
-keep class com.google.android.libraries.identity.googleid.** { *; }
-dontwarn com.google.android.libraries.identity.googleid.**

# --- Coroutines ---
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}
