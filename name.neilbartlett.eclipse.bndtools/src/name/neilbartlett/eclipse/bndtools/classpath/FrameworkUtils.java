package name.neilbartlett.eclipse.bndtools.classpath;

import name.neilbartlett.eclipse.bndtools.Plugin;
import name.neilbartlett.eclipse.bndtools.frameworks.IFramework;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;

public class FrameworkUtils {
	
	private FrameworkUtils() {}
	
	public static IFramework findFramework(String id) throws CoreException {
		IConfigurationElement[] elements = Platform.getExtensionRegistry().getConfigurationElementsFor(Plugin.PLUGIN_ID, Plugin.EXTPOINT_OSGI_FRAMEWORKS);
		for (IConfigurationElement element : elements) {
			String elementId = element.getAttribute("id");
			if(id.equals(elementId)) {
				return (IFramework) element.createExecutableExtension("class");
			}
		}
		return null;
	}

}
