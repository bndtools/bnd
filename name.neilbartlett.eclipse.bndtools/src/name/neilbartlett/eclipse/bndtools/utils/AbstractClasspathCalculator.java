package name.neilbartlett.eclipse.bndtools.utils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.runtime.IPath;

public abstract class AbstractClasspathCalculator implements IClasspathCalculator {
	protected static List<File> pathsToFiles(IWorkspaceRoot root, List<IPath> paths) {
		List<File> result = new ArrayList<File>(paths.size());
		for (IPath path : paths) {
			IResource resource = root.findMember(path);
			if(resource != null)
				result.add(resource.getLocation().toFile());
		}
		return result;
	}
}
