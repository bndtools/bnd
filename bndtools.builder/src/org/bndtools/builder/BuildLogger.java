package org.bndtools.builder;

import java.util.Formatter;

import org.eclipse.core.resources.IncrementalProjectBuilder;

public class BuildLogger {
    public static final int LOG_FULL = 2;
    public static final int LOG_BASIC = 1;
    public static final int LOG_NONE = 0;
    private final int level;
    private final String name;
    private final int kind;
    private final long start = System.currentTimeMillis();
    private final StringBuilder sb = new StringBuilder();
    private final Formatter formatter = new Formatter(sb);
    private boolean used = false;
    private int files = -1;

    public BuildLogger(int level, String name, int kind) {
        this.level = level;
        this.name = name;
        this.kind = kind;
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
        String kindString;
        switch (kind) {
        case IncrementalProjectBuilder.FULL_BUILD :
            kindString = "FULL";
            break;
        case IncrementalProjectBuilder.AUTO_BUILD :
            kindString = "AUTO";
            break;
        case IncrementalProjectBuilder.CLEAN_BUILD :
            kindString = "CLEAN";
            break;
        case IncrementalProjectBuilder.INCREMENTAL_BUILD :
            kindString = "INCREMENTAL";
            break;
        default :
            kindString = String.valueOf(kind);
            break;
        }

        StringBuilder top = new StringBuilder();
        try (Formatter topper = new Formatter(top)) {
            if (files > 0)
                topper.format("BUILD %s %s %d file%s built", kindString, name, files, files > 1 ? "s were" : " was");
            else
                topper.format("BUILD %s %s no build", kindString, name);
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
