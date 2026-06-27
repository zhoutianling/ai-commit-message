# Changelog

## 0.3.4

- Fix: add missing <depends>Git4Idea</depends> — runtime NoClassDefFoundError on GitRepositoryManager was silently crashing the generation task.
- Fix: catch (Throwable) instead of catch (Exception) so errors like NoClassDefFoundError show a dialog instead of being silently swallowed.
- Simplify: settings UI now only has Base URL, API Key, and Model. Language and commit style are hardcoded (Chinese + Conventional Commit).
- Change collection: multi-strategy fallback (handler -> selected changes -> all changes) for broader IDE compatibility.

## 0.3.3

- Fix: use COMMIT_WORKFLOW_HANDLER to collect changes (same approach as Qoder CN), replacing unreliable SELECTED_CHANGES/CHANGES data keys. This fixes the "click does nothing" issue.
- Add top-level 	ry/catch in ctionPerformed so any pre-background exception shows an error dialog instead of silently failing.

## 0.3.2

- Fix: catch RuntimeException from IdeaTextPatchBuilder to prevent silent failures when clicking the generate button.
- Fix: multi-file changes now produce multi-line commit bodies with bullet points per logical change.

## 0.3.1

- Generate unified diffs via IdeaTextPatchBuilder + UnifiedDiffWriter (aligning diff quality with Qoder CN).
- Include the 3 most recent commit messages as style context for the AI.
- Always directly replace the commit message without a Replace/Append prompt.
- Add GitHistoryCollector for recent Git history.

## 0.2.0

- Prepare the plugin for public release.
- Add explicit consent, DeepSeek defaults, configuration test button, PasswordSafe storage.

## 0.1.1

- Default to DeepSeek OpenAI-compatible API. Add configuration test button.

## 0.1.0

- Initial private build.