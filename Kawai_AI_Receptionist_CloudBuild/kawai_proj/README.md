# Kawai AI Receptionist (starter prototype)

This is a starter Android project for experimenting with call screening. It is not yet a live AI answering assistant.

## Build without installing Android Studio

This project includes a GitHub Actions workflow at `.github/workflows/android.yml`.

1. Create a GitHub repository and upload the **contents** of this project (including the hidden `.github` folder).
2. Open the repository's **Actions** tab and enable Actions if prompted.
3. Open **Build Kawai AI Receptionist APK** and choose **Run workflow** (or push/commit a change to `main`).
4. When the workflow finishes successfully, open its run, scroll to **Artifacts**, and download `kawai-ai-receptionist-debug-apk`. Extract the ZIP to get `app-debug.apk`.
5. Transfer the APK to your phone and install it only if you trust the source. Android may ask you to allow installs from the browser/file manager you used.

## Current limitations

- The prototype requests contacts permission and the call-screening role.
- It checks whether an incoming number is saved in contacts and can notify for unknown callers.
- It currently allows calls by default; it does not automatically answer calls.
- It does not capture cellular call audio, transcribe speech, or run a live AI conversation.
- Android/MIUI versions and permissions may affect behavior. Test carefully; do not rely on it for emergency or important calls.
