package name.neilbartlett.eclipse.bndtools.utils;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaModelException;

public class ClasspathCalculator {
	
	private final IJavaProject javaProject;
	
	private final List<IPath> classpathLocations;
	private final List<IPath> sourceLocations;

	public ClasspathCalculator(IJavaProject javaProject) {
		this.javaProject = javaProject;
		
		this.classpathLocations = new ArrayList<IPath>();
		this.sourceLocations = new ArrayList<IPath>(3);
		
		calculateClasspaths();
	}
	private void calculateClasspaths() {
		try {
			classpathLocations.add(javaProject.getOutputLocation());
			IClasspathEntry[] classpathEntries = javaProject.getRawClasspath();
			for (IClasspathEntry entry : classpathEntries) {
				switch (entry.getEntryKind()) {
				case IClasspathEntry.CPE_SOURCE:
					sourceLocations.add(entry.getPath());
					IPath outputLocation = entry.getOutputLocation();
					if(outputLocation != null)
						classpathLocations.add(outputLocation);
					break;
				case IClasspathEntry.CPE_LIBRARY:
					classpathLocations.add(entry.getPath());
					break;
				default:
					break;
				}
			}
		} catch (JavaModelException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	public List<IPath> classpathAsPaths() {
		return Collections.unmodifiableList(classpathLocations);
	}
	public List<File> classpathAsFiles() {
		IWorkspaceRoot root = javaProject.getProject().getWorkspace().getRoot();
		
		List<File> classpath = new ArrayList<File>(classpathLocations.size());
		for (IPath path : classpathLocations) {
			IResource resource = root.findMember(path);
			if(resource != null)
				classpath.add(resource.getLocation().toFile());
		}
		
		return classpath;
	}
	public List<IPath> sourcepathAsPaths() {
		return Collections.unmodifiableList(sourceLocations);
	}
	public List<File> sourcepathAsFiles() {
		IWorkspaceRoot root = javaProject.getProject().getWorkspace().getRoot();
		
		List<File> sourcepath = new ArrayList<File>(sourceLocations.size());
		for (IPath path : sourceLocations) {
			IResource resource = root.findMember(path);
			if(resource != null)
				sourcepath.add(resource.getLocation().toFile());
		}
		
		return sourcepath;
	}
}
