/*******************************************************************************
 * Copyright (c) 2012 Per Kr. Soreide.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Per Kr. Soreide - initial API and implementation
 *******************************************************************************/
package bndtools.release;

import java.util.List;

import aQute.bnd.build.Project;
import aQute.bnd.build.ProjectBuilder;
import aQute.bnd.differ.Baseline;
import aQute.bnd.differ.DiffPluginImpl;
import aQute.bnd.osgi.Builder;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Jar;

public class DiffHelper {

    public static Baseline createBaseline(Project project, String bsn) throws Exception {
        try (ProjectBuilder pb = project.getBuilder(null)) {
            List<Builder> builders = pb.getSubBuilders();
            for (Builder b : builders) {
                if (bsn.equals(b.getBsn())) {
                    return createBaseline(b);
                }
            }
            return null;
        }
    }

    public static Baseline createBaseline(Builder builder) {

        try {

            if (builder instanceof ProjectBuilder) {

                ProjectBuilder projectBuilder = (ProjectBuilder) builder;

                Jar jar = null;
                Jar currentJar = null;

                try {
                    jar = projectBuilder.build();

                    currentJar = projectBuilder.getBaselineJar();
                    if (currentJar == null) {
                        currentJar = projectBuilder.getLastRevision();
                    }
                    if (currentJar == null) {
                        currentJar = new Jar("."); //$NON-NLS-1$
                    }
                    DiffPluginImpl differ = new DiffPluginImpl();
                    String diffignore = projectBuilder.getProperty(Constants.DIFFIGNORE);
                    if (diffignore != null)
                        differ.setIgnore(diffignore);

                    Baseline baseline = new Baseline(projectBuilder, differ);

                    baseline.baseline(jar, currentJar, null);
                    return baseline;
                } finally {
                    if (jar != null)
                        jar.close();
                    if (currentJar != null)
                        currentJar.close();
                }
            }
        } catch (Exception e1) {
            e1.printStackTrace();
        }
        return null;

    }

    public static String removeVersionQualifier(String version) {
        if (version == null) {
            return null;
        }
        // Remove qualifier
        String[] parts = version.split("\\."); //$NON-NLS-1$
        StringBuilder sb = new StringBuilder();
        String sep = "";
        for (int i = 0; i < parts.length; i++) {
            if (i == 3) {
                break;
            }
            sb.append(sep);
            sb.append(parts[i]);
            sep = "."; //$NON-NLS-1$
        }
        return sb.toString();
    }

}
