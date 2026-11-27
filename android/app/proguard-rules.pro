# Lovelyy release (R8) keep rules. Firebase, Compose, Billing, Media3, WorkManager and Credentials ship their
# own consumer rules; only what they do not cover is here.

# Readable Crashlytics stack traces: keep line numbers, hide the original file names.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
# Firebase Crashlytics also needs custom exceptions' names.
-keep public class * extends java.lang.Exception

# Credential Manager loads its Play Services provider by reflection (Google sign-in).
-if class androidx.credentials.CredentialManager
-keep class androidx.credentials.playservices.** {
  *;
}

# Annotations and generic signatures (Firestore, coroutines).
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod
