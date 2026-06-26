# AI Commit Message

AI Commit Message is a lightweight JetBrains IDE plugin that generates Git commit messages from selected changes in the Commit panel.

It is intentionally small:

- no login
- no tool window
- no chat history
- no code completion
- no background repository indexing
- no network request until the user clicks an action

The plugin uses an OpenAI-compatible API configured by the user. DeepSeek is the default provider.

## Features

- Adds `AI Generate Commit Message` to the Git Commit panel.
- Generates concise Conventional Commit style messages in Chinese by default.
- Supports any OpenAI-compatible endpoint through `Base URL`, `API Key`, and `Model`.
- Stores API keys locally with JetBrains PasswordSafe.
- Skips sensitive files such as `.env`, private keys, certificates, keystores, and binary files.
- Requires explicit user consent before sending prompts or selected Git changes to the configured AI service.

## Default DeepSeek Configuration

Open `Settings > Tools > AI Commit Message`:

- Base URL: `https://api.deepseek.com`
- Model: `deepseek-v4-flash`
- API Key: your DeepSeek API key

Use `Test Configuration` to verify the settings.

## Privacy

The plugin does not operate a backend service and does not collect analytics.

When you click `AI Generate Commit Message`, selected Git change summaries and text diffs are sent directly from your IDE to the configured AI service. Read [PRIVACY.md](PRIVACY.md) for details.

## Build

This repository includes a lightweight PowerShell build script for local Android Studio installations:

```powershell
.\scripts\build-plugin.ps1
```

The script compiles against:

```text
C:\Program Files\Android\Android Studio
```

and writes the installable ZIP to the local `outputs` directory used by this workspace.

## License

MIT License. See [LICENSE](LICENSE).
