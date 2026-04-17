# Keep all TooltipTour public API
-keep public class com.lovelysomething.tooltiptour.TooltipTour { *; }
-keep public class com.lovelysomething.tooltiptour.models.** { *; }
-keep public class com.lovelysomething.tooltiptour.registry.** { *; }
-keep public class com.lovelysomething.tooltiptour.ui.** { *; }

# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** { kotlinx.serialization.KSerializer serializer(...); }
-keep,includedescriptorclasses class com.lovelysomething.tooltiptour.**$$serializer { *; }
-keepclassmembers class com.lovelysomething.tooltiptour.** {
    *** Companion;
}
-keepclasseswithmembers class com.lovelysomething.tooltiptour.** {
    kotlinx.serialization.KSerializer serializer(...);
}
