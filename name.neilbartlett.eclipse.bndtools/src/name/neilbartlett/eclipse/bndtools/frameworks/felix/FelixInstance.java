package name.neilbartlett.eclipse.bndtools.frameworks.felix;

import java.io.File;
import java.io.IOException;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import name.neilbartlett.eclipse.bndtools.Plugin;
import name.neilbartlett.eclipse.bndtools.frameworks.IFrameworkInstance;
import name.neilbartlett.eclipse.bndtools.frameworks.OSGiSpecLevel;
import name.neilbartlett.eclipse.bndtools.frameworks.shared.VersionedBundleFile;

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
import org.osgi.framework.Constants;
import org.osgi.framework.Version;

class FelixInstance implements IFrameworkInstance {
	
	private static final String FRAMEWORK_ID = "felix";
	private static final String DISPLAY_FORMAT = "Apache Felix %s";
	
	private static final String FELIX_SYMBOLIC_NAME = "org.apache.felix.main";
	private static final Version FELIX_MINIMUM = new Version(1, 4, 0);
	private static final char VERSION_SEPARATOR = '-';
	
	private static final String SHELL_BUNDLE_ID = "org.apache.felix.shell";
	private static final String SHELL_TUI_BUNDLE_ID = "org.apache.felix.shell.tui";
	
	private static final String FELIX_MAIN = "org.apache.felix.main.Main";
	
	private final IPath instancePath;
	private IStatus status = Status.OK_STATUS;
	private IClasspathEntry classpathEntry = null;
	private Version version = null;
	private String shellVersion = null;
	private String shellTuiVersion = null;

	public FelixInstance(IPath instancePath) {
		this.instancePath = instancePath;
		loadFromInstancePath();
	}
	
	public OSGiSpecLevel getOSGiSpecLevel() {
		// TODO
		return OSGiSpecLevel.r4_2;
	}
	
	private final void loadFromInstancePath() {
		assert instancePath != null;

		// Find bin/felix.jar
		File felixDir = instancePath.toFile();
		if(!felixDir.isDirectory()) {
			this.status = new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Selected resource does not exist or is not a directory.", null);
			return;
		}
		File binDir = new File(felixDir, "bin");
		if(!binDir.isDirectory()) {
			this.status = new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Not a top-level Felix folder. It does not contain a 'bin' sub-folder.", null);
			return;
		}
		File felixJarFile = new File(binDir, "felix.jar");
		if(!felixJarFile.isFile()) {
			this.status = new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Not a top-level Felix folder. Either 'bin/felix.jar' does not exist or it is not a plain file.", null);
			return;
		}
		
		// Validate JAR file
		try {
			JarFile jar = new JarFile(felixJarFile);
			Manifest manifest = jar.getManifest();
			Attributes attribs = manifest.getMainAttributes();
			String symbolicName = attribs.getValue(Constants.BUNDLE_SYMBOLICNAME);
			if(!FELIX_SYMBOLIC_NAME.equals(symbolicName)) {
				this.status = new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, String.format("The file \"bin/felix.jar\" is not a Felix implementation. Expected to find bundle symbolic name \"%s\" but actually found \"%s\".", FELIX_SYMBOLIC_NAME, symbolicName), null);
				return;
			}
			String versionStr = attribs.getValue(Constants.BUNDLE_VERSION);
			if(versionStr == null) {
				this.status = new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, String.format("Missing version identifier in 'bin/felix.jar'"), null);
				return;
			}
			this.version  = new Version(versionStr);
			if(this.version.compareTo(FELIX_MINIMUM) < 0) {
				this.status = new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, String.format("The minimum Felix version supported is %s.", FELIX_MINIMUM.toString()), null);
				return;
			}
		} catch (IOException e) {
			this.status = new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Error opening 'bin/felix.jar' in selected folder.", e);
			return;
		} catch (IllegalArgumentException e) {
			this.status = new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Error parsing version identifier in 'bin/felix.jar' manifest.", e);
			return;
		}
		
		// Find the shell and shell-tui bundles
		File bundleDir = new File(felixDir, "bundle");
		if(bundleDir.exists()) {
			VersionedBundleFile shellBundle = VersionedBundleFile.findHighestVersionBundle(bundleDir, SHELL_BUNDLE_ID, VERSION_SEPARATOR);
			shellVersion = shellBundle != null ? shellBundle.getVersion().toString() : null;
			
			VersionedBundleFile shellTuiBundle = VersionedBundleFile.findHighestVersionBundle(bundleDir, SHELL_TUI_BUNDLE_ID, VERSION_SEPARATOR);
			shellTuiVersion = shellTuiBundle != null ? shellTuiBundle.getVersion().toString() : null;
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
		return String.format(DISPLAY_FORMAT, version != null ? version.toString() : "<unknown>");
	}

	public String getFrameworkId() {
		return FRAMEWORK_ID;
	}

	public IPath getInstancePath() {
		return instancePath;
	}

	public IStatus getStatus() {
		return status;
	}
	
	public boolean isLaunchable() {
		return true;
	}

	public String getStandardProgramArguments(File workingDir) {
		return "";
	}

	public String getStandardVMArguments(File workingDir) {
		return String.format("-Dfelix.home=\"%s\" -Dfelix.config.properties=file:felix/config.properties -Dfelix.cache.rootdir=felix", instancePath.toString());
	}

	public String getShellVersion() {
		return shellVersion;
	}
	
	public String getShellTuiVersion() {
		return shellTuiVersion;
	}

	public String getMainClassName() {
		return FELIX_MAIN;
	}
}
