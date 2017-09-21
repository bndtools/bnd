package org.bndtools.builder;

import java.util.Formatter;

import org.eclipse.core.resources.IncrementalProjectBuilder;

public class BuildLogger {
    public static final int LOG_FULL = 2;
    public static final int LOG_BASIC = 1;
    public static final int LOG_NONE = 0;
    private final int level;
    private final String name;
    private final String kind;
    private final long start = System.currentTimeMillis();
    private final StringBuilder sb = new StringBuilder();
    private final Formatter formatter = new Formatter(sb);
    private boolean used = false;
    private int files = -1;

    public BuildLogger(int level, String name, int kind) {
        this.level = level;
        this.name = name;
        switch (kind) {
        case IncrementalProjectBuilder.FULL_BUILD :
            this.kind = "FULL";
            break;
        case IncrementalProjectBuilder.AUTO_BUILD :
            this.kind = "AUTO";
            break;
        case IncrementalProjectBuilder.CLEAN_BUILD :
            this.kind = "CLEAN";
            break;
        case IncrementalProjectBuilder.INCREMENTAL_BUILD :
            this.kind = "INCREMENTAL";
            break;
        default :
            this.kind = String.valueOf(kind);
            break;
        }

    }

    public void basic(String string) {
        basic(string, (Object[]) null);
    }

    public void basic(String string, Object... args) {
        if (level < LOG_BASIC)
            return;

        message(string, args);
    }

    public void full(String string) {
        full(string, (Object[]) null);
    }

    public void full(String string, Object... args) {
        if (level < LOG_FULL)
            return;

        message(string, args);
    }

    public boolean isEmpty() {
        return !used;
    }

    @Override
    public String toString() {
        return sb.toString();
    }

    private void message(String string, Object[] args) {
        used = true;
        if (args == null) {
            sb.append(string);
        } else {
            formatter.format(string, args);
        }
        sb.append('\n');
    }

    public String format() {
        long end = System.currentTimeMillis();
        full("Duration %.2f sec", (end - start) / 1000f);

        StringBuilder top = new StringBuilder();
        try (Formatter topper = new Formatter(top)) {
            if (files > 0)
                topper.format("BUILD %s %s %d file%s built", kind, name, files, files > 1 ? "s were" : " was");
            else
                topper.format("BUILD %s %s no build", kind, name);
        }

        return top.append('\n').append(sb).toString();
    }

    public boolean isActive() {
        return level != LOG_NONE;
    }

    public void setFiles(int f) {
        files = f;
    }
}
