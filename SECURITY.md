# Security review

## Play Protect warning

On October 3, 2026, Google Play Protect blocked the upstream downloadable SMS2Telegram 1.3.0 APK as potentially harmful. Its UI did not disclose a specific malware family or reason. SMS access alone was not established as the cause. The APK was not installed. We did not prove it matched the published source or that the warning was a false positive.

This fork was built from upstream tag 1.3.0 with local changes. An accepted installation is not proof of safety, and this source review is not a malware certification.

## Source review, October 31, 2026

Reviewed the app-owned Kotlin sources, manifest, resource links, Gradle configuration, local changes, and merged release manifest. Findings:

- App-owned networking consists of HTTPS requests to api.telegram.org for sendMessage and getUpdates, using the configured bot token and destination.
- A user-selected help action opens the upstream GitHub README in a browser.
- No app-owned dynamic code loading, shell execution, native libraries, hidden upload endpoint, advertising, or analytics was found.
- The SMS receiver requires Android's BROADCAST_SMS permission. App storage is private; backup and cleartext traffic are disabled.
- Dependencies are declared from Google Maven, Maven Central, and the Gradle Plugin Portal: AndroidX, Material, Kotlin/coroutines, OkHttp/Okio and build tooling. The Gradle distribution checksum is pinned, and the wrapper jar was compared against the downloaded Gradle distribution.
- The current upstream main differed from 1.3.0 by one commit (95a9ce5): a version-name change and a phone-state permission explanation dialog. No malicious behavior was found in that diff. This branch is rebased onto that reviewed commit and includes its permission explanation.

The review did not audit every line of transitive dependency code, perform a comprehensive vulnerability scan, or establish equivalence with the flagged upstream APK. These remain limits of the assessment.

## Data and operational limits

SMS contents, sender details and SIM labels go to Telegram. Bots do not use Secret Chats. The token and pending SMS are stored in app-private preferences without additional app-level encryption. See PRIVACY_POLICY.md.

Forwarding has no sender filter. It can expose OTPs to anyone in the destination chat. Network retries can duplicate messages after ambiguous acknowledgments. This version requires the first unlock after reboot. A corrupt outbox JSON is currently treated as empty; durable storage is not a guarantee against disk corruption or data clearing.

Do not post bot tokens, real SMS contents, signing keys, device backups, or private migration files in GitHub issues or commits. Use a private vulnerability report if available, or first open an issue requesting a private contact without exploit or credential details.
