# Tooltip Tour — Android SDK

Native Kotlin SDK for [Tooltip Tour](https://app.lovelysomething.com) — the guided walkthrough tool for web, iOS, and Android.

---

## Installation

### JitPack (recommended)

Add JitPack to your project-level `settings.gradle.kts`:

```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}
```

Add the dependency to your app-level `build.gradle.kts`:

```kotlin
dependencies {
    implementation("com.github.lovelysomething:tooltip-tour-android:1.0.0")
}
```

---

## Setup

### 1. Configure in Application.onCreate

```kotlin
class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        TooltipTour.configure(this, siteKey = "sk_your_key")
        // Optional: prefetch all configs at startup for instant first load
        lifecycleScope.launch { TooltipTour.instance?.prefetchAll() }
    }
}
```

### 2. Add the launcher to each screen

Wrap your screen content in a `Box`, add `.ttPage("identifier")` to register the screen,
then place `TTLauncherView()` inside:

```kotlin
Box(
    modifier = Modifier
        .fillMaxSize()
        .ttPage("home")   // registers this screen with the SDK
) {
    // Your screen content here
    YourContent()

    // Launcher overlay — shows welcome card / FAB automatically
    TTLauncherView()
}
```

### 3. Tag targetable elements

Add `.ttTarget("identifier")` to any element you want the tour to highlight:

```kotlin
Button(
    modifier = Modifier.ttTarget("get-started"),
    onClick  = { /* … */ }
) {
    Text("Get started")
}
```

The identifier must match the **selector** set in the Tooltip Tour dashboard.

---

## Visual Inspector

The Visual Inspector lets you capture element identifiers directly from your device and
send them to the dashboard without leaving the app.

### Enable deep link handling

In your `Activity`:

```kotlin
override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    intent.data?.let { TooltipTour.instance?.handleDeepLink(it) }
}

override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    intent.data?.let { TooltipTour.instance?.handleDeepLink(it) }
    // …
}
```

### Register the URL scheme

In `AndroidManifest.xml` (inside your launcher `<activity>`):

```xml
<intent-filter>
    <action android:name="android.intent.action.VIEW" />
    <category android:name="android.intent.category.DEFAULT" />
    <category android:name="android.intent.category.BROWSABLE" />
    <data android:scheme="tooltiptour" android:host="inspect" />
</intent-filter>
```

---

## Scrollable lists

To let the SDK scroll a `LazyColumn` to a target element, use `ttScrollable`:

```kotlin
val listState = rememberLazyListState()
val indexMap  = items.mapIndexed { i, item -> item.id to i }.toMap()

LazyColumn(state = listState) {
    items(items, key = { it.id }) { item ->
        Row(modifier = Modifier.ttTarget(item.id)) { /* … */ }
    }
}
// Wire up the scroll bus
ttScrollable(listState, indexMap)
```

---

## Requirements

- Android 7.0+ (API 24)
- Jetpack Compose
- ComponentActivity (for lifecycle wiring)

---

## Distribution

Built and distributed via **JitPack** — no Maven Central account needed.
To generate the Gradle wrapper after cloning: `gradle wrapper --gradle-version 8.7`

---

## iOS SDK

The equivalent iOS SDK (Swift/SwiftUI) lives at
[tooltip-tour-ios](https://github.com/lovelysomething/tooltip-tour-ios).

---

## License

MIT © Lovely Something Ltd
