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
import java.io.IOException;
import java.net.URL;

import name.neilbartlett.eclipse.bndtools.frameworks.IFrameworkInstance;
import name.neilbartlett.eclipse.bndtools.frameworks.OSGiSpecLevel;
import name.neilbartlett.eclipse.bndtools.prefs.frameworks.FrameworkPreferencesInitializer;
import name.neilbartlett.eclipse.bndtools.utils.BundleUtils;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IAccessRule;
import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.JavaCore;
import org.osgi.framework.Bundle;
import org.osgi.framework.Version;

import aQute.libg.version.VersionRange;

public class FrameworkClasspathContainer implements IClasspathContainer {
	
	private static final String ANNOTATIONS_SYMBOLIC_NAME = "biz.aQute.annotation";
	private static final IClasspathEntry[] EMPTY_ENTRIES = new IClasspathEntry[0];
	
	private IPath path;
	
	private final OSGiSpecLevel specLevel;
	private final IFrameworkInstance frameworkInstance;
	private final boolean useAnnotations;
	
	private IClasspathEntry annotationsEntry = null;
	
	private FrameworkClasspathContainer(OSGiSpecLevel specLevel, IFrameworkInstance frameworkInstance, boolean annotations) {
		this.specLevel = specLevel;
		this.frameworkInstance = frameworkInstance;
		this.useAnnotations = annotations;
	}
	
	private static IPath getAnnotationsPath() {
		IPath bundleLocation = BundleUtils.getBundleLocation(ANNOTATIONS_SYMBOLIC_NAME, new VersionRange("[0.0.384,1)"));
		return bundleLocation;
		/*
		@SuppressWarnings("restriction")
		BundleInfo annotationsBundle = P2Utils.findBundle(ANNOTATIONS_SYMBOLIC_NAME, ANNOTATIONS_VERSION, false);
		IPath annotsPath = P2Utils.getBundleLocationPath(annotationsBundle);
		return annotsPath;
		*/
	}
	
	public static final FrameworkClasspathContainer createForSpecLevel(OSGiSpecLevel specLevel, boolean annotations) {
		IFrameworkInstance frameworkInstance = FrameworkPreferencesInitializer.getFrameworkInstance(specLevel);
		if(frameworkInstance == null)
			return null;
		
		return new FrameworkClasspathContainer(specLevel, frameworkInstance, annotations);
	}
	
	public static final FrameworkClasspathContainer createForSpecificFramework(IFrameworkInstance instance, boolean annotations) {
		return new FrameworkClasspathContainer(null, instance, annotations);
	}
	
	private IClasspathEntry getAnnotationsEntry() {
		if(useAnnotations && annotationsEntry == null) {
			IPath path = getAnnotationsPath();
			if(path != null)
				annotationsEntry = JavaCore.newLibraryEntry(path, null, null, new IAccessRule[0], new IClasspathAttribute[0], false);
		}
		return annotationsEntry;
	}
	
	public IClasspathEntry[] getClasspathEntries() {
		IClasspathEntry[] entries = EMPTY_ENTRIES;
		
		if(frameworkInstance != null && frameworkInstance.getStatus().isOK())
			entries = frameworkInstance.getClasspathEntries();
		
		IClasspathEntry annotations = getAnnotationsEntry();
		if(annotations != null) {
			IClasspathEntry[] copy = new IClasspathEntry[entries.length + 1];
			System.arraycopy(entries, 0, copy, 0, entries.length);
			copy[entries.length] = annotations;
			entries = copy;
		}
		
		return entries;
	}
	
	public OSGiSpecLevel getSpecLevel() {
		return specLevel;
	}
	
	public IFrameworkInstance getFrameworkInstance() {
		return frameworkInstance;
	}
	
	public boolean isUseAnnotations() {
		return useAnnotations;
	}
	
	public String getDescription() {
		if(specLevel != null) {
			return "OSGi Framework " + specLevel.getFormattedName();
		} else {
			return String.format("OSGi Framework (%s)", frameworkInstance.getDisplayString());
		}
	}

	public int getKind() {
		return IClasspathContainer.K_APPLICATION;
	}
	
	void setPath(IPath path) {
		this.path = path;
	}

	public IPath getPath() {
		return path;
	}

}