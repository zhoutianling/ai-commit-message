package com.local.aicommit;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.ui.Messages;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPasswordField;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.FormBuilder;
import org.jetbrains.annotations.Nullable;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JPanel;

public final class AiCommitSettingsConfigurable implements Configurable {
    private JBTextField baseUrlField;
    private JBPasswordField apiKeyField;
    private JBTextField modelField;
    private JCheckBox consentCheckBox;
    private JButton testButton;
    private JPanel panel;

    @Override
    public String getDisplayName() {
        return "AI Commit Message";
    }

    @Override
    public @Nullable JComponent createComponent() {
        AiCommitSettingsState.StateData state = AiCommitSettingsState.getInstance().getState();
        baseUrlField = new JBTextField(state.baseUrl, 40);
        apiKeyField = new JBPasswordField();
        apiKeyField.setText(state.apiKey);
        modelField = new JBTextField(state.model, 30);
        consentCheckBox = new JCheckBox("I understand that prompts and selected Git changes may be sent to the configured AI service.");
        consentCheckBox.setSelected(state.allowSendingChanges);

        testButton = new JButton("Test Configuration");
        testButton.addActionListener(e -> testConfiguration());

        panel = FormBuilder.createFormBuilder()
            .addLabeledComponent(new JBLabel("Base URL"), baseUrlField)
            .addLabeledComponent(new JBLabel("API Key"), apiKeyField)
            .addLabeledComponent(new JBLabel("Model"), modelField)
            .addComponent(consentCheckBox)
            .addComponent(testButton)
            .addComponentFillVertically(new JPanel(), 0)
            .getPanel();
        return panel;
    }

    @Override
    public boolean isModified() {
        AiCommitSettingsState.StateData state = AiCommitSettingsState.getInstance().getState();
        return !text(baseUrlField).equals(state.baseUrl)
            || !text(modelField).equals(state.model)
            || !new String(apiKeyField.getPassword()).equals(state.apiKey)
            || consentCheckBox.isSelected() != state.allowSendingChanges;
    }

    @Override
    public void apply() {
        AiCommitSettingsState.StateData state = AiCommitSettingsState.getInstance().getState();
        state.baseUrl = trimOrDefault(text(baseUrlField), "https://api.deepseek.com");
        state.apiKey = new String(apiKeyField.getPassword());
        state.model = trimOrDefault(text(modelField), "deepseek-v4-flash");
        AiCommitSettingsState.getInstance().normalize();
        state.allowSendingChanges = consentCheckBox.isSelected();
    }

    @Override
    public void reset() {
        AiCommitSettingsState.StateData state = AiCommitSettingsState.getInstance().getState();
        baseUrlField.setText(state.baseUrl);
        apiKeyField.setText(state.apiKey);
        modelField.setText(state.model);
        consentCheckBox.setSelected(state.allowSendingChanges);
    }

    @Override
    public void disposeUIResources() {
        baseUrlField = null;
        apiKeyField = null;
        modelField = null;
        consentCheckBox = null;
        testButton = null;
        panel = null;
    }

    private void testConfiguration() {
        String apiKey = new String(apiKeyField.getPassword()).trim();
        if (apiKey.isEmpty()) {
            Messages.showWarningDialog("API Key is required for the test.", "AI Commit Message");
            return;
        }
        if (!consentCheckBox.isSelected()) {
            Messages.showWarningDialog(
                panel,
                "Please confirm the data-transfer consent before testing the configured AI service.",
                "AI Commit Message"
            );
            return;
        }

        AiCommitSettingsState.StateData testState = new AiCommitSettingsState.StateData();
        testState.baseUrl = trimOrDefault(text(baseUrlField), "https://api.deepseek.com");
        testState.model = trimOrDefault(text(modelField), "deepseek-v4-flash");

        testButton.setEnabled(false);
        testButton.setText("Testing...");
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                String result = new OpenAiCompatibleClient().generate(
                    testState,
                    apiKey,
                    "You are a configuration health check. Reply with exactly OK.",
                    "Reply with exactly OK."
                );
                invokeInSettingsDialog(() ->
                    Messages.showInfoMessage(panel, "Configuration test succeeded: " + result, "AI Commit Message")
                );
            } catch (Exception ex) {
                String message = ex.getMessage() == null ? ex.toString() : ex.getMessage();
                invokeInSettingsDialog(() ->
                    Messages.showErrorDialog(panel, message, "AI Commit Message")
                );
            } finally {
                invokeInSettingsDialog(() -> {
                    if (testButton != null) {
                        testButton.setEnabled(true);
                        testButton.setText("Test Configuration");
                    }
                });
            }
        });
    }

    private void invokeInSettingsDialog(Runnable runnable) {
        ModalityState modalityState = panel == null ? ModalityState.any() : ModalityState.stateForComponent(panel);
        ApplicationManager.getApplication().invokeLater(runnable, modalityState);
    }

    private static String text(JBTextField field) {
        return field == null ? "" : field.getText();
    }

    private static String trimOrDefault(String value, String defaultValue) {
        String trimmed = value == null ? "" : value.trim();
        return trimmed.isEmpty() ? defaultValue : trimmed;
    }
}