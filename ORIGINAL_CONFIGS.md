# Original Configs

## 1. `app/build.gradle.kts` — debug build type

The `debug` block was added. To revert, remove it entirely:

```kotlin
buildTypes {
    release {
        isMinifyEnabled = true
        proguardFiles(
            getDefaultProguardFile("proguard-android-optimize.txt"),
            "proguard-rules.pro"
        )
    }
}
```

## 2. `app/src/main/java/com/freqvox/MainActivity.kt` — enableEdgeToEdge

`SystemBarStyle.dark` was added to force white status bar icons. To revert:

```kotlin
enableEdgeToEdge()
```

Also remove the `SystemBarStyle` import:

```kotlin
import androidx.activity.SystemBarStyle
```
