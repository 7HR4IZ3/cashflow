# Cashflow release shrinking rules. R8 keeps manifest components automatically and
# the libraries below ship their own consumer rules; these cover what they miss.

# Readable crash traces in Play Console once the mapping file is uploaded.
-keepattributes SourceFile,LineNumberTable,*Annotation*,InnerClasses,EnclosingMethod,Signature
-renamesourcefileattribute SourceFile

# kotlinx.serialization: keep generated serializers for our @Serializable models.
-keepclassmembers class app.cashflow.** {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class app.cashflow.**$$serializer { *; }

# Play Billing
-keep class com.android.vending.billing.** { *; }

# Ignore desktop/JVM-only references pulled in by multiplatform libraries.
-dontwarn java.lang.management.**
-dontwarn org.slf4j.**
-dontwarn javax.annotation.**
