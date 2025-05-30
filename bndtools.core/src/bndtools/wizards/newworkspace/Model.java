package bndtools.wizards.newworkspace;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.internal.ide.ChooseWorkspaceData;
import org.eclipse.ui.internal.ide.actions.OpenWorkspaceAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import aQute.bnd.build.Workspace;
import aQute.bnd.wstemplates.FragmentTemplateEngine;
import aQute.bnd.wstemplates.FragmentTemplateEngine.TemplateInfo;
import aQute.bnd.wstemplates.FragmentTemplateEngine.TemplateUpdater;
import aQute.lib.io.IO;
import bndtools.central.sync.WorkspaceSynchronizer;

public class Model implements Runnable {
	static final Logger			log					= LoggerFactory.getLogger(Model.class);
	static final IWorkspace		ECLIPSE_WORKSPACE	= ResourcesPlugin.getWorkspace();
	static final IWorkspaceRoot	ROOT				= ECLIPSE_WORKSPACE.getRoot();
	static final IPath			ROOT_LOCATION		= ROOT.getLocation();
	static final URI			TEMPLATE_HOME		= URI.create("https:://github.com/bndtools/bndtools.workspace.min");

	final static File			current				= ROOT_LOCATION.toFile();

	enum NewWorkspaceType {
		newbnd,
		derive,
		classic
	}

	File				location			= getUniqueWorkspaceName();
	boolean				clean				= false;
	boolean				updateWorkspace		= false;
	boolean				switchWorkspace		= true;
	List<TemplateInfo>	templates			= new ArrayList<>();
	List<TemplateInfo>	selectedTemplates	= new ArrayList<>();
	Progress			validatedUrl		= Progress.init;
	String				urlValidationError;
	String				error;
	String				valid;
	NewWorkspaceType	choice				= NewWorkspaceType.newbnd;
	private Workspace	workspace;

	enum Progress {
		init,
		start,
		finished,
		error
	}

	public Model(Workspace workspace) {
		this.workspace = workspace;
	}

	boolean isValid() {
		String valid;
		if (location.isFile()) {
			valid = "the location " + location + " is not a directory";
		} else if (location.equals(current) && !updateWorkspace) {
			valid = "selected the current workspace, select another directory";
		} else if (!clean && !updateWorkspace && !getDataFiles().isEmpty()) {
			valid = "the target location contains files, set delete files to delete them";
		} else {
			valid = null;
		}
		this.valid = valid;
		return valid != null;
	}

	List<File> getDataFiles() {
		if (!location.isDirectory())
			return Collections.emptyList();

		return Stream.of(location)
			.filter(f -> {
				if (f.getName()
					.equals(".metadata"))
					return false;

				return true;
			})
			.toList();
	}

	boolean execute(TemplateUpdater updater) {
		Display display = PlatformUI.getWorkbench()
			.getDisplay();

		Job job = Job.create("create workspace", mon -> {
			try {
				if (clean) {
					getDataFiles().forEach(IO::delete);
				}
				File b = IO.getFile(location, "cnf/build.bnd");
				if (!updateWorkspace || !b.isFile()) {
					location.mkdirs();
					b.getParentFile()
						.mkdirs();

					IO.store("", b);
				}
				updater.commit();

				if (updateWorkspace) {
					IResource workspaceRoot = ResourcesPlugin.getWorkspace()
						.getRoot();
					workspaceRoot.refreshLocal(IResource.DEPTH_INFINITE, null);
					syncWorkspace();

				} else if (switchWorkspace) {
					display.asyncExec(() -> {
						System.setProperty("osgi.instance.area", location.getAbsolutePath());
						System.setProperty("osgi.instance.area.default", location.getAbsolutePath());

						// show Eclipse Switch Workspace dialog with the new
						// workspace location pre-filled
						ChooseWorkspaceData launchData = new ChooseWorkspaceData(location.getAbsolutePath());
						launchData.workspaceSelected(location.getAbsolutePath());
						launchData.writePersistedData();

						IWorkbenchWindow window = PlatformUI.getWorkbench()
							.getActiveWorkbenchWindow();
						new OpenWorkspaceAction(window).run();

					});
				}
			} catch (Exception e) {
				log.error("creating new workspace {}", e, e);
			}
		});
		job.schedule();
		return true;
	}

	private void syncWorkspace() {
		WorkspaceSynchronizer s = new WorkspaceSynchronizer();

		Job syncjob = Job.create("Sync bnd workspace", monitor -> {
			s.synchronize(true, monitor, () -> {});
		});
		syncjob.setRule(ResourcesPlugin.getWorkspace()
			.getRoot());
		syncjob.schedule();
	}

	void updateWorkspace(boolean useEclipse) {
		if (useEclipse != updateWorkspace) {
			updateWorkspace = useEclipse;
			if (useEclipse) {
				if (workspace.isDefaultWorkspace()) {
					location = current;
				} else {
					location = workspace.getBase();
				}
			} else {
				location = getUniqueWorkspaceName();
			}
		}
	}

	static File getUniqueWorkspaceName() {
		return IO.unique(IO.getFile("~/workspace"), null);
	}

	public void location(File file) {
		location = file;
	}

	public void clean(boolean selection) {
		clean = selection;
	}

	void selectedTemplates(List<TemplateInfo> list) {
		selectedTemplates = list;
	}

	@Override
	public void run() {
		isValid();
	}

	void init(FragmentTemplateEngine templateFragments, Workspace workspace) {}

}
