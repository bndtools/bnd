/**
 * 
 */
package name.neilbartlett.eclipse.bndtools.frameworks.equinox;

import java.io.File;

import name.neilbartlett.eclipse.bndtools.Plugin;
import name.neilbartlett.eclipse.bndtools.frameworks.IFrameworkInstance;

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
	
	private static final String DISPLAY_FORMAT = "Eclipse Equinox %s";
	private static final String EQUINOX_ID = "org.eclipse.osgi";

	private final IPath instancePath;
	
	private String error = null;
	private Version version = null;
	private IClasspathEntry classpathEntry = null;

	public EquinoxInstance(IPath instancePath) {
		this.instancePath = instancePath;
		loadFromInstancePath();
	}
	private final void loadFromInstancePath() {
		assert instancePath != null;
		File file = instancePath.toFile();
		if(!file.exists()) {
			error = "Selected file or folder does not exist.";
			return;
		}
		
		try {
			VersionedPluginFile equinoxJar = findEquinoxJar(file);
			this.version = equinoxJar.getVersion();
			
			IPath jarPath = new Path(equinoxJar.getFile().getAbsolutePath());
			File sourceJarFile = findSourceJar(EQUINOX_ID, equinoxJar.getFile());
			IPath sourcePath = sourceJarFile != null ? new Path(sourceJarFile.getAbsolutePath()) : null;
			classpathEntry = JavaCore.newLibraryEntry(jarPath, sourcePath, null, new IAccessRule[0], new IClasspathAttribute[0], false);
		} catch (CoreException e) {
			this.error = e.getStatus().getMessage();
		}
	}
	
	public IClasspathEntry[] getClasspathEntries() {
		return new IClasspathEntry[] { classpathEntry };
	}
	public String getDisplayString() {
		if(error != null)
			return error;
		return String.format(DISPLAY_FORMAT, version);
	}
	public IPath getInstancePath() {
		return instancePath;
	}
	public String getValidationError() {
		return error;
	}
	
	public String getInstanceURL() {
		return "equinox:" + instancePath.toString();
	}
	
	private static VersionedPluginFile findEquinoxJar(File resource) throws CoreException {
		if(resource.isDirectory()) {
			// Check for top-level Equinox/Eclipse directory containing "plugins/org.eclipse.osgi_<version>.jar"
			File pluginsDir = new File(resource, "plugins");
			if(!pluginsDir.isDirectory())
				throw createError("Not a top-level Equinox or Eclipse folder. It does not contain a 'plugins' sub-folder");
			
			VersionedPluginFile selectedJar = selectEquinoxJar(pluginsDir);
			if(selectedJar == null)
				throw createError("Could not find a file named 'plugins/org.eclipse.osgi_<version>.jar' in the selected folder");
			
			return selectedJar;
		} else if(resource.isFile()) {
			VersionedPluginFile selectedJar = null;
			try {
				selectedJar = VersionedPluginFile.fromPluginJar(EQUINOX_ID, resource);
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
	
	private static VersionedPluginFile selectEquinoxJar(File pluginsDir) {
		VersionedPluginFile selectedJarFile = null;
		
		File[] pluginFiles = pluginsDir.listFiles();
		for (File pluginFile : pluginFiles) {
			try {
				VersionedPluginFile versionedPluginFile = VersionedPluginFile.fromPluginJar(EQUINOX_ID, pluginFile);
				if(versionedPluginFile != null) {
					if(selectedJarFile == null || selectedJarFile.getVersion().compareTo(versionedPluginFile.getVersion()) < 0) {
						selectedJarFile = versionedPluginFile;
					}
				}
			} catch (IllegalArgumentException e) {
				// Ignore plugin JARs with invalid version strings.
			}
		}
		return selectedJarFile;
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
	
	private static CoreException createError(String message) {
		return new CoreException(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, message, null));
	}
	
	public Image createIcon(Device device) {
		return AbstractUIPlugin.imageDescriptorFromPlugin(Plugin.PLUGIN_ID, "/icons/equinox.gif").createImage(false, device);
	}
}