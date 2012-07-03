package org.bndtools.core.obr;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;

import org.apache.felix.bundlerepository.Capability;
import org.apache.felix.bundlerepository.Repository;
import org.apache.felix.bundlerepository.Resolver;
import org.apache.felix.bundlerepository.Resource;
import org.apache.felix.bundlerepository.impl.DataModelHelperImpl;
import org.apache.felix.bundlerepository.impl.RepositoryAdminImpl;
import org.apache.felix.utils.log.Logger;
import org.bndtools.core.obr.model.PullParser;
import org.bndtools.core.obr.model.Referral;
import org.bndtools.core.obr.model.RepositoryImpl;
import org.bndtools.core.utils.filters.ObrConstants;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.osgi.framework.Constants;

import aQute.bnd.build.Container;
import aQute.bnd.build.Container.TYPE;
import aQute.bnd.build.Project;
import aQute.bnd.build.Workspace;
import aQute.bnd.build.model.clauses.ExportedPackage;
import aQute.bnd.build.model.clauses.VersionedClause;
import aQute.bnd.service.IndexProvider;
import aQute.bnd.service.RemoteRepositoryPlugin;
import aQute.bnd.service.RepositoryPlugin;
import aQute.bnd.service.RepositoryPlugin.Strategy;
import aQute.bnd.service.ResolutionPhase;
import aQute.bnd.service.url.URLConnector;
import aQute.lib.deployer.repository.CachingUriResourceHandle;
import aQute.lib.deployer.repository.CachingUriResourceHandle.CachingMode;
import aQute.lib.io.IO;
import aQute.lib.osgi.Builder;
import aQute.lib.osgi.Processor;
import aQute.libg.header.Attrs;
import aQute.libg.header.Parameters;
import aQute.libg.version.Version;
import bndtools.BndConstants;
import bndtools.Central;
import bndtools.Plugin;
import bndtools.api.EE;
import bndtools.api.IBndModel;

public class ResolveOperation implements IRunnableWithProgress {

    private final DataModelHelperImpl helper = new DataModelHelperImpl();

    private final IFile runFile;
    private final IBndModel model;

    private ObrResolutionResult result = null;

    public ResolveOperation(IFile runFile, IBndModel model) {
        this.runFile = runFile;
        this.model = model;
    }

    public void run(IProgressMonitor monitor) {
        @SuppressWarnings("unused")
        SubMonitor progress = SubMonitor.convert(monitor, Messages.ResolveOperation_progressLabel, 0);

        MultiStatus status = new MultiStatus(Plugin.PLUGIN_ID, 0, Messages.ResolveOperation_errorOverview, null);

        // Get the repositories
        List<IndexProvider> indexProviders;
        try {
            indexProviders = loadIndexProviders();
        } catch (Exception e) {
            status.add(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, Messages.ResolveOperation_errorLoadingIndexes, e));
            result = createErrorResult(status);
            return;
        }

        // Create the dummy system bundle and repository admin
        File frameworkFile = findFramework(status);
        if (frameworkFile == null) {
            result = createErrorResult(status);
            return;
        }
        DummyBundleContext bundleContext;
        try {
            bundleContext = new DummyBundleContext(frameworkFile);
        } catch (IOException e) {
            status.add(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, Messages.ResolveOperation_errorReadingSystemBundleManifest, e));
            result = createErrorResult(status);
            return;
        }

        // Load repository indexes
        List<Repository> repos = new LinkedList<Repository>();
        for (IndexProvider prov : indexProviders) {
            String repoName;
            if (prov instanceof RepositoryPlugin) {
                RepositoryPlugin repo = (RepositoryPlugin) prov;
                repoName = repo.getName();
            } else {
                repoName = prov.toString();
            }

            File cacheDir;
            if (prov instanceof RemoteRepositoryPlugin) {
                cacheDir = ((RemoteRepositoryPlugin) prov).getCacheDirectory();
            } else {
                cacheDir = Plugin.getDefault().getStateLocation().toFile();
            }

            try {
                for (URI indexUrl : prov.getIndexLocations()) {
                    addRepository(indexUrl.toURL(), repos, cacheDir);
                }
            } catch (Exception e) {
                status.add(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, Messages.ResolveOperation_errorProcessingIndex + repoName, e));
            }
        }

        RepositoryAdminImpl repoAdmin = new RepositoryAdminImpl(bundleContext, new Logger(Plugin.getDefault().getBundleContext()));

        repos.add(0, repoAdmin.getLocalRepository()); // BUG? Calling
                                                      // `resolver(Repository[])`
                                                      // excludes the local
                                                      // and system repos!
        repos.add(0, repoAdmin.getSystemRepository());
        Resolver resolver = repoAdmin.resolver(repos.toArray(new Repository[repos.size()]));

        // Add project builders
        @SuppressWarnings("unused")
        Set<Resource> projectBuildResources = addProjectBuildBundles(resolver);

        // Add EE capabilities
        EE ee = model.getEE();
        if (ee == null)
            ee = EE.JavaSE_1_6;
        resolver.addGlobalCapability(createEeCapability(ee));
        for (EE compat : ee.getCompatible()) {
            resolver.addGlobalCapability(createEeCapability(compat));
        }

        // Add JRE package capabilities
        try {
            addJREPackageCapabilities(resolver, ee);
        } catch (IOException e) {
            status.add(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, Messages.ResolveOperation_errorAddingPackageCaps, e));
            result = createErrorResult(status);
            return;
        }

        // HACK: add capabilities for usual framework services (not all
        // frameworks declare these statically)
        String[] frameworkServices = new String[] {
                "org.osgi.service.packageadmin.PackageAdmin", "org.osgi.service.startlevel.StartLevel", "org.osgi.service.permissionadmin.PermissionAdmin"}; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        for (String frameworkService : frameworkServices) {
            Map<String,String> props = new HashMap<String,String>();
            props.put(ObrConstants.FILTER_SERVICE, frameworkService);
            resolver.addGlobalCapability(helper.capability(ObrConstants.REQUIREMENT_SERVICE, props));
        }

        // Add system packages-extra capabilities (from -runsystempackages)
        List<ExportedPackage> systemPackages = model.getSystemPackages();
        if (systemPackages != null)
            addSystemPackagesExtraCapabilities(resolver, systemPackages);

        // Add requirements
        List<bndtools.api.Requirement> requirements = model.getRunRequire();
        if (requirements != null)
            for (bndtools.api.Requirement req : requirements) {
                resolver.add(helper.requirement(req.getName(), req.getFilter()));
            }

        boolean resolved = resolver.resolve();

        result = new ObrResolutionResult(resolver, resolved, Status.OK_STATUS, filterGlobalResource(resolver.getRequiredResources()), filterGlobalResource(resolver.getOptionalResources()));
    }

    private void addRepository(URL index, List< ? super Repository> repos, File cacheDir) throws Exception {
        URLConnector connector = getConnector();

        addRepository(index, new HashSet<URL>(), repos, Integer.MAX_VALUE, connector, cacheDir);
    }

    private void addRepository(URL index, Set<URL> visited, List< ? super Repository> repos, int hopCount, URLConnector connector, File cacheDir) throws Exception {
        if (visited.add(index)) {
            CachingUriResourceHandle handle = new CachingUriResourceHandle(index.toExternalForm(), null, cacheDir, connector, CachingMode.PreferRemote);
            handle.setReporter(Central.getWorkspace());
            File file = handle.request();

            RepositoryImpl repo = PullParser.parseRepository(new FileInputStream(file));
            repo.setURI(index.toExternalForm());
            repos.add(repo);

            hopCount--;
            if (hopCount > 0 && repo.getReferrals() != null) {
                for (Referral referral : repo.getReferrals()) {
                    URL referralUrl = new URL(index, referral.getUrl());
                    hopCount = (referral.getDepth() > hopCount) ? hopCount : referral.getDepth();

                    addRepository(referralUrl, visited, repos, hopCount, connector, cacheDir);
                }
            }
        }
    }

    static URLConnector getConnector() throws Exception {
        URLConnector connector = Central.getWorkspace().getPlugin(URLConnector.class);
        if (connector == null) {
            connector = new SimpleURLConnector();
        }
        return connector;
    }

    private static ObrResolutionResult createErrorResult(MultiStatus status) {
        return new ObrResolutionResult(null, false, status, Collections.<Resource> emptyList(), Collections.<Resource> emptyList());
    }

    private void addJREPackageCapabilities(Resolver resolver, EE ee) throws IOException {
        // EE Package Capabilities
        Properties pkgProps = new Properties();
        URL pkgsResource = ResolveOperation.class.getResource(ee.name() + ".properties"); //$NON-NLS-1$
        if (pkgsResource == null)
            throw new IOException(String.format(Messages.ResolveOperation_missingJrePackageDefinition, ee.getEEName()));

        InputStream stream = null;
        try {
            stream = pkgsResource.openStream();
            pkgProps.load(stream);
        } finally {
            if (stream != null)
                IO.close(stream);
        }
        String pkgsStr = pkgProps.getProperty(Constants.FRAMEWORK_SYSTEMPACKAGES);

        Parameters header = new Parameters(pkgsStr);
        for (Entry<String,Attrs> entry : header.entrySet()) {
            String pkgName = Processor.removeDuplicateMarker(entry.getKey());
            String version = entry.getValue().get(Constants.VERSION_ATTRIBUTE);

            Map<String,String> capabilityProps = new HashMap<String,String>();
            capabilityProps.put(ObrConstants.FILTER_PACKAGE, pkgName);
            if (version != null)
                capabilityProps.put(ObrConstants.FILTER_VERSION, version);

            Capability capability = helper.capability(ObrConstants.REQUIREMENT_PACKAGE, capabilityProps);
            resolver.addGlobalCapability(capability);
        }
    }

    private void addSystemPackagesExtraCapabilities(Resolver resolver, Collection< ? extends ExportedPackage> systemPackages) {
        for (ExportedPackage clause : systemPackages) {
            String pkgName = clause.getName();
            String version = clause.getVersionString();

            Map<String,String> capabilityProps = new HashMap<String,String>();
            capabilityProps.put(ObrConstants.FILTER_PACKAGE, pkgName);
            if (version != null)
                capabilityProps.put(ObrConstants.FILTER_VERSION, version);
            Capability capability = helper.capability(ObrConstants.REQUIREMENT_PACKAGE, capabilityProps);
            resolver.addGlobalCapability(capability);
        }
    }

    private Set<Resource> addProjectBuildBundles(Resolver resolver) {
        if (!Project.BNDFILE.equals(runFile.getName()))
            return Collections.emptySet();

        Set<Resource> result = new HashSet<Resource>();
        try {

            Project model = Workspace.getProject(runFile.getProject().getLocation().toFile());
            for (Builder builder : model.getSubBuilders()) {
                File file = new File(model.getTarget(), builder.getBsn() + ".jar"); //$NON-NLS-1$
                if (file.isFile()) {
                    JarInputStream stream = null;
                    try {
                        stream = new JarInputStream(new FileInputStream(file));
                        Manifest manifest = stream.getManifest();

                        Resource resource = helper.createResource(manifest.getMainAttributes());
                        result.add(resource);
                        resolver.add(resource);
                    } catch (IOException e) {
                        Plugin.logError(Messages.ResolveOperation_errorReadingBundle + file, e);
                    } finally {
                        if (stream != null)
                            stream.close();
                    }
                }
            }
        } catch (Exception e) {
            Plugin.logError(Messages.ResolveOperation_errorGettingBuilders + runFile.getProject(), e);
        }
        return result;
    }

    private static List<Resource> filterGlobalResource(Resource[] resources) {
        ArrayList<Resource> result = new ArrayList<Resource>(resources.length);

        for (Resource resource : resources) {
            if (resource != null && resource.getId() != null)
                result.add(resource);
        }

        return result;
    }

    public ObrResolutionResult getResult() {
        return result;
    }

    private List<IndexProvider> loadIndexProviders() throws Exception {
        // Load the OBR providers into a map keyed on repo name
        Map<String,IndexProvider> repoMap = new LinkedHashMap<String,IndexProvider>();
        for (IndexProvider plugin : Central.getWorkspace().getPlugins(IndexProvider.class)) {
            // Filter out non-runtime repos nice and early
            if (plugin.getSupportedPhases().contains(ResolutionPhase.runtime)) {
                String name = (plugin instanceof RepositoryPlugin) ? ((RepositoryPlugin) plugin).getName() : plugin.toString();
                repoMap.put(name, plugin);
            }
        }

        List<IndexProvider> result = new ArrayList<IndexProvider>(repoMap.size());
        List<String> includedRepoNames = model.getRunRepos();

        if (includedRepoNames != null) {
            // Use the specified providers in the order that they are specified
            for (String name : includedRepoNames) {
                IndexProvider repo = repoMap.get(name);
                if (repo != null)
                    result.add(repo);
            }
        } else {
            // Take all the providers in the natural order offered by the
            // Workspace plugins
            for (IndexProvider repo : repoMap.values()) {
                result.add(repo);
            }
        }

        return result;
    }

    private File findFramework(MultiStatus status) {
        String runFramework = model.getRunFramework();
        Parameters header = new Parameters(runFramework);
        if (header.size() != 1) {
            status.add(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, String.format(Messages.ResolveOperation_invalidHeaderFormat, BndConstants.RUNFRAMEWORK), null));
            return null;
        }

        Entry<String,Attrs> entry = header.entrySet().iterator().next();
        VersionedClause clause = new VersionedClause(entry.getKey(), entry.getValue());

        String versionRange = clause.getVersionRange();
        if (versionRange == null)
            versionRange = new Version(0, 0, 0).toString();

        try {
            Container container = getProject().getBundle(clause.getName(), versionRange, Strategy.HIGHEST, null);
            if (container.getType() == TYPE.ERROR) {
                status.add(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, Messages.ResolveOperation_missingFramework + container.getError(), null));
                return null;
            }
            return container.getFile();
        } catch (Exception e) {
            status.add(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, Messages.ResolveOperation_errorFindingFramework, e));
            return null;
        }
    }

    private Project getProject() throws Exception {
        File file = runFile.getLocation().toFile();

        Project result;
        if ("bndrun".equals(runFile.getFileExtension())) { //$NON-NLS-1$
            result = new Project(Central.getWorkspace(), file.getParentFile(), file);

            File bndbnd = new File(file.getParentFile(), Project.BNDFILE);
            if (bndbnd.isFile()) {
                Project parentProject = Workspace.getProject(file.getParentFile());
                result.setParent(parentProject);
            }
        } else if (Project.BNDFILE.equals(runFile.getName())) {
            result = Workspace.getProject(file.getParentFile());
        } else {
            throw new Exception(Messages.ResolveOperation_invalidRunFile + runFile.getLocation());
        }
        return result;
    }

    private Capability createEeCapability(EE ee) {
        Map<String,String> props = new HashMap<String,String>();
        props.put(ObrConstants.FILTER_EE, ee.getEEName());

        return helper.capability(ObrConstants.REQUIREMENT_EE, props);
    }

}
