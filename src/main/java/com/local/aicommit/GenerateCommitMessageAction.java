package com.local.aicommit;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vcs.CommitMessageI;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.VcsDataKeys;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vcs.changes.CurrentContentRevision;
import com.intellij.vcs.commit.AbstractCommitWorkflowHandler;
import org.jetbrains.annotations.NotNull;

import javax.swing.SwingUtilities;
import java.util.ArrayList;
import java.util.List;

public final class GenerateCommitMessageAction extends AnAction {
    @Override
    public void update(@NotNull AnActionEvent event) {
        Project project = event.getData(CommonDataKeys.PROJECT);
        CommitMessageI cm = event.getData(VcsDataKeys.COMMIT_MESSAGE_CONTROL);
        event.getPresentation().setEnabledAndVisible(project != null && cm != null);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        Project project = event.getProject();
        if (project == null) return;

        List<Change> changes = collectChanges(event);
        if (changes.isEmpty()) {
            SwingUtilities.invokeLater(() ->
                Messages.showInfoMessage(project, "No changes selected for commit.", "AI Commit Message"));
            return;
        }

        CommitMessageI commitMessage = event.getData(VcsDataKeys.COMMIT_MESSAGE_CONTROL);
        if (commitMessage == null) return;

        AiCommitSettingsState settings = AiCommitSettingsState.getInstance();
        settings.normalize();
        AiCommitSettingsState.StateData state = settings.getState();

        String apiKey = ApiKeyStore.getApiKey();
        if (apiKey.isBlank()) {
            SwingUtilities.invokeLater(() -> {
                if (Messages.showYesNoDialog(project,
                        "API Key is not configured. Open settings now?",
                        "AI Commit Message", "Open Settings", "Cancel", Messages.getQuestionIcon())
                        == Messages.YES) {
                    ShowSettingsUtil.getInstance().showSettingsDialog(project, "AI Commit Message");
                }
            });
            return;
        }

        if (!state.allowSendingChanges) {
            SwingUtilities.invokeLater(() -> {
                if (Messages.showYesNoDialog(project,
                        "Before generating a commit message, please confirm in settings that selected Git changes may be sent to your configured AI service.",
                        "AI Commit Message", "Open Settings", "Cancel", Messages.getWarningIcon())
                        == Messages.YES) {
                    ShowSettingsUtil.getInstance().showSettingsDialog(project, "AI Commit Message");
                }
            });
            return;
        }

        final String finalApiKey = apiKey;
        final List<Change> capturedChanges = changes;

        new Task.Backgroundable(project, "Generating AI commit message", true) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                try {
                    indicator.setText("Collecting Git changes...");
                    List<String> diffs = DiffCollector.collectDiffs(project, capturedChanges);
                    if (diffs.isEmpty()) {
                        SwingUtilities.invokeLater(() ->
                            Messages.showInfoMessage(project, "No parsable Git changes were found.", "AI Commit Message"));
                        return;
                    }

                    indicator.setText("Reading recent commits...");
                    List<String> recentMessages;
                    try {
                        recentMessages = GitHistoryCollector.collectRecentCommitMessages(project);
                    } catch (Throwable e) {
                        recentMessages = List.of();
                    }

                    indicator.setText("Requesting AI commit message...");
                    OpenAiCompatibleClient client = new OpenAiCompatibleClient();
                    String generated = client.generate(
                        state, finalApiKey,
                        PromptBuilder.systemPrompt(),
                        PromptBuilder.userPrompt(project, capturedChanges.size(), diffs, recentMessages));

                    SwingUtilities.invokeLater(() -> {
                        if (generated != null && !generated.isBlank())
                            commitMessage.setCommitMessage(generated.trim());
                    });
                } catch (Throwable t) {
                    String msg = t.getMessage();
                    final String display = msg != null ? msg : t.getClass().getSimpleName();
                    SwingUtilities.invokeLater(() ->
                        Messages.showErrorDialog(project, display, "AI Commit Message"));
                }
            }
        }.queue();
    }

    private static List<Change> collectChanges(AnActionEvent event) {
        List<Change> changes = new ArrayList<>();

        Object handlerRaw = event.getDataContext().getData(VcsDataKeys.COMMIT_WORKFLOW_HANDLER);
        if (handlerRaw instanceof AbstractCommitWorkflowHandler) {
            try {
                AbstractCommitWorkflowHandler<?, ?> h = (AbstractCommitWorkflowHandler<?, ?>) handlerRaw;
                List<Change> inc = h.getUi().getIncludedChanges();
                if (inc != null && !inc.isEmpty()) changes.addAll(inc);
                List<FilePath> uv = h.getUi().getIncludedUnversionedFiles();
                if (uv != null) for (FilePath fp : uv) changes.add(new Change(null, new CurrentContentRevision(fp)));
            } catch (Exception ignored) {
            }
        }

        if (changes.isEmpty()) {
            Change[] sel = event.getData(VcsDataKeys.SELECTED_CHANGES);
            if (sel != null) for (Change c : sel) if (c != null) changes.add(c);
        }

        if (changes.isEmpty()) {
            Change[] all = event.getData(VcsDataKeys.CHANGES);
            if (all != null) for (Change c : all) if (c != null) changes.add(c);
        }

        return changes;
    }
}