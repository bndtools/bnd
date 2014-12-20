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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import org.bndtools.api.ILogger;
import org.bndtools.api.Logger;
import org.bndtools.utils.osgi.BundleUtils;
import org.bndtools.utils.workspace.FileUtils;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.JavaCore;
import org.osgi.framework.Constants;
import org.osgi.framework.namespace.BundleNamespace;
import org.osgi.framework.namespace.HostNamespace;
import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.framework.namespace.PackageNamespace;
import org.osgi.resource.Capability;
import org.osgi.resource.Namespace;

import aQute.bnd.build.Project;
import aQute.bnd.header.Attrs;
import aQute.bnd.header.Parameters;
import aQute.bnd.osgi.Builder;
import aQute.bnd.osgi.Clazz;
import aQute.bnd.osgi.Descriptors.PackageRef;
import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.Processor;
import aQute.bnd.osgi.resource.CapReqBuilder;
import aQute.bnd.version.VersionRange;
import aQute.lib.collections.MultiMap;
import aQute.libg.filters.AndFilter;
import aQute.libg.filters.Filter;
import aQute.libg.filters.NotFilter;
import aQute.libg.filters.Operator;
import aQute.libg.filters.SimpleFilter;
import bndtools.Plugin;
import bndtools.central.Central;
import bndtools.model.resolution.RequirementWrapper;

public class AnalyseBundleResolutionJob extends Job {
    private static final ILogger logger = Logger.getLogger(AnalyseBundleResolutionJob.class);

    private final File[] files;

    private File[] resultFileArray;
    private Map<String,List<RequirementWrapper>> requirements;
    private Map<String,List<Capability>> capabilities;

    public AnalyseBundleResolutionJob(String name, File[] files) {
        super(name);
        this.files = files;
    }

    @Override
    protected IStatus run(IProgressMonitor monitor) {
        Map<File,Builder> builderMap = new HashMap<File,Builder>();

        // Setup builders and load  all the capabilities
        Map<String,List<Capability>> capabilities = new HashMap<String,List<Capability>>();
        MultiMap<String,String> usedBy = new MultiMap<String,String>();

        for (File inputFile : files) {
            if (inputFile.exists()) {
                try {
                    Builder builder;
                    if (inputFile.getName().endsWith(".bnd")) {
                        builder = setupBuilderForBndFile(inputFile);
                    } else {
                        builder = setupBuilderForJarFile(inputFile);
                    }
                    if (builder == null)
                        continue;
                    builderMap.put(inputFile, builder);
                    loadCapabilities(capabilities, builder);
                } catch (CoreException e) {
                    logger.logError("Error in bnd resolution analysis.", e);
                } catch (Exception e) {
                    logger.logError("Error in bnd resolution analysis.", e);
                }
            }
        }

        // Load all the requirements
        Map<String,List<RequirementWrapper>> requirements = new HashMap<String,List<RequirementWrapper>>();
        for (Entry<File,Builder> entry : builderMap.entrySet()) {
            Builder builder = entry.getValue();
            try {
                loadRequirements(requirements, builder);
            } catch (Exception e) {
                logger.logError("Error in bnd resolution analysis", e);
            }
        }

        // Check for resolved requirements
        for (String namespace : requirements.keySet()) {
            List<RequirementWrapper> rws = requirements.get(namespace);
            List<Capability> candidates = capabilities.get(namespace);

            if (candidates == null)
                continue;

            for (RequirementWrapper rw : rws) {
                String filterStr = rw.requirement.getDirectives().get("filter");
                if (filterStr != null) {
                    aQute.lib.filter.Filter filter = new aQute.lib.filter.Filter(filterStr);
                    for (Capability cand : candidates) {
                        if (filter.matchMap(cand.getAttributes())) {
                            rw.resolved = true;
                            break;
                        }
                    }
                }
            }
        }

        // Generate the final results
        Set<File> resultFiles = builderMap.keySet();
        resultFileArray = resultFiles.toArray(new File[resultFiles.size()]);

        this.requirements = requirements;
        this.capabilities = capabilities;

        // Cleanup
        for (Builder builder : builderMap.values()) {
            builder.close();
        }

        // showResults(resultFileArray, importResults, exportResults);
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

    static Builder setupBuilderForBndFile(File file) throws CoreException {
        IFile[] wsfiles = FileUtils.getWorkspaceFiles(file);
        if (wsfiles == null || wsfiles.length == 0)
            throw new CoreException(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Unable to determine project owner for Bnd file: " + file.getAbsolutePath(), null));

        IProject project = wsfiles[0].getProject();

        // Calculate the manifest
        try {
            Project bndProject = Central.getInstance().getModel(JavaCore.create(project));
            if (bndProject == null)
                return null;
            Builder builder;
            if (file.getName().equals(Project.BNDFILE)) {
                builder = bndProject.getSubBuilders().iterator().next();
            } else {
                builder = bndProject.getSubBuilder(file);
            }

            if (builder == null) {
                builder = new Builder();
                builder.setProperties(file);
            }
            builder.build();
            return builder;
        } catch (Exception e) {
            throw new CoreException(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Bnd analysis failed", e));
        }
    }

    static void loadCapabilities(Map<String,List<Capability>> capMap, Builder builder) throws Exception {
        Jar jar = builder.getJar();
        if (jar == null)
            return;

        Manifest manifest = jar.getManifest();
        if (manifest == null)
            return;
        Attributes attribs = manifest.getMainAttributes();

        // Load export packages
        String exportsPkgStr = attribs.getValue(Constants.EXPORT_PACKAGE);
        Parameters exportsMap = new Parameters(exportsPkgStr);
        for (Entry<String,Attrs> entry : exportsMap.entrySet()) {
            String pkg = Processor.removeDuplicateMarker(entry.getKey());
            org.osgi.framework.Version version = org.osgi.framework.Version.parseVersion(entry.getValue().getVersion());
            CapReqBuilder cb = new CapReqBuilder(PackageNamespace.PACKAGE_NAMESPACE).addAttribute(PackageNamespace.PACKAGE_NAMESPACE, pkg).addAttribute(PackageNamespace.CAPABILITY_VERSION_ATTRIBUTE, version);
            // TODO attributes and directives
            addCapability(capMap, cb.buildSyntheticCapability());
        }

        // Load identity/bundle/host
        String bsn = BundleUtils.getBundleSymbolicName(attribs);
        if (bsn != null) { // Ignore if not a bundle
            org.osgi.framework.Version version = org.osgi.framework.Version.parseVersion(attribs.getValue(Constants.BUNDLE_VERSION));
            // TODO attributes and directives
            addCapability(capMap, new CapReqBuilder(IdentityNamespace.IDENTITY_NAMESPACE).addAttribute(IdentityNamespace.IDENTITY_NAMESPACE, bsn).addAttribute(IdentityNamespace.CAPABILITY_VERSION_ATTRIBUTE, version)
                    .buildSyntheticCapability());
            addCapability(capMap, new CapReqBuilder(BundleNamespace.BUNDLE_NAMESPACE).addAttribute(BundleNamespace.BUNDLE_NAMESPACE, bsn).addAttribute(BundleNamespace.CAPABILITY_BUNDLE_VERSION_ATTRIBUTE, version).buildSyntheticCapability());
            addCapability(capMap, new CapReqBuilder(HostNamespace.HOST_NAMESPACE).addAttribute(HostNamespace.HOST_NAMESPACE, bsn).addAttribute(HostNamespace.CAPABILITY_BUNDLE_VERSION_ATTRIBUTE, version).buildSyntheticCapability());
        }

        // Generic capabilities
        String providesStr = attribs.getValue(Constants.PROVIDE_CAPABILITY);
        Parameters provides = new Parameters(providesStr);
        for (Entry<String,Attrs> entry : provides.entrySet()) {
            String ns = entry.getKey();
            Attrs attrs = entry.getValue();

            CapReqBuilder cb = new CapReqBuilder(ns);
            for (String key : attrs.keySet()) {
                if (key.endsWith(":"))
                    cb.addDirective(key.substring(0, key.length() - 1), attrs.get(key));
                else
                    cb.addAttribute(key, attrs.getTyped(key));
            }
            addCapability(capMap, cb.buildSyntheticCapability());
        }
    }

    private static void addCapability(Map<String,List<Capability>> capMap, Capability cap) {
        List<Capability> capsForNs = capMap.get(cap.getNamespace());
        if (capsForNs == null) {
            capsForNs = new LinkedList<Capability>();
            capMap.put(cap.getNamespace(), capsForNs);
        }
        capsForNs.add(cap);
    }

    /*
    private static void mergeCapabilities(Map<String,List<ExportPackage>> exports, MultiMap<String,String> usedBy, Map<String,Set<Version>> bundleVersions, Builder builder) throws Exception {
        Jar jar = builder.getJar();
        if (jar == null)
            return;
        Manifest manifest = jar.getManifest();
        if (manifest == null)
            return;

        Attributes attribs = manifest.getMainAttributes();
        String exportPkgStr = attribs.getValue(Constants.EXPORT_PACKAGE);
        Parameters exportsMap = new Parameters(exportPkgStr);

        // Merge the exports
        Map<PackageRef,List<PackageRef>> uses = builder.getUses();
        for (Entry<String,Attrs> entry : exportsMap.entrySet()) {
            String pkgName = Processor.removeDuplicateMarker(entry.getKey());
            ExportPackage export = new ExportPackage(pkgName, entry.getValue(), uses.get(pkgName));
            List<ExportPackage> exportList = exports.get(export.getName());
            if (exportList == null) {
                exportList = new LinkedList<ExportPackage>();
                exports.put(export.getName(), exportList);
            }
            exportList.add(export);
        }

        // Merge the used-by package mappings
        Map<PackageRef,Set<PackageRef>> myUsedBy = CollectionUtils.invertMapOfCollection(uses);
        for (Entry<PackageRef,Set<PackageRef>> entry : myUsedBy.entrySet()) {
            String pkgName = entry.getKey().getFQN();
            List<String> users = getFQNList(entry.getValue());

            List<String> mainUsedBy = usedBy.get(pkgName);
            if (mainUsedBy == null) {
                usedBy.put(pkgName, users);
            } else {
                mainUsedBy.addAll(users);
            }
        }

        // Merge the bundle name + version
        String bsn = BundleUtils.getBundleSymbolicName(attribs);
        if (bsn != null) { // Ignore if not a bundle
            String versionStr = attribs.getValue(Constants.BUNDLE_VERSION);
            Version version = null;
            if (versionStr != null) {
                try {
                    version = new Version(versionStr);
                } catch (IllegalArgumentException e) {
                    logger.logError("Error parsing version of bundle: " + bsn, e);
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
    */

    private static List<String> getFQNList(Collection<PackageRef> pkgRefs) {
        List<String> result = new ArrayList<String>(pkgRefs.size());
        for (PackageRef pkgRef : pkgRefs) {
            result.add(pkgRef.getFQN());
        }
        return result;
    }

    static void loadRequirements(Map<String,List<RequirementWrapper>> requirements, Builder builder) throws Exception {
        Jar jar = builder.getJar();
        if (jar == null)
            return;
        Manifest manifest = jar.getManifest();
        if (manifest == null)
            return;
        Attributes attribs = manifest.getMainAttributes();

        // Process imports
        String importPkgStr = attribs.getValue(Constants.IMPORT_PACKAGE);
        Parameters importsMap = new Parameters(importPkgStr);
        for (Entry<String,Attrs> entry : importsMap.entrySet()) {
            String pkgName = Processor.removeDuplicateMarker(entry.getKey());
            Attrs attrs = entry.getValue();

            CapReqBuilder rb = new CapReqBuilder(PackageNamespace.PACKAGE_NAMESPACE);
            String filter = createVersionFilter(PackageNamespace.PACKAGE_NAMESPACE, pkgName, attrs.get(Constants.VERSION_ATTRIBUTE), PackageNamespace.CAPABILITY_VERSION_ATTRIBUTE);
            rb.addDirective(PackageNamespace.REQUIREMENT_FILTER_DIRECTIVE, filter);
            if (Constants.RESOLUTION_OPTIONAL.equals(attrs.get(Constants.RESOLUTION_DIRECTIVE + ":")))
                rb.addDirective(Namespace.REQUIREMENT_RESOLUTION_DIRECTIVE, Namespace.RESOLUTION_OPTIONAL);

            Collection<Clazz> importers = findImportingClasses(pkgName, builder);

            RequirementWrapper rw = new RequirementWrapper();
            rw.requirement = rb.buildSyntheticRequirement();
            rw.requirers = importers;

            addRequirement(requirements, rw);
        }

        // Process require-bundle
        String requireBundleStr = attribs.getValue(Constants.REQUIRE_BUNDLE);
        Parameters requireBundles = new Parameters(requireBundleStr);
        for (Entry<String,Attrs> entry : requireBundles.entrySet()) {
            String bsn = Processor.removeDuplicateMarker(entry.getKey());
            Attrs attrs = entry.getValue();

            CapReqBuilder rb = new CapReqBuilder(BundleNamespace.BUNDLE_NAMESPACE);
            String filter = createVersionFilter(BundleNamespace.BUNDLE_NAMESPACE, bsn, attrs.get(Constants.BUNDLE_VERSION_ATTRIBUTE), BundleNamespace.CAPABILITY_BUNDLE_VERSION_ATTRIBUTE);
            rb.addDirective(BundleNamespace.REQUIREMENT_FILTER_DIRECTIVE, filter);
            if (Constants.RESOLUTION_OPTIONAL.equals(attrs.get(Constants.RESOLUTION_DIRECTIVE + ":")))
                rb.addDirective(Namespace.REQUIREMENT_RESOLUTION_DIRECTIVE, Namespace.RESOLUTION_OPTIONAL);

            RequirementWrapper rw = new RequirementWrapper();
            rw.requirement = rb.buildSyntheticRequirement();
            addRequirement(requirements, rw);
        }

        // Process generic requires
        String requiresStr = attribs.getValue(Constants.REQUIRE_CAPABILITY);
        Parameters requires = new Parameters(requiresStr);
        for (Entry<String,Attrs> entry : requires.entrySet()) {
            String ns = Processor.removeDuplicateMarker(entry.getKey());
            Attrs attrs = entry.getValue();

            CapReqBuilder rb = new CapReqBuilder(ns);
            for (String key : attrs.keySet()) {
                if (key.endsWith(":"))
                    rb.addDirective(key.substring(0, key.length() - 1), attrs.get(key));
                else
                    rb.addAttribute(key, attrs.getTyped(key));
            }

            RequirementWrapper rw = new RequirementWrapper();
            rw.requirement = rb.buildSyntheticRequirement();
            addRequirement(requirements, rw);
        }
    }

    private static final String createVersionFilter(String ns, String value, String rangeStr, String versionAttr) {
        SimpleFilter pkgNameFilter = new SimpleFilter(ns, value);

        Filter filter = pkgNameFilter;
        if (rangeStr != null) {
            VersionRange range = new VersionRange(rangeStr);

            Filter left;
            if (range.includeLow())
                left = new SimpleFilter(versionAttr, Operator.GreaterThanOrEqual, range.getLow().toString());
            else
                left = new NotFilter(new SimpleFilter(versionAttr, Operator.LessThanOrEqual, range.getLow().toString()));

            Filter right;
            if (!range.isRange())
                right = null;
            else if (range.includeHigh())
                right = new SimpleFilter(versionAttr, Operator.LessThanOrEqual, range.getHigh().toString());
            else
                right = new NotFilter(new SimpleFilter(versionAttr, Operator.GreaterThanOrEqual, range.getHigh().toString()));

            AndFilter combined = new AndFilter().addChild(pkgNameFilter).addChild(left);
            if (right != null)
                combined.addChild(right);
            filter = combined;
        }
        return filter.toString();
    }

    private static void addRequirement(Map<String,List<RequirementWrapper>> requirements, RequirementWrapper req) {
        List<RequirementWrapper> listForNs = requirements.get(req.requirement.getNamespace());
        if (listForNs == null) {
            listForNs = new LinkedList<RequirementWrapper>();
            requirements.put(req.requirement.getNamespace(), listForNs);
        }
        listForNs.add(req);
    }

    static List<Clazz> findImportingClasses(String pkgName, Builder builder) {
        List<Clazz> classes = new LinkedList<Clazz>();
        try {
            Collection<Clazz> importers = builder.getClasses("", "IMPORTING", pkgName);

            // Remove *this* package
            for (Clazz clazz : importers) {
                String fqn = clazz.getFQN();
                int dot = fqn.lastIndexOf('.');
                if (dot >= 0) {
                    String pkg = fqn.substring(0, dot);
                    if (!pkgName.equals(pkg))
                        classes.add(clazz);
                }
            }
        } catch (Exception e) {
            logger.logError("Error querying importing classes.", e);
        }
        return classes;
    }

    public File[] getResultFileArray() {
        return resultFileArray;
    }

    public Map<String,List<RequirementWrapper>> getRequirements() {
        return Collections.unmodifiableMap(requirements);
    }

    public Map<String,List<Capability>> getCapabilities() {
        return Collections.unmodifiableMap(capabilities);
    }
}
