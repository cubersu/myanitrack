# kotlinx.serialization - serializer alanlari yansima ile bulunur
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.myanitrack.**$$serializer { *; }
-keepclassmembers class com.myanitrack.** {
    *** Companion;
}
-keepclasseswithmembers class com.myanitrack.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Retrofit - arayuz metotlarinin imzalari korunmali
-keepattributes Signature, Exceptions, RuntimeVisibleAnnotations
-keep,allowobfuscation interface retrofit2.http.**
-dontwarn retrofit2.**
-dontwarn okhttp3.**
-dontwarn okio.**

# Room
-keep class * extends androidx.room.RoomDatabase { <init>(); }
