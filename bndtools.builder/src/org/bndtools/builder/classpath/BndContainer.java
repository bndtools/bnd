package org.bndtools.builder.classpath;

import static java.util.Objects.requireNonNull;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.bndtools.api.BndtoolsConstants;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.launching.IRuntimeClasspathEntry;
import org.eclipse.jdt.launching.JavaRuntime;

public class BndContainer implements IClasspathContainer, Serializable {
	private static final long					serialVersionUID		= 2L;
	public static final String					DESCRIPTION				= "Bnd Bundle Path";
	private static final IClasspathEntry[]		EMPTY_ENTRIES			= new IClasspathEntry[0];
	static final IRuntimeClasspathEntry[]		EMPTY_RUNTIMEENTRIES	= new IRuntimeClasspathEntry[0];
	private final IClasspathEntry[]				entries;
	private final long							lastModified;
	private transient volatile List<IResource>	resources;

	private BndContainer(IClasspathEntry[] entries, long lastModified, List<IResource> resources) {
		this.entries = entries;
		this.lastModified = lastModified;
		this.resources = resources;
	}

	@Override
	public IClasspathEntry[] getClasspathEntries() {
		return entries;
	}

	@Override
	public String getDescription() {
		return DESCRIPTION;
	}

	@Override
	public int getKind() {
		return IClasspathContainer.K_APPLICATION;
	}

	@Override
	public IPath getPath() {
		return BndtoolsConstants.BND_CLASSPATH_ID;
	}

	@Override
	public String toString() {
		return getDescription();
	}

	long lastModified() {
		return lastModified;
	}

	void refresh() throws CoreException {
		List<IResource> files = resources;
		if (files == null) {
			return;
		}
		if (ResourcesPlugin.getWorkspace()
			.isTreeLocked()) {
			return;
		}
		for (IResource target : files) {
			int depth = target.getType() == IResource.FILE ? IResource.DEPTH_ZERO : IResource.DEPTH_INFINITE;
			if (!target.isSynchronized(depth)) {
				target.refreshLocal(depth, null);
			}
		}
		resources = null;
	}

	private static final IClasspathAttribute TEST = JavaCore.newClasspathAttribute("test", Boolean.TRUE.toString());

	IRuntimeClasspathEntry[] getRuntimeClasspathEntries() throws JavaModelException {
		List<IRuntimeClasspathEntry> runtime = new ArrayList<>();
		for (IClasspathEntry cpe : entries) {
			switch (cpe.getEntryKind()) {
				case IClasspathEntry.CPE_LIBRARY : {
					runtime
						.add(JavaRuntime.newArchiveRuntimeClasspathEntry(cpe.getPath(), cpe.getSourceAttachmentPath(),
							cpe.getSourceAttachmentRootPath(), cpe.getAccessRules(), cpe.getExtraAttributes(), false));
					break;
				}
				case IClasspathEntry.CPE_PROJECT : {
					IProject project = ResourcesPlugin.getWorkspace()
						.getRoot()
						.getProject(cpe.getPath()
							.segment(0));
					if (project.isOpen()) {
						IJavaProject javaProject = JavaCore.create(project);
						Set<IPath> seen = new HashSet<>();
						for (IClasspathEntry raw : javaProject.getRawClasspath()) {
							if ((raw.getEntryKind() == IClasspathEntry.CPE_SOURCE) && !hasAttribute(raw, TEST)) {
								IPath output = raw.getOutputLocation();
								if (output == null) {
									// use default output location
									output = javaProject.getOutputLocation();
								}
								if (seen.add(output)) {
									runtime.add(JavaRuntime.newArchiveRuntimeClasspathEntry(output, cpe.getPath(), null,
										cpe.getAccessRules(), cpe.getExtraAttributes(), false));
								}
							}
						}
					}
					break;
				}
				default :
					break;
			}
		}
		return runtime.toArray(EMPTY_RUNTIMEENTRIES);
	}

	private static boolean hasAttribute(IClasspathEntry cpe, IClasspathAttribute attr) {
		return Arrays.stream(cpe.getExtraAttributes())
			.anyMatch(attr::equals);
	}

	static class Builder {
		private final List<IClasspathEntry>	entries			= new ArrayList<>();
		private final List<IResource>		resources		= new ArrayList<>();
		private long						lastModified	= 0L;

		Builder() {}

		Builder updateLastModified(long time) {
			if (time > lastModified) {
				lastModified = time;
			}
			return this;
		}

		long lastModified() {
			return lastModified;
		}

		List<IClasspathEntry> entries() {
			return entries;
		}

		Builder entries(List<IClasspathEntry> entries) {
			requireNonNull(entries);
			if (entries != this.entries) {
				this.entries.clear();
				this.entries.addAll(entries);
			}
			return this;
		}

		Builder entry(IClasspathEntry entry) {
			entries.add(requireNonNull(entry));
			return this;
		}

		Builder entry(int i, IClasspathEntry entry) {
			entries.set(i, requireNonNull(entry));
			return this;
		}

		Builder refresh(IResource resource) {
			resources.add(requireNonNull(resource));
			return this;
		}

		BndContainer build() {
			return new BndContainer(entries.toArray(EMPTY_ENTRIES), lastModified,
				resources.isEmpty() ? null : new ArrayList<>(resources));
		}
	}
}
