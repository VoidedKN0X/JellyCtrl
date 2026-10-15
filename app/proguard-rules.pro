# libmpv calls back into these classes from native code by name.
-keep class dev.jdtech.mpv.** { *; }

# kotlinx.serialization models (library ships its own rules; these keep our DTOs and routes intact).
-keepattributes *Annotation*, InnerClasses, Signature, EnclosingMethod
-keep,includedescriptorclasses class com.vknxworks.jellyctrl.**$$serializer { *; }
-keepclassmembers class com.vknxworks.jellyctrl.** {
    *** Companion;
}
-keepclasseswithmembers class com.vknxworks.jellyctrl.** {
    kotlinx.serialization.KSerializer serializer(...);
}

-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
