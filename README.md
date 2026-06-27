# AI Git Commit Writer

[![Version](https://img.shields.io/badge/version-0.3.0-blue)](https://github.com/zhoutianling/ai-commit-message)
[![License](https://img.shields.io/badge/license-MIT-green)](LICENSE)
[![Android Studio](https://img.shields.io/badge/Android%20Studio-261%2B-orange)](https://developer.android.com/studio)

A lightweight Git commit message generator for JetBrains IDEs and Android Studio. One click in the Commit panel, and AI writes your commit message — with diff quality aligned to Qoder CN (通义灵码).

**Intentionally minimal:**

- No login, no account
- No tool window, no chat history
- No code completion, no background indexing
- No network request until you click a button

## Why This Plugin?

Qoder CN (通义灵码) generates high-quality commit messages by sending proper **unified diffs** and **recent commit history** to its model. This plugin adopts the exact same approach — but lets you use **any OpenAI-compatible API** (DeepSeek, Qwen, OpenAI, etc.) with your own key.

## Features

### Core

- Adds `AI Generate Commit Message` button to the Git Commit panel
- Generates Conventional Commit style messages in Chinese by default
- One-click operation — no confirmation dialogs after clicking

### Diff Quality (aligned with Qoder CN)

- Uses JetBrains `IdeaTextPatchBuilder` + `UnifiedDiffWriter` for standard unified diffs
- Handles new files, renames, and binary/sensitive file skipping
- 50-file / 70K-character limits prevent token overflow

### Smart Context

- Reads the **last 3 commit messages** and passes them to the AI for style and tone matching
- Sends project name, preferred language, and commit style rules

### Privacy & Security

- API keys stored locally via JetBrains PasswordSafe
- Explicit consent required before any data leaves your IDE
- Sensitive files (`.env`, private keys, certs, keystores) auto-skipped
- No telemetry, no author-operated backend

## Quick Start

1. **Install** — Download from [Releases](https://github.com/zhoutianling/ai-commit-message/releases) and install via `Settings > Plugins > Install Plugin from Disk`
2. **Configure** — Open `Settings > Tools > AI Commit Message`
3. **Generate** — Open the Commit panel, click `AI Generate Commit Message`

### Default Configuration

| Setting | Default |
|---------|---------|
| Base URL | `https://api.deepseek.com` |
| Model | `deepseek-v4-flash` |
| Language | 中文 |
| Commit Style | Conventional Commit |

### Recommended Providers

| Provider | Base URL | Model |
|----------|----------|-------|
| [DeepSeek](https://platform.deepseek.com) | `https://api.deepseek.com` | `deepseek-v4-flash` |
| [Qwen (via OpenRouter)](https://openrouter.ai) | `https://openrouter.ai/api/v1` | `qwen/qwen-plus` |
| Any OpenAI-compatible | `your-endpoint` | `your-model` |

## How It Works

```
Commit Panel Click
    ↓
DiffCollector ←─ IdeaTextPatchBuilder (unified diff)
GitHistoryCollector ←─ git log --max-count=3
    ↓
PromptBuilder (system + user prompt with context)
    ↓
OpenAiCompatibleClient → your configured API
    ↓
Commit message written directly to the commit box
```

## Build

Requires [Android Studio](https://developer.android.com/studio) installed at default path.

```powershell
.\scripts\build-plugin.ps1
```

Output: `outputs/ai-commit-message-plugin-0.3.0.zip`

## License

MIT License. See [LICENSE](LICENSE).
