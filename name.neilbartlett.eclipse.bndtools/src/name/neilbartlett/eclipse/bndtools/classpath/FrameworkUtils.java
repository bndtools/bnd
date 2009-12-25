package name.neilbartlett.eclipse.bndtools.classpath;

import name.neilbartlett.eclipse.bndtools.Plugin;
import name.neilbartlett.eclipse.bndtools.frameworks.IFramework;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;

public class FrameworkUtils {
	
	private FrameworkUtils() {}
	
	/**
	 * Find and instantiate a framework object representing the framework with
	 * the specified ID.
	 * 
	 * @param id
	 *            The ID of the framework extension element.
	 * @return An {@link IFramework} object, or {@code null} if no frameworks
	 *         exist with the specified ID.
	 * @throws CoreException
	 *             If there was an error instantiating the {@link IFramework}
	 *             object.
	 */
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
