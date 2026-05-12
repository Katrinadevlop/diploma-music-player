# Сохраняем сигнатуры дженериков — Retrofit использует их через рефлексию.
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes EnclosingMethod
-keepattributes InnerClasses

# Retrofit
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation
-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement
-dontwarn javax.annotation.**

# OkHttp
-dontwarn okhttp3.internal.platform.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# kotlinx.serialization
-keepattributes RuntimeVisibleAnnotations,AnnotationDefault
-keep,includedescriptorclasses class ru.musikkk.player.**$$serializer { *; }
-keepclassmembers class ru.musikkk.player.** {
    *** Companion;
}
-keepclasseswithmembers class ru.musikkk.player.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Сгенерированный код Hilt
-keep class dagger.hilt.** { *; }
-keep class hilt_aggregated_deps.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper

# Сохраняем дженерик-информацию для компонент-модели Hilt.
-keepclassmembers class ** {
    @dagger.* <methods>;
}

# Media3
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**
