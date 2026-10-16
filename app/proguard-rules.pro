# ProGuard rules for DeX Launcher

# Keep Shizuku AIDL and UserService classes
-keep class com.example.dexlauncher.IShizukuService { *; }
-keep class com.example.dexlauncher.IShizukuService$Stub { *; }
-keep class com.example.dexlauncher.shizuku.ShizukuUserService { *; }
-keep class dev.rikka.shizuku.** { *; }

# Keep Compose
-keepclassmembers class * extends androidx.compose.runtime.State { *; }
