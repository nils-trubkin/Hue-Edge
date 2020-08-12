# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** e(...);
    public static *** i(...);
}

#-dontwarn org.jetbrains.annotations.**
#-keep class kotlin.Metadata { *; }

-keep class com.nilstrubkin.hueedge.discovery.* { *; }
-keepnames class com.nilstrubkin.hueedge.discovery.* { *; }

-keep class com.nilstrubkin.hueedge.resources.* { *; }
-keepnames class com.nilstrubkin.hueedge.resources.* { *; }

-keepnames class androidx.navigation.fragment.NavHostFragment

-keepclassmembers class com.nilstrubkin.hueedge.* { *; }

-keepclassmembers enum com.nilstrubkin.hueedge.* { *; }

#-addconfigurationdebugging