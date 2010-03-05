/*******************************************************************************
 * Copyright (c) 2010 Neil Bartlett.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Neil Bartlett - initial API and implementation
 *******************************************************************************/
package name.neilbartlett.eclipse.bndtools.builder;

import java.io.*;
import java.util.*;
import java.util.jar.*;

import name.neilbartlett.eclipse.bndtools.*;
import name.neilbartlett.eclipse.bndtools.Plugin;
import name.neilbartlett.eclipse.bndtools.classpath.*;
import name.neilbartlett.eclipse.bndtools.utils.*;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.osgi.framework.*;

import aQute.bnd.build.*;
import aQute.bnd.plugin.*;
import aQute.lib.osgi.Constants;
import aQute.libg.version.Version;

public class BndIncrementalBuilder extends IncrementalProjectBuilder {

	public static final String		BUILDER_ID						= Plugin.PLUGIN_ID
																			+ ".bndbuilder";
	public static final String		MARKER_BND_PROBLEM				= Plugin.PLUGIN_ID
																			+ ".bndproblem";
	public static final String		MARKER_BND_CLASSPATH_PROBLEM	= Plugin.PLUGIN_ID
																			+ ".bnd_classpath_problem";

	private static final String		BND_SUFFIX						= ".bnd";
	private static final String		CLASS_SUFFIX					= ".class";
	private static final String		JAR_SUFFIX						= ".jar";

	private IClasspathCalculator	projectClasspathCalculator;

	static {
		Activator act = new Activator();
		BundleContext context = Plugin.getDefault().getBundleContext();
		try {
			act.start( context);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	@Override protected IProject[] build(int kind,
			@SuppressWarnings("rawtypes") Map args, IProgressMonitor monitor)
			throws CoreException {
		IProject project = getProject();
		BndProject bndProject = BndProject.create(project);

		if (bndProject.getLastBuildTime() == -1 || kind == FULL_BUILD) {
			rebuildBndFile(bndProject, monitor);
		} else {
			// IResourceDelta delta = getDelta(project);
			// if(delta == null)
			rebuildBndFile(bndProject, monitor);
			// else
			// rebuildBndFile(bndProject, monitor);
		}
		bndProject.markBuilt();
		return new IProject[]{ project.getWorkspace().getRoot().getProject(Project.BNDCNF)};
	}

	@Override protected void clean(IProgressMonitor monitor)
			throws CoreException {
		// Clear markers
		getProject().deleteMarkers(MARKER_BND_PROBLEM, true,
				IResource.DEPTH_INFINITE);

		// Delete target files
		BndProject bndProject = BndProject.create(getProject());
		try {
			bndProject.getModel().clean();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		bndProject.clearAll();
	}

	/**
	 * Get the specified file as an exported bundle, IFF it is a bundle,
	 * otherwise returns {@code null}.
	 * 
	 * @param jarFile
	 *            The JAR file which may be a bundle.
	 * @return An {@link ExportedBundle} object representing the bundle, or
	 *         {@code null} if the specified file is not a bundle.
	 */
	private ExportedBundle getBundleFromJar(IFile jarFile) {
		JarInputStream jarInStream = null;
		try {
			jarInStream = new JarInputStream(jarFile.getContents());
			Manifest manifest = jarInStream.getManifest();
			if (manifest != null) {
				Attributes attributes = manifest.getMainAttributes();

				// Check this is an R4 bundle
				String manifestVersion = (String) attributes
						.getValue(Constants.BUNDLE_MANIFESTVERSION);
				if (manifestVersion == null
						|| !"2".equals(manifestVersion.trim())) {
					return null;
				}

				// Check for symbolic name and trim off any attributes (e.g.
				// "singleton")
				String symbolicName = (String) attributes
						.getValue(Constants.BUNDLE_SYMBOLICNAME);
				if (symbolicName == null) {
					return null;
				} else {
					symbolicName = symbolicName.trim();
					int semicolon = symbolicName.indexOf(';');
					if (semicolon != -1) {
						symbolicName = symbolicName.substring(0, semicolon);
					}
				}

				// Parse the version
				String versionStr = (String) attributes
						.getValue(Constants.BUNDLE_VERSION);
				Version version = new Version(0, 0, 0);
				try {
					if (versionStr != null) {
						version = new Version(versionStr);
					}
				} catch (IllegalArgumentException e) {
					// Ignore
				}

				return new ExportedBundle(jarFile.getFullPath(), null,
						symbolicName, version);
			}
		} catch (IOException e) {
			Plugin.getDefault().getLog().log(
					new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0,
							"Error while checking JAR for bundle-ness.", e));
		} catch (CoreException e) {
			Plugin.getDefault().getLog().log(
					new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0,
							"Error while checking JAR for bundle-ness.", e));
		} finally {
			if (jarInStream != null) {
				try {
					jarInStream.close();
				} catch (IOException e) {
					// Ignore
				}
			}
		}
		return null;
	}

	class ClassFileVisitor implements IResourceDeltaVisitor {

		private final BndProject				bndProject;
		private final Collection<? super IFile>	changedBndFiles;

		ClassFileVisitor(BndProject bndProject,
				Collection<? super IFile> changedBndFiles) {
			this.bndProject = bndProject;
			this.changedBndFiles = changedBndFiles;
		}

		public boolean visit(IResourceDelta delta) throws CoreException {
			// Check for classes that may be included in one or more bnd files
			if (delta.getResource().getType() == IResource.FILE
					&& delta.getResource().getName().endsWith(CLASS_SUFFIX)) {
				checkClassFile(bndProject, (IFile) delta.getResource());
			}
			return false;
		}

		void checkClassFile(BndProject bndProject, IFile classFile) {
			// Find the package name for this classfile, if it is in the project
			// classpath
			String packageNameInProject = null;
			for (IPath classFolder : projectClasspathCalculator
					.classpathAsWorkspacePaths()) {
				packageNameInProject = packageNameForClassFilePath(classFolder,
						classFile.getFullPath());
				if (packageNameInProject != null)
					break;
			}

			// Which bundles are affected?
			Collection<BndFileModel> fileModels = bndProject.getAllFileModels();
			for (BndFileModel model : fileModels) {
				String packageName = null;
				if (model.getClasspath() == null) {
					packageName = packageNameInProject;
				} else {
					for (IPath classFolder : model.getClasspath()) {
						packageName = packageNameForClassFilePath(classFolder,
								classFile.getFullPath());
						if (packageName != null)
							break;
					}
				}
				if (packageName != null && model.containsPackage(packageName)) {
					changedBndFiles.add(getProject().getWorkspace().getRoot()
							.getFile(model.getPath()));
				}
			}
		}

		String packageNameForClassFilePath(IPath folderPath, IPath classFilePath) {
			if (folderPath.isPrefixOf(classFilePath)) {
				IPath relativePath = PathUtils.makeRelativeTo(classFilePath,
						folderPath); // classFilePath.makeRelativeTo(folderPath);
				IPath packagePath = relativePath.removeLastSegments(1);
				String packageName = packagePath.toString().replace('/', '.');
				return packageName;
			}
			return null;
		}
	}

	class BundleJarVisitor implements IResourceProxyVisitor,
			IResourceDeltaVisitor {

		private final Collection<? super IFile>				deletedJarFiles;
		private final Collection<? super ExportedBundle>	bundles;
		private final Collection<? extends IResource>		exportDirs;

		BundleJarVisitor(Collection<? super ExportedBundle> bundles,
				Collection<? extends IResource> exportDirs) {
			this(new LinkedList<IFile>(), bundles, exportDirs);
		}

		BundleJarVisitor(Collection<? super IFile> deletedJarFiles,
				Collection<? super ExportedBundle> bundles,
				Collection<? extends IResource> exportDirs) {
			this.deletedJarFiles = deletedJarFiles;
			this.bundles = bundles;
			this.exportDirs = exportDirs;
		}

		public boolean visit(IResourceProxy proxy) throws CoreException {
			// Check for JAR files...
			if (proxy.getType() == IResource.FILE
					&& proxy.getName().toLowerCase().endsWith(JAR_SUFFIX)) {
				IFile file = (IFile) proxy.requestResource();

				// Now check it's in the exported bundles dir
				boolean exported = false;
				for (IResource exportDir : exportDirs) {
					if (exportDir.getProjectRelativePath().isPrefixOf(
							file.getProjectRelativePath())) {
						exported = true;
						break;
					}
				}

				// Crack it open and see if it's really a bundle
				if (exported) {
					ExportedBundle bundle = getBundleFromJar(file);
					if (bundle != null) {
						bundles.add(bundle);
					}
				}
			}
			return false;
		}

		public boolean visit(IResourceDelta delta) throws CoreException {
			// Check for bundle JARs
			IResource resource = delta.getResource();
			if (resource.getType() == IResource.FILE
					&& resource.getName().endsWith(JAR_SUFFIX)) {
				IFile file = (IFile) resource;
				if (delta.getKind() == IResourceDelta.REMOVED) {
					deletedJarFiles.add(file);
				} else if (delta.getKind() == IResourceDelta.ADDED
						|| delta.getKind() == IResourceDelta.CHANGED) {
					ExportedBundle bundle = getBundleFromJar(file);
					if (bundle != null) {
						bundles.add(bundle);
					}
				}
			}
			return false;
		}
	}

	class AllFoldersVisitor implements IResourceProxyVisitor,
			IResourceDeltaVisitor {
		public boolean visit(IResourceProxy proxy) throws CoreException {
			return proxy.getType() == IResource.PROJECT
					|| proxy.getType() == IResource.FOLDER;
		}

		public boolean visit(IResourceDelta delta) throws CoreException {
			return delta.getResource().getType() == IResource.PROJECT
					|| delta.getResource().getType() == IResource.FOLDER;
		}
	}

	class BndFileVisitor implements IResourceProxyVisitor,
			IResourceDeltaVisitor {
		private final Collection<? super IFile>	deletedBndFiles;
		private final Collection<? super IFile>	changedBndFiles;

		BndFileVisitor(Collection<? super IFile> changedBndFiles) {
			this(new LinkedList<IFile>(), changedBndFiles);
		}

		BndFileVisitor(Collection<? super IFile> deletedBndFiles,
				Collection<? super IFile> changedBndFiles) {
			this.deletedBndFiles = deletedBndFiles;
			this.changedBndFiles = changedBndFiles;
		}

		public boolean visit(IResourceProxy proxy) throws CoreException {
			// Check for Bnd files
			if (proxy.getType() == IResource.FILE
					&& proxy.getName().toLowerCase().endsWith(BND_SUFFIX)) {
				changedBndFiles.add((IFile) proxy.requestResource());
			}
			return false;
		}

		public boolean visit(IResourceDelta delta) throws CoreException {
			if (delta.getResource() instanceof IFile) {
				IFile file = (IFile) delta.getResource();
				// Check for Bnd files
				if (file.getName().endsWith(BND_SUFFIX)) {
					if (delta.getKind() == IResourceDelta.ADDED
							|| delta.getKind() == IResourceDelta.CHANGED) {
						changedBndFiles.add(file);
					} else if (delta.getKind() == IResourceDelta.REMOVED) {
						deletedBndFiles.add(file);
					}
				}
			}
			return false;
		}
	}

	private void rebuildBndFile(BndProject bndProject, IProgressMonitor monitor)
			throws CoreException {
		SubMonitor progress = SubMonitor.convert(monitor, 2);
		Project model = bndProject.getModel();
		model.refresh();

		// Get or create the build model for this bnd file
		IFile bndFile = bndProject.getProject().getFile(Project.BNDFILE);

		// Clear markers
		if (bndFile.exists()) {
			bndFile.deleteMarkers(MARKER_BND_PROBLEM, true,
					IResource.DEPTH_INFINITE);
		}

		try {
			File files[] = model.build();
			if (files != null)
				for (File f : files) {
					Central.refresh(Central.toPath(model, f));
				}
			progress.worked(1);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// Report errors
		for (String errorMessage : model.getErrors()) {
			IMarker marker = bndFile.createMarker(MARKER_BND_PROBLEM);
			marker.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_ERROR);
			marker.setAttribute(IMarker.MESSAGE, errorMessage);
			marker.setAttribute(IMarker.LINE_NUMBER, 1);
			model.clear();
		}
	}

}
