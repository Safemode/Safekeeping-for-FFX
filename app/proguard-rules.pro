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

# --- Moshi ---
# Moshi and Room ship their own consumer rules, and all JSON models use @JsonClass(generateAdapter
# = true), so R8 keeps the generated *JsonAdapter classes. These keeps are belt-and-braces for the
# model data classes parsed from the bundled JSON / backup files, whose members are only touched
# through the generated adapters and so could otherwise look unused to R8.
-keep,allowobfuscation @com.squareup.moshi.JsonClass class * { *; }
-keepclassmembers class com.safemode.safekeepingforffx.data.backup.** { <fields>; }
-keepclassmembers class com.safemode.safekeepingforffx.data.reference.** { <fields>; }