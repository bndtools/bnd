package org.bndtools.builder.classpath;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.launching.IRuntimeClasspathEntry;
import org.eclipse.jdt.launching.IRuntimeClasspathEntryResolver2;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.JavaRuntime;

public class BndContainerRuntimeClasspathEntryResolver implements IRuntimeClasspathEntryResolver2 {

	public BndContainerRuntimeClasspathEntryResolver() {}

	@Override
	public IRuntimeClasspathEntry[] resolveRuntimeClasspathEntry(IRuntimeClasspathEntry entry, IJavaProject project)
		throws CoreException {
		IClasspathContainer container = BndContainerInitializer.getClasspathContainer(project);
		if (container instanceof BndContainer) {
			BndContainer bndContainer = (BndContainer) container;
			return bndContainer.getRuntimeClasspathEntries();
		}
		return BndContainer.EMPTY_RUNTIMEENTRIES;
	}

	@Override
	public IVMInstall resolveVMInstall(IClasspathEntry entry) throws CoreException {
		return null;
	}

	@Override
	public boolean isVMInstallReference(IClasspathEntry entry) {
		return false;
	}

	@Override
	public IRuntimeClasspathEntry[] resolveRuntimeClasspathEntry(IRuntimeClasspathEntry entry,
		ILaunchConfiguration configuration) throws CoreException {
		IJavaProject project = entry.getJavaProject();
		if (project == null) {
			project = JavaRuntime.getJavaProject(configuration);
		}
		return resolveRuntimeClasspathEntry(entry, project);
	}
}
