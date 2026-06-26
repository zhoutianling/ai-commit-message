package com.local.aicommit;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.VcsDataKeys;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vcs.changes.ContentRevision;
import com.intellij.openapi.vcs.changes.ChangeListManager;
import com.intellij.openapi.vfs.VirtualFile;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

final class DiffCollector {
    private static final String[] SENSITIVE_NAMES = {
        ".env", ".env.local", ".env.production", "id_rsa", "id_dsa", "known_hosts"
    };

    private DiffCollector() {
    }

    static String collect(Project project, Change[] actionChanges, int maxCharacters) {
        List<Change> changes = new ArrayList<>();
        if (actionChanges != null && actionChanges.length > 0) {
            for (Change change : actionChanges) {
                if (change != null) {
                    changes.add(change);
                }
            }
        }
        if (changes.isEmpty()) {
            Collection<Change> allChanges = ChangeListManager.getInstance(project).getAllChanges();
            changes.addAll(allChanges);
        }
        if (changes.isEmpty()) {
            return "";
        }

        Map<String, ChangeSnapshot> snapshots = new LinkedHashMap<>();
        for (Change change : changes) {
            ChangeSnapshot snapshot = snapshot(project, change);
            if (snapshot != null) {
                snapshots.put(snapshot.path + ":" + snapshot.status, snapshot);
            }
        }

        StringBuilder builder = new StringBuilder();
        builder.append("Changed files:\n");
        for (ChangeSnapshot snapshot : snapshots.values()) {
            builder.append("- ")
                .append(snapshot.status)
                .append(" ")
                .append(snapshot.path);
            if (snapshot.sensitive) {
                builder.append(" [content skipped: sensitive]");
            } else if (snapshot.binary) {
                builder.append(" [content skipped: binary]");
            }
            builder.append('\n');
        }
        builder.append("\nDiff details:\n");
        for (ChangeSnapshot snapshot : snapshots.values()) {
            builder.append("\n--- ").append(snapshot.status).append(" ").append(snapshot.path).append('\n');
            if (snapshot.sensitive) {
                builder.append("[Sensitive file content skipped]\n");
            } else if (snapshot.binary) {
                builder.append("[Binary file content skipped]\n");
            } else if (snapshot.diff == null || snapshot.diff.isBlank()) {
                builder.append("[No textual diff available]\n");
            } else {
                builder.append(snapshot.diff).append('\n');
            }
            if (builder.length() > maxCharacters) {
                builder.setLength(maxCharacters);
                builder.append("\n[Diff truncated at ").append(maxCharacters).append(" characters]\n");
                break;
            }
        }
        return builder.toString();
    }

    private static ChangeSnapshot snapshot(Project project, Change change) {
        ContentRevision before = change.getBeforeRevision();
        ContentRevision after = change.getAfterRevision();
        String path = pathOf(after != null ? after : before);
        if (path == null || path.isBlank()) {
            VirtualFile vf = change.getVirtualFile();
            path = vf == null ? "<unknown>" : vf.getPath();
        }
        String relativePath = relativize(project, path);
        boolean sensitive = isSensitive(relativePath);
        boolean binary = isBinary(relativePath);
        String diff = "";
        if (!sensitive && !binary) {
            diff = buildTextDiff(before, after);
        }
        return new ChangeSnapshot(relativePath, change.getType().name(), sensitive, binary, diff);
    }

    private static String buildTextDiff(ContentRevision before, ContentRevision after) {
        String oldContent = safeContent(before);
        String newContent = safeContent(after);
        if (oldContent == null && newContent == null) {
            return "";
        }
        if (oldContent == null) {
            return limitedContent("New file content", newContent);
        }
        if (newContent == null) {
            return limitedContent("Deleted file content", oldContent);
        }
        if (oldContent.equals(newContent)) {
            return "";
        }
        return simpleLineDiff(oldContent, newContent, 220);
    }

    private static String simpleLineDiff(String oldContent, String newContent, int maxChangedLines) {
        String[] oldLines = oldContent.split("\\R", -1);
        String[] newLines = newContent.split("\\R", -1);
        int commonPrefix = 0;
        int maxPrefix = Math.min(oldLines.length, newLines.length);
        while (commonPrefix < maxPrefix && oldLines[commonPrefix].equals(newLines[commonPrefix])) {
            commonPrefix++;
        }
        int oldSuffix = oldLines.length - 1;
        int newSuffix = newLines.length - 1;
        while (oldSuffix >= commonPrefix && newSuffix >= commonPrefix && oldLines[oldSuffix].equals(newLines[newSuffix])) {
            oldSuffix--;
            newSuffix--;
        }

        StringBuilder builder = new StringBuilder();
        int contextStart = Math.max(0, commonPrefix - 3);
        int oldContextEnd = Math.min(oldLines.length - 1, oldSuffix + 3);
        int newContextEnd = Math.min(newLines.length - 1, newSuffix + 3);
        builder.append("@@ simplified textual diff @@\n");
        int emitted = 0;
        for (int i = contextStart; i < commonPrefix && i < oldLines.length; i++) {
            builder.append(' ').append(oldLines[i]).append('\n');
        }
        for (int i = commonPrefix; i <= oldSuffix && i < oldLines.length; i++) {
            builder.append('-').append(oldLines[i]).append('\n');
            emitted++;
            if (emitted >= maxChangedLines) {
                builder.append("[removed lines truncated]\n");
                break;
            }
        }
        emitted = 0;
        for (int i = commonPrefix; i <= newSuffix && i < newLines.length; i++) {
            builder.append('+').append(newLines[i]).append('\n');
            emitted++;
            if (emitted >= maxChangedLines) {
                builder.append("[added lines truncated]\n");
                break;
            }
        }
        int contextEnd = Math.max(oldContextEnd, newContextEnd);
        for (int i = Math.max(oldSuffix + 1, commonPrefix); i <= contextEnd && i < oldLines.length; i++) {
            builder.append(' ').append(oldLines[i]).append('\n');
        }
        return builder.toString();
    }

    private static String limitedContent(String label, String content) {
        if (content == null) {
            return "";
        }
        int limit = Math.min(content.length(), 12000);
        String suffix = content.length() > limit ? "\n[content truncated]" : "";
        return label + ":\n" + content.substring(0, limit) + suffix;
    }

    private static String safeContent(ContentRevision revision) {
        if (revision == null) {
            return null;
        }
        try {
            return revision.getContent();
        } catch (Exception ignored) {
            return null;
        }
    }

    private static String pathOf(ContentRevision revision) {
        return revision == null || revision.getFile() == null ? null : revision.getFile().getPath();
    }

    private static String relativize(Project project, String path) {
        String base = project.getBasePath();
        if (base == null) {
            return path;
        }
        try {
            String normalizedBase = new File(base).getCanonicalPath();
            String normalizedPath = new File(path).getCanonicalPath();
            if (normalizedPath.startsWith(normalizedBase + File.separator)) {
                return normalizedPath.substring(normalizedBase.length() + 1).replace('\\', '/');
            }
        } catch (Exception ignored) {
        }
        return path.replace('\\', '/');
    }

    private static boolean isSensitive(String path) {
        String lower = path.toLowerCase(Locale.ROOT);
        for (String name : SENSITIVE_NAMES) {
            if (lower.endsWith("/" + name) || lower.equals(name)) {
                return true;
            }
        }
        return lower.endsWith(".pem")
            || lower.endsWith(".key")
            || lower.endsWith(".p12")
            || lower.endsWith(".jks")
            || lower.endsWith(".keystore")
            || lower.contains("/secrets/")
            || lower.contains("/.ssh/");
    }

    private static boolean isBinary(String path) {
        String lower = path.toLowerCase(Locale.ROOT);
        return lower.endsWith(".png")
            || lower.endsWith(".jpg")
            || lower.endsWith(".jpeg")
            || lower.endsWith(".gif")
            || lower.endsWith(".webp")
            || lower.endsWith(".ico")
            || lower.endsWith(".pdf")
            || lower.endsWith(".zip")
            || lower.endsWith(".jar")
            || lower.endsWith(".apk")
            || lower.endsWith(".aab")
            || lower.endsWith(".so")
            || lower.endsWith(".dll")
            || lower.endsWith(".exe");
    }
}
