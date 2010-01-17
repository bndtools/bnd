package name.neilbartlett.eclipse.bndtools.builder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import name.neilbartlett.eclipse.bndtools.Plugin;

import org.eclipse.core.resources.IFile;
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
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import aQute.bnd.plugin.Activator;
import aQute.lib.osgi.Builder;
import aQute.lib.osgi.Constants;
import aQute.lib.osgi.Instruction;
import aQute.lib.osgi.Jar;
import aQute.lib.osgi.eclipse.EclipseClasspath;

public class BndIncrementalBuilder extends IncrementalProjectBuilder {
	
	public static final String BUILDER_ID = Plugin.PLUGIN_ID + ".bndbuilder";
	public static final String MARKER_BND_ERROR = Plugin.PLUGIN_ID + ".bndproblem";

	private static final String BND_SUFFIX = ".bnd";
	private static final String CLASS_SUFFIX = ".class";

	
	private final Map<IPath, BndBuildModel> buildModels = new HashMap<IPath, BndBuildModel>();
	private List<IPath> classFileLocations = null;

	@Override
	protected IProject[] build(int kind, Map args, IProgressMonitor monitor) throws CoreException {
		getProjectClasspaths();
		if(kind == FULL_BUILD) {
			fullBuild(monitor);
		} else {
			IResourceDelta delta = getDelta(getProject());
			if(delta == null)
				fullBuild(monitor);
			else
				incrementalBuild(delta, monitor);
		}
		return null;
	}
	void getProjectClasspaths() {
		// Look for class file locations
		classFileLocations = new ArrayList<IPath>();
		IJavaProject javaProject = JavaCore.create(getProject());
		try {
			classFileLocations.add(javaProject.getOutputLocation());
			IClasspathEntry[] classpathEntries = javaProject.getRawClasspath();
			for (IClasspathEntry entry : classpathEntries) {
				switch (entry.getEntryKind()) {
				case IClasspathEntry.CPE_SOURCE:
					IPath outputLocation = entry.getOutputLocation();
					if(outputLocation != null)
						classFileLocations.add(outputLocation);
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
	protected void incrementalBuild(IResourceDelta delta, IProgressMonitor monitor) throws CoreException {
		SubMonitor progress = SubMonitor.convert(monitor);
		
		List<IFile> addedOrChanged = new LinkedList<IFile>();
		List<IFile> deleted = new LinkedList<IFile>();
		
		delta.accept(new DeltaVisitor(addedOrChanged, deleted), 0);
		int deletedSize = deleted.size();
		progress.setWorkRemaining(addedOrChanged.size() + deletedSize);
		
		processBndFileDeletions(deleted, progress.newChild(deletedSize));
		for (IFile file : addedOrChanged) {
			rebuildBndFile(file, progress.newChild(1));
		}
	}
	protected void fullBuild(IProgressMonitor monitor) throws CoreException {
		SubMonitor progress = SubMonitor.convert(monitor);
		
		List<IFile> bndFiles = new LinkedList<IFile>();
		getProject().accept(new ResourceVisitor(bndFiles), 0);
		
		progress.setWorkRemaining(bndFiles.size());
		for (IFile file : bndFiles) {
			rebuildBndFile(file, progress.newChild(1));
		}
	}
	@Override
	protected void clean(IProgressMonitor monitor) throws CoreException {
		// Clear markers
		getProject().deleteMarkers(MARKER_BND_ERROR, true, IResource.DEPTH_INFINITE);
		
		// Delete target files
		List<IPath> paths = new ArrayList<IPath>();
		for(Iterator<Entry<IPath, BndBuildModel>> iter = buildModels.entrySet().iterator(); iter.hasNext(); ) {
			Entry<IPath, BndBuildModel> entry = iter.next();
			iter.remove();
			
			IPath targetPath = entry.getValue().getTargetPath();
			if(targetPath != null)
				paths.add(targetPath);
		}
		deletePaths(paths, monitor);
	}
	void processBndFileDeletions(Collection<? extends IFile> bndFiles, IProgressMonitor monitor) throws CoreException {
		final Collection<IPath> deletions = new ArrayList<IPath>(bndFiles.size());
		for (IFile file : bndFiles) {
			BndBuildModel model = buildModels.remove(file.getFullPath());
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
	void rebuildBndFile(IFile bndFile, IProgressMonitor monitor) throws CoreException {
		SubMonitor progress = SubMonitor.convert(monitor, 2);
		
		// Get or create the build model for this bnd file
		BndBuildModel buildModel = buildModels.get(bndFile.getFullPath());
		if(buildModel == null) {
			buildModel = new BndBuildModel(bndFile.getFullPath());
			buildModels.put(bndFile.getFullPath(), buildModel);
		}
		
		// Clear markers
		bndFile.deleteMarkers(MARKER_BND_ERROR, true, IResource.DEPTH_INFINITE);
		
		// Create the builder
		final Builder builder = new Builder();
		builder.setPedantic(Activator.getDefault().isPedantic() || Activator.getDefault().isDebugging());
		
		// Initialise the builder classpath
		File projectDir = getProject().getLocation().toFile();
		try {
			EclipseClasspath eclipseClasspath = new EclipseClasspath(builder, projectDir.getParentFile(), projectDir);
			builder.setClasspath((File[]) eclipseClasspath.getClasspath().toArray(new File[0]));
			builder.setSourcepath((File[]) eclipseClasspath.getSourcepath().toArray(new File[0]));
		} catch (Exception e) {
			// TODO report exception
			e.printStackTrace();
			return;
		}
		
		// Set the initial properties for the builder
		try {
			builder.setProperties(bndFile.getLocation().toFile());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return;
		}
		
		// Analyse the bundle
		try {
			Set<Instruction> includes = new HashSet<Instruction>();
			includes.addAll(getInstructionsFromHeader(builder, Constants.PRIVATE_PACKAGE).keySet());
			includes.addAll(getInstructionsFromHeader(builder, Constants.EXPORT_PACKAGE).keySet());
			buildModel.setIncludes(includes);
			
			Jar jar = builder.build();
			progress.worked(1);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
		}
		
		// Report errors
		if(!builder.getErrors().isEmpty()) {
			// TODO report errors
			return;
		}
		
		// Check the output file path
		final IPath oldTargetPath = buildModel.getTargetPath();
		final IPath targetPath;
		String targetPathStr = builder.getProperty("-output");
		if(targetPathStr == null) {
			targetPath = bndFile.getFullPath().removeLastSegments(1).append(builder.getBsn() + ".jar");
		} else {
			targetPath = bndFile.getFullPath().removeLastSegments(1).append(targetPathStr);
		}
		buildModel.setTargetPath(targetPath);
		
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
				} else {
					progress.setWorkRemaining(9);
				}
				
				ByteArrayOutputStream jarBits = new ByteArrayOutputStream();
				Jar jar = builder.getJar();
				try {
					jar.write(jarBits);
					ByteArrayInputStream inputStream = new ByteArrayInputStream(jarBits.toByteArray());
					if(targetFile.exists())
						targetFile.setContents(inputStream, IResource.NONE, progress.newChild(9));
					else
						targetFile.create(inputStream, IResource.NONE, progress.newChild(9));
					targetFile.setDerived(true);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
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
	
	class DeltaVisitor implements IResourceDeltaVisitor {
		
		private final Collection<? super IFile> bndFiles;
		private final Collection<? super IFile> deleted;

		public DeltaVisitor(Collection<? super IFile> addedOrChanged, Collection<? super IFile> deleted) {
			this.bndFiles = addedOrChanged;
			this.deleted = deleted;
		}

		public boolean visit(IResourceDelta delta) throws CoreException {
			IResource resource = delta.getResource();
			
			// Recurse into folders
			if(resource.getType() == IResource.FOLDER || resource.getType() == IResource.PROJECT)
				return true;
			
			// Check for Bnd files
			if(resource.getType() == IResource.FILE && resource.getName().endsWith(BND_SUFFIX)) {
				if(delta.getKind() == IResourceDelta.ADDED || delta.getKind() == IResourceDelta.CHANGED) {
					bndFiles.add((IFile) resource);
				} else if(delta.getKind() == IResourceDelta.REMOVED) {
					deleted.add((IFile) resource);
				}
			}
			
			if(resource.getType() == IResource.FILE && resource.getName().endsWith(CLASS_SUFFIX)) {
				checkClassFile((IFile) resource);
			}
			
			return false;
		}

		void checkClassFile(IFile classFile) {
			Set<String> affectedPackages = new HashSet<String>();
			
			// Check if it's in any of the classpath class file locations
			for (IPath location : classFileLocations) {
				if(location.isPrefixOf(classFile.getFullPath())) {
					// Yes it is; check the package name
					IPath relativePath = classFile.getFullPath().makeRelativeTo(location);
					IPath packagePath = relativePath.removeLastSegments(1);
					
					String packageName = packagePath.toString().replace('/', '.');
					affectedPackages.add(packageName);
					break;
				}
			}
			
			// Which bundles are affected?
			Iterator<Entry<IPath, BndBuildModel>> iter = buildModels.entrySet().iterator();
			while(iter.hasNext()) {
				Entry<IPath, BndBuildModel> entry = iter.next();
				IPath bndPath = entry.getKey();
				BndBuildModel bundle = entry.getValue();
				
				if(bundle.containsAny(affectedPackages)) {
					bndFiles.add(getProject().getWorkspace().getRoot().getFile(bndPath));
				}
			}
		}
	}
	
	class ResourceVisitor implements IResourceProxyVisitor {
		private final Collection<? super IFile> bndFiles;
		public ResourceVisitor(Collection<? super IFile> bndFiles) {
			this.bndFiles = bndFiles;
		}
		public boolean visit(IResourceProxy proxy) throws CoreException {
			// Recurse into folders
			if(proxy.getType() == IResource.PROJECT || proxy.getType() == IResource.FOLDER)
				return true;
			
			// Check for Bnd files
			if(proxy.getType() == IResource.FILE && proxy.getName().endsWith(BND_SUFFIX)) {
				bndFiles.add((IFile) proxy.requestResource());
			}
			
			return false;
		}
	}
}
