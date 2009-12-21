package name.neilbartlett.eclipse.bndtools.frameworks.felix;

import java.io.File;
import java.io.IOException;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import name.neilbartlett.eclipse.bndtools.Plugin;
import name.neilbartlett.eclipse.bndtools.frameworks.IFrameworkInstance;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IAccessRule;
import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.swt.graphics.Device;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.Constants;
import org.osgi.framework.Version;

class FelixInstance implements IFrameworkInstance {
	
	private static final String DISPLAY_FORMAT = "Apache Felix %s";
	private static final String FRAMEWORK_ID = "felix";
	private static final String FELIX_SYMBOLIC_NAME = "org.apache.felix.main";
	
	private final IPath instancePath;
	private String error = null;
	private IClasspathEntry classpathEntry = null;
	private Version version = null;

	public FelixInstance(IPath instancePath) {
		this.instancePath = instancePath;
		loadFromInstancePath();
	}
	
	private final void loadFromInstancePath() {
		assert instancePath != null;

		// Find bin/felix.jar
		File felixDir = instancePath.toFile();
		if(!felixDir.isDirectory()) {
			error = "Selected resource does not exist or is not a directory.";
			return;
		}
		File binDir = new File(felixDir, "bin");
		if(!binDir.isDirectory()) {
			error = "Not a top-level Felix folder. It does not contain a 'bin' sub-folder.";
			return;
		}
		File felixJarFile = new File(binDir, "felix.jar");
		if(!felixJarFile.isFile()) {
			error = "Not a top-level Felix folder. Either 'bin/felix.jar' does not exist or it is not a plain file.";
			return;
		}
		
		// Validate JAR file
		try {
			JarFile jar = new JarFile(felixJarFile);
			Manifest manifest = jar.getManifest();
			Attributes attribs = manifest.getMainAttributes();
			String symbolicName = attribs.getValue(Constants.BUNDLE_SYMBOLICNAME);
			if(!FELIX_SYMBOLIC_NAME.equals(symbolicName)) {
				error = String.format("The file 'bin/felix.jar' is not a Felix implemenation. Expected to find bundle symbolic name %s but actually found %s.", FELIX_SYMBOLIC_NAME, symbolicName);
				return;
			}
			String versionStr = attribs.getValue(Constants.BUNDLE_VERSION);
			if(versionStr == null) {
				error = String.format("Missing version identifier in 'bin/felix.jar'");
				return;
			}
			this.version  = new Version(versionStr);
		} catch (IOException e) {
			error = "Error opening 'bin/felix.jar' in selected folder.";
			return;
		} catch (IllegalArgumentException e) {
			error = "Error parsing version identifier in 'bin/felix.jar' manifest.";
			return;
		}
		
		IPath jarPath = new Path(felixJarFile.getAbsolutePath());
		this.classpathEntry  = JavaCore.newLibraryEntry(jarPath, null, null, new IAccessRule[0], new IClasspathAttribute[0], false);
	}

	public Image createIcon(Device device) {
		return AbstractUIPlugin.imageDescriptorFromPlugin(Plugin.PLUGIN_ID, "/icons/felix.gif").createImage(false, device);
	}

	public IClasspathEntry[] getClasspathEntries() {
		return new IClasspathEntry[] { classpathEntry };
	}

	public String getDisplayString() {
		return String.format(DISPLAY_FORMAT, version.toString());
	}

	public String getFrameworkId() {
		return FRAMEWORK_ID;
	}

	public IPath getInstancePath() {
		return instancePath;
	}

	public String getValidationError() {
		return error;
	}

}
