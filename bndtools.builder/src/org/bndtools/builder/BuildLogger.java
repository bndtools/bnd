package org.bndtools.builder;

import java.util.Formatter;

import aQute.bnd.build.Project;

public class BuildLogger {
    static final int LOG_FULL = 2;
    static final int LOG_BASIC = 1;
    static final int LOG_NONE = 0;
    private final int level;
    long start = System.currentTimeMillis();
    Formatter formatter = new Formatter();
    boolean used = false;

    public BuildLogger(int level) {
        this.level = level;
    }

    public void basic(String string, Object... args) {
        if (level < LOG_BASIC)
            return;

        message(string, args);
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
        return formatter.toString();
    }

    private void message(String string, Object... args) {
        used = true;
        formatter.format(string, args);
        formatter.format("\n");
    }

    public String toString(Project model, int files) {
        long end = System.currentTimeMillis();
        full("Duration %.2f sec", (end - start) / 1000f);

        String top = "BUILD " + model;
        if (files > 0)
            top += " " + files + " file" + (files == 1 ? " was" : "s were") + " built";
        else
            top += " no build";
        top += "\n";

        return top + formatter;

    }

}
