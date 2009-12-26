package name.neilbartlett.eclipse.bndtools.classpath;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

import name.neilbartlett.eclipse.bndtools.Plugin;
import name.neilbartlett.eclipse.bndtools.frameworks.IFramework;
import name.neilbartlett.eclipse.bndtools.frameworks.IFrameworkInstance;
import name.neilbartlett.eclipse.bndtools.utils.P2Utils;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.internal.provisional.frameworkadmin.BundleInfo;
import org.eclipse.jdt.core.ClasspathContainerInitializer;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.osgi.framework.Version;

import aQute.bnd.plugin.Activator;

public class FrameworkClasspathContainerInitializer extends
		ClasspathContainerInitializer {
	
	public static final String FRAMEWORK_CONTAINER_ID = "name.neilbartlett.eclipse.bndtools.FRAMEWORK_CONTAINER";
	public static final String PROP_ANNOTATIONS_LIB = "annotations";
	
	private static final Version ANNOTATIONS_VERSION = new Version(0, 0, 384);
	private static final String ANNOTATIONS_SYMBOLIC_NAME = "biz.aQute.annotation";

	@Override
	public void initialize(IPath containerPath, IJavaProject project)
			throws CoreException {
		IFrameworkInstance instance = getFrameworkInstanceForContainerPath(containerPath);
		
		// Read additional properties & check the annotation property
		Map<String, String> properties = getPropertiesForContainerPath(containerPath);
		IPath annotationsPath = null;
		if(properties != null && Boolean.TRUE.toString().equals(properties.get(PROP_ANNOTATIONS_LIB))) {
			annotationsPath = getAnnotationsPath();
		}
		
		IClasspathContainer container = new FrameworkClasspathContainer(containerPath, instance, annotationsPath);
		JavaCore.setClasspathContainer(containerPath, new IJavaProject[] { project }, new IClasspathContainer[] { container } , null);
	}
	
	private static IPath getAnnotationsPath() {
		BundleInfo annotationsBundle = P2Utils.findBundle(ANNOTATIONS_SYMBOLIC_NAME, ANNOTATIONS_VERSION, false);
		IPath annotsPath = P2Utils.getBundleLocationPath(annotationsBundle);
		return annotsPath;
	}
	
	private static Map<String, String> readProperties(String propsString) {
		Map<String, String> properties = new HashMap<String, String>();
		
		StringTokenizer tokenizer = new StringTokenizer(propsString, ";");
		while(tokenizer.hasMoreTokens()) {
			String propName;
			String encodedPropValue;
			
			String token = tokenizer.nextToken();
			int equalIndex = token.indexOf('=');
			if(equalIndex < 0) {
				propName = token;
				encodedPropValue = "";
			} else {
				propName = token.substring(0, equalIndex);
				encodedPropValue = token.substring(equalIndex + 1);
			}
			
			try {
				String propValue = URLDecoder.decode(encodedPropValue, "UTF-8");
				properties.put(propName, propValue);
			} catch (UnsupportedEncodingException e) {
				// Can't happen
			}
		}
		
		return properties;
	}
	
	public static Map<String, String> getPropertiesForContainerPath(IPath containerPath) {
		Map<String, String> properties = null;
		if(containerPath.segmentCount() == 4) {
			String propsSegment = containerPath.segment(3);
			properties = readProperties(propsSegment);
		}
		return properties;
	}
	
	public static IFrameworkInstance getFrameworkInstanceForContainerPath(IPath containerPath) {
		if(containerPath == null || containerPath.segmentCount() < 3)
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
