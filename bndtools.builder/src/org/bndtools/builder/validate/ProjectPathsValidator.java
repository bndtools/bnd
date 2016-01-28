package org.bndtools.builder.validate;

import static aQute.bnd.osgi.Constants.DEFAULT_PROP_BIN_DIR;
import static aQute.bnd.osgi.Constants.DEFAULT_PROP_SRC_DIR;
import static aQute.bnd.osgi.Constants.DEFAULT_PROP_TESTBIN_DIR;
import static aQute.bnd.osgi.Constants.DEFAULT_PROP_TESTSRC_DIR;

import java.io.File;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

import org.bndtools.api.BndtoolsConstants;
import org.bndtools.api.IProjectValidator;
import org.bndtools.api.IValidator;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;

import aQute.bnd.build.Project;
import aQute.bnd.build.Workspace;
import aQute.bnd.osgi.Builder;
import aQute.bnd.osgi.Constants;
import aQute.service.reporter.Reporter.SetLocation;
import bndtools.central.Central;

/**
 * Verify that the build path setup for Eclipse matches the actual settings in bnd.
 */
public class ProjectPathsValidator implements IValidator, IProjectValidator {
    final static IPath JRE_CONTAINER = new Path("org.eclipse.jdt.launching.JRE_CONTAINER");

    /*
     * The parts of the test, needed to know what we missed
     */
    enum SetupTypes {
        testsrc, bndcontainer;
    }

    /*
     * Old validate method for backward compatibility (did not want to create yet another extension point for this).
     */
    @Override
    public IStatus validate(Builder builder) {
        return Status.OK_STATUS;
    }

    /**
     * Validate the project
     */
    @Override
    public void validateProject(Project model) throws Exception {
        //
        // We must have, a project and assume this is already reported
        //
        if (model == null) {
            return;
        }

        IJavaProject javaProject = Central.getJavaProject(model);
        if (javaProject == null) {
            model.error("Bndtools: The project in %s is not linked with a Java project.", model.getBase());
            return;
        }

        //
        // Verify if we have the right relation to the cnf folder ...
        //
        Project w;
        try {
            w = Workspace.getProject(model.getBase());
        } catch (Exception e) {
            w = null;
        }
        if (w == null || w != model) {
            model.error("Bndtools: Error in setup, likely the cnf folder is not ../cnf relative to the project folder '%s'. The workspace is in '%s'.", model.getBase(), model.getWorkspace().getBase());
            return;
        }

        //
        // Get the different bnd directories ...
        //
        File bin = model.getOutput();
        File testsrc = model.getTestSrc();
        File testbin = model.getTestOutput();
        Set<File> sourcePath = new HashSet<File>(model.getSourcePath());

        //
        // All the things we should find when we have traversed the build path
        //
        Set<SetupTypes> found = EnumSet.allOf(SetupTypes.class);

        for (IClasspathEntry cpe : javaProject.getRawClasspath()) {
            int kind = cpe.getEntryKind();
            switch (kind) {
            case IClasspathEntry.CPE_VARIABLE :
                warning(model, null, null, cpe, "Bndtools: Found a variable in the eclipse build path, this variable is not available during continuous integration", cpe).file(new File(model.getBase(), ".classpath").getAbsolutePath());
                break;

            case IClasspathEntry.CPE_LIBRARY :
                warning(model, null, null, cpe, "Bndtools: The .classpath contains a library that will not be available during continuous integration: %s", cpe.getPath()).file(new File(model.getBase(), ".classpath").getAbsolutePath());
                break;

            case IClasspathEntry.CPE_CONTAINER :
                if (BndtoolsConstants.BND_CLASSPATH_ID.segment(0).equals(cpe.getPath().segment(0)))
                    found.remove(SetupTypes.bndcontainer);
                else {
                    IPath path = cpe.getPath();
                    if (JRE_CONTAINER.segment(0).equals(path.segment(0))) {
                        if (path.segmentCount() == 1) {
                            // warning because default might vary <classpathentry kind="con" path="org.eclipse.jdt.launching.JRE_CONTAINER"/>
                            warning(model, null, path.toString(), cpe,
                                    "Bndtools: The .classpath contains a default JRE container: %s. This makes it undefined to what version you compile and this might differ between Continuous Integration and Eclipse", path)
                                            .file(new File(model.getBase(), ".classpath").getAbsolutePath());
                        } else {
                            String segment = path.segment(1);
                            if ("org.eclipse.jdt.internal.debug.ui.launcher.StandardVMType".equals(segment) && path.segmentCount() == 3) {
                                // check javac version <classpathentry kind="con" path="org.eclipse.jdt.launching.JRE_CONTAINER/org.eclipse.jdt.internal.debug.ui.launcher.StandardVMType/JavaSE-1.7"/>

                                String javac = model.getProperty(Constants.JAVAC_SOURCE, "1.5");
                                if (!path.segment(2).endsWith(javac)) {
                                    warning(model, null, path.toString(), cpe, "Bndtools: The .JRE container is set to %s but bnd is compiling against %s", path.segment(2), javac)
                                            .file(new File(model.getBase(), ".classpath").getAbsolutePath());
                                }
                            } else {
                                // warning because local/machine specific <classpathentry kind="con" path="org.eclipse.jdt.launching.JRE_CONTAINER/org.eclipse.jdt.internal.launching.macosx.MacOSXType/Java SE 7 [1.7.0_71]"/>
                                warning(model, null, path.toString(), cpe,
                                        "Bndtools: The .classpath contains an non-portable JRE container: %s. This makes it undefined to what version you compile and this might differ between Continuous Integration and Eclipse", path)
                                                .file(new File(model.getBase(), ".classpath").getAbsolutePath());
                            }
                        }
                    } else {
                        warning(model, null, path.toString(), cpe, "Bndtools: The .classpath contains an unknown container: %s. This could make your build less portable.", path)
                                .file(new File(model.getBase(), ".classpath").getAbsolutePath());
                    }
                }
                break;

            case IClasspathEntry.CPE_SOURCE :
                File file = toFile(cpe.getPath());
                if (file == null) {
                    model.warning("Bndtools: Found virtual file for '%s'", cpe.getPath()).details(cpe);
                } else {
                    File output = toFile(cpe.getOutputLocation());
                    if (output == null)
                        output = toFile(javaProject.getOutputLocation());

                    if (file.equals(testsrc)) {
                        //
                        // We're talking about the test source directory
                        // This should be linked to testbin
                        //
                        found.remove(SetupTypes.testsrc);
                        if (!testbin.equals(output)) {
                            warning(model, DEFAULT_PROP_TESTBIN_DIR, testbin, cpe, "Bndtools: testsrc folder '%s' has output folder set to '%s', which does not match bnd's testbin folder '%s'", file, output, testbin);
                        }
                    } else {
                        //
                        // We must have a source directory. They must be linked to the bin
                        // folder and on the bnd source path. Since the source path has
                        // potentially multiple entries, we remove this one so we can check
                        // later if we had all of them
                        //
                        if (sourcePath.remove(file)) {
                            if (!bin.equals(output)) {
                                warning(model, DEFAULT_PROP_BIN_DIR, bin, cpe, "Bndtools: src folder '%s' has output folder set to '%s', which does not match bnd's bin folder '%s'", file, output, bin);
                            }
                        } else {
                            warning(model, DEFAULT_PROP_SRC_DIR, null, cpe, "Bndtools: Found source folder '%s' that is not on bnd's source path '%s'", file, model.getProperty(Constants.DEFAULT_PROP_SRC_DIR));
                        }
                    }
                }
                break;

            default :
                break;
            }

        }

        //
        // If we had not see all source entries, then we should
        // have something in sourcePath
        //
        for (File file : sourcePath) {
            warning(model, DEFAULT_PROP_SRC_DIR, file, null, "Bndtools: bnd's src folder '%s' is not in the Eclipse build path", file);
        }

        //
        // Check if we had all the different things we needed to check
        //
        for (SetupTypes t : found) {
            switch (t) {
            case testsrc :
                if (testsrc.isDirectory()) // if the testsrc directory does not exist, then don't warn
                    warning(model, DEFAULT_PROP_TESTSRC_DIR, null, null, "Bndtools: bnd's testsrc folder '%s' is not in the Eclipse build path", testsrc);
                break;

            case bndcontainer :
                warning(model, null, null, null, "Bndtools: The build path does not refer to the bnd container '%s'", BndtoolsConstants.BND_CLASSPATH_ID.segment(0));
                break;

            default :
                break;
            }
        }
    }

    private SetLocation warning(Project model, String header, Object context, IClasspathEntry cpe, String format, Object... args) {
        String prefix = model.getBase().getAbsolutePath();
        for (int i = 0; i < args.length; i++) {
            args[i] = relative(prefix, args[i]);
        }
        SetLocation loc = model.warning(format, args);
        if (header != null)
            loc.header(header);
        if (context != null) {
            loc.context(relative(prefix, context).toString());
        }
        if (cpe != null) {
            loc.details(cpe);
        }
        loc.file(model.getPropertiesFile().getAbsolutePath());
        return loc;
    }

    private Object relative(String prefix, Object object) {
        if (!(object instanceof File))
            return object;

        String path = object.toString();
        if (!path.startsWith(prefix))
            return object;

        return path.substring(prefix.length() + 1);
    }

    private File toFile(IPath path) {
        if (path == null)
            return null;

        IFile file = ResourcesPlugin.getWorkspace().getRoot().getFile(path);
        if (file != null)
            return file.getLocation().toFile().getAbsoluteFile();
        return null;
    }
}