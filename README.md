# SMS Fwd

Android SMS-to-Telegram forwarder, based on [SMS2Telegram 1.3.1 source](https://github.com/imTigger/SMS2Telegram/tree/95a9ce580fe6a078e1eb36faa4d32b30506040b5).

Package: `com.andreyvit.homesms`. Version: `1.3.1-fwd.1` (8).

Originally installed as **Home SMS**; this source revision uses the **SMS Fwd** display name. The separate package ID is retained for compatibility.

A small personal fork for a dedicated, always-charging SMS receiver. It keeps the original interface while improving delivery recovery and dual-SIM labels. The maintainer has used this version since October 3, 2026; it is not tested across all Android devices.

## Setup

1. Build and sign the APK below, then install it on your Android phone. This fork is a separate app from the original SMS2Telegram; it does not upgrade that installation.
2. Create a bot with Telegram’s [@BotFather](https://t.me/BotFather). Keep the token private. Start a chat with your bot, or add it to the destination group/channel with permission to post.
3. Open SMS Fwd, grant SMS permission, enter the bot token and destination chat ID, then validate the setup. **Find Chat ID** can discover chats from recent bot message updates; for a channel, enter its numeric chat ID manually if it is not listed.
4. Enable forwarding. Enable **Show SIM Name** and grant phone-state permission for SIM labels. Rename your SIMs in Android settings to distinguish the lines.
5. Allow background/autostart operation and unrestricted battery usage in your phone’s settings. Disable the old forwarder if you want to avoid duplicate delivery.
6. Send a real SMS to each line while the phone is locked, and confirm both the Android inbox and Telegram destination. Also test recovery after temporarily disconnecting Wi-Fi.

Existing inbox messages are not imported. Only newly received SMS is forwarded. MMS and RCS are not supported.

## Changes from upstream

- Queue new deliveries with WorkManager APPEND_OR_REPLACE to avoid the KEEP completion race.
- Recover pending messages on app opening, boot completion, package replacement, and a periodic recovery check.
- Show one-based SIM numbers and Android SIM names; resolve subscription IDs before slot fallbacks.
- Join multipart SMS without inserted newlines and split long Telegram messages without breaking surrogate pairs.
- Check durable enqueue writes and Telegram JSON responses.
- Disable Android backup and cleartext traffic.

The upstream MIT license and copyright are retained in LICENSE.md. Original setup documentation is in [docs/UPSTREAM_README.md](docs/UPSTREAM_README.md). Messages go to Telegram through the configured bot; Telegram receives their contents.

## Security review

Google Play Protect blocked the upstream downloadable 1.3.0 APK during setup. The reason was not established, and this project does not claim it was a false positive. This fork was built from source; that flagged APK is not included or used. See [SECURITY.md](SECURITY.md) for the review scope and remaining limitations.

## Build

Use JDK 17, Android SDK platform 36, and Android build tools 35.0.0. Android Studio is optional.

```sh
export JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home
export ANDROID_HOME=/opt/homebrew/share/android-commandlinetools
./gradlew testDebugUnitTest assembleRelease
```

Unsigned APK: `app/build/outputs/apk/release/app-release-unsigned.apk`.

To update the installed app, sign with its existing key. For your own installation, generate and keep your own signing key. The example below assumes a key alias `home-sms` and private files stored outside the repository. Keep a secure backup: a different key cannot update an existing installation in place.

```sh
"$ANDROID_HOME/build-tools/35.0.0/apksigner" sign \
  --ks "$HOME/.local/share/home-sms-forwarder/signing.jks" \
  --ks-key-alias home-sms \
  --ks-pass "file:$HOME/.local/share/home-sms-forwarder/signing-password" \
  --out app/build/outputs/apk/release/home-sms.apk \
  app/build/outputs/apk/release/app-release-unsigned.apk
```

Increase versionCode for a future release. Bot credentials are configured on the device and are not included here.

## Testing and device setup

Unit tests cover Unicode message splitting. The instrumentation tests in `HomeSmsDeviceTest` were used as individually invoked migration and offline-recovery steps on the configured phone. Do not run that whole class as a generic test suite: migration requires a private migration.json, and delivery tests send messages to the configured Telegram destination.

The dedicated Xiaomi phone also needed MIUI autostart enabled and PowerKeeper disabled to stop its nightly process cleanup. Those are device settings, not app changes. The old SMS2Telegram installation was retained disabled for rollback.

After reboot, unlock the phone once: this version uses credential-protected storage and does not support Direct Boot. Network errors can cause retries; an ambiguous Telegram response can produce a duplicate. Force-stopping or disabling the app prevents normal reception until it is enabled/opened again.

## Contributing and license

The original installation was based on upstream tag `1.3.0` (`2732370b8245e8c72fcb2afc06d14f063ce232f2`). This branch is rebased onto upstream `95a9ce580fe6a078e1eb36faa4d32b30506040b5` (1.3.1), adding its phone-state permission explanation while retaining our fixes and upstream history. It is not an official release by Tiger-Workshop. Please report fork-specific issues here; general upstream issues belong in the original project.

Licensed under MIT. The original [LICENSE.md](LICENSE.md), including Tiger-Workshop’s copyright, is unchanged. See [PRIVACY_POLICY.md](PRIVACY_POLICY.md) for this fork’s data handling.
