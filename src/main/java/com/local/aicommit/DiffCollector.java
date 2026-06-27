package com.local.aicommit;

import com.intellij.openapi.diff.impl.patch.*;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vcs.changes.ContentRevision;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

import com.intellij.project.ProjectKt;

final class DiffCollector {
    private static final int MAX_FILE = 50;
    private static final long MAX_PATCH_LEN = 70000L;
    private static final int MAX_SINGLE_LINE_LEN = 300;

    private DiffCollector() {}

    static List<String> collectDiffs(Project project, Change[] changes) {
        if (project == null || project.getBasePath() == null || changes == null || changes.length == 0) {
            return List.of();
        }

        List<Change> changeList = new ArrayList<>();
        for (Change c : changes) {
            if (c != null) changeList.add(c);
        }
        if (changeList.isEmpty()) return List.of();

        List<String> result = new ArrayList<>();
        AtomicLong totalLength = new AtomicLong(0L);

        for (Change change : changeList) {
            if (result.size() >= MAX_FILE || totalLength.get() >= MAX_PATCH_LEN) break;

            try {
                if (!isValidChange(change)) continue;

                List<FilePatch> patches = IdeaTextPatchBuilder.buildPatch(
                    project,
                    Collections.singletonList(change),
                    Path.of(project.getBasePath()),
                    false
                );

                if (patches == null || patches.isEmpty()) {
                    String label = emptyPatchLabel(change);
                    if (label != null) result.add(label);
                    continue;
                }

                if (isPatchTooLarge(patches, totalLength)) continue;

                StringWriter writer = new StringWriter();
                try {
                    UnifiedDiffWriter.write(
                        project,
                        ProjectKt.getStateStore(project).getProjectBasePath(),
                        patches,
                        writer,
                        "\n",
                        null,
                        List.of()
                    );
                } finally {
                    writer.close();
                }
                String diffContent = writer.toString();
                if (diffContent != null && !diffContent.isBlank()) {
                    result.add(diffContent);
                }
            } catch (IOException | VcsException e) {
                tryNewFileFallback(change, result, totalLength);
            }
        }
        return result;
    }

    private static boolean isValidChange(Change change) {
        ContentRevision rev = change.getAfterRevision() != null ? change.getAfterRevision() : change.getBeforeRevision();
        if (rev == null || rev.getFile().getFileType().isBinary()) return false;
        try {
            String content = rev.getContent();
            if (content == null || content.isBlank()) return true;
            return !isSingleLineLargeFile(content);
        } catch (VcsException e) {
            return true;
        }
    }

    private static boolean isSingleLineLargeFile(String content) {
        return !content.contains("\n") && !content.contains("\r") && content.length() > MAX_SINGLE_LINE_LEN;
    }

    private static boolean isPatchTooLarge(List<FilePatch> patches, AtomicLong totalLength) {
        long len = 0;
        for (FilePatch patch : patches) {
            if (!(patch instanceof TextFilePatch tp)) continue;
            for (PatchHunk hunk : tp.getHunks()) {
                len += hunk.getText() != null ? hunk.getText().length() : 0;
            }
        }
        if (totalLength.get() + len > MAX_PATCH_LEN) return true;
        totalLength.addAndGet(len);
        return false;
    }

    private static String emptyPatchLabel(Change change) {
        String name = null;
        if (change.getAfterRevision() != null) {
            name = change.getAfterRevision().getFile().getName();
        } else if (change.getBeforeRevision() != null) {
            name = change.getBeforeRevision().getFile().getName();
        }
        return (name != null && !name.isBlank()) ? name + " change mod" : null;
    }

    private static void tryNewFileFallback(Change change, List<String> result, AtomicLong totalLength) {
        ContentRevision after = change.getAfterRevision();
        if (after == null) return;
        try {
            String content = after.getContent();
            String path = after.getFile().getPath();
            if (content == null || content.isBlank()) {
                result.add("new file: " + after.getFile().getName());
                return;
            }
            if (content.length() > MAX_PATCH_LEN) {
                content = content.substring(0, (int) MAX_PATCH_LEN);
            }
            String[] lines = content.split("\\R", -1);
            StringBuilder sb = new StringBuilder();
            sb.append("--- /dev/null\n");
            sb.append("+++ b/").append(path).append("\n");
            sb.append("@@ -0,0 +1,").append(lines.length).append(" @@\n");
            for (String line : lines) {
                sb.append("+").append(line).append("\n");
            }
            result.add(sb.toString());
            totalLength.addAndGet(sb.length());
        } catch (VcsException e) {
            result.add(after.getFile().getName() + " change mod");
        }
    }
}