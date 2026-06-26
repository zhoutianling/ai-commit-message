# Marketplace Listing Draft

## Name

AI Commit Message

## Short Description

Generate Git commit messages from selected changes using your own OpenAI-compatible API key.

## Description

AI Commit Message is a lightweight Git commit message generator for JetBrains IDEs and Android Studio.

It adds a single action to the Commit panel: `AI Generate Commit Message`.

The plugin is intentionally small. It does not require login, does not create a tool window, does not provide chat or code completion, and does not make network requests until you explicitly click an action.

By default, it is configured for DeepSeek's OpenAI-compatible API:

- Base URL: `https://api.deepseek.com`
- Model: `deepseek-v4-flash`

You can also use any OpenAI-compatible API by changing the Base URL and Model in settings.

Privacy-oriented behavior:

- API keys are stored locally through JetBrains PasswordSafe.
- The plugin requires explicit consent before sending prompts or selected Git changes.
- Sensitive files such as `.env`, private keys, certificates, and keystores are skipped.
- No telemetry or author-operated backend is used.

## Tags

Git, VCS, AI, Commit, DeepSeek, Android Studio

## License

MIT

## Privacy Policy URL

https://github.com/zhoutl/ai-commit-message/blob/main/PRIVACY.md
