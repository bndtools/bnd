package name.neilbartlett.eclipse.bndtools.prefs.frameworks;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

import name.neilbartlett.eclipse.bndtools.Plugin;
import name.neilbartlett.eclipse.bndtools.frameworks.IFramework;
import name.neilbartlett.eclipse.bndtools.frameworks.IFrameworkInstance;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;


public class FrameworkPreferencesInitializer extends AbstractPreferenceInitializer {
	
	static final String PROP_FRAMEWORK_LIST = "frameworks";
	static final String PROP_FRAMEWORK_PREFIX = "framework_";
	
	@Override
	public void initializeDefaultPreferences() {
		Preferences node = new InstanceScope().getNode(Plugin.PLUGIN_ID);
		node.put(PROP_FRAMEWORK_LIST, "");
	}

	/**
	 * Load the list of framework URLs into the specified list
	 * @param list The list into which to load the framework URLs.
	 */
	public static synchronized void loadFrameworkUrls(List<? super String> list) {
		Preferences node = new InstanceScope().getNode(Plugin.PLUGIN_ID);
		String listStr = node.get(PROP_FRAMEWORK_LIST, ""); //$NON-NLS-1$
		StringTokenizer tokenizer = new StringTokenizer(listStr, ","); //$NON-NLS-1$
		while(tokenizer.hasMoreTokens()) {
			String token = tokenizer.nextToken().trim();
			
			String frameworkUrl = node.get(PROP_FRAMEWORK_PREFIX + token, ""); //$NON-NLS-1$ 
			if(frameworkUrl != null && frameworkUrl.length() > 0) {
				list.add(frameworkUrl);
			}
		}
	}
	
	public static synchronized final void saveFrameworkUrls(List<? extends String> list) throws BackingStoreException {
		Preferences node = new InstanceScope().getNode(Plugin.PLUGIN_ID);
		node.clear();
		
		StringBuilder frameworkListStr = new StringBuilder();
		int i = 0;
		for (Iterator<? extends String> iterator = list.iterator(); iterator.hasNext(); i++) {
			String frameworkUrl = iterator.next();
			frameworkListStr.append(i);
			if(iterator.hasNext()) frameworkListStr.append(',');
			
			node.put(PROP_FRAMEWORK_PREFIX + i, frameworkUrl);
		}
		node.put(PROP_FRAMEWORK_LIST, frameworkListStr.toString());
	}
	
	public static List<IFrameworkInstance> loadFrameworkInstanceList() {
		List<String> frameworkUrls = new ArrayList<String>();
		loadFrameworkUrls(frameworkUrls);
		List<IFrameworkInstance> instances = new ArrayList<IFrameworkInstance>(frameworkUrls.size());
		
		for (String frameworkUrl : frameworkUrls) {
			int colonIndex = frameworkUrl.indexOf(':');
			if(colonIndex > 0) {
				String frameworkId = frameworkUrl.substring(0, colonIndex);
				String resourcePath = frameworkUrl.substring(colonIndex + 1);
				
				try {
					IFramework framework = findFramework(frameworkId);
					IFrameworkInstance instance = framework.createFrameworkInstance(new File(resourcePath));
					
					instances.add(instance);
				} catch (CoreException e) {
				}
			}
		}
		return instances;
	}
	
	public static void saveFrameworkInstancesList(List<IFrameworkInstance> instances) throws BackingStoreException {
		List<String> urlList = new ArrayList<String>();
		
		for (IFrameworkInstance instance : instances) {
			urlList.add(instance.getInstanceURL());
		}
		
		saveFrameworkUrls(urlList);
	}
	
	static IFramework findFramework(String id) throws CoreException {
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
