package com.local.aicommit;

import com.intellij.credentialStore.CredentialAttributes;
import com.intellij.credentialStore.Credentials;
import com.intellij.ide.passwordSafe.PasswordSafe;

final class ApiKeyStore {
    private static final CredentialAttributes ATTRIBUTES =
        new CredentialAttributes("AiCommitMessage");

    private ApiKeyStore() {
    }

    static String getApiKey() {
        Credentials credentials = PasswordSafe.getInstance().get(ATTRIBUTES);
        if (credentials == null) {
            return "";
        }
        String password = credentials.getPasswordAsString();
        return password == null ? "" : password;
    }

    static void setApiKey(String apiKey) {
        String value = apiKey == null ? "" : apiKey.trim();
        if (value.isEmpty()) {
            PasswordSafe.getInstance().set(ATTRIBUTES, null);
        } else {
            PasswordSafe.getInstance().setPassword(ATTRIBUTES, value);
        }
    }
}