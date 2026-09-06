# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# Keep Compose runtime annotations and interfaces
-keepattributes *Annotation*, InnerClasses

# Preserve Osyster models and state objects
-keep class dev.qtremors.osyster.monitor.** { *; }
-keep class dev.qtremors.osyster.ui.theme.** { *; }

# kotlinx.serialization rules for type-safe Navigation Compose routes
-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1> {
    static <1>$Companion Companion;
}

-if @kotlinx.serialization.Serializable class ** {
    static **$* *;
}
-keepclassmembers class <2>$<3> {
    kotlinx.serialization.KSerializer serializer(...);
}

-if @kotlinx.serialization.Serializable class ** {
    public static ** INSTANCE;
}
-keepclassmembers class <1> {
    public kotlinx.serialization.KSerializer serializer(...);
}

-if @kotlinx.serialization.Serializer class **
-keepclassmembers class <1> {
    public kotlinx.serialization.KSerializer serializer(...);
}

# Keep all @Serializable data classes and objects in Osyster
-keep class dev.qtremors.osyster.** implements kotlinx.serialization.KSerializer { *; }
-keep @kotlinx.serialization.Serializable class dev.qtremors.osyster.** { *; }
