# Pixel Game Booster ProGuard / R8 rules
-keep class dev.rikka.shizuku.** { *; }
-keep class org.lsposed.hiddenapibypass.** { *; }

# Gson models & TypeConverters
-keepclassmembers class com.pixel.gamebooster.data.model.** { *; }
-keepclassmembers class com.pixel.gamebooster.overlay.model.** { *; }
-keepclassmembers class com.pixel.gamebooster.data.db.entity.** { *; }

-dontwarn javax.annotation.Nullable
