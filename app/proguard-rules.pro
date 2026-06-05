# ──────────────────────────────────────────────────────────────────────────────
# GeoVision Mobile ProGuard/R8 Rules - Security Hardened
# ──────────────────────────────────────────────────────────────────────────────

# Keep attributes for debugging and reflection
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable
-keepattributes Exceptions,InnerClasses,EnclosingMethod
-keepattributes Signature,RuntimeVisibleAnnotations,RuntimeVisibleParameterAnnotations

# ──────────────────────────────────────────────────────────────────────────────
# Keep GeoVision Classes
# ──────────────────────────────────────────────────────────────────────────────
-keep class com.geovision.mobile.** { *; }
-keep interface com.geovision.mobile.** { *; }
-keep enum com.geovision.mobile.** { *; }

# Keep ViewModels
-keep class * extends androidx.lifecycle.ViewModel
-keep class * extends androidx.lifecycle.AndroidViewModel

# Keep Data Classes (Kotlin)
-keep class com.geovision.mobile.data.** { *; }

# ──────────────────────────────────────────────────────────────────────────────
# Keep Third Party Libraries
# ──────────────────────────────────────────────────────────────────────────────

# osmdroid
-keep class org.osmdroid.** { *; }
-keep interface org.osmdroid.** { *; }
-keep enum org.osmdroid.** { *; }

# MapLibre
-keep class com.mapbox.mapboxsdk.** { *; }
-keep interface com.mapbox.mapboxsdk.** { *; }
-keep class com.maplibre.** { *; }
-keep interface com.maplibre.** { *; }

# JTS Topology Suite
-keep class org.locationtech.jts.** { *; }
-keep interface org.locationtech.jts.** { *; }

# Jetpack & AndroidX
-keep class androidx.** { *; }
-keep interface androidx.** { *; }
-keep enum androidx.** { *; }

# Jetpack Compose
-keep class androidx.compose.** { *; }
-keep interface androidx.compose.** { *; }

# Kotlin Coroutines
-keep class kotlin.coroutines.** { *; }
-keep class kotlinx.coroutines.** { *; }

# Serialization
-keepclassmembers class * implements java.io.Serializable { *; }

# ──────────────────────────────────────────────────────────────────────────────
# Kotlin Specific Rules
# ──────────────────────────────────────────────────────────────────────────────
-keep class kotlin.** { *; }
-keep class kotlin.Metadata { *; }
-dontwarn kotlin.**
-keepclassmembers class **$WhenMappings {
    <fields>;
}

# ──────────────────────────────────────────────────────────────────────────────
# Remove Debugging Information in Release
# ──────────────────────────────────────────────────────────────────────────────
-dontusemixedcaseclassnames
-verbose
-allowaccessmodification
-repackageclasses com.geovision.mobile.obfuscated

# ──────────────────────────────────────────────────────────────────────────────
# Security: Remove Logging and Debug Info
# ──────────────────────────────────────────────────────────────────────────────
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}

# But keep error logging for debugging critical issues
-keep class android.util.Log { 
    public static *** e(...);
    public static *** w(...);
}

# ──────────────────────────────────────────────────────────────────────────────
# Keep Enum.values() and Enum.valueOf()
# ──────────────────────────────────────────────────────────────────────────────
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# ──────────────────────────────────────────────────────────────────────────────
# Keep Native Methods
# ──────────────────────────────────────────────────────────────────────────────
-keepclasseswithmembernames class * {
    native <methods>;
}

# ──────────────────────────────────────────────────────────────────────────────
# Keep View Constructors for Inflation
# ──────────────────────────────────────────────────────────────────────────────
-keepclasseswithmembers class * {
    public <init>(android.content.Context, android.util.AttributeSet);
}

# ──────────────────────────────────────────────────────────────────────────────
# Additional Security Rules
# ──────────────────────────────────────────────────────────────────────────────

# Prevent stripping of data classes with annotations
-keep class com.geovision.mobile.data.** { 
    <init>(...); 
    *; 
}

# Keep Parcelable implementations
-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

# Optimize aggressive
-optimizationpasses 5
-optimizations !code/simplification/cast,!field/*,!class/merging/*

# Optional annotation and desktop-only APIs referenced by transitive libraries.
-dontwarn com.google.errorprone.annotations.CanIgnoreReturnValue
-dontwarn com.google.errorprone.annotations.CheckReturnValue
-dontwarn com.google.errorprone.annotations.Immutable
-dontwarn com.google.errorprone.annotations.RestrictedApi
-dontwarn java.awt.Color
-dontwarn java.awt.Font
-dontwarn java.awt.Rectangle
-dontwarn java.awt.Shape
-dontwarn java.awt.font.FontRenderContext
-dontwarn java.awt.font.GlyphVector
-dontwarn java.awt.geom.AffineTransform
-dontwarn java.awt.geom.Ellipse2D$Double
-dontwarn java.awt.geom.GeneralPath
-dontwarn java.awt.geom.Line2D$Double
-dontwarn java.awt.geom.PathIterator
-dontwarn java.awt.geom.Point2D$Double
-dontwarn java.awt.geom.Point2D
-dontwarn java.awt.geom.Rectangle2D$Double
-dontwarn java.awt.geom.Rectangle2D
-dontwarn javax.xml.stream.XMLInputFactory
-dontwarn javax.xml.stream.XMLStreamException
-dontwarn javax.xml.stream.XMLStreamReader
