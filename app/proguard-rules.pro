# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

-keepattributes *Annotation*, EnclosingMethod, Signature, InnerClasses

# GreenDao rules
-keepclassmembers class * extends org.greenrobot.greendao.AbstractDao {
public static java.lang.String TABLENAME;
}
-keep class **$Properties
# If you do not use SQLCipher:
-dontwarn org.greenrobot.greendao.database.**
# If you do not use Rx:
-dontwarn rx.**

# Proguard configuration for Jackson 2.x
-keepnames class com.fasterxml.jackson.** { *; }
-dontwarn com.fasterxml.jackson.databind.**

-keep class com.fasterxml.jackson.databind.ObjectMapper {
    public <methods>;
    protected <methods>;
}
-keep class com.fasterxml.jackson.databind.ObjectWriter {
    public ** writeValueAsString(**);
}

-keep class com.androidplot.** { *; }
-keep class android.support.v7.widget.SearchView { *; }

 ## UXCAM
 -keep class com.uxcam.** { *; }
 -dontwarn com.uxcam.**

 ## Smooch
-dontwarn okio.**
-keep class com.google.gson.** { *; }
-keepclassmembers enum * { *; }
-keepclassmembers enum io.smooch.core.model.** { *; }

-dontwarn com.roughike.bottombar.**
-dontwarn com.beloo.widget.chipslayoutmanager.**

#Retrofit rules
# Platform calls Class.forName on types which do not exist on Android to determine platform.
-dontnote retrofit2.Platform
# Platform used when running on Java 8 VMs. Will not be used at runtime.
-dontwarn retrofit2.Platform$Java8
# Retain declared checked exceptions for use by a Proxy instance.
-keepattributes Exceptions
# Retain class members
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}

# Otto
-keepclassmembers class ** {
    @com.squareup.otto.Subscribe public *;
    @com.squareup.otto.Produce public *;
}

# OkHttp
-keep class com.squareup.okhttp.** { *; }
-keep interface com.squareup.okhttp.** { *; }
-dontwarn com.squareup.okhttp.**
-dontwarn okhttp3.**


-keep public class com.google.android.gms.* { public *; }
-dontwarn com.google.android.gms.**
-dontwarn org.apache.log4j.**
-dontwarn com.googlecode.mp4parser.**