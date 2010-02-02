package name.neilbartlett.eclipse.bndtools.project;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

public class BndProjectProperties {
	
	public static final String BND_PROPERTIES_FILE = "bnd.properties";
	public static final String BUNDLE_EXPORT_DIRS = "bundle.export.dirs";
	
	private final Properties properties = new Properties();
	private final IProject project;
	
	public BndProjectProperties(IProject project) {
		this.project = project;
	}
	public List<IResource> getExportedBundleDirs() {
		List<IResource> resources = new LinkedList<IResource>();
		
		String exportDirListStr = properties.getProperty(BUNDLE_EXPORT_DIRS, ""); //$NON-NLS-1$
		StringTokenizer tokenizer = new StringTokenizer(exportDirListStr);
		while(tokenizer.hasMoreTokens()) {
			IResource resource = project.findMember(tokenizer.nextToken());
			if(resource != null)
				resources.add(resource);
		}
		return resources;
	}
	public void setExportedBundleDirs(Collection<? extends IResource> dirs) {
		StringBuilder builder = new StringBuilder();
		for(Iterator<? extends IResource> iter = dirs.iterator(); iter.hasNext(); ) {
			builder.append(iter.next().getProjectRelativePath());
			if(iter.hasNext()) builder.append(',');
		}
		properties.put(BUNDLE_EXPORT_DIRS, builder.toString());
	}
	public void load() throws CoreException, IOException {
		properties.clear();
		if(project != null) {
			IResource member = project.findMember(BND_PROPERTIES_FILE);
			if(member != null && member.exists() && member instanceof IFile) {
				InputStream contents = null;
				try {
					contents = ((IFile) member).getContents();
					properties.load(contents);
				} finally {
					try {
						if(contents != null) contents.close();
					} catch (IOException e) {
					}
				}
			}
		}
	}
	public void save() throws CoreException, IOException {
		final ByteArrayOutputStream out = new ByteArrayOutputStream();
		properties.store(out, "");
		
		IWorkspaceRunnable op = new IWorkspaceRunnable() {
			public void run(IProgressMonitor monitor) throws CoreException {
				IFile file = project.getFile(BND_PROPERTIES_FILE);
				if(file.exists()) {
					file.setContents(new ByteArrayInputStream(out.toByteArray()), false, true, null);
				} else {
					file.create(new ByteArrayInputStream(out.toByteArray()), false, null);
				}
			}
		};
		ResourcesPlugin.getWorkspace().run(op, null);
	}
}