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

# --- Readable crash logs ---
# Shrink and optimize, but don't rename classes and methods. The source is public, so obfuscation
# protects nothing, and without it a stack trace pasted into a GitHub issue names the real class and
# method (e.g. SphereGridRepository.applyBuild) instead of a.b.c. It costs about 66 KB (under 2%) of
# APK size.
#
# What still needs that build's mapping.txt (archived in release-mappings/): R8 renumbers lines and
# replaces the file name with an r8-map-id-<hash> that identifies the build, and it inlines small
# methods, so a frame can point at the caller. Retracing with the mapping turns those back into exact
# source lines; without it the class and method are usually enough to find the problem.
-dontobfuscate
# Line tables and the file attribute, so retrace has something to map back from.
-keepattributes SourceFile,LineNumberTable

# --- Moshi ---
# Moshi and Room ship their own consumer rules, and all JSON models use @JsonClass(generateAdapter
# = true), so R8 keeps the generated *JsonAdapter classes. These keeps are belt-and-braces for the
# model data classes parsed from the bundled JSON / backup files, whose members are only touched
# through the generated adapters and so could otherwise look unused to R8.
-keep,allowobfuscation @com.squareup.moshi.JsonClass class * { *; }
-keepclassmembers class com.safemode.safekeepingforffx.data.backup.** { <fields>; }
-keepclassmembers class com.safemode.safekeepingforffx.data.reference.** { <fields>; }