/*
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package org.bndtools.elph.util;

import static org.bndtools.elph.util.IO.Verbosity.DEBUG;
import static org.bndtools.elph.util.IO.Verbosity.INFO;
import static org.bndtools.elph.util.IO.Verbosity.LOG;
import static org.bndtools.elph.util.IO.Verbosity.OFF;
import static org.bndtools.elph.util.Objects.stringEquals;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.Scanner;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class IO {
    public enum Verbosity {OFF, INFO, LOG, DEBUG}
    /** Use statics so settings are global */
    private static Verbosity verbosity = OFF;
    private static boolean quiet;
    /** Single scanner for the whole process */
    final static Scanner SCANNER = new Scanner(System.in);

    public static FileTime getLastModified(Path file) {
        if (Files.isRegularFile(file)) {
            try {
                return Files.getLastModifiedTime(file);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return FileTime.from(Instant.EPOCH);
    }

    public boolean isEnabled(Verbosity v) { return !quiet && v.compareTo(verbosity) <= 0; }

    public void reportDirectory(String title, Object oldPath, Object newPath) {
        if (null == newPath || stringEquals(oldPath, newPath)) {
            reportf("%40s: %s", title, stringify(oldPath));
        } else {
            reportf("%40s: %s (was: %s)", title, stringify(newPath), stringify(oldPath));
        }
    }

    public Error error(String message, Object... details) {
        System.err.println("ERROR: " + message);
        for (Object detail: details) System.err.println(detail);
        System.exit(1);
        throw new Error();
    }

    public void warn(String message, Object... details) {
        System.err.println("WARNING: " + message);
        for (Object detail : details) System.err.println(detail);
    }

    public void info(Supplier<String> msg) { if (isEnabled(INFO)) System.out.println(msg.get()); }
    public void infof(String msg, Object...inserts) { if (isEnabled(INFO)) System.out.printf((msg) + "%n", inserts); }
    public void log(Supplier<String> msg) { if (isEnabled(LOG)) System.out.println(msg.get()); }
    public void logf(String msg, Object...inserts) { if (isEnabled(LOG)) System.out.printf((msg) + "%n", inserts); }
    public void debug(Supplier<String> msg) { if (isEnabled(DEBUG)) System.out.println(msg.get()); }
    public void debugf(String msg, Object...inserts) { if (isEnabled(DEBUG)) System.out.printf((msg) + "%n", inserts); }
    public void report(Object msg) { if (!quiet) System.out.println(msg); }
    public void reportf(String msg, Object... inserts) { if (!quiet) System.out.printf((msg) + "%n", inserts); }

    public Path verifyOrCreateFile(String desc, Path file) {
        verifyOrCreateDir("Parent of " + desc, file.getParent());
        if (Files.exists(file) && !Files.isDirectory(file) && Files.isWritable(file)) return file;
        try {
            return Files.createFile(file);
        } catch (IOException e) {
            throw error("Could not create " + desc + ": " + file);
        }
    }

    public void writeFile(String desc, Path file, String contents) {
        verifyOrCreateDir("Parent of " + desc, file.getParent());
        try {
            Files.writeString(file, contents);
        } catch (IOException e) {
            throw error("Could not write to " + desc + ": " + file, e);
        }
    }

    public void readFile(String desc, Path file, Consumer<String> actionPerLine) {
        try {
            Files.readAllLines(file).forEach(actionPerLine);
        } catch (IOException e) {
            throw error("Could not read from " + desc + ": " + file, e);
        }
    }

    public Path verifyOrCreateDir(String desc, Path dir) {
        if (Files.isDirectory(dir)) return dir;
        if (Files.exists(dir)) throw error("Expected directory but found file for " + desc + ": " + dir);
        try {
            return Files.createDirectory(dir);
        } catch (IOException e) {
            throw error("Could not create " + desc + ": " + dir);
        }
    }

    public Path verifyDir(String desc, Path dir) {
        if (Files.isDirectory(dir)) return dir;
        throw error("Could not locate " + desc + ": " + dir);
    }

    public void pause() {
        if (quiet) return;
        report("Press return to continue, or control-C to quit.");
        try(Scanner s = new Scanner(System.in)) {s.nextLine();}
    }

    private static String stringify(Object setting) {
        return setting == null ? "<not specified>" : setting.toString();
    }
}
