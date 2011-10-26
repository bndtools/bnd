package org.bndtools.core.obr;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;

import org.apache.felix.bundlerepository.Capability;
import org.apache.felix.bundlerepository.DataModelHelper;
import org.apache.felix.bundlerepository.Reason;
import org.apache.felix.bundlerepository.Requirement;
import org.apache.felix.bundlerepository.Resolver;
import org.apache.felix.bundlerepository.Resource;
import org.apache.felix.bundlerepository.impl.DataModelHelperImpl;
import org.apache.felix.bundlerepository.impl.RepositoryAdminImpl;
import org.apache.felix.utils.log.Logger;
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
import aQute.bnd.service.OBRIndexProvider;
import aQute.bnd.service.OBRResolutionMode;
import aQute.bnd.service.RepositoryPlugin;
import aQute.bnd.service.RepositoryPlugin.Strategy;
import aQute.lib.io.IO;
import aQute.lib.osgi.Builder;
import aQute.lib.osgi.Processor;
import aQute.libg.header.OSGiHeader;
import aQute.libg.version.Version;
import bndtools.BndConstants;
import bndtools.Central;
import bndtools.Plugin;
import bndtools.api.EE;
import bndtools.api.IBndModel;
import bndtools.model.clauses.VersionedClause;
import bndtools.preferences.obr.ObrPreferences;
import bndtools.types.Pair;

public class ResolveOperation implements IRunnableWithProgress {

    private final DataModelHelper helper = new DataModelHelperImpl();

    private final IFile runFile;
    private final IBndModel model;

    private ObrResolutionResult result = null;

    public ResolveOperation(IFile runFile, IBndModel model) {
        this.runFile = runFile;
        this.model = model;
    }

    public void run(IProgressMonitor monitor) {
        SubMonitor progress = SubMonitor.convert(monitor, "Resolving...", 0);

        MultiStatus status = new MultiStatus(Plugin.PLUGIN_ID, 0, "Problems during OBR resolution", null);

        // Get the repositories
        List<OBRIndexProvider> indexProviders;
        try {
            indexProviders = loadIndexProviders();
        } catch (Exception e) {
            status.add(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Error loading OBR indexes.", e));
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
            status.add(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Error reading system bundle manifest.", e));
            result = createErrorResult(status);
            return;
        }


        // Load repositories
        RepositoryAdminImpl repoAdmin = new RepositoryAdminImpl(bundleContext, new Logger(Plugin.getDefault().getBundleContext()));
        for (OBRIndexProvider prov : indexProviders) {
            String repoName = (prov instanceof RepositoryPlugin) ? ((RepositoryPlugin) prov).getName() : prov.toString();
            try {
                for (URL indexUrl : prov.getOBRIndexes()) {
                    repoAdmin.addRepository(indexUrl);
                }
            } catch (Exception e) {
                status.add(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Error processing index for repository " + repoName, e));
            }
        }

        Resolver resolver = repoAdmin.resolver();

        // Add project builders
        Set<Resource> projectBuildResources = addProjectBuildBundles(resolver);

        // Add EE capabilities
        EE ee = model.getEE();
        if (ee == null)
            ee = EE.J2SE_1_5; // TODO: read default from the workbench
        resolver.addGlobalCapability(createEeCapability(ee));
        for (EE compat : ee.getCompatible()) {
            resolver.addGlobalCapability(createEeCapability(compat));
        }

        // Add JRE package capabilities
        try {
            addJREPackageCapabilities(resolver, ee);
        } catch (IOException e) {
            status.add(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Error adding JRE package capabilities", e));
            result = createErrorResult(status);
            return;
        }

        // HACK: add capabilities for usual framework services (not all frameworks declare these statically)
        String[] frameworkServices = new String[] { "org.osgi.service.packageadmin.PackageAdmin", "org.osgi.service.startlevel.StartLevel", "org.osgi.service.permissionadmin.PermissionAdmin" };
        for (String frameworkService : frameworkServices) {
            Map<String, String> props = new HashMap<String, String>();
            props.put(ObrConstants.FILTER_SERVICE, frameworkService);
            resolver.addGlobalCapability(helper.capability(ObrConstants.REQUIREMENT_SERVICE, props));
        }

        // Add requirements
        List<Requirement> requirements = model.getRunRequire();
        if (requirements != null) for (Requirement req : requirements) {
            resolver.add(req);
        }

        boolean resolved = resolver.resolve();

        result = new ObrResolutionResult(resolved, Status.OK_STATUS, filterGlobalResource(resolver.getRequiredResources()), filterGlobalResource(resolver.getOptionalResources()),
                Arrays.asList(resolver.getUnsatisfiedRequirements()));
    }

    private ObrResolutionResult createErrorResult(MultiStatus status) {
        return new ObrResolutionResult(false, status, Collections.<Resource>emptyList(), Collections.<Resource>emptyList(), Collections.<Reason>emptyList());
    }

    private void addJREPackageCapabilities(Resolver resolver, EE ee) throws IOException {
        // EE Package Capabilities
        Properties pkgProps = new Properties();
        URL pkgsResource = ResolveOperation.class.getResource(ee.name() + ".properties");
        if (pkgsResource == null)
            throw new IOException(String.format("No JRE package definition available for Execution Env %s.", ee.getEEName()));

        InputStream stream = null;
        try {
            stream = pkgsResource.openStream();
            pkgProps.load(stream);
        } finally {
            if (stream != null) IO.close(stream);
        }
        String pkgsStr = pkgProps.getProperty(Constants.FRAMEWORK_SYSTEMPACKAGES);

        Map<String, Map<String, String>> header = OSGiHeader.parseHeader(pkgsStr);
        for (Entry<String, Map<String, String>> entry : header.entrySet()) {
            String pkgName = Processor.removeDuplicateMarker(entry.getKey());
            String version = entry.getValue().get(Constants.VERSION_ATTRIBUTE);

            Map<String, String> capabilityProps = new HashMap<String, String>();
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
                File file = new File(model.getTarget(), builder.getBsn() + ".jar");
                if (file.isFile()) {
                    try {
                        JarInputStream stream = new JarInputStream(new FileInputStream(file));
                        Manifest manifest = stream.getManifest();

                        Resource resource = helper.createResource(manifest.getMainAttributes());
                        result.add(resource);
                        resolver.add(resource);
                    } catch (IOException e) {
                        Plugin.logError("Error reading project bundle " + file, e);
                    }
                }
            }
        } catch (Exception e) {
            Plugin.logError("Error getting builders for project: " + runFile.getProject(), e);
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

    private List<OBRIndexProvider> loadIndexProviders() throws Exception {
        Pair<List<OBRIndexProvider>, Set<String>> preferences = ObrPreferences.loadAvailableReposAndExclusions();
        List<OBRIndexProvider> repos = new ArrayList<OBRIndexProvider>(preferences.getFirst().size());

        for (OBRIndexProvider plugin : preferences.getFirst()) {
            if (plugin.getSupportedModes().contains(OBRResolutionMode.runtime)) {
                String name = (plugin instanceof RepositoryPlugin) ? ((RepositoryPlugin) plugin).getName() : plugin.toString();
                if (!preferences.getSecond().contains(name))
                    repos.add(plugin);
            }
        }

        return repos;
    }

    private File findFramework(MultiStatus status) {
        String runFramework = model.getRunFramework();
        Map<String, Map<String, String>> header = OSGiHeader.parseHeader(runFramework);
        if (header.size() != 1) {
            status.add(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Invalid format for " + BndConstants.RUNFRAMEWORK + " header", null));
            return null;
        }

        Entry<String, Map<String, String>> entry = header.entrySet().iterator().next();
        VersionedClause clause = new VersionedClause(entry.getKey(), entry.getValue());

        String versionRange = clause.getVersionRange();
        if (versionRange == null)
            versionRange = new Version(0, 0, 0).toString();

        try {
            Container container = getProject().getBundle(clause.getName(), versionRange, Strategy.HIGHEST, null);
            if (container.getType() == TYPE.ERROR) {
                status.add(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Unable to find specified OSGi framework: " + container.getError(), null));
                return null;
            }
            return container.getFile();
        } catch (Exception e) {
            status.add(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Error while trying to find the specified OSGi framework.", e));
            return null;
        }
    }

    private Project getProject() throws Exception {
        File file = runFile.getLocation().toFile();

        Project result;
        if ("bndrun".equals(runFile.getFileExtension())) {
            result = new Project(Central.getWorkspace(), file.getParentFile(), file);

            File bndbnd = new File(file.getParentFile(), Project.BNDFILE);
            if (bndbnd.isFile()) {
                Project parentProject = Workspace.getProject(file.getParentFile());
                result.setParent(parentProject);
            }
        } else if (Project.BNDFILE.equals(runFile.getName())) {
            result = Workspace.getProject(file.getParentFile());
        } else {
            throw new Exception("Invalid run file: " + runFile.getLocation());
        }
        return result;
    }

    private Capability createEeCapability(EE ee) {
        Map<String, String> props = new HashMap<String, String>();
        props.put(ObrConstants.FILTER_EE, ee.getEEName());

        return helper.capability(ObrConstants.REQUIREMENT_EE, props);
    }

}
