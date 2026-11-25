# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-keepclassmembers class com.survive.app.data.** { *** Companion; }
-keepclasseswithmembers class com.survive.app.data.** { kotlinx.serialization.KSerializer serializer(...); }
