package bndtools.internal.decorator;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;

import aQute.bnd.build.Project;
import aQute.bnd.build.Workspace;
import aQute.lib.osgi.Builder;
import aQute.lib.osgi.Constants;
import aQute.lib.osgi.Instruction;
import aQute.lib.osgi.Instructions;
import aQute.libg.header.Parameters;
import bndtools.Central;
import bndtools.api.ILogger;
import bndtools.utils.SWTConcurrencyUtil;

public class ExportedPackageDecoratorJob extends Job {

    private final IProject project;
    private final ILogger logger;

    public ExportedPackageDecoratorJob(IProject project, ILogger logger) {
        super("Update exported packages");
        this.project = project;
        this.logger = logger;
    }

    @Override
    protected IStatus run(IProgressMonitor monitor) {
        try {
            Project model = Workspace.getProject(project.getLocation().toFile());
            Collection< ? extends Builder> builders = model.getSubBuilders();

            Set<Instruction> allExports = new HashSet<Instruction>();
            for (Builder builder : builders) {
                Parameters exportClauses = new Parameters(builder.getProperty(Constants.EXPORT_PACKAGE));
                Instructions exports = new Instructions(exportClauses);
                allExports.addAll(exports.keySet());
            }
            Central.setExportedPackageModel(project, allExports);

            Display display = PlatformUI.getWorkbench().getDisplay();
            SWTConcurrencyUtil.execForDisplay(display, true, new Runnable() {
                public void run() {
                    PlatformUI.getWorkbench().getDecoratorManager().update("bndtools.exportedPackageDecorator");
                }
            });

        } catch (Exception e) {
            logger.logWarning("Error persisting exported package model.", e);
        }

        return Status.OK_STATUS;
    }

}
