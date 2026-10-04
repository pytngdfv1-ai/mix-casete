# No optimizar agresivamente para evitar crashes en release
-dontoptimize

# NewPipe Extractor y Rhino (motor JavaScript)
-keep class org.schabi.newpipe.extractor.** { *; }
-dontwarn org.schabi.newpipe.extractor.**
-dontwarn org.mozilla.javascript.**
-dontwarn java.beans.**

# Media3 / ExoPlayer
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# Room
-keep class com.mixcasete.app.data.** { *; }
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.**

# Kotlin coroutines y flows
-keep class kotlinx.coroutines.** { *; }
-dontwarn kotlinx.coroutines.**

# Modelos propios
-keep class com.mixcasete.app.audio.** { *; }
-keep class com.mixcasete.app.tv.** { *; }
-keep class com.mixcasete.app.ui.** { *; }
-keep class com.mixcasete.app.player.** { *; }

# OkHttp / Okio (usado por datasource)
-dontwarn okhttp3.**
-dontwarn okio.**

# JSON de Android
-keep class org.json.** { *; }

# WebView / JavaScript bridge (IFrame de YouTube)
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# Mantener datos de la app
-keep class com.mixcasete.app.MainActivity { *; }
-keep class com.mixcasete.app.tv.TvActivity { *; }
