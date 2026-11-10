# Lugmety Order Assistant ProGuard Rules

# Room Database reflection preservation
-keepclassmembers class * {
    @androidx.room.Dao *;
    @androidx.room.Database *;
    @androidx.room.Entity *;
}

-dontwarn androidx.room.paging.**

# Keep models and entities
-keep class com.lugmety.orderassistant.data.local.entity.** { *; }
-keep class com.lugmety.orderassistant.data.local.dao.** { *; }

# Accessibility and Services
-keep class com.lugmety.orderassistant.service.** { *; }

# Kotlin Coroutines
-keepattributes *Annotation*, InnerClasses, EnclosingMethod
-keepclassmembers class kotlinx.coroutines.** { *; }
