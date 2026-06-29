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
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import java.awt.event.ItemEvent;
import java.util.Arrays;

public final class AiCommitSettingsConfigurable implements Configurable {
    private JComboBox<String> providerCombo;
    private JBTextField baseUrlField;
    private JBPasswordField apiKeyField;
    private JComboBox<String> modelCombo;
    private JCheckBox consentCheckBox;
    private JButton testButton;
    private JPanel panel;
    private boolean adjusting;

    @Override
    public String getDisplayName() {
        return "AI Commit Message";
    }

    @Override
    public @Nullable JComponent createComponent() {
        AiCommitSettingsState.StateData state = AiCommitSettingsState.getInstance().getState();

        String[] providers = AiCommitSettingsState.PRESETS.keySet().toArray(new String[0]);
        providerCombo = new JComboBox<>(providers);
        providerCombo.setSelectedItem(state.provider);
        providerCombo.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED && !adjusting) {
                String sel = (String) e.getItem();
                AiCommitSettingsState inst = AiCommitSettingsState.getInstance();
                inst.getState().provider = sel;
                inst.applyProviderDefaults();
                AiCommitSettingsState.StateData updated = inst.getState();
                adjusting = true;
                baseUrlField.setText(updated.baseUrl);
                repopulateModelCombo(updated.provider, updated.model);
                adjusting = false;
            }
        });

        baseUrlField = new JBTextField(state.baseUrl, 40);

        apiKeyField = new JBPasswordField();
        apiKeyField.setText(state.apiKey);

        modelCombo = new JComboBox<>();
        repopulateModelCombo(state.provider, state.model);

        consentCheckBox = new JCheckBox("I understand that prompts and selected Git changes may be sent to the configured AI service.");
        consentCheckBox.setSelected(state.allowSendingChanges);

        testButton = new JButton("Test Configuration");
        testButton.addActionListener(e -> testConfiguration());

        panel = FormBuilder.createFormBuilder()
            .addLabeledComponent(new JBLabel("Provider"), providerCombo)
            .addLabeledComponent(new JBLabel("Base URL"), baseUrlField)
            .addLabeledComponent(new JBLabel("API Key"), apiKeyField)
            .addLabeledComponent(new JBLabel("Model"), modelCombo)
            .addComponent(consentCheckBox)
            .addComponent(testButton)
            .addComponentFillVertically(new JPanel(), 0)
            .getPanel();
        return panel;
    }

    private void repopulateModelCombo(String provider, String selectModel) {
        modelCombo.removeAllItems();
        AiCommitSettingsState.Preset preset = AiCommitSettingsState.PRESETS.get(provider);
        if (preset != null && preset.models().length > 0) {
            for (String m : preset.models()) modelCombo.addItem(m);
            modelCombo.setEditable(false);
            modelCombo.setSelectedItem(selectModel);
        } else {
            modelCombo.setEditable(true);
            modelCombo.setSelectedItem(selectModel);
        }
    }

    @Override
    public boolean isModified() {
        AiCommitSettingsState.StateData state = AiCommitSettingsState.getInstance().getState();
        return !nv(providerCombo).equals(nv(state.provider))
            || !text(baseUrlField).equals(nv(state.baseUrl))
            || !nv(modelCombo).equals(nv(state.model))
            || !new String(apiKeyField.getPassword()).equals(nv(state.apiKey))
            || consentCheckBox.isSelected() != state.allowSendingChanges;
    }

    @Override
    public void apply() {
        AiCommitSettingsState.StateData state = AiCommitSettingsState.getInstance().getState();
        state.provider = nv(providerCombo);
        state.baseUrl = text(baseUrlField).trim();
        state.apiKey = new String(apiKeyField.getPassword());
        state.model = nv(modelCombo);
        AiCommitSettingsState.getInstance().normalize();
        state.allowSendingChanges = consentCheckBox.isSelected();
    }

    @Override
    public void reset() {
        AiCommitSettingsState.StateData state = AiCommitSettingsState.getInstance().getState();
        providerCombo.setSelectedItem(state.provider);
        baseUrlField.setText(state.baseUrl);
        apiKeyField.setText(state.apiKey);
        repopulateModelCombo(state.provider, state.model);
        consentCheckBox.setSelected(state.allowSendingChanges);
    }

    @Override
    public void disposeUIResources() {
        providerCombo = null;
        baseUrlField = null;
        apiKeyField = null;
        modelCombo = null;
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
            Messages.showWarningDialog(panel,
                "Please confirm the data-transfer consent before testing the configured AI service.",
                "AI Commit Message");
            return;
        }
        String baseUrl = text(baseUrlField).trim();
        String model = nv(modelCombo).trim();

        AiCommitSettingsState.StateData testState = new AiCommitSettingsState.StateData();
        testState.baseUrl = baseUrl.isEmpty() ? "https://api.deepseek.com" : baseUrl;
        testState.model = model.isEmpty() ? "deepseek-v4-flash" : model;

        testButton.setEnabled(false);
        testButton.setText("Testing...");
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                String result = new OpenAiCompatibleClient().generate(
                    testState, apiKey,
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
    private static String nv(JComboBox<String> c) { return c == null || c.getSelectedItem() == null ? "" : c.getSelectedItem().toString(); }
    private static String nv(String s) { return s == null ? "" : s; }
}