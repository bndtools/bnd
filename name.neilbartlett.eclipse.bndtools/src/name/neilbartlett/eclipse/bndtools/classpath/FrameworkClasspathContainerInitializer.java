package name.neilbartlett.eclipse.bndtools.classpath;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

import name.neilbartlett.eclipse.bndtools.Plugin;
import name.neilbartlett.eclipse.bndtools.frameworks.IFramework;
import name.neilbartlett.eclipse.bndtools.frameworks.IFrameworkInstance;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.ClasspathContainerInitializer;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;

import aQute.bnd.plugin.Activator;

public class FrameworkClasspathContainerInitializer extends
		ClasspathContainerInitializer {
	
	public static final String FRAMEWORK_CONTAINER_ID = "name.neilbartlett.eclipse.bndtools.FRAMEWORK_CONTAINER";

	@Override
	public void initialize(IPath containerPath, IJavaProject project)
			throws CoreException {
		IFrameworkInstance instance = getFrameworkInstanceForContainerPath(containerPath);
		IClasspathContainer container = new FrameworkClasspathContainer(containerPath, instance);
		JavaCore.setClasspathContainer(containerPath, new IJavaProject[] { project }, new IClasspathContainer[] { container } , null);
	}
	
	public static IFrameworkInstance getFrameworkInstanceForContainerPath(IPath containerPath) {
		if(containerPath == null || containerPath.segmentCount() != 3)
			return null;
			//throw new CoreException(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Invalid OSGi framework container path.", null));
		
		String frameworkId = containerPath.segment(1);
		try {
			IFramework framework = FrameworkUtils.findFramework(frameworkId);
			String instancePathEncoded = containerPath.segment(2);
			String instancePath = URLDecoder.decode(instancePathEncoded, "UTF-8");
			
			File frameworkPath = new File(instancePath);
			IFrameworkInstance instance = framework.createFrameworkInstance(frameworkPath);
			return instance;
		} catch (UnsupportedEncodingException e) {
			Activator.getDefault().getLog().log(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Error initialising OSGi framework classpath.", e));
			return null;
		} catch (CoreException e) {
			Activator.getDefault().getLog().log(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Error initialising OSGi framework classpath.", e));
			return null;
		}
	}
}
