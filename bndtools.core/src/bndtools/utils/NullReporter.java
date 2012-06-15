package bndtools.utils;

import java.util.Collections;
import java.util.List;

import aQute.libg.reporter.Reporter;

public class NullReporter implements Reporter {

    public void error(String s, Object... args) {}

    public void warning(String s, Object... args) {}

    public void progress(String s, Object... args) {}

    public void trace(String s, Object... args) {}

    public List<String> getWarnings() {
        return Collections.emptyList();
    }

    public List<String> getErrors() {
        return Collections.emptyList();
    }

    public boolean isPedantic() {
        return false;
    }

    public boolean isExceptions() {
        return false;
    }

}
