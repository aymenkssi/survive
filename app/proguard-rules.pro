# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-keepclassmembers class com.survive.app.data.** { *** Companion; }
-keepclasseswithmembers class com.survive.app.data.** { kotlinx.serialization.KSerializer serializer(...); }

# Mapsforge : thèmes de rendu chargés par nom depuis les ressources Java
-keep class org.mapsforge.map.rendertheme.** { *; }
-dontwarn org.mapsforge.**
-dontwarn com.caverock.androidsvg.**
