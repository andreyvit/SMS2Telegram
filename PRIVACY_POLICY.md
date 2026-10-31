# SMS Fwd data handling

Updated October 31, 2026. This describes the SMS Fwd fork, not an official Tiger-Workshop release.

## Data flow

SMS Fwd receives new SMS broadcasts, reads the sender and message body, adds the receipt time and available SIM label, and sends them over HTTPS to the configured Telegram Bot API destination. Setup also queries Telegram bot updates to help discover chat IDs. There is no maintainer-operated server, advertising, or analytics.

Telegram receives forwarded content, including any verification codes or other sensitive messages. Bot messages are not Telegram Secret Chats. Destination chat members can read the forwarded messages. Choose a private destination and protect the bot token.

## Local storage

The bot token, chat ID, preferences, and last forwarding status are stored in Android app-private preferences. Pending SMS contents and sender information are persisted in an app-private outbox until Telegram acknowledges delivery. Failed deliveries remain queued for retry. The last status can retain the most recent sender information.

These preferences have no additional app-level encryption. Android’s sandbox and device storage protections apply. Android app backup is disabled. Clearing app data or uninstalling removes local configuration and pending messages; it does not delete messages already sent to Telegram or retained by the system SMS app.

## Permissions

- RECEIVE_SMS: receive new SMS; the app does not request READ_SMS or read the existing inbox.
- READ_PHONE_STATE: obtain active subscription and SIM label information.
- INTERNET and library network-state permissions: contact Telegram and schedule network-dependent work.
- RECEIVE_BOOT_COMPLETED and WorkManager scheduling permissions: resume pending delivery after startup and run recovery work.

Forwarding can be disabled in the app. Already queued messages remain locally stored. Force-stop prevents background operation until the app is opened again. After reboot, this version needs the phone to be unlocked once before credential-protected storage becomes available.

Report problems in this fork’s GitHub issues, without posting real SMS contents, bot tokens, or other private data. The upstream developer does not maintain this fork.
