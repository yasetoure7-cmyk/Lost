# Lost Earth: The Last Survivor — Expanded Visual Upgrade

This archive is the editable Android source project. The upgrade keeps the existing campaign, database, controls, combat loop, zones, and procedural OpenGL ES renderer.

## Changes in this upgrade

- Expanded the playable movement boundary from 104 to 148 world units.
- Enlarged the terrain plane and camera far plane so the expanded outskirts remain visible.
- Added outer suburban blocks, more road connections, abandoned vehicles, roadside barriers, industrial clutter, rubble fields, and a forest belt.
- Added more layered equipment details to the existing human soldier mesh.
- Added extra silhouette and clothing detail to the existing zombie mesh, including neck/jaw forms, separate infected eyes, torn panels, and ragged shoulder shapes.
- Updated KSP to 2.3.6 to include the headless AWT application-service fix for issue #2763.

## Important graphics note

This is a procedural-graphics upgrade, not a photorealistic asset replacement. The current project does not contain rigged GLB/FBX character models, Mixamo animation files, or high-resolution environment asset packs. Human and zombie characters are still rendered from procedural OpenGL meshes. The new geometry adds more world space and environmental detail while preserving the existing lightweight Android renderer. A later photorealistic pass will require licensed rigged character/environment assets and a model-import pipeline.

## Build

The project uses the existing Gradle wrapper configuration and GitHub Actions workflow. Push the project contents to the repository's `main` branch (or run the workflow manually) to build the debug APK. The workflow uploads `app/build/outputs/apk/debug/*.apk` as the `Lost-Earth-APK` artifact.

## Android build requirements

- JDK 17
- Gradle wrapper 9.3.1
- Android SDK platform 36
- Android build tools 36.0.0

## Source layout

- `app/src/main/java/com/example/game/WorldGenerator.kt` — procedural world geometry and map boundary.
- `app/src/main/java/com/example/game/LostEarthGlRenderer.kt` — OpenGL scene, camera, and terrain rendering.
- `app/src/main/java/com/example/game/HumanoidMeshRenderer.kt` — player, survivor, squad, and zombie meshes.
- `gradle/libs.versions.toml` — dependency and plugin versions.
- `.github/workflows/main.yml` — GitHub Actions APK build.
