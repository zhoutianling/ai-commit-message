package com.local.aicommit;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.components.StoragePathMacros;
import com.intellij.openapi.application.ApplicationManager;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
import java.util.Map;

@Service(Service.Level.APP)
@State(name = "AiCommitSettings", storages = @Storage(StoragePathMacros.NON_ROAMABLE_FILE))
public final class AiCommitSettingsState implements PersistentStateComponent<AiCommitSettingsState.StateData> {
    public static final class StateData {
        public String provider = "DeepSeek";
        public String baseUrl = "https://api.deepseek.com";
        public String apiKey = "";
        public String model = "deepseek-v4-flash";
        public boolean allowSendingChanges = false;
    }

    public static final Map<String, Preset> PRESETS = new LinkedHashMap<>();
    static {
        PRESETS.put("DeepSeek", new Preset("https://api.deepseek.com",
            new String[]{"deepseek-v4-flash"}));
        PRESETS.put("OpenAI", new Preset("https://ai.yun.dev/v1",
            new String[]{"gpt-5.5", "gpt-5.4"}));
        PRESETS.put("Custom", new Preset("", new String[0]));
    }

    public record Preset(String baseUrl, String[] models) {}

    private StateData state = new StateData();

    public static AiCommitSettingsState getInstance() {
        return ApplicationManager.getApplication().getService(AiCommitSettingsState.class);
    }

    @Override
    public @NotNull StateData getState() {
        return state;
    }

    @Override
    public void loadState(@NotNull StateData state) {
        this.state = state;
        normalize();
    }

    public void applyProviderDefaults() {
        Preset preset = PRESETS.get(state.provider);
        if (preset == null) return;
        if (!preset.baseUrl().isEmpty()) {
            state.baseUrl = preset.baseUrl();
        }
        if (preset.models().length > 0) {
            state.model = preset.models()[0];
        }
    }

    public void normalize() {
        if (state.provider == null || state.provider.trim().isEmpty()) {
            state.provider = "DeepSeek";
        }
        Preset preset = PRESETS.get(state.provider);
        if (preset == null || preset.models().length == 0) {
            // Custom provider — keep whatever user typed
            if (state.baseUrl == null) state.baseUrl = "";
            if (state.model == null) state.model = "";
        } else {
            if (state.baseUrl == null || state.baseUrl.trim().isEmpty()) {
                state.baseUrl = preset.baseUrl();
            }
            if (state.model == null || state.model.trim().isEmpty()) {
                state.model = preset.models()[0];
            }
        }
        if (state.apiKey == null) {
            state.apiKey = "";
        }
    }
}