package org.bndtools.builder.validate;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.bndtools.api.IValidator;
import org.bndtools.api.Logger;
import org.bndtools.api.ProjectLayout;
import org.bndtools.api.ProjectPaths;
import org.bndtools.builder.NewBuilder;
import org.bndtools.utils.javaproject.JavaProjectUtils;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;

import aQute.bnd.osgi.Builder;
import aQute.bnd.osgi.Constants;

public class ProjectPathsValidator implements IValidator {
    public enum ProjectPathsValidatorFlag {
        SRC(0x01), //
        BIN(0x02), //
        TESTSRC(0x04), //
        TESTBIN(0x08), //
        TOOMANYSOURCESETS(0x10);

        private final int value;

        private ProjectPathsValidatorFlag(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }

    private IStatus getReport(ProjectPathsValidatorFlag flag, String sourceSetName, String bndValue, String eclipseValue, String property) {
        String s = String.format("%s inconsistency: bnd has '%s' while Eclipse has '%s'. Set the bnd '%s' property or change the Eclipse project setup.", sourceSetName, bndValue, eclipseValue, property);
        return new Status(IStatus.ERROR, NewBuilder.PLUGIN_ID, flag.getValue(), s, null);
    }

    @Override
    public IStatus validate(Builder builder) {
        IStatus status = Status.OK_STATUS;

        try {
            IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(builder.getBase().getName());
            IJavaProject javaProject = JavaCore.create(project);
            Map<String,String> sourceOutputLocations = JavaProjectUtils.getSourceOutputLocations(javaProject);

            ProjectPaths bndLayout = ProjectPaths.get(ProjectLayout.BND);
            String src = builder.getProperty(Constants.DEFAULT_PROP_SRC_DIR, bndLayout.getSrc());
            String bin = builder.getProperty(Constants.DEFAULT_PROP_BIN_DIR, bndLayout.getBin());
            String testSrc = builder.getProperty(Constants.DEFAULT_PROP_TESTSRC_DIR, bndLayout.getTestSrc());
            String testBin = builder.getProperty(Constants.DEFAULT_PROP_TESTBIN_DIR, bndLayout.getTestBin());

            List<IStatus> reports = new LinkedList<IStatus>();

            int index = 0;
            for (Map.Entry<String,String> entry : sourceOutputLocations.entrySet()) {
                String entrySrc = entry.getKey();
                String entryBin = entry.getValue();

                switch (index) {
                case 0 :
                    if (!src.equals(entrySrc)) {
                        reports.add(getReport(ProjectPathsValidatorFlag.SRC, "Main source folder", src, entrySrc, Constants.DEFAULT_PROP_SRC_DIR));
                    }
                    if (!bin.equals(entryBin)) {
                        reports.add(getReport(ProjectPathsValidatorFlag.BIN, "Main source output folder", bin, entryBin, Constants.DEFAULT_PROP_BIN_DIR));
                    }
                    break;
                case 1 :
                    if (!testSrc.equals(entrySrc)) {
                        reports.add(getReport(ProjectPathsValidatorFlag.TESTSRC, "Test source folder", testSrc, entrySrc, Constants.DEFAULT_PROP_TESTSRC_DIR));
                    }
                    if (!testBin.equals(entryBin)) {
                        reports.add(getReport(ProjectPathsValidatorFlag.TESTBIN, "Test source output folder", testBin, entryBin, Constants.DEFAULT_PROP_TESTBIN_DIR));
                    }
                    break;

                default :
                    reports.add(new Status(IStatus.ERROR, this.getClass().getName(), ProjectPathsValidatorFlag.TOOMANYSOURCESETS.getValue(), "Too many Eclipse source sets defined, bnd project can have at most 2", null));
                    break;
                }
                index++;
            }

            if (!reports.isEmpty()) {
                IStatus[] reportsArray = new IStatus[reports.size()];
                reports.toArray(reportsArray);
                status = new MultiStatus(NewBuilder.PLUGIN_ID, 0, reportsArray, "Project paths mismatch" + (reports.size() > 1 ? "es" : ""), null);
            }
        } catch (Throwable e) {
            Logger.getLogger(this.getClass()).logError("Error during project paths validation", e);
        }

        return status;
    }
}