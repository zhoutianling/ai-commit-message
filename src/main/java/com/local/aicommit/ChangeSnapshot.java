package com.local.aicommit;

final class ChangeSnapshot {
    final String path;
    final String status;
    final boolean sensitive;
    final boolean binary;
    final String diff;

    ChangeSnapshot(String path, String status, boolean sensitive, boolean binary, String diff) {
        this.path = path;
        this.status = status;
        this.sensitive = sensitive;
        this.binary = binary;
        this.diff = diff;
    }
}
