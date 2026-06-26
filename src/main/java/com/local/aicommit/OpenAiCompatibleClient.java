package com.local.aicommit;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

final class OpenAiCompatibleClient {
    private final HttpClient httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(20))
        .build();

    String generate(AiCommitSettingsState.StateData settings, String apiKey, String systemPrompt, String userPrompt)
        throws IOException, InterruptedException {
        String endpoint = normalizeBaseUrl(settings.baseUrl) + "/chat/completions";
        String body = "{"
            + "\"model\":\"" + json(settings.model) + "\","
            + "\"temperature\":0.2,"
            + "\"messages\":["
            + "{\"role\":\"system\",\"content\":\"" + json(systemPrompt) + "\"},"
            + "{\"role\":\"user\",\"content\":\"" + json(userPrompt) + "\"}"
            + "]"
            + "}";

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(endpoint))
            .timeout(Duration.ofSeconds(90))
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer " + apiKey)
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        int status = response.statusCode();
        if (status < 200 || status >= 300) {
            throw new IOException("AI request failed: HTTP " + status + " " + brief(response.body()));
        }
        String content = extractFirstMessageContent(response.body());
        if (content == null || content.isBlank()) {
            throw new IOException("AI response did not contain a message.");
        }
        return cleanup(content);
    }

    private static String normalizeBaseUrl(String baseUrl) {
        String value = baseUrl == null ? "" : baseUrl.trim();
        if (value.endsWith("/")) {
            value = value.substring(0, value.length() - 1);
        }
        if (value.endsWith("/chat/completions")) {
            value = value.substring(0, value.length() - "/chat/completions".length());
        }
        return value;
    }

    private static String cleanup(String value) {
        String trimmed = value.trim();
        if (trimmed.startsWith("```")) {
            trimmed = trimmed.replaceFirst("^```[a-zA-Z]*\\s*", "");
            trimmed = trimmed.replaceFirst("\\s*```$", "");
        }
        return trimmed.trim();
    }

    private static String extractFirstMessageContent(String json) throws IOException {
        String key = "\"content\"";
        int keyIndex = json.indexOf(key);
        while (keyIndex >= 0) {
            int colon = json.indexOf(':', keyIndex + key.length());
            if (colon < 0) {
                return null;
            }
            int quote = nextNonWhitespace(json, colon + 1);
            if (quote >= 0 && json.charAt(quote) == '"') {
                return readJsonString(json, quote);
            }
            keyIndex = json.indexOf(key, keyIndex + key.length());
        }
        return null;
    }

    private static int nextNonWhitespace(String json, int start) {
        for (int i = start; i < json.length(); i++) {
            if (!Character.isWhitespace(json.charAt(i))) {
                return i;
            }
        }
        return -1;
    }

    private static String readJsonString(String json, int quoteIndex) throws IOException {
        StringBuilder builder = new StringBuilder();
        boolean escaping = false;
        for (int i = quoteIndex + 1; i < json.length(); i++) {
            char ch = json.charAt(i);
            if (escaping) {
                switch (ch) {
                    case '"':
                    case '\\':
                    case '/':
                        builder.append(ch);
                        break;
                    case 'b':
                        builder.append('\b');
                        break;
                    case 'f':
                        builder.append('\f');
                        break;
                    case 'n':
                        builder.append('\n');
                        break;
                    case 'r':
                        builder.append('\r');
                        break;
                    case 't':
                        builder.append('\t');
                        break;
                    case 'u':
                        if (i + 4 >= json.length()) {
                            throw new IOException("Invalid unicode escape in AI response.");
                        }
                        builder.append((char) Integer.parseInt(json.substring(i + 1, i + 5), 16));
                        i += 4;
                        break;
                    default:
                        builder.append(ch);
                }
                escaping = false;
            } else if (ch == '\\') {
                escaping = true;
            } else if (ch == '"') {
                return builder.toString();
            } else {
                builder.append(ch);
            }
        }
        throw new IOException("Unterminated string in AI response.");
    }

    private static String json(String value) {
        if (value == null) {
            return "";
        }
        StringBuilder builder = new StringBuilder(value.length() + 16);
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            switch (ch) {
                case '"':
                    builder.append("\\\"");
                    break;
                case '\\':
                    builder.append("\\\\");
                    break;
                case '\b':
                    builder.append("\\b");
                    break;
                case '\f':
                    builder.append("\\f");
                    break;
                case '\n':
                    builder.append("\\n");
                    break;
                case '\r':
                    builder.append("\\r");
                    break;
                case '\t':
                    builder.append("\\t");
                    break;
                default:
                    if (ch < 0x20) {
                        builder.append(String.format("\\u%04x", (int) ch));
                    } else {
                        builder.append(ch);
                    }
            }
        }
        return builder.toString();
    }

    private static String brief(String body) {
        if (body == null) {
            return "";
        }
        String normalized = body.replace('\n', ' ').replace('\r', ' ').trim();
        return normalized.length() > 300 ? normalized.substring(0, 300) + "..." : normalized;
    }
}
