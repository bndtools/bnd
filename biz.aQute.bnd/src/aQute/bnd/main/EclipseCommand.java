package aQute.bnd.main;

import java.io.File;
import java.io.IOException;

import aQute.bnd.build.Project;
import aQute.bnd.build.Workspace;
import aQute.bnd.osgi.Processor;
import aQute.lib.getopt.Arguments;
import aQute.lib.getopt.Description;
import aQute.lib.getopt.Options;
import aQute.lib.io.IO;

public class EclipseCommand extends Processor {

	private final bnd bnd;

	public EclipseCommand(bnd bnd) {
		super(bnd);
		this.bnd = bnd;
	}

	@Arguments(arg = {})
	@Description("Synchronized the ./cnf/.settings directory to all the projects")
	interface SyncSettings extends Options {

	}

	@Description("Synchronized the ./cnf/.settings directory to all the projects")
	public void _sync(SyncSettings sync) {
		Workspace workspace = bnd.getWorkspace();
		if (workspace == null) {
			error("Need to be in workspace");
			return;
		}

		File sourceDir = workspace.getFile(Workspace.CNFDIR + "/.settings");
		if (!sourceDir.isDirectory()) {
			error("The Eclipse  .settings directory is not a directory: %s", sourceDir);
			return;
		}

		File[] toCopy = sourceDir.listFiles();

		for (Project p : workspace.getAllProjects()) {
			if (p.getName()
				.equals(Project.BNDCNF))
				continue;

			File targetDir = p.getFile(".settings");
			targetDir.mkdirs();

			if (!targetDir.isDirectory()) {
				error("Cannot create .settings directory in %s", p);
				continue;
			}

			for (File sourceFile : toCopy) {
				bnd.trace("Copying to %s to %s", sourceFile, targetDir);
				try {
					File targetFile = new File(targetDir, sourceFile.getName());
					IO.copy(sourceFile, targetFile);
				} catch (IOException e) {
					exception(e, "Failed to copy %s to %s because %s", sourceFile, targetDir, e.getMessage());
				}
			}
		}
	}
}
