package bndtools.wizards.workspace;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.felix.bundlerepository.Capability;
import org.apache.felix.bundlerepository.Reason;
import org.apache.felix.bundlerepository.RepositoryAdmin;
import org.apache.felix.bundlerepository.Resolver;
import org.apache.felix.bundlerepository.Resource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;

import bndtools.api.EE;
import bndtools.bindex.GlobalCapabilityGenerator;
import bndtools.bindex.IRepositoryIndexProvider;
import bndtools.utils.Requestor;

public class ResolveOperation implements IRunnableWithProgress {

    private final RepositoryAdmin repoAdmin;
    private final File systemBundle;
    private final Requestor<Collection<? extends Resource>> selectedRequestor;
    private final List<IRepositoryIndexProvider> indexProviders;

    private final Collection<Resource> selected;
    private final List<Resource> required = new ArrayList<Resource>();
    private final List<Resource> optional = new ArrayList<Resource>();
    private final List<Reason> unresolved = new ArrayList<Reason>();

    private boolean resolved = false;

    public ResolveOperation(Collection<Resource> selected, RepositoryAdmin repoAdmin, File systemBundle, Requestor<Collection<? extends Resource>> selectionRequestor, List<IRepositoryIndexProvider> indexProviders) {
        this.selected = selected;
        this.repoAdmin = repoAdmin;
        this.systemBundle = systemBundle;
        this.selectedRequestor = selectionRequestor;
        this.indexProviders = indexProviders;
    }

    public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
        resolved = false;
        int work = 4 + indexProviders.size();
        SubMonitor progress = SubMonitor.convert(monitor, "", work);

        try {
            // Add indexes
            for (IRepositoryIndexProvider provider : indexProviders) {
                provider.initialise(progress.newChild(1, SubMonitor.SUPPRESS_NONE));
                for (URL repoUrl : provider.getUrls()) {
                    repoAdmin.addRepository(repoUrl.toExternalForm());
                }
                --work;
            }

            // Create resolver and add selected resources
            Resolver resolver = repoAdmin.resolver();
            selected.addAll(selectedRequestor.request(progress.newChild(1, SubMonitor.SUPPRESS_NONE)));
            for (Resource resource : selected) {
                resolver.add(resource);
            }

            // Add global capabilities
            if (systemBundle == null)
                throw new IllegalStateException("System bundle not defined");
            GlobalCapabilityGenerator capabilityGenerator = new GlobalCapabilityGenerator(systemBundle);
            for (Capability capability : capabilityGenerator.listCapabilities(EE.J2SE_1_5)) {
                resolver.addGlobalCapability(capability);
            }

            // Resolve and report missing stuff
            resolved = resolver.resolve();
            progress.worked(1);

            if (!resolved) {
                Reason[] unsatisfiedRequirements = resolver.getUnsatisfiedRequirements();
                for (Reason reason : unsatisfiedRequirements) {
                    unresolved.add(reason);
                }
            } else {
                // Add to the required set
                for (Resource resource : resolver.getRequiredResources()) {
                    if (resource.getURI() == null)
                        // This is the fake "resource" representing global
                        // capabilities
                        continue;

                    required.add(resource);
                    Reason[] reasons = resolver.getReason(resource);
                    if (reasons != null) {
                        System.out.println("Adding resource " + resource + " for the following reasons:");
                        for (Reason reason : reasons) {
                            System.out.println("   " + reason.getRequirement() + " REQUIRED BY " + reason.getResource());
                        }
                    }
                }

                // Add to the optional set
                for (Resource resource : resolver.getOptionalResources()) {
                    if (resource == null || resource.getURI() == null)
                        continue;
                    optional.add(resource);
                }
            }
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } finally {
            for (IRepositoryIndexProvider provider : indexProviders) {
                for (URL url : provider.getUrls()) {
                    repoAdmin.removeRepository(url.toExternalForm());
                }
            }
        }
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
        return unresolved;
    }

}
