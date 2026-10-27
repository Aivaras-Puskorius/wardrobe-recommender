# Clankdrobe

Android app. Catalog of the clothes you own, outfit suggestions built from it.

Status: milestone 2 in progress. Items are stored on the phone, starting with 17 sample items. Weather and photo recognition are still samples.

| Wardrobe | Item | Add item | Outfit ideas |
|---|---|---|---|
| <img src="app/src/test/screenshots/app.wardrobe.feature.wardrobe.WardrobeScreenshotTest.grid.png" width="200"> | <img src="app/src/test/screenshots/app.wardrobe.feature.item.ItemDetailScreenshotTest.loaded.png" width="200"> | <img src="app/src/test/screenshots/app.wardrobe.feature.item.AddItemScreenshotTest.suggestion.png" width="200"> | <img src="app/src/test/screenshots/app.wardrobe.feature.outfits.OutfitResultsScreenshotTest.outfits.png" width="200"> |

## Requirements

- JDK 17
- Android SDK Platform 37: `sdkmanager "platforms;android-37.0"`
- Device or emulator with Android 8.0 (API 26) or newer. Portrait only.

## Build and run

```
./gradlew assembleDebug     # build app/build/outputs/apk/debug/app-<abi>-debug.apk
./gradlew installDebug      # install on the connected device
./gradlew installRelease    # install the minified build, for demos
```

The build makes one APK per processor type, and the install tasks pick the device's. The release build is shrunk with R8 (arm64 about 19 MB, most of it ML Kit's native code and the image model) and signed with the local debug key, so it installs over the debug build. Not for publishing.

## Use

| Screen | Actions |
|---|---|
| Wardrobe tab | Category chips. Filter icon: colors, seasons. Tap a card for details. Add item button. |
| Item | Edit. Delete, with Undo on the snackbar. Style it: outfit ideas for this item, today. |
| Add item | Take a photo or Choose from gallery, then confirm the suggested type and color, set material, fit, warmth and seasons, Save. |
| Outfits tab | Pick a day and an item, Show outfits. Like or dislike each outfit. |
| Settings tab | Theme: System, Light, Dark. |

## Prototype limits

| Feature | Now |
|---|---|
| Storage | Room database on the phone. The first launch adds the 17 sample items. |
| Photos | Taken with the phone's camera app or picked from the gallery, kept at full size in app storage. Sample items show drawings. |
| Photo recognition | On the phone. A clothes model trained for the app (`tools/train_garment_model.py`) suggests the type. An ImageNet model splits its pants, shoes and outerwear classes. ML Kit's standard labels (Google Play services downloads that model on first use) suggest wool or leather, and decide the type when the clothes model is unsure. They don't tell denim apart, so jeans need ImageNet's "jean" label and usually come out as trousers. When no model is sure of the type, the form leaves it for you to pick. The clothes model's boots class learned from 55 hand-sorted photos, so it finds some boots and calls some dark leather shoes boots. The color comes from the middle of the photo, so a plain background works best. `adb logcat -s MlKitRecognizer` shows the raw labels. |
| Outfits | Built by rules from the wardrobe and the day's sample weather, up to three per item. No memory of past suggestions yet. |
| Weather | Fixed sample week for Kaunas, starting today. Celsius only. |
| Likes | Kept until the results screen closes. |
| Theme | Kept until the app restarts. |

## Test

```
./gradlew testDebugUnitTest      # unit and Compose UI tests (Robolectric)
./gradlew verifyRoborazziDebug   # same, plus screenshot comparison
./gradlew recordRoborazziDebug   # re-record screenshots after an intended UI change
./gradlew lintDebug
```

- Reference screenshots: `app/src/test/screenshots/`
- Database schemas, one file per version: `app/schemas/`. The debug build packs them as assets for `WardrobeMigrationTest`.
- CI: `.github/workflows/ci.yml` runs `assembleDebug assembleRelease verifyRoborazziDebug lintDebug` on every push to `main` and on pull requests.

## Code

```
app/src/main/java/app/wardrobe/
  AppContainer.kt         shared objects: repository, recognizer, forecast, theme
  data/                   repository (Room, and in memory for tests), sample data, stand-in recognizer
  domain/model/           items, colors, forecast
  domain/engine/          outfit rules, scoring and reasons
  feature/wardrobe/       grid and filters
  feature/item/           detail, add, edit
  feature/outfits/        generator, results, outfit titles and reason sentences
  feature/settings/       settings
  navigation/             routes, back stacks per tab, app shell
  ui/                     theme, shared components, labels
tools/
  theme_palette.py        generates ui/theme/Color.kt, checks WCAG AA contrast
  install_icon.py         imports a Material Symbols icon into res/drawable
```

Screens with data: a ViewModel exposing one `StateFlow` of UI state, and a stateless `...Content` composable that the screenshot tests render. Settings reads the theme from `AppContainer`.

## Stack

Kotlin 2.4.21, Jetpack Compose (BOM 2026.09.00), Material 3, Navigation 3 1.2.0, Room 2.8.5 with KSP 2.3.12, Coil 3.6.3, ML Kit image labeling 16.0.8 and custom image labeling 17.0.3, AGP 9.4.1, Gradle 9.8.2. Tests: JUnit 4, Robolectric 4.17, Roborazzi 1.77.0.

Icons: Material Symbols, Apache License 2.0. Image model `app/src/main/assets/models/imagenet_efficientnet_lite0_int8.tflite`: EfficientNet-Lite0 int8 by TensorFlow, from tensorflow/efficientnet on Kaggle, Apache License 2.0. Clothes model `garments_mobilenetv2_int8.tflite`: trained by `tools/train_garment_model.py` on the clothing dataset by Alexey Grigorev (github.com/alexeygrigorev/clothing-dataset, CC0 1.0), starting from Keras 3.15.1's MobileNetV2 ImageNet weights (Keras: Apache License 2.0).
