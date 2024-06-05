/*
 * Copyright (c) 2021,2023 IBM Corporation and others.
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
package org.bndtools.elph.bnd;

import static java.lang.String.join;
import static java.util.Collections.unmodifiableList;
import static org.osgi.framework.Constants.BUNDLE_SYMBOLICNAME;

import java.io.IOError;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Stream;

import org.bndtools.elph.util.IO;

final class BndProject {
    final Path root;
    final String name;
    final String symbolicName;
    final List<String> initialDeps;
    final FileTime timestamp;
    final boolean isNoBundle;
    final boolean publishWlpJarDisabled;

    BndProject(Path root) {
        this.root = root;
        this.name = root.getFileName().toString();
        Properties props = getBndProps(root);
        this.symbolicName = Optional.of(props)
                .map(p -> p.getProperty(BUNDLE_SYMBOLICNAME))
                .map(val -> val.replaceFirst(";.*", ""))
                .map(String::trim)
                .orElse(null);
        List<String> deps = new ArrayList<>();
        deps.addAll(getPathProp(props, "-buildpath"));
        deps.addAll(getPathProp(props, "-testpath"));
        deps.remove("");
        this.isNoBundle = props.containsKey("-nobundles");
        this.publishWlpJarDisabled = "true".equals(props.getProperty("publish.wlp.jar.disabled"));
        this.initialDeps = unmodifiableList(deps);
        this.timestamp = IO.getLastModified(root.resolve("bnd.bnd"));
    }

    private static Properties getBndProps(Path root) {
        Path bndPath = root.resolve("bnd.bnd");
        Path bndOverridesPath = root.resolve("bnd.overrides");
        Properties bndProps = new Properties();
        try (var bndRdr = Files.newBufferedReader(bndPath)) {
            bndProps.load(bndRdr);
            if (Files.exists(bndOverridesPath)) {
                try (var overrideRdr = Files.newBufferedReader(bndOverridesPath)) {
                    bndProps.load(overrideRdr);
                }
            }
        } catch (IOException e) {
            throw new IOError(e);
        }
        return bndProps;
    }

    private static List<String> getPathProp(Properties props, String key) {
        String val = props.getProperty(key, "");
        return Stream.of(val.split(",\\s*"))
                .map(s -> s.replaceFirst(";.*", "")) // chop off qualifiers
                .map(s -> s.replaceFirst("\\.\\./([^/]+)/.*", "$1")) // parse relative dirs ../*/
                .toList();
    }

    boolean symbolicNameDiffersFromName() {
        return Objects.nonNull(symbolicName) && !Objects.equals(name, symbolicName);
    }

    @Override
    public String toString() { return name; }

    public String details() {
        return ("===%s===%n" +
                "         dir: %s%n" +
                "symbolicName: %s%n" +
                "        deps: %s").formatted(
                name,
                root.getFileName(),
                symbolicName,
                join("%n              ", initialDeps));
    }
}
