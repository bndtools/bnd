/*******************************************************************************
 * Copyright (c) 2010 Per Kr. Soreide.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Per Kr. Soreide - initial API and implementation
 *******************************************************************************/
package bndtools.release.api;

import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.team.core.history.IFileHistory;
import org.eclipse.team.core.history.IFileHistoryProvider;
import org.eclipse.team.core.history.IFileRevision;
import org.eclipse.team.core.subscribers.Subscriber;
import org.eclipse.team.core.synchronize.SyncInfoSet;

import aQute.bnd.build.Project;
import aQute.bnd.differ.Baseline;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Domain;
import aQute.bnd.osgi.Jar;
import aQute.bnd.service.RepositoryPlugin;
import aQute.bnd.service.diff.Delta;
import aQute.bnd.service.diff.Diff;
import aQute.bnd.service.diff.Diff.Ignore;
import aQute.bnd.service.diff.Type;
import aQute.bnd.version.Version;

public class ReleaseUtils {

	/**
	 * @param jar the jar
	 * @return the Jar name as it appears in the Repository e.g. bndtools.release-1.0.0.jar
	 */
	public static String getJarFileName(Jar jar) {
		try {
		    Domain domain = Domain.domain(jar.getManifest());
			return domain.getBundleSymbolicName().getKey() + '-' + stripVersionQualifier(domain.getBundleVersion()) + ".jar";
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Strips the qualifier part of a version string.
	 * @param version
	 * @return Version string without qualifier
	 */
	public static String stripVersionQualifier(String version) {
		Version ver = new Version(version);
		StringBuilder sb = new StringBuilder();
		sb.append(ver.getMajor());
		sb.append('.');
		sb.append(ver.getMinor());
		sb.append('.');
		sb.append(ver.getMicro());
		return sb.toString();
	}

	public static String getBundleSymbolicName(Jar jar) {
		try {
		    Domain domain = Domain.domain(jar.getManifest());
		    return domain.getBundleSymbolicName().getKey();
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	public static String getBundleVersion(Jar jar) {
		try {
            Domain domain = Domain.domain(jar.getManifest());
            return domain.getBundleVersion();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public static IFolder getLocalRepoLocation(RepositoryPlugin repository) {
		try {
			Method m = repository.getClass().getMethod("getRoot");
			if (m.getReturnType() == File.class) {
				return (IFolder) toWorkspaceResource((File)m.invoke(repository));
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
 		return null;
	}

	public static IResource toWorkspaceResource(File workspaceFile) {
		IWorkspaceRoot wsRoot = ResourcesPlugin.getWorkspace().getRoot();
		IPath workingCopyPath = Path.fromOSString(workspaceFile.getAbsolutePath());
		IResource resource = wsRoot.getContainerForLocation(workingCopyPath);
		if (resource == null) {
			resource = wsRoot.getFileForLocation(workingCopyPath);
		}
		return resource;
	}

	public static IResource toResource(File file) {
		IWorkspaceRoot wsRoot = ResourcesPlugin.getWorkspace().getRoot();
		IPath workingCopyPath = Path.fromOSString(file.getAbsolutePath());
		IFile resource = wsRoot.getFileForLocation(workingCopyPath);
		return resource;
	}

	public static IProject getProject(Project project) {
		IResource res = toWorkspaceResource(project.getBase());
		if (res == null) {
			return null;
		}
		return res.getProject();
	}

	public static IFile getJarFileLocation(RepositoryPlugin repository, Jar jar) {
		IFolder repoRoot = ReleaseUtils.getLocalRepoLocation(repository);
		String symbName = ReleaseUtils.getBundleSymbolicName(jar);
		String jarName = ReleaseUtils.getJarFileName(jar);

		IFolder repoSymbNameFolder = repoRoot.getFolder(symbName);
		return repoSymbNameFolder.getFile(jarName);
	}

	/**
	 * If the bundle repository is shared e.g. CVS, this will return the remote file revision if it exists
	 * @param repository
	 * @param jar
	 * @return The file revision or <code>null</code> if the bundle repository is not shared or the Jar does not exist in the remote repository
	 */
	public static IFileRevision getTeamRevision(RepositoryPlugin repository, Jar jar) {
		IFolder repoRoot = ReleaseUtils.getLocalRepoLocation(repository);
		IProject repoProject = repoRoot.getProject();

		RepositoryProvider repoProvider = RepositoryProvider.getProvider(repoProject);
		if (repoProvider == null) {
			return null;
		}

		IFile path = getJarFileLocation(repository, jar);

		IFileRevision[] revs = getTeamRevisions(path, IFileHistoryProvider.SINGLE_REVISION, new NullProgressMonitor());
		if (revs == null) {
			return null;
		}
		return revs.length == 0 ? null : revs[0];

	}

    /**
     * Returns the file revisions for the given resource. If the flags contains IFileHistoryProvider.SINGLE_REVISION
     * then only the revision corresponding to the base corresponding to the local resource is fetched. If the flags
     * contains IFileHistoryProvider.SINGLE_LINE_OF_DESCENT the resulting history will be restricted to a single
     * line-of-descent (e.g. a single branch).
     * 
     * @param resource
     * @param flags
     * @param monitor
     * @return the file revisions for the given resource
     */
	public static IFileRevision[] getTeamRevisions(IResource resource, int flags, IProgressMonitor monitor) {

		RepositoryProvider provider = RepositoryProvider.getProvider(resource.getProject());
		if (provider == null) {
			return null;
		}

		IFileHistory history = provider.getFileHistoryProvider().getFileHistoryFor(resource, flags, monitor);
		if (history == null) {
			return new IFileRevision[0];
		}
		return history.getFileRevisions();
	}

    /**
     * Checks if everything is in sync except *.bnd & packageinfo files (which could be updated during the release)
     * 
     * @param project
     * @param monitor
     * @return true when everything is in sync
     * @throws CoreException
     */
	public static boolean isTeamProjectUpToDate(IProject project, IProgressMonitor monitor) throws CoreException {
		return getTeamOutOfSyncResources(project, monitor).length == 0;
	}

	public static IResource[] getTeamOutOfSyncResources(IProject project, IProgressMonitor monitor) throws CoreException {
		RepositoryProvider provider = RepositoryProvider.getProvider(project);
		if (provider == null) {
			return new IResource[0];
		}
		Subscriber subscriber = provider.getSubscriber();
		subscriber.refresh(new IResource[] {project}, IResource.DEPTH_INFINITE, monitor);

		SyncInfoSet sis = new SyncInfoSet();
		subscriber.collectOutOfSync(new IResource[] {project}, IResource.DEPTH_INFINITE, sis, monitor);
		List<IResource> res = new ArrayList<IResource>();
		for (IResource resource : sis.getResources()) {
			if (resource instanceof IFile) {
				IFile file = (IFile) resource;
				if (file.getName().endsWith(".bnd") || file.getName().equals("packageinfo")) {
					continue;
				}
			}
			res.add(resource);
		}
		return res.toArray(new IResource[res.size()]);
	}

	public static String stripInstructions(String header) {
		if (header == null) {
			return null;
		}
		int idx = header.indexOf(';');
		if (idx > -1) {
			return header.substring(0, idx);
		}
		return header;
	}

	public static boolean needsRelease(Baseline baseline) {
        Delta delta = baseline.getDiff().getDelta(new Ignore() {
            public boolean contains(Diff diff) {
               if ("META-INF/MANIFEST.MF".equals(diff.getName())) { //$NON-NLS-1$
                   return true;
               }
               if (diff.getType() == Type.HEADER && diff.getName().startsWith(Constants.BUNDLE_VERSION)) {
                   return true;
               }
               return false;
            }});
        if (delta != Delta.UNCHANGED && delta != Delta.IGNORED) {
            return true;
        }
        return false;
	}
}
