# Marketplace Listing Draft

## Name

AI Git Commit Writer

## Short Description

Generate Git commit messages from selected changes using DeepSeek or any OpenAI-compatible API. Diff quality aligned with Qoder CN (通义灵码).

## Description

**AI Git Commit Writer** is a lightweight Git commit message generator for JetBrains IDEs and Android Studio. One click in the Commit panel — no login, no tool window, no background indexing.

### How It's Different

Most AI commit plugins send basic file-change summaries. This plugin generates **standard unified diffs** using JetBrains `IdeaTextPatchBuilder` (the same pipeline Qoder CN uses) and includes **recent commit history** for style matching — giving you messages that feel consistent with your project's conventions.

### Features

- One-click commit message generation in the Commit panel
- Standard unified diffs via `IdeaTextPatchBuilder` + `UnifiedDiffWriter`
- Recent commit history (last 3) passed to AI for style matching
- Direct replace — no Replace/Append confirmation dialog
- Supports DeepSeek, Qwen, OpenAI, or any OpenAI-compatible endpoint
- API key stored locally via JetBrains PasswordSafe
- Auto-skips sensitive files (.env, keys, certs, keystores) and binaries
- Explicit consent required before any data leaves your IDE
- No telemetry, no author-operated backend

### Default Configuration

- Base URL: `https://api.deepseek.com`
- Model: `deepseek-v4-flash`
- Language: 中文
- Commit Style: Conventional Commit

### Quick Start

1. Install from disk (`Settings > Plugins > Install Plugin from Disk`)
2. Configure your API key at `Settings > Tools > AI Commit Message`
3. Open the Commit panel, click `AI Generate Commit Message`

## Tags

Git, VCS, AI, Commit, DeepSeek, Android Studio, Conventional Commits

## License

MIT

## Privacy Policy URL

https://github.com/zhoutianling/ai-commit-message/blob/main/PRIVACY.md
