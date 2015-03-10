package org.bndtools.builder.validate;

import static aQute.bnd.osgi.Constants.DEFAULT_PROP_BIN_DIR;
import static aQute.bnd.osgi.Constants.DEFAULT_PROP_SRC_DIR;
import static aQute.bnd.osgi.Constants.DEFAULT_PROP_TESTBIN_DIR;
import static aQute.bnd.osgi.Constants.DEFAULT_PROP_TESTSRC_DIR;

import java.io.File;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.bndtools.api.IProjectValidator;
import org.bndtools.api.IValidator;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;

import aQute.bnd.build.Project;
import aQute.bnd.build.Workspace;
import aQute.bnd.osgi.Builder;
import aQute.service.reporter.Reporter.SetLocation;
import bndtools.central.Central;

/**
 * Verify that the build path setup for Eclipse matches the actual settings in bnd.
 */
public class ProjectPathsValidator implements IValidator, IProjectValidator {

    /*
     * The parts of the test, needed to know what we missed
     */
    enum SetupTypes {
        bin, bin_test, test, bndcontainer;
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
            model.error("Eclipse: The project in %s is not linked with a Java project.", model.getBase());
            return;
        }

        //
        // Verify if we have the right relation to the cnf folder ...
        //

        Project w = Workspace.getProject(model.getBase());
        if (w == null || w != model) {
            model.error("Eclipse: Error in setup, likely the cnf folder is not ../cnf relative from the project folder %s. The workspace is in %s", model.getBase(), model.getWorkspace().getBase());
            return;
        }

        //
        // Get the different bnd directories ...
        //

        File bin = model.getOutput();
        File test = model.getTestSrc();
        File bin_test = model.getTestOutput();
        Set<File> sourcePath = new HashSet<File>(model.getSourcePath());

        // TODO remove, as long as bnd does not support the multiple entries on sourcepath

        if (sourcePath.size() == 1 && sourcePath.iterator().next().equals(model.getBase()))
            return;

        //
        // All the things we should find when we have traversed the build path
        //

        Set<SetupTypes> found = new HashSet<SetupTypes>(EnumSet.allOf(SetupTypes.class));

        for (IClasspathEntry cpe : javaProject.getRawClasspath()) {

            int kind = cpe.getEntryKind();
            switch (kind) {
            case IClasspathEntry.CPE_VARIABLE :
                warning(model, null, null, cpe, "Eclipse: Found a variable in the eclipse build path, this variable is not available during continuous integration", cpe);
                break;

            case IClasspathEntry.CPE_CONTAINER :
                if ("aQute.bnd.classpath.container".equals(cpe.getPath().toString()))
                    found.remove(SetupTypes.bndcontainer);
                break;

            case IClasspathEntry.CPE_SOURCE :
                File file = toFile(cpe.getPath());
                if (file == null) {
                    model.warning("Eclipse: Found virtual file for %s", cpe).details(cpe);
                } else {
                    File output = toFile(cpe.getOutputLocation());
                    if (output == null)
                        output = toFile(javaProject.getOutputLocation());

                    if (file.equals(test)) {
                        //
                        // We're talking about the test source directory
                        // This should be linked to bin_test
                        //

                        found.remove(SetupTypes.test);

                        if (bin_test.equals(output)) {
                            found.remove(SetupTypes.bin_test);
                        } else
                            warning(model, DEFAULT_PROP_TESTBIN_DIR, bin_test, cpe, "Eclipse: Source test folder %s has output set to %s, which does not match bnd's bin_test folder %s", file, output, bin_test);
                    } else {

                        //
                        // We must have a source directory. They must be linked to the bin
                        // folder and on the bnd source path. Since the source path has
                        // potentially multiple entries, we remove this one so we can check
                        // later if we had all of them
                        //

                        if (sourcePath.remove(file.getAbsoluteFile())) {

                            if (!bin.equals(output)) {
                                warning(model, DEFAULT_PROP_BIN_DIR, bin, cpe, "Eclipse: Source folder %s has output set to %s, \n" + "which does not match bnd's bin folder %s", file, output, bin_test);
                            }

                            found.remove(SetupTypes.bin);

                        } else {
                            warning(model, DEFAULT_PROP_SRC_DIR, null, cpe, "Eclipse: Found source folder %s that is not on the source path %s", file, model.getSourcePath());
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

        for (File f : sourcePath) {
            warning(model, DEFAULT_PROP_SRC_DIR, f, null, "Eclipse: Source directory '%s' defined in bnd and not on the Eclipse build path", f);
        }

        //
        // Check if we had all the different things we needed to check
        //

        for (SetupTypes t : found) {
            switch (t) {
            case bin :
                warning(model, DEFAULT_PROP_BIN_DIR, null, null, "Eclipse: No entry on the build path uses the bnd bin directory %s", bin);
                break;

            case bin_test :
                warning(model, DEFAULT_PROP_TESTBIN_DIR, null, null, "Eclipse: No entry on the build path uses the bnd bin_test directory %s", bin_test);
                break;

            case bndcontainer :
                warning(model, null, null, null, "Eclipse: The build path does not refer to a bnd container");
                break;

            case test :
                warning(model, DEFAULT_PROP_TESTSRC_DIR, null, null, "Eclipse: No test folder %s found", test);
                break;

            default :
                break;

            }
        }
    }

    private Object relative(Project model, Set<File> sourcePath) {
        String prefix = model.getBase().getAbsolutePath();
        List<String> rel = new ArrayList<String>();
        for (File f : sourcePath)
            rel.add(relative(prefix, f).toString());
        return rel;
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
            return file.getLocation().toFile();
        return null;
    }
}