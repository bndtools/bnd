package name.neilbartlett.eclipse.bndtools.utils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;

import aQute.lib.osgi.Processor;

public class BndFileClasspathCalculator extends AbstractClasspathCalculator {
	
	private final String classpathStr;
	private final IWorkspaceRoot root;
	private final IPath bndFullPath;
	
	private final List<IPath> classpathLocations;
	
	public BndFileClasspathCalculator(String classpathStr, IWorkspaceRoot root, IPath bndFullPath) throws IOException, CoreException {
		this.classpathStr = classpathStr;
		this.root = root;
		this.bndFullPath = bndFullPath;
		this.classpathLocations = new ArrayList<IPath>();
		
		calculatePaths();
	}
	private void calculatePaths() throws IOException, CoreException {
		IPath parentPath = bndFullPath.removeLastSegments(1);
		
		Map<String, Map<String, String>> header = Processor.parseHeader(classpathStr, null);
		for (String pathStr : header.keySet()) {
			classpathLocations.add(parentPath.append(pathStr));
		}
	}
	public List<IPath> classpathAsPaths() {
		return Collections.unmodifiableList(classpathLocations);
	}
	public List<File> classpathAsFiles() {
		return pathsToFiles(root, classpathLocations);
	}
	public List<IPath> sourcepathAsPaths() {
		return Collections.emptyList();
	}
	public List<File> sourcepathAsFiles() {
		return Collections.emptyList();
	}
}
