# ProGuard / R8 keep rules for KoLmafia Android release builds.
# Minify is enabled; keep reflective DI, HTTP engines, and Compose entry points.

# Kotlin / coroutines
-keepclassmembers class kotlinx.coroutines.** { *; }
-dontwarn kotlinx.coroutines.**

# Koin
-keep class org.koin.** { *; }
-keepclassmembers class * {
    @org.koin.core.annotation.* <methods>;
}
-dontwarn org.koin.**

# Ktor
-keep class io.ktor.** { *; }
-dontwarn io.ktor.**
-dontwarn io.ktor.utils.io.**

# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keep,includedescriptorclasses class net.sourceforge.kolmafia.**$$serializer { *; }
-keepclassmembers class net.sourceforge.kolmafia.** {
    *** Companion;
}
-keepclasseswithmembers class net.sourceforge.kolmafia.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-dontwarn kotlinx.serialization.**

# Compose
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# App + shared runtime entry points
-keep class net.sourceforge.kolmafia.android.** { *; }
-keep class net.sourceforge.kolmafia.ui.** { *; }
-keep class net.sourceforge.kolmafia.di.** { *; }
-keep class net.sourceforge.kolmafia.ash.GameRuntimeLibrary { *; }

# Multiplatform Settings
-keep class com.russhwolf.settings.** { *; }
-dontwarn com.russhwolf.settings.**
