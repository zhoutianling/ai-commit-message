# Changelog

## 0.3.1

- Fix: catch RuntimeException from IdeaTextPatchBuilder to prevent silent failures when clicking the generate button.
- Fix: multi-file changes now produce multi-line commit bodies with bullet points per logical change (instead of always a single line).
- Harden: empty diff collection shows a proper info message instead of silently returning.
- Harden: Git history collection failure no longer blocks generation (falls back gracefully).

## 0.3.0

- Generate unified diffs via `IdeaTextPatchBuilder` + `UnifiedDiffWriter` (aligning diff quality with Qoder CN).
- Include the 3 most recent commit messages as style context for the AI.
- Always directly replace the commit message without a Replace/Append prompt.
- Remove `ChangeSnapshot`; new `DiffCollector` returns `List<String>` of unified diffs.
- Add `GitHistoryCollector` for recent Git history.

## 0.2.0

- Prepare the plugin for public release.
- Add explicit consent before sending prompts or Git changes to the configured AI service.
- Add DeepSeek defaults.
- Add a configuration test button.
- Store API keys locally through JetBrains PasswordSafe.
- Skip sensitive and binary file contents.

## 0.1.1

- Default to DeepSeek OpenAI-compatible API.
- Add a configuration test button.

## 0.1.0

- Initial private build.