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
- CLI: `gradle assembleDebug` (requires JDK 17 and Android SDK platform 35/build-tools installed).
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
