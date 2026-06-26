# Privacy Policy

AI Commit Message is a local JetBrains IDE plugin. It does not require an account, does not operate a backend service, and does not collect analytics or telemetry.

## Data Stored Locally

The plugin stores configuration in the IDE settings:

- Base URL
- Model
- preferred language
- commit style
- maximum diff size
- consent state

The API key is stored locally using JetBrains PasswordSafe.

## Data Sent to AI Services

The plugin sends data only when the user explicitly clicks one of these actions:

- `Test Configuration`
- `AI Generate Commit Message`

For `Test Configuration`, the plugin sends a minimal health-check prompt to the configured AI service.

For `AI Generate Commit Message`, the plugin sends selected Git change summaries and text diffs directly from the IDE to the configured OpenAI-compatible API endpoint.

The plugin does not send data to any server controlled by this plugin's author.

## Sensitive File Handling

The plugin skips file contents for common sensitive or binary files, including:

- `.env` files
- private keys
- certificates
- keystores
- `.ssh` files
- binary media and archive files

Skipped files may still be included by path and change status so the generated commit message can describe the type of change.

## User Control

The plugin requires explicit consent in `Settings > Tools > AI Commit Message` before testing or generating messages.

Users control the configured API endpoint and API key. Data handling by that external AI service is governed by that service's own terms and privacy policy.

## Contact

For privacy questions, contact: tianling.zhou@mob.cool
