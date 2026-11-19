# My Photos (Android starter app)

This is a simple offline gallery app with three local profiles. Each profile has its own
"Show this phone's photos" switch. Turning the switch off hides the device-photo grid
inside that profile only. It does not delete or move any photos.

## Important behaviour
- Profile 1, Profile 2 and Profile 3 are local app profiles, not Google accounts.
- No sign-in, upload, cloud backup, or network permission is included.
- The visibility setting is per profile.
- Android photo permission is app-wide. The app can read device photos when permission
  is granted, but its own UI hides the gallery separately for each profile.
- This starter displays images from MediaStore. Android 14+ may offer limited photo
  access; grant full photo access in Android settings if you want the full gallery.
- This is a starter project, not a finished Google Photos replacement. It currently
  shows a grid only; albums, search, editing, video playback, and secure app locking
  are not included.

## Build an APK
1. Install Android Studio on a computer.
2. Open this folder as an existing Android project.
3. Allow Gradle sync and install any requested Android SDK (API 35).
4. Select Build > Build Bundle(s) / APK(s) > Build APK(s).
5. The debug APK will be in `app/build/outputs/apk/debug/`.

For a release APK, configure your own signing key in Android Studio before distribution.
