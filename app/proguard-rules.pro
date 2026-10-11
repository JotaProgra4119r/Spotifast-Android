# Proguard rules for Spotifast Mobile
-keepattributes *Annotation*
-dontwarn okhttp3.**
-dontwarn okio.**
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
