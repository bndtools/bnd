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
package bndtools.tasks;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.JavaCore;

import aQute.bnd.build.Project;
import aQute.lib.osgi.Builder;
import aQute.lib.osgi.Clazz;
import aQute.lib.osgi.Constants;
import aQute.lib.osgi.Jar;
import aQute.lib.osgi.Processor;
import aQute.libg.version.Version;
import aQute.libg.version.VersionRange;
import bndtools.Plugin;
import bndtools.model.importanalysis.ExportPackage;
import bndtools.model.importanalysis.ImportPackage;
import bndtools.model.importanalysis.RequiredBundle;
import bndtools.utils.BundleUtils;
import bndtools.utils.CollectionUtils;
import bndtools.utils.FileUtils;

public class AnalyseBundleResolutionJob extends Job {

	private final File[] files;

    private File[] resultFileArray;
    private ArrayList<ImportPackage> importResults;
    private ArrayList<ExportPackage> exportResults;
    private ArrayList<RequiredBundle> requiredBundleResults;

	public AnalyseBundleResolutionJob(String name, File[] files) {
		super(name);
		this.files = files;
	}

	@Override
	protected IStatus run(IProgressMonitor monitor) {
		Map<File, Builder> builderMap = new HashMap<File, Builder>();

		// Setup builders and merge together all the capabilities
		Map<String, List<ExportPackage>> exports = new HashMap<String, List<ExportPackage>>();
		Map<String, Set<String>> usedBy = new HashMap<String, Set<String>>();
		Map<String, Set<Version>> bundleVersions = new HashMap<String, Set<Version>>();
		for (File inputFile : files) {
		    if(inputFile.exists()) {
    			try {
    				Builder builder;
    				if(inputFile.getName().endsWith(".bnd")) {
    					builder = setupBuilderForBndFile(inputFile);
    				} else {
    					builder = setupBuilderForJarFile(inputFile);
    				}
    				builderMap.put(inputFile, builder);
    				mergeCapabilities(exports, usedBy, bundleVersions, builder);
    			} catch (CoreException e) {
    			    // TODO Auto-generated catch block
    			    e.printStackTrace();
    			} catch (Exception e) {
    				// TODO Auto-generated catch block
    				e.printStackTrace();
    			}
		    }
		}

		// Merge together all the requirements, with access to the available capabilities
		Map<String, List<ImportPackage>> imports = new HashMap<String, List<ImportPackage>>();
		Map<String, List<RequiredBundle>> requiredBundles = new HashMap<String, List<RequiredBundle>>();
		for (Entry<File, Builder> entry : builderMap.entrySet()) {
			Builder builder = entry.getValue();

			try {
				mergeRequirements(imports, exports, usedBy, requiredBundles, bundleVersions, builder);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		// Generate the final results
		Set<File> resultFiles = builderMap.keySet();
		resultFileArray = resultFiles.toArray(new File[resultFiles.size()]);

		importResults = new ArrayList<ImportPackage>();
		for (List<ImportPackage> list : imports.values()) {
			importResults.addAll(list);
		}
		exportResults = new ArrayList<ExportPackage>();
		for (List<ExportPackage> list : exports.values()) {
			exportResults.addAll(list);
		}
		requiredBundleResults = new ArrayList<RequiredBundle>();
		for(List<RequiredBundle> list : requiredBundles.values()) {
		    requiredBundleResults.addAll(list);
		}

		//showResults(resultFileArray, importResults, exportResults);
		return Status.OK_STATUS;
	}

	static Builder setupBuilderForJarFile(File file) throws IOException, CoreException {
		Builder builder = new Builder();
		Jar jar = new Jar(file);
		builder.setJar(jar);
		try {
			builder.analyze();
		} catch (Exception e) {
			throw new CoreException(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Bnd analysis failed", e));
		}
		return builder;
	}

	static Builder setupBuilderForBndFile(File file) throws IOException, CoreException {
        IFile[] wsfiles = FileUtils.getWorkspaceFiles(file);
        if (wsfiles == null || wsfiles.length == 0)
            throw new CoreException(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Unable to determine project owner for Bnd file: " + file.getAbsolutePath(),
                    null));

        IProject project = wsfiles[0].getProject();

		// Calculate the manifest
		try {
			Project bndProject = Plugin.getDefault().getCentral().getModel(JavaCore.create(project));
			Builder builder;
			if(file.getName().equals(Project.BNDFILE)) {
			    builder = bndProject.getSubBuilders().iterator().next();
			} else {
			    builder = bndProject.getSubBuilder(file);
			}

			if(builder == null) {
			    builder = new Builder();
			    builder.setProperties(file);
			}
			builder.build();
			return builder;
		} catch (Exception e) {
			throw new CoreException(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Bnd analysis failed", e));
		}
	}
	void mergeCapabilities(Map<String, List<ExportPackage>> exports, Map<String, Set<String>> usedBy, Map<String, Set<Version>> bundleVersions, Builder builder) throws Exception {
		Jar jar = builder.getJar();
		if (jar == null)
		    return;
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

        // Merge the bundle name + version
        String bsn = BundleUtils.getBundleSymbolicName(attribs);
        if(bsn != null) { // Ignore if not a bundle
            String versionStr = attribs.getValue(Constants.BUNDLE_VERSION);
            Version version = null;
            if (versionStr != null) {
                try {
                    version = new Version(versionStr);
                } catch (IllegalArgumentException e) {
                    Plugin.logError("Error parsing version of bundle: " + bsn, e);
                }
            }
            if (version == null)
                version = new Version(0);
            Set<Version> versions = bundleVersions.get(bsn);
            if (versions == null) {
                versions = new HashSet<Version>();
                bundleVersions.put(bsn, versions);
            }
            versions.add(version);
        }
	}
	void mergeRequirements(Map<String, List<ImportPackage>> imports, Map<String, List<ExportPackage>> exports, Map<String, Set<String>> usedBy,
	        Map<String, List<RequiredBundle>> requiredBundles, Map<String, Set<Version>> bundleVersions, Builder builder) throws Exception {
		Jar jar = builder.getJar();
		Manifest manifest = jar.getManifest();
		if(manifest == null)
			return;
		Attributes attribs = manifest.getMainAttributes();

		// Process imports
		final Map<String, Map<String, String>> importsMap = Processor.parseHeader(attribs.getValue(Constants.IMPORT_PACKAGE), null);
		for(Entry<String, Map<String, String>> entry : importsMap.entrySet()) {
			String pkgName = entry.getKey();
			Map<String, String> importAttribs = entry.getValue();

			// Calculate the importing classes for this import
			Map<String, List<Clazz>> classMap = new HashMap<String, List<Clazz>>();
			Collection<Clazz> classes = Collections.emptyList();
			try {
				classes = builder.getClasses("", "IMPORTING", pkgName);
			} catch (Exception e) {
				Plugin.log(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Error querying importing classes.", e));
			}
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

		// Process require-bundles
        final Map<String, Map<String, String>> requiredBundleMap = Processor.parseHeader(attribs.getValue(Constants.REQUIRE_BUNDLE), null);
        for(Entry<String, Map<String, String>> entry : requiredBundleMap.entrySet()) {
            String name = entry.getKey();
            Map<String, String> rbAttribs = entry.getValue();

            // Check if the required bundle is already included in the closure
            boolean satisfied = false;
            Set<Version> includedVersions = bundleVersions.get(name);
            if(includedVersions != null) {
                String versionRangeStr = rbAttribs.get(Constants.BUNDLE_VERSION_ATTRIBUTE);
                VersionRange versionRange = (versionRangeStr != null) ? new VersionRange(versionRangeStr) : new VersionRange("0");
                for (Version includedVersion : includedVersions) {
                    if(versionRange.includes(includedVersion)) {
                        satisfied = true;
                        break;
                    }
                }
            }

            RequiredBundle rb = new RequiredBundle(name, rbAttribs, satisfied);
            List<RequiredBundle> rbList = requiredBundles.get(name);
            if(rbList == null) {
                rbList = new LinkedList<RequiredBundle>();
                requiredBundles.put(name, rbList);
            }
            rbList.add(rb);
        }
	}
	/*
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
	*/

	public File[] getResultFileArray() {
        return resultFileArray;
    }

	public List<ImportPackage> getImportResults() {
        return importResults;
    }

	public List<ExportPackage> getExportResults() {
        return exportResults;
    }

    public List<RequiredBundle> getRequiredBundles() {
        return requiredBundleResults;
    }
}
