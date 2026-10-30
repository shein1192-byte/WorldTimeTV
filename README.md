# WorldTimeTV v1.5 — manual system clock (experimental)

For Xiaomi S65 Mini LED with network ADB TCP/5555 enabled.

- Manual date and time via TV remote; no city/timezone picker.
- Live display of the **existing TV timezone's UTC offset**, including DST, when applicable.
- Uses embedded local ADB client to execute `settings put global auto_time 0` and `cmd alarm set-time <epoch_ms>`.
- Connects to **127.0.0.1:5555**, the current TV's loopback address: no IP discovery/input is needed.
- **Does not require Shizuku or ship its binaries**: functionality is replaced with direct ADB rather than repackaging the third-party Shizuku manager.
- User must enable network ADB TCP port 5555 and approve the application's own RSA ADB key on the TV if requested.
- The timezone itself is NOT changed. All manual inputs are interpreted in the TV's existing timezone.
- Security: turn off ADB networking when it is not needed.

Build/test: GitHub Actions; operation depends on vendor firmware accepting loopback ADB and shell clock commands. Not yet hardware-verified.
