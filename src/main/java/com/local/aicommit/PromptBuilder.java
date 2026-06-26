package com.local.aicommit;

import com.intellij.openapi.project.Project;

final class PromptBuilder {
    private PromptBuilder() {
    }

    static String systemPrompt() {
        return "You generate Git commit messages. Output only the commit message. "
            + "Do not use Markdown fences. Do not explain your reasoning.";
    }

    static String userPrompt(Project project, AiCommitSettingsState.StateData settings, String diffSummary) {
        return "Project: " + project.getName() + "\n"
            + "Language: " + settings.language + "\n"
            + "Commit style: " + settings.commitStyle + "\n"
            + "Rules:\n"
            + "- Prefer one concise subject line.\n"
            + "- For Conventional Commit, use exactly: type(scope): summary\n"
            + "- Allowed types: feat, fix, refactor, perf, docs, test, build, ci, chore, style, revert.\n"
            + "- Use Chinese for the summary unless the diff clearly requires technical English.\n"
            + "- Add a body only when the change is too broad for one line.\n"
            + "- Do not include secrets or quote sensitive values.\n\n"
            + diffSummary;
    }
}
