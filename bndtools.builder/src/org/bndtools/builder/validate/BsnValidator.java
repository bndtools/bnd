package org.bndtools.builder.validate;

import java.io.File;

import org.bndtools.api.IValidator;
import org.eclipse.core.runtime.IStatus;

import aQute.bnd.build.Project;
import aQute.bnd.build.Workspace;
import aQute.bnd.osgi.Builder;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Processor;
import aQute.service.reporter.Reporter.SetLocation;

public class BsnValidator implements IValidator {

    @Override
    public IStatus validate(Builder builder) throws Exception {
        Project project = Workspace.getProject(builder.getBase());
        if (project == null) {
            builder.error("Eclipse: Cannot find associated project for %s", builder);
            return null;
        }

        String bsn = builder.getBsn();
        if (bsn == null) {
            loc(bsn, builder, builder.warning("Eclipse: Bundle Symbolic Name not valid, get null"));
            return null;
        }

        if (!bsn.startsWith(project.getName())) {
            loc(bsn, builder, builder.warning("Eclipse: The Bundle Symbolic Name must %s start with the project name %s, " + "which must be the project's directory %s name", bsn, project, project.getBase()));
            return null;
        }

        File pf = builder.getPropertiesFile();
        String rover = bsn.substring(project.getName().length());
        if (rover.startsWith(".")) {
            rover = rover.substring(1);

            if (pf == null) {
                loc(bsn, builder, builder.warning("Eclipse: The Bundle Symbolic %s starts with the project name %s " + "but then continues while it is not a sub-bundle", bsn, project));
                return null;
            }
            String suffix = removeExtension(pf.getName());
            if (!suffix.equals(rover)) {
                loc(bsn, builder, builder.warning("Eclipse: The Bundle Symbolic %s starts with the project name %s but then does not end with the subbuilder name", bsn, project, pf.getName()));
            }
            return null;
        }

        if (rover.isEmpty()) {
            if (pf != null) {
                loc(bsn, builder, builder.warning("Eclipse: The Bundle Symbolic %s is a sub-bundle %s but uses the project name", bsn, pf.getName(), project));
            }
            return null;
        }

        loc(bsn, builder, builder.warning("Eclipse: The Bundle Symbolic %s starts with the project name %s " + "but then continues but lacks the separating '.'", bsn, project));
        return null;
    }

    private void loc(String bsn, Processor p, SetLocation loc) {
        File file = p.getPropertiesFile();
        if (file != null)
            loc.file(file.getAbsolutePath());
        loc.header(Constants.BUNDLE_SYMBOLICNAME);
        if (bsn != null)
            loc.context(bsn);
    }

    private String removeExtension(String name) {
        if (name.endsWith(".bnd"))
            return name.substring(0, name.length() - 4);
        return name;
    }

}
