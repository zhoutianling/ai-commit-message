package com.local.aicommit;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.components.StoragePathMacros;
import com.intellij.openapi.application.ApplicationManager;
import org.jetbrains.annotations.NotNull;

@Service(Service.Level.APP)
@State(name = "AiCommitSettings", storages = @Storage(StoragePathMacros.NON_ROAMABLE_FILE))
public final class AiCommitSettingsState implements PersistentStateComponent<AiCommitSettingsState.StateData> {
    public static final class StateData {
        public String baseUrl = "https://api.deepseek.com";
        public String model = "deepseek-v4-flash";
        public boolean allowSendingChanges = false;
    }

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

    public void normalize() {
        if (state.baseUrl == null || state.baseUrl.trim().isEmpty()) {
            state.baseUrl = "https://api.deepseek.com";
        }
        if (state.model == null || state.model.trim().isEmpty()) {
            state.model = "deepseek-v4-flash";
        }
    }
}