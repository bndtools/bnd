package name.neilbartlett.eclipse.bndtools.utils;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.runtime.IPath;

public interface IClasspathCalculator {

	public List<IPath> classpathAsWorkspacePaths();

	public List<File> classpathAsFiles();

	public List<IPath> sourcepathAsWorkspacePaths();

	public List<File> sourcepathAsFiles();

}
