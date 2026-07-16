# Android Limitations

HealthDesk is local-only and does not use a backend, cloud sync, Firebase, Google services, or remote crash/analytics services.

Known Android constraints for the MVP:

- Exact alarms may be restricted by Android version, manufacturer policy, or user settings. HealthDesk uses exact alarms when available and falls back to inexact alarms when required.
- Battery optimization can delay background reminders or focus alarms on some devices. Users may need to allow unrestricted battery usage for maximum reliability.
- Notification delivery depends on `POST_NOTIFICATIONS` permission on Android 13+ and on the user's channel settings.
- Foreground focus sessions remain visible through a persistent notification, but lock-screen visibility depends on system privacy settings.
- Boot, app update, time change, and timezone change resync are implemented, but final reliability must be verified on physical devices from different manufacturers.
- Import/export uses Android document providers and share sheet locally. Real provider behavior should be verified on physical devices before release.
