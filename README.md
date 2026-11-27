# YUMY Intruder Selfie 1.0.0 (MVP)

Android native starter for Xiaomi / HyperOS.

## Included
- Compose security dashboard (Vietnamese UI)
- DeviceAdminReceiver callbacks for Android `ACTION_PASSWORD_FAILED` and `ACTION_PASSWORD_SUCCEEDED`
- Local event history (SharedPreferences JSON; bounded to 100 events)
- Explicit, foreground CameraX front-camera test and private app-internal photo storage
- GitHub Actions workflow to build and upload a named debug APK artifact

## Important platform boundary
This app cannot observe failed Xiaomi system Face Unlock attempts. Android's public `DeviceAdminReceiver` password callback concerns device/profile challenge passwords; it is not a system-wide biometric failure feed. `BiometricPrompt` callbacks apply only to an authentication session started by this app. Camera capture is deliberately user-initiated while the app is visible. The app does not secretly activate the camera, bypass Android/HyperOS privacy controls, or collect credentials.

## Build
- Android Studio: open this folder, sync Gradle, then Build > Build APK(s).
- CLI: `gradle assembleDebug` (requires JDK 17 and Android SDK platform 35/build-tools installed; Java and Kotlin compilation targets are both set to JVM 17).
- GitHub: push to `main` or `master`, or run **Build YUMY Intruder Selfie APK** from Actions. Download the artifact `YUMY_Intruder_Selfie_v1.0.0_debug`.

## Device Admin setup
1. Install and open the app.
2. Tap **Bật theo dõi hệ thống** and review the Android administrator disclosure.
3. Enable it only if you accept the stated event-monitoring scope.
4. Android may not deliver password-failure events on every OEM / lock configuration. Validate on the target Xiaomi device. This is not proof of Face Unlock monitoring.

## Camera test
Tap **Kiểm tra camera trước**, grant camera permission, and tap **Chụp ảnh thử**. Images are saved under the app's internal files directory and are not uploaded.

## Current MVP limitations
- No silent/background camera capture.
- No system-wide Face Unlock / fingerprint failure monitoring.
- Event log is local and not yet Room-encrypted; photo files are app-private but not independently encrypted in this MVP.
- Debug APK only; release signing is intentionally not configured. Add a private signing key through GitHub Actions secrets before producing a distributable release.


## v1.0.1 – Camera capture verification

- Adds an in-app **ẢNH ĐÃ LƯU** gallery with thumbnails and full-size preview.
- After CameraX reports a successful capture, the app verifies that the output file exists and has non-zero bytes.
- Shows the filename and file size on success; reports CameraX error details on failure.
- Photos remain in app-private storage (`filesDir/intruder_photos`) and are not automatically exposed in the public Gallery app.
- The camera test is user-initiated. Android/HyperOS does not expose a public API for third-party apps to observe system Face Unlock failures or silently open the camera from the lock screen.
- Java and Kotlin JVM targets are both 17.

After returning from the camera test, the dashboard refreshes the saved-photo list.


## v1.0.2 updates
- Added per-photo deletion and delete-all with confirmation.
- Added app-level six-digit PIN lock, PIN change/disable requiring the current PIN, and relock after the app leaves the foreground.
- PIN is stored as a salted PBKDF2-HMAC-SHA256 hash; the plaintext PIN is not stored.
- System failed-password callbacks remain event logging only; they do not trigger background camera capture.

## v1.0.2 – Photo management and app lock

- Delete an individual saved photo from the photo list; the app asks for confirmation first.
- Delete all saved photos with a separate confirmation.
- Set a six-digit app PIN, change it, or disable it after confirming the current PIN.
- The app asks for the PIN on launch and again after the app has left the foreground.
- PIN verification uses a per-PIN random salt and PBKDF2-HMAC-SHA256 (120,000 iterations); the plaintext PIN is not persisted.
- Photos remain in the app-private `filesDir/intruder_photos` folder. Deleting a photo removes that file from the app-private folder.
- This app lock protects access to the app UI; it does not replace Android's device lock or encrypt the photo files themselves.

System password-failure callbacks are still recorded as events only. They do not automatically launch the camera from the lock screen or background.
