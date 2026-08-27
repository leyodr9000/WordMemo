# Keep kotlinx.serialization generated classes
-keepclassmembers class com.ley.wordmemo.**.*Serializable {
    *** Companion;
}
-keep,includedescriptorclasses class com.ley.wordmemo.**.$$serializer { *; }
-keepclasseswithmembers class com.ley.wordmemo.** {
    kotlinx.serialization.KSerializer serializer(...);
}