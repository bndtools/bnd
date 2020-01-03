package bndtools.jareditor.internal;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectNature;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import aQute.bnd.service.result.Result;
import aQute.lib.strings.Strings;

/**
 * Create a temporary file system in one of the projects. The code attempts to
 * clean up the temporary files as much as possible
 */
public class TemporaryFile {
	final static String			ID		= ".bndtools.jareditor.temp";
	final static IWorkspaceRoot	root	= ResourcesPlugin.getWorkspace()
		.getRoot();
	final static AtomicLong		id		= new AtomicLong(10000000);

	static IFolder				tempFolder;
	static Path					tempDir;

	public static Result<IFolder, String> tempFolder(URI source, String path, IProgressMonitor monitor) {
		try {
			String[] parts = Strings.lastPathSegment(path);
			String name = parts == null ? path : parts[1];
			String idstring = Long.toString(id.incrementAndGet());

			return select().map((IFolder folder) -> {
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
			return Result.err(e.getMessage());
		}
	}

	private static Result<IFolder, String> select() throws CoreException, IOException {
		if (tempFolder == null) {
			IProject[] projects = root.getProjects();

			for (IProject p : projects) {
				Stream.of(p.members(IResource.HIDDEN))
					.filter(IResource::isHidden)
					.filter(r -> ID.equals(r.getName()))
					.forEach(r -> {
						delete(r);
					});
			}
		} else if (!tempFolder.isAccessible()) {
			delete(tempFolder);
		} else
			return Result.ok(tempFolder);

		if (tempDir == null)
			tempDir = Files.createTempDirectory("BndEditorTempFile");

		if (!tempDir.toFile()
			.isDirectory()) {
			Result.err("Cannot create temporary directory for temporary IResource folder");
		}

		IProject[] projects = root.getProjects();
		assert projects != null && projects.length > 0 : "there must always be at least one project";

		IProject victim = getVictim(projects);
		assert victim != null;

		IFolder folder = victim.getFolder(ID);
		if (folder.exists())
			delete(folder);

		folder.createLink(tempDir.toUri(), IResource.HIDDEN, null);

		return Result.ok(tempFolder = folder);
	}

	private static void delete(IResource r) {
		try {
			r.delete(true, null);
		} catch (CoreException e) {
			// ignore, best effort
		}
	}

	private static IProject getVictim(IProject[] projects) throws CoreException {
		assert projects != null && projects.length > 0 : "Require at least 1 project";

		IProject victim = projects[0];

		//
		// A hack. If you select a class file the JDT decompiler
		// wants a Java project. So we create the temp files in the
		// first java project we can see.
		//

		for (IProject p : projects) {
			IProjectNature nature = p.getNature("org.eclipse.jdt.core.javanature");
			if (nature != null) {
				victim = p;
			}
		}
		return victim;
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
