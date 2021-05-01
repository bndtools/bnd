package bndtools.jareditor.internal;

import java.io.IOException;
import java.net.URI;
import java.util.concurrent.atomic.AtomicLong;

import org.bndtools.api.BndtoolsConstants;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import aQute.bnd.result.Result;
import aQute.bnd.exceptions.Exceptions;
import aQute.lib.strings.Strings;

/**
 * Create a temporary file system in one of the projects. The code attempts to
 * clean up the temporary files as much as possible
 */
public class TemporaryFile {
	private final static AtomicLong			id			= new AtomicLong(10000000);

	private final static TemporaryProject	tempProject	= new TemporaryProject();

	public static Result<IFolder, String> tempFolder(URI source, String path, IProgressMonitor monitor) {
		try {
			String[] parts = Strings.lastPathSegment(path);
			String name = parts == null ? path : parts[1];
			String idstring = Long.toString(id.incrementAndGet());

			return selectTempProject().map((IFolder folder) -> {
				//
				// Create a folder/file.ext. Many Eclipse and java routines
				// create for example a window name out of the path or use
				// the extension. So we want to keep the name equal.
				//

				IFolder unique = folder.getFolder(idstring);
				if (unique.exists())
					unique.delete(true, null);

				unique.create(true, true, monitor);

				IFolder actualFolder = unique.getFolder(name);
				if (actualFolder.exists())
					actualFolder.delete(true, monitor);

				actualFolder.createLink(source, IResource.REPLACE, monitor);
				return actualFolder;
			});
		} catch (Exception e) {
			return Result.err("Error creating temp folder for JAREditor %s", Exceptions.toString(e));
		}
	}

	private static Result<IFolder, String> selectTempProject() throws CoreException, IOException {
		IProject project = tempProject.getProject();
		if (project == null) {
			return Result.err("Unable to get temp project %s", BndtoolsConstants.BNDTOOLS_JAREDITOR_TEMP_PROJECT_NAME);
		}
		return Result.ok(project.getFolder("temp"));
	}

	public static void dispose(IFolder model) {
		if (model == null)
			return;

		try {
			model.getParent()
				.delete(true, null);
		} catch (CoreException e) {
			// best effort
		}
	}
}
