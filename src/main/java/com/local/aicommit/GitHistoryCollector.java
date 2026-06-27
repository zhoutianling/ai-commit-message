package com.local.aicommit;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vfs.VirtualFile;
import git4idea.GitCommit;
import git4idea.history.GitHistoryUtils;
import git4idea.repo.GitRepository;
import git4idea.repo.GitRepositoryManager;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

final class GitHistoryCollector {
    private GitHistoryCollector() {}

    static List<String> collectRecentCommitMessages(Project project) {
        List<String> result = new ArrayList<>();
        GitRepository repo = findRepository(project);
        if (repo == null) return result;

        try {
            List<GitCommit> commits = GitHistoryUtils.history(project, repo.getRoot(), "--max-count=3");
            if (commits == null || commits.isEmpty()) return result;

            List<GitCommit> recent = commits.stream()
                .sorted(Comparator.comparing(GitCommit::getTimestamp).reversed())
                .limit(3)
                .collect(Collectors.toList());

            for (GitCommit commit : recent) {
                String msg = commit.getFullMessage();
                if (msg != null && !msg.isBlank()) {
                    result.add(msg);
                }
            }
        } catch (VcsException e) {
            // silently ignore, history is optional context
        }
        return result;
    }

    private static GitRepository findRepository(Project project) {
        VirtualFile projectFile = project.getProjectFile();
        if (projectFile != null) {
            GitRepository repo = GitRepositoryManager.getInstance(project).getRepositoryForFile(projectFile);
            if (repo != null) return repo;
        }
        List<GitRepository> repos = GitRepositoryManager.getInstance(project).getRepositories();
        return repos.isEmpty() ? null : repos.get(0);
    }
}