package bndtools.refactor;

import java.io.InputStream;
import java.net.URI;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileInfo;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.resource.DeleteResourceChange;
import org.eclipse.ltk.core.refactoring.resource.ResourceChange;

public class CreateFileChange extends ResourceChange {

	private final IPath			path;
	private final InputStream	source;
	private final int			updateFlags;
	private final String		encoding;

	/**
	 * Construct a CreateFileChange object.
	 *
	 * @param path The path of the new file.
	 * @param source Provides the content of the new file.
	 * @param updateFlags Flags for creation (for possible values see
	 *            {@link IFile#create(InputStream, int, IProgressMonitor)}).
	 */
	public CreateFileChange(IPath path, InputStream source, int updateFlags, String encoding) {
		this.path = path;
		this.source = source;
		this.updateFlags = updateFlags;
		this.encoding = encoding;
	}

	@Override
	protected IResource getModifiedResource() {
		return ResourcesPlugin.getWorkspace()
			.getRoot()
			.getFile(path);
	}

	@Override
	public String getName() {
		return String.format("Create file %s", path.toString());
	}

	@Override
	public Change perform(IProgressMonitor monitor) throws CoreException {
		SubMonitor progress = SubMonitor.convert(monitor, encoding != null ? 2 : 1);

		IWorkspaceRoot root = ResourcesPlugin.getWorkspace()
			.getRoot();

		IFile file = root.getFile(path);
		file.create(source, updateFlags, progress.newChild(1, SubMonitor.SUPPRESS_NONE));

		if (encoding != null)
			file.setCharset(encoding, progress.newChild(1, SubMonitor.SUPPRESS_NONE));

		return new DeleteResourceChange(path, true);
	}

	@Override
	public RefactoringStatus isValid(IProgressMonitor pm) throws CoreException {
		RefactoringStatus result = new RefactoringStatus();
		IFile file = ResourcesPlugin.getWorkspace()
			.getRoot()
			.getFile(path);

		URI location = file.getLocationURI();
		if (location == null) {
			result.addFatalError(String.format("The location for file %s is unknown", path));
			return result;
		}

		IFileInfo jFile = EFS.getStore(location)
			.fetchInfo();
		if (jFile.exists()) {
			result.addFatalError(String.format("File %s already exists", path));
			return result;
		}
		return result;
	}

}
