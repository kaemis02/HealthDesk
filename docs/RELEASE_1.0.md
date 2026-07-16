# HealthDesk 1.0 Release

## Release Identity

- Application ID: `com.kaemis.healthdesk`
- Version name: `1.0.0`
- Version code: `1`
- License: `GPL-3.0-or-later`
- Distribution target: F-Droid

## Local Verification

Run from `V2/`:

```sh
./gradlew test
./gradlew assembleRelease
```

The release build is intentionally unsigned in this workspace. Do not add a keystore, passwords, or signing properties to the source tree. F-Droid builds and signs its own APK after accepting its external `fdroiddata` metadata.

## F-Droid Submission

Before creating the external F-Droid merge request:

1. Push this exact source to a public repository.
2. Create and push the immutable `v1.0.0` tag on the release commit.
3. Add screenshots under `fastlane/metadata/android/en-US/images/phoneScreenshots/` from a physical device or emulator.
4. Create `metadata/com.kaemis.healthdesk.yml` in a fork of `fdroiddata` using the public repository URL and the `v1.0.0` commit.
5. Build with `gradle: [assembleRelease]` and verify with F-Droid CI before opening the merge request.

The included Fastlane text metadata and icon are source-controlled so F-Droid can consume release descriptions without depending on external services.
