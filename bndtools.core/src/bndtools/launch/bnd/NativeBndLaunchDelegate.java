package bndtools.launch.bnd;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.jdt.internal.launching.JavaRemoteApplicationLaunchConfigurationDelegate;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;

import aQute.bnd.build.Project;
import aQute.bnd.build.ProjectLauncher;
import aQute.bnd.build.RunSession;
import aQute.lib.strings.Strings;
import bndtools.launch.util.LaunchUtils;

/**
 * The link between the Eclipse launching subsystem and the bnd launcher. We bypass the standard Eclipse launching and
 * will always setup the launch as a bnd launch and attach the debugger if necessary. This has the advantage we can add
 * features in bnd and we are sure fidelity is maintained.
 */
public class NativeBndLaunchDelegate extends JavaRemoteApplicationLaunchConfigurationDelegate {

    /*
     * The Eclipse launch interface.
     */
    @Override
    public void launch(ILaunchConfiguration configuration, String mode, final ILaunch launch, IProgressMonitor m) throws CoreException {
        IProgressMonitor monitor = m == null ? new NullProgressMonitor() : m;

        final Project model = LaunchUtils.getBndProject(configuration);
        if (model == null) {
            abort("Cannot locate model", null, IJavaLaunchConfigurationConstants.ERR_UNSPECIFIED_PROJECT);
        } else {

            boolean debug = "debug".equals(mode);
            try {
                List<LaunchThread> lts = new ArrayList<LaunchThread>();
                ProjectLauncher projectLauncher = model.getProjectLauncher();
                try {

                    List< ? extends RunSession> sessions = projectLauncher.getRunSessions();

                    for (RunSession session : sessions)
                        try {
                            monitor.setTaskName("start session " + session.getLabel());
                            LaunchThread lt = new LaunchThread(projectLauncher, session, launch);
                            if (debug) {
                                monitor.setTaskName("start debug " + session.getLabel());
                                lt.doDebug(monitor);
                            }
                            launch.addProcess(lt);
                            lts.add(lt);

                        } catch (Exception e) {
                            projectLauncher.exception(e, "Starting session %s in project %s", session.getName(), model);
                        }

                } catch (Exception e) {
                    projectLauncher.exception(e, "starting processes");
                }

                if (projectLauncher.isOk()) {
                    for (LaunchThread lt : lts) {
                        lt.start();
                    }
                } else {
                    @SuppressWarnings("restriction")
                    String msg = Strings.join("\n", projectLauncher.getErrors());
                    abort(msg, null, IJavaLaunchConfigurationConstants.ERR_INTERNAL_ERROR);
                }
            } catch (Exception e) {
                abort("Cannot locate model", e, IJavaLaunchConfigurationConstants.ERR_UNSPECIFIED_PROJECT);
            }
        }
        monitor.done();
    }
}
