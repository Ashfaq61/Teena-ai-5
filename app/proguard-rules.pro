# TEENA AI ProGuard / R8 rules
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Room entities & DAOs
-keepclassmembers class * extends androidx.room.RoomDatabase {
    void clearAllTables();
}
