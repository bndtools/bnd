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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.Map.Entry;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import name.neilbartlett.eclipse.bndtools.Plugin;
import name.neilbartlett.eclipse.bndtools.utils.BndFileClasspathCalculator;
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

import aQute.lib.osgi.Builder;
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
		try {
			Manifest manifest;
			if(file.getName().endsWith(".bnd")) {
				manifest = getManifestForBndfile();
			} else {
				Jar jar = null;
				try {
					jar = new Jar(file.getName(), file.getLocation().toFile());
					manifest = jar.getManifest();
				} finally {
					if(jar != null) jar.close();
				}
			}
			showManifest(manifest);
			return Status.OK_STATUS;
		} catch (CoreException e) {
			return new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Error loading properties.", e);
		} catch (IOException e) {
			return new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Error loading properties.", e);
		}
	}
	
	Manifest getManifestForBndfile() throws IOException, CoreException {
		Builder builder = new Builder();
		
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
			Jar jar = builder.getJar();
			return jar.getManifest();
		} catch (Exception e) {
			throw new CoreException(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Bnd analysis failed", e));
		} finally {
			builder.close();
		}
	}
	protected void showManifest(Manifest manifest) throws IOException {
		if(manifest != null) {
			Attributes attribs = manifest.getMainAttributes();
			final Map<String, Map<String, String>> importsMap = Processor.parseHeader(attribs.getValue(Constants.IMPORT_PACKAGE), null);
			final Map<String, Map<String, String>> exportsMap = Processor.parseHeader(attribs.getValue(Constants.EXPORT_PACKAGE), null);
			importsMap.keySet().removeAll(exportsMap.keySet());
			
			
			// Work out the exports, remembering their using-imports as we go.
			Map<String, Set<String>> usedByMap = new HashMap<String, Set<String>>();
			final List<ExportPackage> exports = new ArrayList<ExportPackage>(exportsMap.size());
			for (Entry<String,Map<String,String>> entry : exportsMap.entrySet()) {
				ExportPackage export = new ExportPackage(entry.getKey(), entry.getValue());
				exports.add(export);
				List<String> uses = export.getUses();
				if(uses != null) {
					for (String importName : uses) {
						Set<String> importUsedBy = usedByMap.get(importName);
						if(importUsedBy == null ) {
							importUsedBy = new TreeSet<String>();
							usedByMap.put(importName, importUsedBy);
						}
						importUsedBy.add(export.getName());
					}
				}
			}
			
			// Now do the imports
			final List<ImportPackage> imports = new ArrayList<ImportPackage>();
			for(Entry<String,Map<String,String>> entry : importsMap.entrySet()) {
				Set<String> usedBy = usedByMap.get(entry.getKey());
				imports.add(new ImportPackage(entry.getKey(), entry.getValue(), usedBy));
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
}
