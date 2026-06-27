package com.local.aicommit;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vcs.CommitMessageI;
import com.intellij.openapi.vcs.VcsDataKeys;
import com.intellij.openapi.vcs.changes.Change;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class GenerateCommitMessageAction extends AnAction {
    @Override
    public void update(@NotNull AnActionEvent event) {
        Project project = event.getData(CommonDataKeys.PROJECT);
        CommitMessageI commitMessage = event.getData(VcsDataKeys.COMMIT_MESSAGE_CONTROL);
        event.getPresentation().setEnabledAndVisible(project != null && commitMessage != null);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        Project project = event.getData(CommonDataKeys.PROJECT);
        CommitMessageI commitMessage = event.getData(VcsDataKeys.COMMIT_MESSAGE_CONTROL);
        Change[] changes = event.getData(VcsDataKeys.SELECTED_CHANGES);
        if (changes == null || changes.length == 0) {
            changes = event.getData(VcsDataKeys.CHANGES);
        }

        if (project == null || commitMessage == null) {
            return;
        }

        AiCommitSettingsState settings = AiCommitSettingsState.getInstance();
        settings.normalize();
        AiCommitSettingsState.StateData state = settings.getState();

        String apiKey = ApiKeyStore.getApiKey();
        if (apiKey.isBlank()) {
            int answer = Messages.showYesNoDialog(
                project,
                "API Key is not configured. Open settings now?",
                "AI Commit Message",
                "Open Settings",
                "Cancel",
                Messages.getQuestionIcon()
            );
            if (answer == Messages.YES) {
                ShowSettingsUtil.getInstance().showSettingsDialog(project, "AI Commit Message");
            }
            return;
        }

        if (!state.allowSendingChanges) {
            int answer = Messages.showYesNoDialog(
                project,
                "Before generating a commit message, please confirm in settings that selected Git changes may be sent to your configured AI service.",
                "AI Commit Message",
                "Open Settings",
                "Cancel",
                Messages.getWarningIcon()
            );
            if (answer == Messages.YES) {
                ShowSettingsUtil.getInstance().showSettingsDialog(project, "AI Commit Message");
            }
            return;
        }

        final String finalApiKey = apiKey;
        final Change[] capturedChanges = changes;

        new Task.Backgroundable(project, "Generating AI commit message", true) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                try {
                    indicator.setText("Collecting Git changes...");
                    List<String> diffs = DiffCollector.collectDiffs(project, capturedChanges);
                    if (diffs.isEmpty()) {
                        showInfo(project, "No Git changes were found.");
                        return;
                    }

                    indicator.setText("Reading recent commits...");
                    List<String> recentMessages = GitHistoryCollector.collectRecentCommitMessages(project);

                    indicator.setText("Requesting AI commit message...");
                    OpenAiCompatibleClient client = new OpenAiCompatibleClient();
                    String generated = client.generate(
                        state,
                        finalApiKey,
                        PromptBuilder.systemPrompt(),
                        PromptBuilder.userPrompt(project, state, diffs, recentMessages)
                    );
                    applyGeneratedMessage(project, commitMessage, generated);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    showError(project, "Generation was interrupted.");
                } catch (Exception ex) {
                    showError(project, ex.getMessage() == null ? ex.toString() : ex.getMessage());
                }
            }
        }.queue();
    }

    private static void applyGeneratedMessage(Project project, CommitMessageI commitMessage, String generated) {
        ApplicationManager.getApplication().invokeLater(() -> {
            if (generated == null || generated.isBlank()) {
                Messages.showWarningDialog(project, "AI returned an empty commit message.", "AI Commit Message");
                return;
            }
            commitMessage.setCommitMessage(generated.trim());
        });
    }

    private static void showInfo(Project project, String message) {
        ApplicationManager.getApplication().invokeLater(
            () -> Messages.showInfoMessage(project, message, "AI Commit Message")
        );
    }

    private static void showError(Project project, String message) {
        ApplicationManager.getApplication().invokeLater(
            () -> Messages.showErrorDialog(project, message, "AI Commit Message")
        );
    }
}