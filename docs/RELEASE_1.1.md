# HealthDesk 1.1 Release Notes

- Version name: `1.1.0`
- Version code: `2`
- Application ID: `com.kaemis.healthdesk`
- License: `GPL-3.0-or-later`

## Validation

Run from `V2/`:

```sh
./gradlew test
./gradlew assembleDebug
./gradlew assembleRelease
```

Full-screen alarms, Android 12+ splash rendering, exact-alarm permission behavior,
and lock-screen notification behavior require manual verification on a physical device.
