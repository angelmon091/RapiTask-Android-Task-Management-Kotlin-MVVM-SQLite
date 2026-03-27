# Reglas básicas para RapiTask (Material3 + Room + Glide)

# Mantener clases de entidades de Room
-keepclassmembers class com.example.proyectofinal.Note { *; }
-keepclassmembers class com.example.proyectofinal.Reminder { *; }
-keepclassmembers class com.example.proyectofinal.Subtask { *; }

# Glide
-keep public class * extends com.bumptech.glide.module.AppGlideModule
-keep public class * extends com.bumptech.glide.module.LibraryGlideModule
-keep class com.bumptech.glide.GeneratedAppGlideModuleImpl

# Mantener ViewBinding
-keep class com.example.proyectofinal.databinding.** { *; }

# Optimización general
-dontwarn com.google.android.material.**
-dontwarn androidx.room.**
