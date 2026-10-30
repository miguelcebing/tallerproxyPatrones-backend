package com.miguelcebing.tallerproxy.logging;

import java.util.ArrayList;
import java.util.List;

public class StackTraceSummarizer {

    private final int maxFrames;

    public StackTraceSummarizer(int maxFrames) {
        this.maxFrames = Math.max(1, maxFrames);
    }

    public List<String> summarize(Throwable error) {
        List<String> summary = new ArrayList<>();
        summary.add(error.getClass().getName() + ": " + error.getMessage());
        StackTraceElement[] frames = error.getStackTrace();
        int limit = Math.min(maxFrames, frames.length);
        for (int index = 0; index < limit; index++) {
            summary.add("at " + frames[index]);
        }
        int remaining = frames.length - limit;
        if (remaining > 0) {
            summary.add("... " + remaining + " more");
        }
        Throwable cause = error.getCause();
        if (cause != null) {
            summary.add("caused by " + cause.getClass().getName() + ": " + cause.getMessage());
        }
        return List.copyOf(summary);
    }
}
