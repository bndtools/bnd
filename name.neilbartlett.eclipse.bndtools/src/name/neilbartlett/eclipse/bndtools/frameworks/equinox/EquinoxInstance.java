/**
 * 
 */
package name.neilbartlett.eclipse.bndtools.frameworks.equinox;

import java.io.File;

import name.neilbartlett.eclipse.bndtools.Plugin;
import name.neilbartlett.eclipse.bndtools.frameworks.IFrameworkInstance;
import name.neilbartlett.eclipse.bndtools.frameworks.OSGiSpecLevel;
import name.neilbartlett.eclipse.bndtools.frameworks.shared.VersionedBundleFile;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.IAccessRule;
import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.swt.graphics.Device;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.Version;

class EquinoxInstance implements IFrameworkInstance {
	
	private static final String FRAMEWORK_ID = "equinox";
	private static final String DISPLAY_FORMAT = "Eclipse Equinox %s";
	
	private static final String EQUINOX_ID = "org.eclipse.osgi";
	private static final char VERSION_SEPARATOR = '_';
	
	private static final String EQUINOX_STARTER = "org.eclipse.core.runtime.adaptor.EclipseStarter";
	private static final String PROGRAM_ARGS = "-console -consoleLog -configuration " + FRAMEWORK_ID;

	private final IPath instancePath;
	
	private IStatus status;
	private Version version = null;
	private IClasspathEntry classpathEntry = null;

	public EquinoxInstance(IPath instancePath) {
		this.instancePath = instancePath;
		loadFromInstancePath();
	}
	
	public OSGiSpecLevel getOSGiSpecLevel() {
		// TODO: calculate correctly
		return OSGiSpecLevel.r4_2;
	}
	
	private final void loadFromInstancePath() {
		assert instancePath != null;
		File file = instancePath.toFile();
		if(!file.exists()) {
			this.status = new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Selected file or folder does not exist.", null);
			return;
		}
		
		try {
			VersionedBundleFile equinoxJar = findEquinoxJar(file);
			this.version = equinoxJar.getVersion();
			
			IPath jarPath = new Path(equinoxJar.getFile().getAbsolutePath());
			File sourceJarFile = findSourceJar(EQUINOX_ID, equinoxJar.getFile());
			IPath sourcePath = sourceJarFile != null ? new Path(sourceJarFile.getAbsolutePath()) : null;
			classpathEntry = JavaCore.newLibraryEntry(jarPath, sourcePath, null, new IAccessRule[0], new IClasspathAttribute[0], false);
			this.status = Status.OK_STATUS;
		} catch (CoreException e) {
			this.status = e.getStatus();
		}
	}
	
	public IClasspathEntry[] getClasspathEntries() {
		return new IClasspathEntry[] { classpathEntry };
	}
	public String getDisplayString() {
		return String.format(DISPLAY_FORMAT, version != null ? version.toString() : "<error>");
	}
	public IPath getInstancePath() {
		return instancePath;
	}
	
	public IStatus getStatus() {
		return status;
	}
	
	private static VersionedBundleFile findEquinoxJar(File resource) throws CoreException {
		if(resource.isDirectory()) {
			// Check for top-level Equinox/Eclipse directory containing "plugins/org.eclipse.osgi_<version>.jar"
			File pluginsDir = new File(resource, "plugins");
			if(!pluginsDir.isDirectory())
				throw createError("Not a top-level Equinox or Eclipse folder. It does not contain a 'plugins' sub-folder");
			
			VersionedBundleFile selectedJar = VersionedBundleFile.findHighestVersionBundle(pluginsDir, EQUINOX_ID, VERSION_SEPARATOR);
			if(selectedJar == null)
				throw createError("Could not find a file named 'plugins/org.eclipse.osgi_<version>.jar' in the selected folder");
			
			return selectedJar;
		} else if(resource.isFile()) {
			VersionedBundleFile selectedJar = null;
			try {
				selectedJar = VersionedBundleFile.fromBundleJar(EQUINOX_ID, resource, VERSION_SEPARATOR);
			} catch (IllegalArgumentException e) {
				// Ignore
			}
			
			if(selectedJar == null)
				throw createError("Selected file is not an Equinox JAR file.");
			return selectedJar;
		} else {
			throw createError("Selected resource does not exist or is not a plain file or directory.");
		}
	}
	
	private static File findSourceJar(String id, File jar) {
		String jarName = jar.getName();
		String versionSuffix = jarName.substring(id.length());
		String sourceJarName = id + ".source" + versionSuffix;
		
		File sourceJar = new File(jar.getParent(), sourceJarName);
		if(!sourceJar.isFile())
			return null;
		
		return sourceJar;
	}
	
	public Image createIcon(Device device) {
		return AbstractUIPlugin.imageDescriptorFromPlugin(Plugin.PLUGIN_ID, "/icons/equinox.gif").createImage(false, device);
	}

	public String getFrameworkId() {
		return FRAMEWORK_ID;
	}
	
	private static CoreException createError(String message) {
		return new CoreException(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, message, null));
	}
	
	public boolean isLaunchable() {
		return true;
	}
	
	public String getStandardProgramArguments(File workingDir) {
		return PROGRAM_ARGS;
	}
	public String getStandardVMArguments(File workingDir) {
		return String.format("-Dosgi.syspath=\"%s\" -Declipse.ignoreApp=true", workingDir.toString());
	}

	public String getMainClassName() {
		return EQUINOX_STARTER;
	}
	
}