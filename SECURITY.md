# Security Policy

Do not commit Android signing keys, keystore passwords, live-stream keys, platform access tokens, private endpoints, user data, analytics credentials, or production secrets.

Use Android/CI secret stores and protected deployment settings for sensitive values. Keep test values non-sensitive and clearly marked.

If a secret is exposed, revoke or rotate it immediately, remove it from active branches and CI settings, review repository history and workflow logs, and rebuild with replacement credentials.

Never place live stream keys, signing credentials, personal data, or private tokens in public issues, pull requests, screenshots, examples, logs, or documentation.
