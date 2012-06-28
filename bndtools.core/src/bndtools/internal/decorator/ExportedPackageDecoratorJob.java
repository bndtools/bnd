package bndtools.internal.decorator;

import java.text.MessageFormat;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import org.osgi.framework.Version;

import aQute.bnd.build.Project;
import aQute.bnd.build.Workspace;
import aQute.lib.osgi.Builder;
import aQute.lib.osgi.Constants;
import aQute.lib.osgi.Descriptors.PackageRef;
import aQute.lib.osgi.Jar;
import aQute.lib.osgi.Packages;
import aQute.lib.osgi.Processor;
import aQute.libg.header.Attrs;
import bndtools.Central;
import bndtools.Plugin;
import bndtools.api.ILogger;
import bndtools.utils.SWTConcurrencyUtil;

public class ExportedPackageDecoratorJob extends Job implements ISchedulingRule {

    private static final ConcurrentMap<String,ExportedPackageDecoratorJob> instances = new ConcurrentHashMap<String,ExportedPackageDecoratorJob>();

    private final IProject project;
    private final ILogger logger;

    public static void scheduleForProject(IProject project, ILogger logger) {
        ExportedPackageDecoratorJob job = new ExportedPackageDecoratorJob(project, logger);

        if (instances.putIfAbsent(project.getFullPath().toPortableString(), job) == null) {
            job.schedule(1000);
        }
    }

    ExportedPackageDecoratorJob(IProject project, ILogger logger) {
        super("Update exported packages: " + project.getName());

        this.project = project;
        this.logger = logger;

        setRule(this);
    }

    @Override
    protected IStatus run(IProgressMonitor monitor) {
        instances.remove(project.getFullPath().toPortableString());

        try {
            Project model = Workspace.getProject(project.getLocation().toFile());
            Collection< ? extends Builder> builders = model.getSubBuilders();

            Map<String,SortedSet<Version>> allExports = new HashMap<String,SortedSet<Version>>();

            for (Builder builder : builders) {
                Jar jar = null;
                try {
                    builder.build();
                    Packages exports = builder.getExports();
                    if (exports != null) {
                        for (Entry<PackageRef,Attrs> export : exports.entrySet()) {
                            Version version;
                            String versionStr = export.getValue().get(Constants.VERSION_ATTRIBUTE);
                            try {
                                version = Version.parseVersion(versionStr);
                                String pkgName = Processor.removeDuplicateMarker(export.getKey().getFQN());
                                SortedSet<Version> versions = allExports.get(pkgName);
                                if (versions == null) {
                                    versions = new TreeSet<Version>();
                                    allExports.put(pkgName, versions);
                                }
                                versions.add(version);
                            } catch (IllegalArgumentException e) {
                                // Seems to be an invalid export, ignore it...
                            }
                        }
                    }
                } catch (Exception e) {
                    Plugin.getDefault().getLogger().logWarning(MessageFormat.format("Unable to process exported packages for builder of {0}.", builder.getPropertiesFile()), e);
                } finally {
                    if (jar != null)
                        jar.close();
                }
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

    public boolean contains(ISchedulingRule rule) {
        return this == rule;
    }

    public boolean isConflicting(ISchedulingRule rule) {
        return rule instanceof ExportedPackageDecoratorJob;
    }

}
