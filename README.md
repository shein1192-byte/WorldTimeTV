# World Time TV

Android TV world clock for Xiaomi S65 (HyperOS), controlled with a remote.

- Russian UI and 14 cities
- Large time and date, offline operation
- Saves selected city
- This version does **not** modify the system time zone.

## Build
Go to **Actions → Build Android TV APK → Run workflow** or push to main. On success download the artifact `WorldTimeTV-debug-apk`, extract `app-debug.apk`, and install it on the TV.

The app shows real-world time based on the TV's system clock. To change the *system* time zone on Xiaomi S65, the verified ADB command is `cmd alarm set-timezone Asia/Vladivostok`; normal apps cannot execute it without privileged rights.

## System time zone (v1.1)
Select a city, then click APPLY SYSTEM TIME ZONE. Requires Shizuku started through ADB and permission granted to this app. The app sends cmd alarm set-timezone through the ADB shell identity and verifies persist.sys.timezone. This changes the system timezone, not the UTC clock. Restart Shizuku after reboot without root.

## v1.2
Choose a city using the remote and press OK: its timezone is immediately applied to the TV via Shizuku, without a separate Apply button. The Android TV banner now displays the clock graphic. To refresh the launcher icon, install the newly built APK first; some firmware caches the previous image. Shizuku needs to be running and authorized to update the system timezone. This updates timezone only, not clock UTC time.

Version 1.5 build verification on GitHub Actions.
