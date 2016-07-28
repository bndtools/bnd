/*******************************************************************************
 * Copyright (c) 2008-2010 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *******************************************************************************/

package org.bndtools.builder.classpath;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IAccessRule;
import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.JavaCore;

class ClasspathContainerSerializationHelper<C extends IClasspathContainer> {
    C readContainer(File file) throws IOException, ClassNotFoundException {
        try (FileInputStream in = new FileInputStream(file)) {
            return readContainer(in);
        }
    }

    C readContainer(InputStream in) throws IOException, ClassNotFoundException {
        ObjectInputStream is = new ObjectInputStream(new BufferedInputStream(in)) {
            {
                enableResolveObject(true);
            }

            @Override
            protected Object resolveObject(Object o) throws IOException {
                if (o instanceof ProjectEntryReplace) {
                    return ((ProjectEntryReplace) o).getEntry();
                } else if (o instanceof LibraryEntryReplace) {
                    return ((LibraryEntryReplace) o).getEntry();
                } else if (o instanceof ClasspathAttributeReplace) {
                    return ((ClasspathAttributeReplace) o).getAttribute();
                } else if (o instanceof AccessRuleReplace) {
                    return ((AccessRuleReplace) o).getAccessRule();
                } else if (o instanceof PathReplace) {
                    return ((PathReplace) o).getPath();
                }
                return super.resolveObject(o);
            }
        };
        @SuppressWarnings("unchecked")
        C container = (C) is.readObject();
        return container;
    }

    void writeContainer(C container, File file) throws IOException {
        try (FileOutputStream out = new FileOutputStream(file)) {
            writeContainer(container, out);
        }
    }

    void writeContainer(C container, OutputStream out) throws IOException {
        ObjectOutputStream os = new ObjectOutputStream(new BufferedOutputStream(out)) {
            {
                enableReplaceObject(true);
            }

            @Override
            protected Object replaceObject(Object o) throws IOException {
                if (o instanceof IClasspathEntry) {
                    IClasspathEntry e = (IClasspathEntry) o;
                    if (e.getEntryKind() == IClasspathEntry.CPE_PROJECT) {
                        return new ProjectEntryReplace(e);
                    } else if (e.getEntryKind() == IClasspathEntry.CPE_LIBRARY) {
                        return new LibraryEntryReplace(e);
                    }
                } else if (o instanceof IClasspathAttribute) {
                    return new ClasspathAttributeReplace((IClasspathAttribute) o);
                } else if (o instanceof IAccessRule) {
                    return new AccessRuleReplace((IAccessRule) o);
                } else if (o instanceof IPath) {
                    return new PathReplace((IPath) o);
                }
                return super.replaceObject(o);
            }
        };
        os.writeObject(container);
        os.flush();
    }

    /**
     * A library IClasspathEntry replacement used for object serialization
     */
    static final class LibraryEntryReplace implements Serializable {
        private static final long serialVersionUID = 3901667379326978799L;

        private final IPath path;
        private final IPath sourceAttachmentPath;
        private final IPath sourceAttachmentRootPath;
        private final IClasspathAttribute[] extraAttributes;
        private final boolean exported;
        private final IAccessRule[] accessRules;

        LibraryEntryReplace(IClasspathEntry entry) {
            this.path = entry.getPath();
            this.sourceAttachmentPath = entry.getSourceAttachmentPath();
            this.sourceAttachmentRootPath = entry.getSourceAttachmentRootPath();
            this.accessRules = entry.getAccessRules();
            this.extraAttributes = entry.getExtraAttributes();
            this.exported = entry.isExported();
        }

        IClasspathEntry getEntry() {
            return JavaCore.newLibraryEntry(path, sourceAttachmentPath, sourceAttachmentRootPath, //
                    accessRules, extraAttributes, exported);
        }
    }

    /**
     * A project IClasspathEntry replacement used for object serialization
     */
    static final class ProjectEntryReplace implements Serializable {
        private static final long serialVersionUID = -2397483865904288762L;

        private final IPath path;
        private final IClasspathAttribute[] extraAttributes;
        private final IAccessRule[] accessRules;
        private final boolean exported;
        private final boolean combineAccessRules;

        ProjectEntryReplace(IClasspathEntry entry) {
            this.path = entry.getPath();
            this.accessRules = entry.getAccessRules();
            this.extraAttributes = entry.getExtraAttributes();
            this.exported = entry.isExported();
            this.combineAccessRules = entry.combineAccessRules();
        }

        IClasspathEntry getEntry() {
            return JavaCore.newProjectEntry(path, accessRules, //
                    combineAccessRules, extraAttributes, exported);
        }
    }

    /**
     * An IClasspathAttribute replacement used for object serialization
     */
    static final class ClasspathAttributeReplace implements Serializable {
        private static final long serialVersionUID = 6370039352012628029L;

        private final String name;
        private final String value;

        ClasspathAttributeReplace(IClasspathAttribute attribute) {
            this.name = attribute.getName();
            this.value = attribute.getValue();
        }

        IClasspathAttribute getAttribute() {
            return JavaCore.newClasspathAttribute(name, value);
        }
    }

    /**
     * An IAccessRule replacement used for object serialization
     */
    static final class AccessRuleReplace implements Serializable {
        private static final long serialVersionUID = 7315582893941374715L;

        private final IPath pattern;
        private final int kind;

        AccessRuleReplace(IAccessRule accessRule) {
            pattern = accessRule.getPattern();
            kind = accessRule.getKind() | (accessRule.ignoreIfBetter() ? IAccessRule.IGNORE_IF_BETTER : 0);
        }

        IAccessRule getAccessRule() {
            return JavaCore.newAccessRule(pattern, kind);
        }
    }

    /**
     * An IPath replacement used for object serialization
     */
    static final class PathReplace implements Serializable {
        private static final long serialVersionUID = -2361259525684491181L;

        private final String path;

        PathReplace(IPath path) {
            this.path = path.toPortableString();
        }

        IPath getPath() {
            return Path.fromPortableString(path);
        }
    }
}
