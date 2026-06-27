package com.local.aicommit;

import com.intellij.openapi.project.Project;
import java.util.List;
import java.util.stream.Collectors;

final class PromptBuilder {
    private PromptBuilder() {}

    static String systemPrompt() {
        return "You generate Git commit messages. Output only the commit message. "
            + "Do not use Markdown fences. Do not explain your reasoning.";
    }

    static String userPrompt(Project project, AiCommitSettingsState.StateData settings,
                             List<String> codeDiffs, List<String> recentCommitMessages) {
        int fileCount = codeDiffs.size();

        StringBuilder sb = new StringBuilder();
        sb.append("Project: ").append(project.getName()).append("\n");
        sb.append("Language: ").append(settings.language).append("\n");
        sb.append("Commit style: ").append(settings.commitStyle).append("\n");

        if (recentCommitMessages != null && !recentCommitMessages.isEmpty()) {
            sb.append("\nRecent commit messages (for style reference):\n");
            for (String msg : recentCommitMessages) {
                String trimmed = msg.trim();
                if (!trimmed.isEmpty()) {
                    sb.append("  ").append(trimmed.replace("\n", "\n  ")).append("\n");
                }
            }
        }

        sb.append("\nRules:\n");
        sb.append("- For Conventional Commit, use exactly: type(scope): summary\n");
        sb.append("- Allowed types: feat, fix, refactor, perf, docs, test, build, ci, chore, style, revert.\n");
        sb.append("- Use ").append(settings.language).append(" for the summary unless the diff clearly requires technical English.\n");
        sb.append("- Do not include secrets or quote sensitive values.\n");
        sb.append("- Match the style and tone of recent commit messages shown above.\n");
        sb.append("- ").append(fileCount).append(" file(s) changed. ");

        if (fileCount <= 2) {
            sb.append("Describe the changes concisely in one line.\n");
        } else {
            sb.append("If the changes span multiple concerns, use a multi-line commit body: ");
            sb.append("one subject line summarizing the overall change, followed by a blank line, ");
            sb.append("then one bullet (-) per distinct logical change. ");
            sb.append("Group related files together under a common change description.\n");
        }

        sb.append("\nCode changes:\n");
        String diffs = codeDiffs.stream()
            .filter(d -> d != null && !d.isBlank())
            .collect(Collectors.joining("\n"));
        sb.append(diffs);

        return sb.toString();
    }
}