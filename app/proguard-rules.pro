# TEENA AI ProGuard / R8 rules
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
-keepattributes *Annotation*, InnerClasses

# Room entities & DAOs
-keepclassmembers class * extends androidx.room.RoomDatabase {
    void clearAllTables();
}

# kotlinx.serialization
-dontwarn kotlinx.serialization.**
