# Основные правила для Android TV
-keep class com.example.androidtv.** { *; }

# Правила для Media3
-keep class androidx.media3.** { *; }
-keep interface androidx.media3.** { *; }

# Правила для ExoPlayer
-keepclassmembers class * implements androidx.media3.exoplayer.source.MediaSource {
    <init>(...);
}

# Правила для OkHttp
-dontwarn okhttp3.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }

# Общие правила для Android
-keepattributes Signature
-keepattributes *Annotation*
-keep class * extends java.lang.Exception

# Правила для Kotlin
-keep class kotlin.** { *; }
-keep class kotlin.Metadata { *; }
-dontwarn kotlin.**
-keepclassmembers class **$WhenMappings {
    <fields>;
}
-keepclassmembers class kotlin.Metadata {
    public <methods>;
} 