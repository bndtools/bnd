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
package name.neilbartlett.eclipse.bndtools.classpath;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

import name.neilbartlett.eclipse.bndtools.Plugin;
import name.neilbartlett.eclipse.bndtools.frameworks.IFramework;
import name.neilbartlett.eclipse.bndtools.frameworks.IFrameworkInstance;
import name.neilbartlett.eclipse.bndtools.frameworks.OSGiSpecLevel;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.ClasspathContainerInitializer;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;

import aQute.bnd.plugin.Activator;

public class FrameworkClasspathContainerInitializer extends
		ClasspathContainerInitializer {
	
	private static final String FRAMEWORK_CONTAINER_ID = "name.neilbartlett.eclipse.bndtools.FRAMEWORK_CONTAINER";
	private static final String PROP_ANNOTATIONS_LIB = "annotations";
	private static final String PROP_INSTANCE_URL = "url"; 
	
	@Override
	public void initialize(IPath containerPath, IJavaProject project) throws CoreException {
		FrameworkClasspathContainer classpathContainer = createClasspathContainerForPath(containerPath);
		JavaCore.setClasspathContainer(containerPath, new IJavaProject[] { project }, new IClasspathContainer[] { classpathContainer } , null);
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
	
	private static Map<String, String> getPropertiesForContainerPath(IPath containerPath) {
		Map<String, String> properties = null;
		if(containerPath.segmentCount() == 3) {
			String propsSegment = containerPath.segment(2);
			properties = readProperties(propsSegment);
		}
		return properties;
	}
	
	public static IPath createPathForContainer(FrameworkClasspathContainer container) {
		IPath path = new Path(FRAMEWORK_CONTAINER_ID);
		
		OSGiSpecLevel specLevel = container.getSpecLevel();
		if(specLevel != null) {
			path = path.append(specLevel.toString());
			if(container.isUseAnnotations()) {
				path = path.append(PROP_ANNOTATIONS_LIB + "=true");
			}
		} else {
			IFrameworkInstance instance = container.getFrameworkInstance();
			path = path.append(instance.getFrameworkId());
			
			StringBuilder builder = new StringBuilder();
			if(container.isUseAnnotations()) {
				builder.append(PROP_ANNOTATIONS_LIB).append("=true;");
			}
			
			try {
				builder.append(PROP_INSTANCE_URL).append('=');
				builder.append(URLEncoder.encode(instance.getInstancePath().toString(), "UTF-8"));
			} catch (UnsupportedEncodingException e) {
				// Can't happen
			}
			path = path.append(builder.toString());
		}
		return path;
	}
	public static FrameworkClasspathContainer createClasspathContainerForPath(IPath containerPath) {
		FrameworkClasspathContainer result = null;
		
		if(containerPath == null || containerPath.segmentCount() < 1) {
			return null;
		}
		
		Map<String, String> properties = getPropertiesForContainerPath(containerPath);
		boolean useAnnotations = (properties != null && Boolean.TRUE.toString().equals(properties.get(PROP_ANNOTATIONS_LIB)));
		
		try {
			OSGiSpecLevel specLevel = Enum.valueOf(OSGiSpecLevel.class, containerPath.segment(1));
			result = FrameworkClasspathContainer.createForSpecLevel(specLevel, useAnnotations);
			result.setPath(containerPath);
		} catch (IllegalArgumentException e) {
			String frameworkId = containerPath.segment(1);
			if(properties == null) return null;
			String instanceUrl = properties.get(PROP_INSTANCE_URL);
			if(instanceUrl == null) return null;
			
			try {
				IFramework framework = FrameworkUtils.findFramework(frameworkId);
				if(framework == null) return null;
				
				IFrameworkInstance instance = framework.createFrameworkInstance(new File(instanceUrl));
				result = FrameworkClasspathContainer.createForSpecificFramework(instance, useAnnotations);
				result.setPath(containerPath);
			} catch (CoreException ce) {
				Activator.getDefault().getLog().log(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Error creating framework instance.", ce));
				return null;
			}
		}
		return result;
	}
}
