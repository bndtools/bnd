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
package org.bndtools.elph.bnd;

import java.nio.file.Path;
import java.util.Collection;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toCollection;

/**
 * Outside this package, projects are referred to using their (absolute) path objects.
 * This is a collection of convenience functions for translation from Path to String.
 */
public enum ProjectPaths {
    ;

    public static Stream<String> asNames(Collection<Path> projects) {
        return projects.stream().map(ProjectPaths::toName);
    }
    public static Set<String> toNames(Collection<Path> projects) { return asNames(projects).collect(toCollection(TreeSet::new)); }
    public static String toName(Path project) {
        return project.getFileName().toString();
    }
    public static String toMultilineString(Collection<Path> projects) { return projects.stream().map(ProjectPaths::toName).collect(joining("\n\t", "[\t", "\n]"));}
    public static String toInlineString(Collection<Path> projects) { return projects.stream().map(ProjectPaths::toName).collect(joining(" ", "[ ", " ]"));}
}
