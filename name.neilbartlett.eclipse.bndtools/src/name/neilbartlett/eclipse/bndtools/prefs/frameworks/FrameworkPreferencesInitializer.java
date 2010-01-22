/*******************************************************************************
 * Copyright (c) 2010 Neil Bartlett.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Neil Bartlett - initial API and implementation
 *******************************************************************************/
package name.neilbartlett.eclipse.bndtools.prefs.frameworks;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;

import name.neilbartlett.eclipse.bndtools.Plugin;
import name.neilbartlett.eclipse.bndtools.classpath.FrameworkUtils;
import name.neilbartlett.eclipse.bndtools.frameworks.IFramework;
import name.neilbartlett.eclipse.bndtools.frameworks.IFrameworkInstance;
import name.neilbartlett.eclipse.bndtools.frameworks.OSGiSpecLevel;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;

import aQute.bnd.plugin.Activator;


public class FrameworkPreferencesInitializer extends AbstractPreferenceInitializer {
	
	static final String PROP_FRAMEWORK_LIST = "frameworks";
	static final String PROP_FRAMEWORK_PREFIX = "framework_";
	
	@Override
	public void initializeDefaultPreferences() {
		List<String> autoConfigUrls = new LinkedList<String>();
		
		IConfigurationElement[] elements = Platform.getExtensionRegistry().getConfigurationElementsFor(Plugin.PLUGIN_ID, Plugin.EXTPOINT_OSGI_FRAMEWORKS);
		for (IConfigurationElement element : elements) {
			String id = element.getAttribute("id");
			if(Boolean.parseBoolean(element.getAttribute("supportsAutoConfig"))) {
				try {
					IFramework framework = (IFramework) element.createExecutableExtension("class");
					Collection<File> locations = framework.getAutoConfiguredLocations();
					if(locations != null) for (File location : locations) {
						String url = id + ":" + location.getAbsolutePath();
						autoConfigUrls.add(url);
					}
				} catch (CoreException e) {
					Activator.getDefault().getLog().log(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Error instantiating framework type \"" + id + "\"", e));
				}
			}
		}
		try {
			saveFrameworkUrls(autoConfigUrls);
		} catch (BackingStoreException e) {
			Activator.getDefault().getLog().log(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Error initialising default framework preferences.", e));
		}
	}

	/**
	 * Load the list of framework URLs into the specified list
	 * @param list The list into which to load the framework URLs.
	 */
	public static synchronized void loadFrameworkUrls(List<? super String> list) {
		Preferences node = new DefaultScope().getNode(Plugin.PLUGIN_ID);
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
		Preferences node = new DefaultScope().getNode(Plugin.PLUGIN_ID);
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
		node.sync();
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
					IFramework framework = FrameworkUtils.findFramework(frameworkId);
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
			String url = instance.getFrameworkId() + ":" + instance.getInstancePath();
			urlList.add(url);
		}
		
		saveFrameworkUrls(urlList);
	}

	public static IFrameworkInstance getFrameworkInstance(OSGiSpecLevel specLevel) {
		// TODO: add preferences for mapping spec levels to a preferred instance
		List<IFrameworkInstance> list = loadFrameworkInstanceList();
		for (IFrameworkInstance instance : list) {
			if(instance.getOSGiSpecLevel() == specLevel)
				return instance;
		}
		return null;
	}
}
