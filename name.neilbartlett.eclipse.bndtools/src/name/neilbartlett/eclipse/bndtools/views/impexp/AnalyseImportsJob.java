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
package name.neilbartlett.eclipse.bndtools.views.impexp;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.Map.Entry;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import name.neilbartlett.eclipse.bndtools.Plugin;
import name.neilbartlett.eclipse.bndtools.utils.BndFileClasspathCalculator;
import name.neilbartlett.eclipse.bndtools.utils.CollectionUtils;
import name.neilbartlett.eclipse.bndtools.utils.IClasspathCalculator;
import name.neilbartlett.eclipse.bndtools.utils.ProjectClasspathCalculator;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IViewReference;
import org.eclipse.ui.IWorkbenchPage;

import aQute.lib.osgi.Analyzer;
import aQute.lib.osgi.Builder;
import aQute.lib.osgi.Clazz;
import aQute.lib.osgi.Constants;
import aQute.lib.osgi.Jar;
import aQute.lib.osgi.Processor;

public class AnalyseImportsJob extends Job {

	private final IFile file;
	private final IWorkbenchPage page;

	public AnalyseImportsJob(String name, IFile file, IWorkbenchPage page) {
		super(name);
		this.file = file;
		this.page = page;
	}

	@Override
	protected IStatus run(IProgressMonitor monitor) {
		Builder builder = new Builder();
		try {
			if(file.getName().endsWith(".bnd")) {
				setupBuilderForBndFile(builder);
			} else {
				setupBuilderForJarFile(builder);
			}
			showResult(builder);
			return Status.OK_STATUS;
		} catch (CoreException e) {
			return new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Error loading properties.", e);
		} catch (IOException e) {
			return new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Error loading properties.", e);
		} finally {
			builder.close();
		}
	}
	
	void setupBuilderForJarFile(Analyzer analyzer) throws IOException, CoreException {
		Jar jar = new Jar(file.getName(), file.getLocation().toFile());
		analyzer.setJar(jar);
		try {
			analyzer.analyze();
		} catch (Exception e) {
			throw new CoreException(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Bnd analysis failed", e));
		}
	}
	
	void setupBuilderForBndFile(Builder builder) throws IOException, CoreException {
		// Read the properties
		Properties props = new Properties();
		InputStream content = file.getContents();
		props.load(content);
		builder.setProperties(props);
		
		// Set up the builder classpath
		IClasspathCalculator classpathCalculator;
		String classpathStr = builder.getProperty(Constants.CLASSPATH);
		if(classpathStr != null) {
			classpathCalculator = new BndFileClasspathCalculator(classpathStr, file.getWorkspace().getRoot(), file.getFullPath());
		} else {
			classpathCalculator = new ProjectClasspathCalculator(JavaCore.create(file.getProject()));
		}
		builder.setClasspath(classpathCalculator.classpathAsFiles().toArray(new File[0]));
		
		// Calculate the manifest
		try {
			builder.build();
		} catch (Exception e) {
			throw new CoreException(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Bnd analysis failed", e));
		}
	}
	protected void showResult(Builder builder) throws IOException {
		Jar jar = builder.getJar();
		Manifest manifest = jar.getManifest();
		if(manifest == null)
			return;
		
		Attributes attribs = manifest.getMainAttributes();
		final Map<String, Map<String, String>> importsMap = Processor.parseHeader(attribs.getValue(Constants.IMPORT_PACKAGE), null);
		final Map<String, Map<String, String>> exportsMap = Processor.parseHeader(attribs.getValue(Constants.EXPORT_PACKAGE), null);
		
		Map<String, Set<String>> uses = builder.getUses();
		Map<String, Set<String>> usedBy = CollectionUtils.invertMapOfCollection(uses);
		
		final List<ExportPackage> exports = new ArrayList<ExportPackage>(exportsMap.size());
		for (Entry<String,Map<String,String>> entry : exportsMap.entrySet()) {
			ExportPackage export = new ExportPackage(entry.getKey(), entry.getValue(), uses.get(entry.getKey()));
			exports.add(export);
		}
		
		// Now do the imports
		final List<ImportPackage> imports = new ArrayList<ImportPackage>();
		for(Entry<String,Map<String,String>> entry : importsMap.entrySet()) {
			Collection<Clazz> classes = builder.getClasses("", "IMPORTING", entry.getKey());
			Map<String, List<Clazz>> classMap = new HashMap<String, List<Clazz>>();
			
			for (Clazz clazz : classes) {
				String fqn = clazz.getFQN();
				int index = fqn.lastIndexOf('.');
				if (index < 0)
					continue;
				String pkg = fqn.substring(0, index);
				
				List<Clazz> list = classMap.get(pkg);
				if(list == null) {
					list = new LinkedList<Clazz>();
					classMap.put(pkg, list);
				}
				list.add(clazz);
			}
			imports.add(new ImportPackage(entry.getKey(), exportsMap.containsKey(entry.getKey()), entry.getValue(), usedBy.get(entry.getKey()), classMap));
		}
		
		
		Display display = page.getWorkbenchWindow().getShell().getDisplay();
		display.asyncExec(new Runnable() {
			public void run() {
				IViewReference viewRef = page.findViewReference(ImportsExportsView.VIEW_ID);
				if(viewRef != null) {
					ImportsExportsView view = (ImportsExportsView) viewRef.getView(false);
					if(view != null) {
						view.setInput(file, imports, exports);
					}
				}
			}
		});
	}
}
