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
import aQute.libg.version.Version;
import aQute.libg.version.VersionRange;

public class AnalyseImportsJob extends Job {

	private final IFile[] files;
	private final IWorkbenchPage page;

	public AnalyseImportsJob(String name, IFile[] files, IWorkbenchPage page) {
		super(name);
		this.files = files;
		this.page = page;
	}

	@Override
	protected IStatus run(IProgressMonitor monitor) {
		Map<IFile, Builder> builderMap = new HashMap<IFile, Builder>();
		
		// Setup builders & all exports & used-by mappings
		Map<String, List<ExportPackage>> exports = new HashMap<String, List<ExportPackage>>();
		Map<String, Set<String>> usedBy = new HashMap<String, Set<String>>();
		for (IFile inputFile : files) {
			try {
				Builder builder = new Builder();
				if(inputFile.getName().endsWith(".bnd")) {
					setupBuilderForBndFile(inputFile, builder);
				} else {
					setupBuilderForJarFile(inputFile, builder);
				}
				builderMap.put(inputFile, builder);
				
				mergeExports(exports, usedBy, builder);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (CoreException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		// Calculate the imports
		Map<String, List<ImportPackage>> imports = new HashMap<String, List<ImportPackage>>();
		for (Entry<IFile, Builder> entry : builderMap.entrySet()) {
			Builder builder = entry.getValue();
			
			try {
				mergeImports(imports, exports, usedBy, builder);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		// Generate the final results
		Set<IFile> resultFiles = builderMap.keySet();
		IFile[] resultFileArray = (IFile[]) resultFiles.toArray(new IFile[resultFiles.size()]);
		
		List<ImportPackage> importResults = new ArrayList<ImportPackage>();
		for (List<ImportPackage> list : imports.values()) {
			importResults.addAll(list);
		}
		List<ExportPackage> exportResults = new ArrayList<ExportPackage>();
		for (List<ExportPackage> list : exports.values()) {
			exportResults.addAll(list);
		}
		
		showResults(resultFileArray, importResults, exportResults);
		return Status.OK_STATUS;
	}
	
	static void setupBuilderForJarFile(IFile file, Analyzer analyzer) throws IOException, CoreException {
		Jar jar = new Jar(file.getName(), file.getLocation().toFile());
		analyzer.setJar(jar);
		try {
			analyzer.analyze();
		} catch (Exception e) {
			throw new CoreException(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Bnd analysis failed", e));
		}
	}
	
	static void setupBuilderForBndFile(IFile file, Builder builder) throws IOException, CoreException {
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
	void mergeExports(Map<String, List<ExportPackage>> exports, Map<String, Set<String>> usedBy, Builder builder) throws IOException {
		Jar jar = builder.getJar();
		Manifest manifest = jar.getManifest();
		if(manifest == null)
			return;
		
		Attributes attribs = manifest.getMainAttributes();
		Map<String, Map<String, String>> exportsMap = Processor.parseHeader(attribs.getValue(Constants.EXPORT_PACKAGE), null);
		
		// Merge the exports
		Map<String, Set<String>> uses = builder.getUses();
		for(Entry<String, Map<String, String>> entry : exportsMap.entrySet()) {
			ExportPackage export = new ExportPackage(entry.getKey(), entry.getValue(), uses.get(entry.getKey()));
			List<ExportPackage> exportList = exports.get(export.getName());
			if(exportList == null) {
				exportList = new LinkedList<ExportPackage>();
				exports.put(export.getName(), exportList);
			}
			exportList.add(export);
		}
		
		// Merge the used-by package mappings
		Map<String, Set<String>> myUsedBy = CollectionUtils.invertMapOfCollection(uses);
		for (Entry<String, Set<String>> entry : myUsedBy.entrySet()) {
			String packageName = entry.getKey();
			Set<String> mainUsedBy = usedBy.get(packageName);
			if(mainUsedBy == null) {
				usedBy.put(packageName, entry.getValue());
			} else {
				mainUsedBy.addAll(entry.getValue());
			}
		}
	}
	void mergeImports(Map<String, List<ImportPackage>> imports, Map<String, List<ExportPackage>> exports, Map<String, Set<String>> usedBy, Builder builder) throws IOException {
		Jar jar = builder.getJar();
		Manifest manifest = jar.getManifest();
		if(manifest == null)
			return;
		
		Attributes attribs = manifest.getMainAttributes();
		final Map<String, Map<String, String>> importsMap = Processor.parseHeader(attribs.getValue(Constants.IMPORT_PACKAGE), null);

		for(Entry<String, Map<String, String>> entry : importsMap.entrySet()) {
			String pkgName = entry.getKey();
			Map<String, String> importAttribs = entry.getValue();
			
			// Calculate the importing classes for this import
			Collection<Clazz> classes = builder.getClasses("", "IMPORTING", pkgName);
			Map<String, List<Clazz>> classMap = new HashMap<String, List<Clazz>>();
			for(Clazz clazz : classes) {
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
			
			// Check if this is a self-import
			boolean selfImport = false;
			List<ExportPackage> matchingExports = exports.get(pkgName);
			if(matchingExports != null) {
				String versionRangeStr = importAttribs.get(Constants.VERSION_ATTRIBUTE);
				VersionRange versionRange = (versionRangeStr != null) ? new VersionRange(versionRangeStr) : new VersionRange("0");
				for (ExportPackage export : matchingExports) {
					String versionStr = export.getAttribs().get(Constants.VERSION_ATTRIBUTE);
					Version version = (versionStr != null) ? new Version(versionStr) : new Version(0);
					if(versionRange.includes(version)) {
						selfImport = true;
						break;
					}
				}
			}
			
			ImportPackage importPackage = new ImportPackage(pkgName, selfImport, importAttribs, usedBy.get(pkgName), classMap);
			List<ImportPackage> importList = imports.get(pkgName);
			if(importList == null) {
				importList = new LinkedList<ImportPackage>();
				imports.put(pkgName, importList);
			}
			importList.add(importPackage);
		}
	}
	void showResults(final IFile[] files, final List<ImportPackage> imports, final List<ExportPackage> exports) {
		Display display = page.getWorkbenchWindow().getShell().getDisplay();
		display.asyncExec(new Runnable() {
			public void run() {
				IViewReference viewRef = page.findViewReference(ImportsExportsView.VIEW_ID);
				if(viewRef != null) {
					ImportsExportsView view = (ImportsExportsView) viewRef.getView(false);
					if(view != null) {
						view.setInput(files, imports, exports);
					}
				}
			}
		});
	}
}
