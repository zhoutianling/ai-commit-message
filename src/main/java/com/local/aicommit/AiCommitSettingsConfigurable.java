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

import javax.swing.*;
import java.awt.FlowLayout;
import java.util.List;

public final class AiCommitSettingsConfigurable implements Configurable {
    private JBTextField baseUrlField;
    private JBPasswordField apiKeyField;
    private JComboBox<String> modelCombo;
    private JButton fetchButton;
    private JButton testButton;
    private JCheckBox consentCheckBox;
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

        modelCombo = new JComboBox<>();
        modelCombo.setEditable(false);
        if (!state.model.isEmpty()) {
            modelCombo.addItem(state.model);
            modelCombo.setSelectedItem(state.model);
        }

        fetchButton = new JButton("Fetch Models");

        JPanel modelRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        modelRow.add(modelCombo);
        modelRow.add(fetchButton);

        consentCheckBox = new JCheckBox("I understand that prompts and selected Git changes may be sent to the configured AI service.");
        consentCheckBox.setSelected(state.allowSendingChanges);

        testButton = new JButton("Test Configuration");
        testButton.addActionListener(e -> testConfiguration());
        fetchButton.addActionListener(e -> fetchModels());

        panel = FormBuilder.createFormBuilder()
            .addLabeledComponent(new JBLabel("Base URL"), baseUrlField)
            .addLabeledComponent(new JBLabel("API Key"), apiKeyField)
            .addLabeledComponent(new JBLabel("Model"), modelRow)
            .addComponent(consentCheckBox)
            .addComponent(testButton)
            .addComponentFillVertically(new JPanel(), 0)
            .getPanel();
        return panel;
    }

    private void fetchModels() {
        String apiKey = new String(apiKeyField.getPassword()).trim();
        if (apiKey.isEmpty()) {
            Messages.showWarningDialog("API Key is required to fetch models.", "AI Commit Message");
            return;
        }
        if (!consentCheckBox.isSelected()) {
            Messages.showWarningDialog(panel, "Please confirm the data-transfer consent first.", "AI Commit Message");
            return;
        }
        String baseUrl = text(baseUrlField).trim();
        if (baseUrl.isEmpty()) {
            Messages.showWarningDialog("Base URL is required.", "AI Commit Message");
            return;
        }

        fetchButton.setEnabled(false);
        fetchButton.setText("Fetching...");
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                List<String> models = new OpenAiCompatibleClient().fetchModels(baseUrl, apiKey);
                invokeInSettingsDialog(() -> {
                    modelCombo.removeAllItems();
                    for (String m : models) modelCombo.addItem(m);
                    if (!models.isEmpty()) modelCombo.setSelectedIndex(0);
                    fetchButton.setEnabled(true);
                    fetchButton.setText("Fetch Models");
                    Messages.showInfoMessage(panel, "Found " + models.size() + " model(s).", "AI Commit Message");
                });
            } catch (Exception ex) {
                String m = ex.getMessage() == null ? ex.toString() : ex.getMessage();
                invokeInSettingsDialog(() -> {
                    fetchButton.setEnabled(true);
                    fetchButton.setText("Fetch Models");
                    Messages.showErrorDialog(panel, m, "AI Commit Message");
                });
            }
        });
    }

    @Override
    public boolean isModified() {
        AiCommitSettingsState.StateData state = AiCommitSettingsState.getInstance().getState();
        return !text(baseUrlField).equals(nv(state.baseUrl))
            || !new String(apiKeyField.getPassword()).equals(nv(state.apiKey))
            || !nvCombo(modelCombo).equals(nv(state.model))
            || consentCheckBox.isSelected() != state.allowSendingChanges;
    }

    @Override
    public void apply() {
        AiCommitSettingsState.StateData state = AiCommitSettingsState.getInstance().getState();
        state.baseUrl = text(baseUrlField).trim();
        state.apiKey = new String(apiKeyField.getPassword());
        state.model = nvCombo(modelCombo);
        AiCommitSettingsState.getInstance().normalize();
        state.allowSendingChanges = consentCheckBox.isSelected();
    }

    @Override
    public void reset() {
        AiCommitSettingsState.StateData state = AiCommitSettingsState.getInstance().getState();
        baseUrlField.setText(state.baseUrl);
        apiKeyField.setText(state.apiKey);
        modelCombo.removeAllItems();
        if (!state.model.isEmpty()) {
            modelCombo.addItem(state.model);
            modelCombo.setSelectedItem(state.model);
        }
        consentCheckBox.setSelected(state.allowSendingChanges);
    }

    @Override
    public void disposeUIResources() {
        baseUrlField = null;
        apiKeyField = null;
        modelCombo = null;
        fetchButton = null;
        testButton = null;
        consentCheckBox = null;
        panel = null;
    }

    private void testConfiguration() {
        String apiKey = new String(apiKeyField.getPassword()).trim();
        if (apiKey.isEmpty()) {
            Messages.showWarningDialog("API Key is required for the test.", "AI Commit Message");
            return;
        }
        if (!consentCheckBox.isSelected()) {
            Messages.showWarningDialog(panel, "Please confirm the data-transfer consent first.", "AI Commit Message");
            return;
        }
        String baseUrl = text(baseUrlField).trim();
        if (baseUrl.isEmpty()) {
            Messages.showWarningDialog("Base URL is required.", "AI Commit Message");
            return;
        }
        String model = nvCombo(modelCombo).trim();
        if (model.isEmpty()) {
            Messages.showWarningDialog("Model is required. Use Fetch Models or type a model name.", "AI Commit Message");
            return;
        }

        testButton.setEnabled(false);
        testButton.setText("Testing...");
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                String result = new OpenAiCompatibleClient().generate(
                    baseUrl, apiKey, model,
                    "You are a configuration health check. Reply with exactly OK.",
                    "Reply with exactly OK.");
                invokeInSettingsDialog(() ->
                    Messages.showInfoMessage(panel, "Test OK: " + result, "AI Commit Message"));
            } catch (Exception ex) {
                String m = ex.getMessage() == null ? ex.toString() : ex.getMessage();
                invokeInSettingsDialog(() ->
                    Messages.showErrorDialog(panel, m, "AI Commit Message"));
            } finally {
                invokeInSettingsDialog(() -> {
                    if (testButton != null) { testButton.setEnabled(true); testButton.setText("Test Configuration"); }
                });
            }
        });
    }

    private void invokeInSettingsDialog(Runnable r) {
        ModalityState ms = panel == null ? ModalityState.any() : ModalityState.stateForComponent(panel);
        ApplicationManager.getApplication().invokeLater(r, ms);
    }

    private static String text(JBTextField f) { return f == null ? "" : f.getText(); }
    private static String nvCombo(JComboBox<String> c) { return c == null || c.getSelectedItem() == null ? "" : c.getSelectedItem().toString(); }
    private static String nv(String s) { return s == null ? "" : s; }
}