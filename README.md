# World Time TV

Android TV world clock for Xiaomi S65 (HyperOS), controlled with a remote.

- Russian UI and 14 cities
- Large time and date, offline operation
- Saves selected city
- This version does **not** modify the system time zone.

## Build
Go to **Actions → Build Android TV APK → Run workflow** or push to main. On success download the artifact `WorldTimeTV-debug-apk`, extract `app-debug.apk`, and install it on the TV.

The app shows real-world time based on the TV's system clock. To change the *system* time zone on Xiaomi S65, the verified ADB command is `cmd alarm set-timezone Asia/Vladivostok`; normal apps cannot execute it without privileged rights.
