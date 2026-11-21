# Release shrinking rules. Libraries in use (Koin, kotlinx-serialization, Compose, DataStore)
# ship their own consumer rules, so few project-specific rules are needed.

# Spec 012: Play's asset-delivery-ktx (core/content) refers to a build-time annotation that no published
# play-services-basement contains; nothing reads it at run time. The rule is the one R8 itself generates.
-dontwarn com.google.android.gms.common.annotation.NoNullnessRewrite

# Bergamot's JNI functions are found by the class and method names (core/translation/native/bergamot_jni.cpp):
# proguard-android-optimize.txt already keeps the names of classes with native methods.
