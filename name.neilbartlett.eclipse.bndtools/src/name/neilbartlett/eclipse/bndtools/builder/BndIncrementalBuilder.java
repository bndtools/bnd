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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;

import name.neilbartlett.eclipse.bndtools.BndProject;
import name.neilbartlett.eclipse.bndtools.Plugin;
import name.neilbartlett.eclipse.bndtools.classpath.ExportedBundle;
import name.neilbartlett.eclipse.bndtools.classpath.WorkspaceRepositoryClasspathContainerInitializer;
import name.neilbartlett.eclipse.bndtools.project.BndProjectProperties;
import name.neilbartlett.eclipse.bndtools.utils.BndFileClasspathCalculator;
import name.neilbartlett.eclipse.bndtools.utils.IClasspathCalculator;
import name.neilbartlett.eclipse.bndtools.utils.PathUtils;
import name.neilbartlett.eclipse.bndtools.utils.ProjectClasspathCalculator;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IResourceProxy;
import org.eclipse.core.resources.IResourceProxyVisitor;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jdt.core.JavaCore;

import aQute.lib.osgi.Builder;
import aQute.lib.osgi.Constants;
import aQute.lib.osgi.Instruction;
import aQute.lib.osgi.Jar;
import aQute.libg.version.Version;

public class BndIncrementalBuilder extends IncrementalProjectBuilder {
	
	public static final String BUILDER_ID = Plugin.PLUGIN_ID + ".bndbuilder";
	public static final String MARKER_BND_PROBLEM = Plugin.PLUGIN_ID + ".bndproblem";
	public static final String MARKER_BND_CLASSPATH_PROBLEM = Plugin.PLUGIN_ID + ".bnd_classpath_problem";

	private static final String BND_SUFFIX = ".bnd";
	private static final String CLASS_SUFFIX = ".class";
	private static final String JAR_SUFFIX = ".jar";

	private IClasspathCalculator projectClasspathCalculator;

	@Override
	protected IProject[] build(int kind, @SuppressWarnings("rawtypes") Map args, IProgressMonitor monitor) throws CoreException {
		IProject project = getProject();
		BndProject bndProject = BndProject.create(project);
		
		projectClasspathCalculator = new ProjectClasspathCalculator(JavaCore.create(project));
		
		if(bndProject.getLastBuildTime() == -1 || kind == FULL_BUILD) {
			fullBuild(bndProject, monitor);
		} else {
			IResourceDelta delta = getDelta(project);
			if(delta == null)
				fullBuild(bndProject, monitor);
			else
				incrementalBuild(bndProject, delta, monitor);
		}
		bndProject.markBuilt();
		return null;
	}
	protected void fullBuild(BndProject bndProject, IProgressMonitor monitor) throws CoreException {
		SubMonitor progress = SubMonitor.convert(monitor);

		// Check the export dirs
		BndProjectProperties projProps = new BndProjectProperties(bndProject.getProject());
		List<IResource> exportDirs;
		try {
			projProps.load();
			exportDirs = projProps.getExportedBundleDirs();
		} catch (IOException e) {
			throw new CoreException(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Error loading Bnd project properties.", e));
		}

		// Search for files to process
		List<IFile> bndFiles = new LinkedList<IFile>();
		List<ExportedBundle> bundleFiles = new LinkedList<ExportedBundle>();
		
		CompositeResourceProxyVisitor resourceVisitor = new CompositeResourceProxyVisitor();
		resourceVisitor.addDelegate(new AllFoldersVisitor());
		resourceVisitor.addDelegate(new BndFileVisitor(bndFiles));
		resourceVisitor.addDelegate(new BundleJarVisitor(bundleFiles, exportDirs));
		getProject().accept(resourceVisitor, 0);
		
		// Build the Bnd files
		progress.setWorkRemaining(bndFiles.size());
		for (IFile file : bndFiles) {
			rebuildBndFile(bndProject, file, null, bundleFiles, progress.newChild(1));
		}
		
		// Notify the workspace repository that some exports may have changed.
		WorkspaceRepositoryClasspathContainerInitializer.getInstance().resetProjectExports(bndProject.getProject(), bundleFiles, progress.newChild(1));
	}
	protected void incrementalBuild(BndProject bndProject, IResourceDelta delta, IProgressMonitor monitor) throws CoreException {
		SubMonitor progress = SubMonitor.convert(monitor);
		
		// Set up some buckets for the changes we are about to detect
		List<IFile> deletedBndFiles = new LinkedList<IFile>();
		List<IFile> changedBndFiles = new LinkedList<IFile>();
		List<ExportedBundle> changedBundles = new LinkedList<ExportedBundle>();
		List<IFile> deletedJarFiles = new LinkedList<IFile>();
		
		// Does the delta contain the bnd.properties file, meaning we will have to reprocess the exported bundles entirely?
		IResourceDelta bndPropertiesDelta = delta.findMember(new Path(BndProjectProperties.BND_PROPERTIES_FILE));
		
		// Get the export dirs from the cached project metadata, if not changed, otherwise read it again from the file
		List<IResource> exportDirs;
		if(bndPropertiesDelta == null) {
			exportDirs = bndProject.getExportDirs();
		} else {
			try {
				BndProjectProperties projProps = new BndProjectProperties(bndProject.getProject());
				projProps.load();
				exportDirs = projProps.getExportedBundleDirs();
				bndProject.setExportDirs(exportDirs);
			} catch (IOException e) {
				throw new CoreException(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Error loading Bnd project properties.", e));
			}
		}

		// Create the delta visitor
		CompositeResourceDeltaVisitor deltaVisitor = new CompositeResourceDeltaVisitor();
		deltaVisitor.addDelegate(new AllFoldersVisitor());
		deltaVisitor.addDelegate(new BndFileVisitor(deletedBndFiles, changedBndFiles));
		deltaVisitor.addDelegate(new ClassFileVisitor(bndProject, changedBndFiles));
		
		// If the export dirs changed when we need to do a full scan of bundles, otherwise just a delta scan is fine
		if(bndPropertiesDelta == null) {
			deltaVisitor.addDelegate(new BundleJarVisitor(deletedJarFiles, changedBundles, exportDirs));
		} else {
			CompositeResourceProxyVisitor resourceVisitor = new CompositeResourceProxyVisitor();
			resourceVisitor.addDelegate(new AllFoldersVisitor());
			resourceVisitor.addDelegate(new BundleJarVisitor(changedBundles, exportDirs));
			
			getProject().accept(resourceVisitor, 0);
		}
		
		// Examine the delta
		delta.accept(deltaVisitor, 0);
		
		// Process the bnd file deletions
		int deletedSize = deletedBndFiles.size();
		progress.setWorkRemaining(changedBndFiles.size() + deletedSize);
		processBndFileDeletions(bndProject, deletedBndFiles, progress.newChild(deletedSize));
		
		// Process the changed bnd files
		for (IFile file : changedBndFiles) {
			rebuildBndFile(bndProject, file, deletedJarFiles, changedBundles, progress.newChild(1));
		}

		// Inform the workspace repository about changed bundle files
		if(bndPropertiesDelta == null) {
			WorkspaceRepositoryClasspathContainerInitializer.getInstance().bundlesChanged(bndProject.getProject(), deletedJarFiles, changedBundles, progress.newChild(1));
		} else {
			WorkspaceRepositoryClasspathContainerInitializer.getInstance().resetProjectExports(bndProject.getProject(), changedBundles, progress.newChild(1));
		}
	}
	@Override
	protected void clean(IProgressMonitor monitor) throws CoreException {
		// Clear markers
		getProject().deleteMarkers(MARKER_BND_PROBLEM, true, IResource.DEPTH_INFINITE);
		
		// Delete target files
		BndProject bndProject = BndProject.create(getProject());
		List<IPath> paths = new ArrayList<IPath>();
		for (BndFileModel fileModel : bndProject.getAllFileModels()) {
			IPath targetPath = fileModel.getTargetPath();
			if(targetPath != null)
				paths.add(targetPath);
		}
		deletePaths(paths, monitor);
		bndProject.clearAll();
	}
	void processBndFileDeletions(BndProject bndProject, Collection<? extends IFile> bndFiles, IProgressMonitor monitor) throws CoreException {
		final Collection<IPath> deletions = new ArrayList<IPath>(bndFiles.size());
		for (IFile file : bndFiles) {
			BndFileModel model = bndProject.removeFileModel(file.getFullPath());
			if(model != null) {
				IPath targetPath = model.getTargetPath();
				if(targetPath != null)
					deletions.add(targetPath);
			}
		}
		deletePaths(deletions, monitor);
	}
	void deletePaths(final Collection<? extends IPath> paths, IProgressMonitor monitor) throws CoreException {
		final IWorkspace workspace = getProject().getWorkspace();
		workspace.run(new IWorkspaceRunnable() {
			public void run(IProgressMonitor monitor) throws CoreException {
				SubMonitor progress = SubMonitor.convert(monitor, paths.size());
				for(IPath path : paths) {
					IFile file = workspace.getRoot().getFile(path);
					if(file.exists())
						file.delete(false, progress.newChild(1));
					else
						progress.worked(1);
				}
			}
		}, monitor);
	}
	void rebuildBndFile(BndProject bndProject, final IFile bndFile, final List<IFile> deletedJarFiles, final List<ExportedBundle> exports, IProgressMonitor monitor) throws CoreException {
		SubMonitor progress = SubMonitor.convert(monitor, 2);
		
		// Get or create the build model for this bnd file
		BndFileModel fileModel = bndProject.getFileModel(bndFile.getFullPath());
		
		// Clear markers
		bndFile.deleteMarkers(MARKER_BND_PROBLEM, true, IResource.DEPTH_INFINITE);
		
		// Create the builder
		final Builder builder = new Builder();
		//builder.setPedantic(Plugin.getDefault().isPedantic() || Plugin.getDefault().isDebugging());
		
		// Set the initial properties for the builder
		try {
			builder.setProperties(bndFile.getLocation().toFile());
		} catch (IOException e) {
			throw new CoreException(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Error loading Bnd properties.", e));
		}
		
		// Initialise the builder classpath
		String classpathStr = builder.getProperty(Constants.CLASSPATH);
		try {
			if(classpathStr == null) {
				fileModel.setClasspath(null);
				builder.setClasspath(projectClasspathCalculator.classpathAsFiles().toArray(new File[0]));
			} else {
				BndFileClasspathCalculator calculator = new BndFileClasspathCalculator(classpathStr, getProject().getWorkspace().getRoot(), bndFile.getFullPath());
				fileModel.setClasspath(calculator.classpathAsWorkspacePaths());
				builder.setClasspath(calculator.classpathAsFiles().toArray(new File[0]));
			}
			builder.setSourcepath(projectClasspathCalculator.sourcepathAsFiles().toArray(new File[0]));
		} catch (IOException e) {
			throw new CoreException(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Error setting bundle classpath", e));
		}
		
		// Analyse the bundle
		String tmpSymbolicName = "";
		Version tmpVersion = new Version(0,0,0);
		try {
			Set<Instruction> includes = new HashSet<Instruction>();
			includes.addAll(getInstructionsFromHeader(builder, Constants.PRIVATE_PACKAGE).keySet());
			includes.addAll(getInstructionsFromHeader(builder, Constants.EXPORT_PACKAGE).keySet());
			fileModel.setIncludes(includes);
			
			Jar jar = builder.build();
			progress.worked(1);
			
			// Check the symbolic name and version (needed to report addition/change of the bundle later)
			Attributes attributes = jar.getManifest().getMainAttributes();
			tmpSymbolicName = attributes.getValue(Constants.BUNDLE_SYMBOLICNAME);
			try {
				String versionStr = builder.getProperty(Constants.BUNDLE_VERSION);
				if(versionStr != null) {
					tmpVersion = new Version(versionStr);
				}
			} catch (IllegalArgumentException e) {
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
		}
		final Version version = tmpVersion;
		final String symbolicName = tmpSymbolicName;
		
		// Report errors
		for (String errorMessage : builder.getErrors()) {
			IMarker marker = bndFile.createMarker(MARKER_BND_PROBLEM);
			marker.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_ERROR);
			marker.setAttribute(IMarker.MESSAGE, errorMessage);
		}
		
		// Check the output file path
		final IPath oldTargetPath = fileModel.getTargetPath();
		final IPath targetPath;
		String targetPathStr = builder.getProperty("-output");
		if(targetPathStr == null) {
			targetPath = bndFile.getFullPath().removeLastSegments(1).append(builder.getBsn() + ".jar");
		} else {
			targetPath = bndFile.getFullPath().removeLastSegments(1).append(targetPathStr);
		}
		fileModel.setTargetPath(targetPath);
		
		// Perform the delete of the old bundle and write of the new bundle in a single
		// workspace operation
		final IWorkspace workspace = getProject().getWorkspace();
		IWorkspaceRunnable workspaceOp = new IWorkspaceRunnable() {
			public void run(IProgressMonitor monitor) throws CoreException {
				SubMonitor progress = SubMonitor.convert(monitor, 10);
				
				IFile targetFile = workspace.getRoot().getFile(targetPath);
				if(oldTargetPath != null && !oldTargetPath.equals(targetPath)) {
					IFile oldTargetFile = workspace.getRoot().getFile(oldTargetPath);
					oldTargetFile.delete(false, progress.newChild(1));
					if(deletedJarFiles != null) deletedJarFiles.add(oldTargetFile);
				} else {
					progress.setWorkRemaining(9);
				}
				
				if(!builder.getErrors().isEmpty() && targetFile.exists()) {
					targetFile.delete(false, progress.newChild(9));
					if(deletedJarFiles != null) deletedJarFiles.add(targetFile);
				} else {
					ByteArrayOutputStream jarBits = new ByteArrayOutputStream();
					Jar jar = builder.getJar();
					try {
						long bndTimestamp = bndFile.getLocalTimeStamp();
						
						jar.write(jarBits);
						ByteArrayInputStream inputStream = new ByteArrayInputStream(jarBits.toByteArray());
						if(targetFile.exists()) {
							long targetTimestamp = targetFile.getLocalTimeStamp();
							if(bndTimestamp >= targetTimestamp) {
								targetFile.setContents(inputStream, IResource.NONE, progress.newChild(9));
							}
							exports.add(new ExportedBundle(targetFile.getFullPath(), bndFile.getFullPath(), symbolicName, version));
						} else {
							targetFile.create(inputStream, IResource.NONE, progress.newChild(9));
							exports.add(new ExportedBundle(targetFile.getFullPath(), bndFile.getFullPath(), symbolicName, version));
						}
						targetFile.setDerived(true);
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		};
		workspace.run(workspaceOp, progress.newChild(1));
	}
	private Map<Instruction, Map<String, String>> getInstructionsFromHeader(Builder builder, String name) {
		Map<Instruction, Map<String, String>> result;
		Map<String,Map<String,String>> map;
		String propStr = builder.getProperty(name);
		if(propStr == null)
			map = Collections.emptyMap();
		else
			map = builder.parseHeader(propStr);
		result = Instruction.replaceWithInstruction(map);
		return result;
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
	ExportedBundle getBundleFromJar(IFile jarFile) {
		JarInputStream jarInStream = null;
		try {
			jarInStream = new JarInputStream(jarFile.getContents());
			Manifest manifest = jarInStream.getManifest();
			if(manifest != null) {
				Attributes attributes = manifest.getMainAttributes();
				
				// Check this is an R4 bundle
				String manifestVersion = (String) attributes.getValue(Constants.BUNDLE_MANIFESTVERSION);
				if(manifestVersion == null || !"2".equals(manifestVersion.trim())) {
					return null;
				}
				
				// Check for symbolic name and trim off any attributes (e.g. "singleton")
				String symbolicName = (String) attributes.getValue(Constants.BUNDLE_SYMBOLICNAME);
				if(symbolicName == null) {
					return null;
				} else {
					symbolicName = symbolicName.trim();
					int semicolon = symbolicName.indexOf(';');
					if(semicolon != -1) {
						symbolicName = symbolicName.substring(0, semicolon);
					}
				}
				
				// Parse the version
				String versionStr = (String) attributes.getValue(Constants.BUNDLE_VERSION);
				Version version = new Version(0, 0, 0);
				try {
					if(versionStr != null) {
						version = new Version(versionStr);
					}
				} catch (IllegalArgumentException e) {
					// Ignore
				}
				
				return new ExportedBundle(jarFile.getFullPath(), null, symbolicName, version);
			}
		} catch (IOException e) {
			Plugin.getDefault().getLog().log(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Error while checking JAR for bundle-ness.", e));
		} catch (CoreException e) {
			Plugin.getDefault().getLog().log(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Error while checking JAR for bundle-ness.", e));
		} finally {
			if(jarInStream != null) {
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
		
		private final BndProject bndProject;
		private final Collection<? super IFile> changedBndFiles;
		
		ClassFileVisitor(BndProject bndProject, Collection<? super IFile> changedBndFiles) {
			this.bndProject = bndProject;
			this.changedBndFiles = changedBndFiles;
		}
		public boolean visit(IResourceDelta delta) throws CoreException {
			// Check for classes that may be included in one or more bnd files
			if(delta.getResource().getType() == IResource.FILE && delta.getResource().getName().endsWith(CLASS_SUFFIX)) {
				checkClassFile(bndProject, (IFile) delta.getResource());
			}
			return false;
		}
		void checkClassFile(BndProject bndProject, IFile classFile) {
			// Find the package name for this classfile, if it is in the project classpath
			String packageNameInProject = null;
			for(IPath classFolder : projectClasspathCalculator.classpathAsWorkspacePaths()) {
				packageNameInProject = packageNameForClassFilePath(classFolder, classFile.getFullPath());
				if(packageNameInProject != null) break;
			}
			
			// Which bundles are affected?
			Collection<BndFileModel> fileModels = bndProject.getAllFileModels();
			for (BndFileModel model : fileModels) {
				String packageName = null;
				if(model.getClasspath() == null) {
					packageName = packageNameInProject;
				} else {
					for (IPath classFolder : model.getClasspath()) {
						packageName = packageNameForClassFilePath(classFolder, classFile.getFullPath());
						if(packageName != null) break;
					}
				}
				if(packageName != null && model.containsPackage(packageName)) {
					changedBndFiles.add(getProject().getWorkspace().getRoot().getFile(model.getPath()));
				}
			}
		}
		String packageNameForClassFilePath(IPath folderPath, IPath classFilePath) {
			if(folderPath.isPrefixOf(classFilePath)) {
				IPath relativePath = PathUtils.makeRelativeTo(classFilePath, folderPath); //classFilePath.makeRelativeTo(folderPath);
				IPath packagePath = relativePath.removeLastSegments(1);
				String packageName = packagePath.toString().replace('/', '.');
				return packageName;
			}
			return null;
		}
	}
	class BundleJarVisitor implements IResourceProxyVisitor, IResourceDeltaVisitor {

		private final Collection<? super IFile> deletedJarFiles;
		private final Collection<? super ExportedBundle> bundles;
		private final Collection<? extends IResource> exportDirs;
		
		BundleJarVisitor(Collection<? super ExportedBundle> bundles, Collection<? extends IResource> exportDirs) {
			this(new LinkedList<IFile>(), bundles, exportDirs);
		}
		BundleJarVisitor(Collection<? super IFile> deletedJarFiles, Collection<? super ExportedBundle> bundles, Collection<? extends IResource> exportDirs) {
			this.deletedJarFiles = deletedJarFiles;
			this.bundles = bundles;
			this.exportDirs = exportDirs;
		}
		public boolean visit(IResourceProxy proxy) throws CoreException {
			// Check for JAR files...
			if(proxy.getType() == IResource.FILE && proxy.getName().toLowerCase().endsWith(JAR_SUFFIX)) {
				IFile file = (IFile) proxy.requestResource();
				
				// Now check it's in the exported bundles dir
				boolean exported = false;
				for (IResource exportDir : exportDirs) {
					if(exportDir.getProjectRelativePath().isPrefixOf(file.getProjectRelativePath())) {
						exported = true;
						break;
					}
				}
				
				// Crack it open and see if it's really a bundle
				if(exported) {
					ExportedBundle bundle = getBundleFromJar(file);
					if(bundle != null) {
						bundles.add(bundle);
					}
				}
			}
			return false;
		}
		public boolean visit(IResourceDelta delta) throws CoreException {
			// Check for bundle JARs
			IResource resource = delta.getResource();
			if(resource.getType() == IResource.FILE && resource.getName().endsWith(JAR_SUFFIX)) {
				IFile file = (IFile) resource;
				if(delta.getKind() == IResourceDelta.REMOVED) {
					deletedJarFiles.add(file);
				} else if(delta.getKind() == IResourceDelta.ADDED || delta.getKind() == IResourceDelta.CHANGED) {
					ExportedBundle bundle = getBundleFromJar(file);
					if(bundle != null) {
						bundles.add(bundle);
					}
				}
			}
			return false;
		}
	}
	class AllFoldersVisitor implements IResourceProxyVisitor, IResourceDeltaVisitor {
		public boolean visit(IResourceProxy proxy) throws CoreException {
			return proxy.getType() == IResource.PROJECT || proxy.getType() == IResource.FOLDER;
		}
		public boolean visit(IResourceDelta delta) throws CoreException {
			return delta.getResource().getType() == IResource.PROJECT || delta.getResource().getType() == IResource.FOLDER;
		}
	}
	class BndFileVisitor implements IResourceProxyVisitor, IResourceDeltaVisitor {
		private final Collection<? super IFile> deletedBndFiles;
		private final Collection<? super IFile> changedBndFiles;
		
		BndFileVisitor(Collection<? super IFile> changedBndFiles) {
			this(new LinkedList<IFile>(), changedBndFiles);
		}
		BndFileVisitor(Collection<? super IFile> deletedBndFiles, Collection<? super IFile> changedBndFiles) {
			this.deletedBndFiles = deletedBndFiles;
			this.changedBndFiles = changedBndFiles;
		}
		public boolean visit(IResourceProxy proxy) throws CoreException {
			// Check for Bnd files
			if(proxy.getType() == IResource.FILE && proxy.getName().toLowerCase().endsWith(BND_SUFFIX)) {
				changedBndFiles.add((IFile) proxy.requestResource());
			}
			return false;
		}
		public boolean visit(IResourceDelta delta) throws CoreException {
			if(delta.getResource() instanceof IFile) {
				IFile file = (IFile) delta.getResource();
				// Check for Bnd files
				if(file.getName().endsWith(BND_SUFFIX)) {
					if(delta.getKind() == IResourceDelta.ADDED || delta.getKind() == IResourceDelta.CHANGED) {
						changedBndFiles.add(file);
					} else if(delta.getKind() == IResourceDelta.REMOVED) {
						deletedBndFiles.add(file);
					}
				}
			}
			return false;
		}
	}
}
