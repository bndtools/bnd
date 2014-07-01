package org.bndtools.builder.validate;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.bndtools.api.IValidator;
import org.bndtools.api.Logger;
import org.bndtools.builder.NewBuilder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;

import aQute.bnd.osgi.Builder;

public class JavaVersionsValidator implements IValidator {
    public enum JavaVersionsValidatorFlag {
        SOURCE(0x01), //
        TARGET(0x02);

        private final int value;

        private JavaVersionsValidatorFlag(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }

    private static final String ECLIPSE_COMPILER_COMPLIANCE = "org.eclipse.jdt.core.compiler.compliance";
    private static final String ECLIPSE_COMPILER_SOURCE = "org.eclipse.jdt.core.compiler.source";
    private static final String ECLIPSE_COMPILER_TARGET = "org.eclipse.jdt.core.compiler.codegen.targetPlatform";

    private static final String BND_COMPILER_SOURCE = "javac.source";
    private static final String BND_COMPILER_TARGET = "javac.target";

    private IStatus getReport(JavaVersionsValidatorFlag flag, String javaVersionTitle, String bndValue, String eclipseValue, String property) {
        String s = String.format("Java %s inconsistency: bnd has '%s' while Eclipse has '%s'. Set the bnd '%s' property or change the Eclipse project setup.", javaVersionTitle, bndValue, eclipseValue, property);
        return new Status(IStatus.WARNING, NewBuilder.PLUGIN_ID, flag.getValue(), s, null);
    }

    @Override
    public IStatus validate(Builder builder) {
        IStatus status = Status.OK_STATUS;

        try {
            IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(builder.getBase().getName());
            IJavaProject javaProject = JavaCore.create(project);

            @SuppressWarnings("unchecked")
            Map<String,String> options = javaProject.getOptions(true);

            List<IStatus> reports = new LinkedList<IStatus>();

            String eclipseCompilerCompliance = options.get(ECLIPSE_COMPILER_COMPLIANCE);

            String eclipseCompilerSource = options.get(ECLIPSE_COMPILER_SOURCE);
            String bndSource = builder.getProperty(BND_COMPILER_SOURCE);
            if (bndSource != null) {
                String eclipseValue = (eclipseCompilerSource != null) ? eclipseCompilerSource : eclipseCompilerCompliance;
                if (!bndSource.equals(eclipseValue)) {
                    reports.add(getReport(JavaVersionsValidatorFlag.SOURCE, "source version", bndSource, eclipseValue, BND_COMPILER_SOURCE));
                }
            }

            String eclipseCompilerTarget = options.get(ECLIPSE_COMPILER_TARGET);
            String bndTarget = builder.getProperty(BND_COMPILER_TARGET);
            if (bndTarget != null) {
                String eclipseValue = (eclipseCompilerTarget != null) ? eclipseCompilerTarget : eclipseCompilerCompliance;
                if (!bndTarget.equals(eclipseValue)) {
                    reports.add(getReport(JavaVersionsValidatorFlag.TARGET, "target version", bndTarget, eclipseValue, BND_COMPILER_TARGET));
                }
            }

            if (!reports.isEmpty()) {
                IStatus[] reportsArray = new IStatus[reports.size()];
                reports.toArray(reportsArray);
                status = new MultiStatus(NewBuilder.PLUGIN_ID, 0, reportsArray, "Project paths mismatch" + (reports.size() > 1 ? "es" : ""), null);
            }
        } catch (Throwable e) {
            Logger.getLogger(this.getClass()).logError("Error during java versions validation", e);
        }

        return status;
    }
}