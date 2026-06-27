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
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.VcsDataKeys;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vcs.changes.CurrentContentRevision;
import com.intellij.vcs.commit.AbstractCommitWorkflowHandler;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public final class GenerateCommitMessageAction extends AnAction {
    @Override
    public void update(@NotNull AnActionEvent event) {
        Project project = event.getData(CommonDataKeys.PROJECT);
        Object handler = event.getDataContext().getData(VcsDataKeys.COMMIT_WORKFLOW_HANDLER);
        event.getPresentation().setEnabledAndVisible(project != null && handler instanceof AbstractCommitWorkflowHandler);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        try {
            Project project = event.getProject();
            Object handlerObj = event.getDataContext().getData(VcsDataKeys.COMMIT_WORKFLOW_HANDLER);
            CommitMessageI commitMessage = event.getData(VcsDataKeys.COMMIT_MESSAGE_CONTROL);

            if (project == null || commitMessage == null || !(handlerObj instanceof AbstractCommitWorkflowHandler)) {
                return;
            }

            @SuppressWarnings("unchecked")
            AbstractCommitWorkflowHandler<?, ?> handler = (AbstractCommitWorkflowHandler<?, ?>) handlerObj;

            List<Change> changes = collectChanges(handler);
            if (changes.isEmpty()) {
                Messages.showInfoMessage(project, "No changes selected for commit.", "AI Commit Message");
                return;
            }

            AiCommitSettingsState settings = AiCommitSettingsState.getInstance();
            settings.normalize();
            AiCommitSettingsState.StateData state = settings.getState();

            String apiKey = ApiKeyStore.getApiKey();
            if (apiKey.isBlank()) {
                int answer = Messages.showYesNoDialog(
                    project, "API Key is not configured. Open settings now?",
                    "AI Commit Message", "Open Settings", "Cancel", Messages.getQuestionIcon());
                if (answer == Messages.YES) {
                    ShowSettingsUtil.getInstance().showSettingsDialog(project, "AI Commit Message");
                }
                return;
            }

            if (!state.allowSendingChanges) {
                int answer = Messages.showYesNoDialog(
                    project, "Before generating a commit message, please confirm in settings that selected Git changes may be sent to your configured AI service.",
                    "AI Commit Message", "Open Settings", "Cancel", Messages.getWarningIcon());
                if (answer == Messages.YES) {
                    ShowSettingsUtil.getInstance().showSettingsDialog(project, "AI Commit Message");
                }
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
                            showInfo(project, "No parsable Git changes were found.");
                            return;
                        }

                        indicator.setText("Reading recent commits...");
                        List<String> recentMessages;
                        try {
                            recentMessages = GitHistoryCollector.collectRecentCommitMessages(project);
                        } catch (Exception e) {
                            recentMessages = List.of();
                        }

                        indicator.setText("Requesting AI commit message...");
                        OpenAiCompatibleClient client = new OpenAiCompatibleClient();
                        String generated = client.generate(
                            state, finalApiKey,
                            PromptBuilder.systemPrompt(),
                            PromptBuilder.userPrompt(project, state, diffs, recentMessages));
                        applyGeneratedMessage(project, commitMessage, generated);
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                        showError(project, "Generation was interrupted.");
                    } catch (Exception ex) {
                        String msg = ex.getMessage();
                        showError(project, msg != null ? msg : ex.getClass().getSimpleName());
                    }
                }
            }.queue();
        } catch (Exception e) {
            Project project = event.getProject();
            if (project != null) {
                String msg = e.getMessage();
                ApplicationManager.getApplication().invokeLater(() ->
                    Messages.showErrorDialog(project, msg != null ? msg : e.getClass().getSimpleName(), "AI Commit Message"));
            }
        }
    }

    private static List<Change> collectChanges(AbstractCommitWorkflowHandler<?, ?> handler) {
        List<Change> changeList = new ArrayList<>();
        try {
            List<Change> included = handler.getUi().getIncludedChanges();
            if (included != null && !included.isEmpty()) {
                changeList.addAll(included);
            }
            List<FilePath> unversioned = handler.getUi().getIncludedUnversionedFiles();
            if (unversioned != null && !unversioned.isEmpty()) {
                for (FilePath fp : unversioned) {
                    changeList.add(new Change(null, new CurrentContentRevision(fp)));
                }
            }
        } catch (Exception ignored) {
            // handler may be in an invalid state; return whatever we collected
        }
        return changeList;
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
            () -> Messages.showInfoMessage(project, message, "AI Commit Message"));
    }

    private static void showError(Project project, String message) {
        ApplicationManager.getApplication().invokeLater(
            () -> Messages.showErrorDialog(project, message, "AI Commit Message"));
    }
}