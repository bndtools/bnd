package bndtools.wizards.obr;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import org.eclipse.jface.operation.IRunnableWithProgress;

import aQute.bnd.build.Container;
import aQute.bnd.build.Project;
import aQute.bnd.build.Workspace;
import aQute.bnd.service.OBRIndexProvider;
import aQute.bnd.service.RepositoryPlugin.Strategy;
import bndtools.Central;
import bndtools.Plugin;
import bndtools.api.EE;
import bndtools.api.IBndModel;

public class ResolveOperation implements IRunnableWithProgress {

    private final DataModelHelper helper = new DataModelHelperImpl();

    private final List<Resource> required = new ArrayList<Resource>();
    private final List<Resource> optional = new ArrayList<Resource>();
    private final List<Reason> unresolved = new ArrayList<Reason>();
    private boolean resolved = false;

    private final IFile runFile;
    private final IBndModel model;
    private final List<OBRIndexProvider> repos;


    public ResolveOperation(IFile runFile, IBndModel model, List<OBRIndexProvider> repos) {
        this.runFile = runFile;
        this.model = model;
        this.repos = repos;
    }

    public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
        try {
            // Create the dummy system bundle and repository admin
            File frameworkFile = findFramework();
            DummyBundleContext bundleContext = new DummyBundleContext(frameworkFile);
            RepositoryAdminImpl repoAdmin = new RepositoryAdminImpl(bundleContext, new Logger(Plugin.getDefault().getBundleContext()));

            // Populate repository URLs
            for (OBRIndexProvider repo : repos) {
                for (URL indexUrl : repo.getOBRIndexes()) {
                    repoAdmin.addRepository(indexUrl);
                }
            }
            Resolver resolver = repoAdmin.resolver();

            // Add EE capabilities
            EE ee = model.getEE();
            if (ee == null)
                ee = EE.J2SE_1_5; // TODO: read default from the workbench
            resolver.addGlobalCapability(createEeCapability(ee));
            for (EE compat : ee.getCompatible()) {
                resolver.addGlobalCapability(createEeCapability(compat));
            }

            // Add requirements
            for (Requirement req : model.getRunRequire()) {
                resolver.add(req);
            }

            resolved = resolver.resolve();

            if (resolved) {
                required.addAll(Arrays.asList(resolver.getRequiredResources()));
                optional.addAll(Arrays.asList(resolver.getOptionalResources()));
            } else {
                unresolved.addAll(Arrays.asList(resolver.getUnsatisfiedRequirements()));
            }
        } catch (Exception e) {
            throw new InvocationTargetException(e);
        }
    }

    private Capability createEeCapability(EE ee) {
        Map<String, String> props = new HashMap<String, String>();
        props.put(ObrConstants.FILTER_EE, ee.getEEName());

        return helper.capability(ObrConstants.REQUIREMENT_EE, props);
    }

    private File findFramework() throws Exception {
        // TODO: get the requested framework from the model
        Container container = getProject().getBundle("org.apache.felix.framework", "latest", Strategy.HIGHEST, null);
        return container.getFile();
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

    public boolean isResolved() {
        return resolved;
    }

    public List<Resource> getRequired() {
        return Collections.unmodifiableList(required);
    }

    public List<Resource> getOptional() {
        return Collections.unmodifiableList(optional);
    }

    public List<Reason> getUnresolved() {
        return Collections.unmodifiableList(unresolved);
    }

}
