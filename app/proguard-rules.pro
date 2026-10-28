# ============================================================
# ProGuard / R8 rules for TaskTracker release builds
# ============================================================

# ---- General Android ----
-dontwarn android.support.**
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes EnclosingMethod
-keepattributes InnerClasses
-keepattributes SourceFile,LineNumberTable

# ---- Kotlin metadata ----
-keep class kotlin.Metadata { *; }
-keepclassmembers class kotlinx.serialization.json.** { *; }

# ---- Coroutines ----
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlin.coroutines.ContinuationKt { *; }
-dontwarn kotlinx.coroutines.flow.**

# ---- kotlinx.serialization ----
# Keep serializers for all @Serializable types in our model package
-keep,includedescriptorclasses class com.example.tasktracker.domain.model.**$$serializer { *; }
-keepclassmembers class com.example.tasktracker.domain.model.** {
    *** Companion;
}
-keepclasseswithmembers class com.example.tasktracker.domain.model.** {
    kotlinx.serialization.KSerializer serializer(...);
}
# Keep @SerialName constants referenced by reflection
-keepclassmembers @kotlinx.serialization.Serializable class ** {
    *** Companion;
}

# ---- Retrofit / OkHttp ----
-keepattributes RuntimeVisibleAnnotations,RuntimeVisibleParameterAnnotations
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keep class kotlin.coroutines.Continuation { *; }
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}

# ---- Room ----
-keep class * extends androidx.room.RoomDatabase { <init>(); }
-dontwarn androidx.room.paging.**

# ---- Hilt / Dagger ----
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.lifecycle.HiltViewModel { *; }
-keepclassmembers class * {
    @dagger.hilt.android.HiltAndroidApp <init>(...);
    @dagger.hilt.android.AndroidEntryPoint <init>(...);
    @javax.inject.Inject <init>(...);
}

# ---- Compose ----
-dontwarn androidx.compose.**

# ---- Coil ----
-dontwarn coil3.**

# ---- Our app ----
-keep class com.example.yogaapp.YogaApp { *; }
-keep class com.example.yogaapp.di.** { *; }
-keep class com.example.yogaapp.data.local.entity.** { *; }
-dontwarn com.example.yogaapp.data.network.dto.**

# ---- WorkManager ----
-keep class * extends androidx.work.ListenableWorker { <init>(...); }
