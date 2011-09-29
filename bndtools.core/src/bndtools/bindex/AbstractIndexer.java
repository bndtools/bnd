package bndtools.bindex;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.osgi.impl.bundle.obr.resource.RepositoryImpl;
import org.osgi.impl.bundle.obr.resource.RequirementImpl;
import org.osgi.impl.bundle.obr.resource.ResourceImpl;
import org.osgi.impl.bundle.obr.resource.Tag;
import org.osgi.service.obr.Requirement;
import org.osgi.service.obr.Resource;

public abstract class AbstractIndexer implements IRepositoryIndexProvider {

    public static final String CATEGORY_NO_RUNTIME = "NORUNTIME";
    public static final String REQUIREMENT_EXCLUDE = "exclude";

    public static final Set<String> BUILD_ONLY_BSNS = new HashSet<String>(Arrays.asList(new String[] {
            "osgi.core",
            "org.eclipse.osgi",
            "org.apache.felix.framework",
            "org.knopflerfish.framework",
            "ee.minimum",
            "org.osgi.ee.minimum",
            "de.kalpatec.pojosr.framework"
    }));

    private URL url = null;
    private File outputFile = null;

    protected abstract String getTaskLabel();

    protected abstract void generateResources(RepositoryImpl bindex, List<Resource> result, IProgressMonitor monitor) throws Exception;

    public void initialise(IProgressMonitor monitor) throws Exception {
        int workRemaining = 9;
        SubMonitor progress = SubMonitor.convert(monitor, getTaskLabel(), workRemaining);

        // Early exit if possible!
        if (url != null)
            return;

        // Setup Bindex
        File repoFile;
        RepositoryImpl bindex;
        if (outputFile != null)
            repoFile = outputFile;
        else {
            repoFile = File.createTempFile("repository", ".xml");
            repoFile.deleteOnExit();
        }
        url = repoFile.toURI().toURL();
        bindex = new RepositoryImpl(url);

        progress.worked(1);
        workRemaining--;

        List<Resource> resources = new ArrayList<Resource>();
        generateResources(bindex, resources, progress.newChild(6));

        // Sort and generate index
        List<Resource> sorted = new ArrayList<Resource>(resources);
        Collections.sort(sorted, new Comparator<Resource>() {
            public int compare(Resource o1, Resource o2) {
                String s1 = getResourceName(o1);
                String s2 = getResourceName(o2);
                return s1.compareTo(s2);
            }
        });
        progress.worked(1);
        workRemaining--;
        Tag tag = doIndex(resources, "LocalRepo");
        PrintWriter printWriter = new PrintWriter(repoFile);
        try {
            tag.print(0, printWriter);
        } finally {
            printWriter.close();
        }
        progress.worked(1);
        workRemaining--;
    }

    public File getOutputFile() {
        return outputFile;
    }

    public void setOutputFile(File outputFile) {
        this.outputFile = outputFile;
    }

    public URL getUrl() {
        return url;
    }

    protected void reset() {
        url = null;
    }

    private Tag doIndex(Collection<? extends Resource> resources, String name) throws IOException {
        Tag repository = new Tag("repository");
        repository.addAttribute("lastmodified", new Date());
        repository.addAttribute("name", name);

        for (Resource resource : resources) {
            Tag xml = ResourceImpl.toXML(resource);
            repository.addContent(xml);
        }

        return repository;
    }

    private String getResourceName(Resource resource) {
        String s = resource.getSymbolicName();
        if (s != null)
            return s;
        else {
            return "no-symbolic-name";
        }
    }

    protected void customizeResourceEntry(ResourceImpl resource) {
        String bsn = resource.getSymbolicName();
        if (BUILD_ONLY_BSNS.contains(bsn)) {
            RequirementImpl modeRequirement = new RequirementImpl("mode");
            modeRequirement.setFilter("(mode=build)");
            resource.addRequirement(modeRequirement);
        }

        Collection<Requirement> reqs = resource.getRequirementList();
        for (Iterator<Requirement> iter = reqs.iterator(); iter.hasNext(); ) {
            Requirement req = iter.next();
            if ("service".equals(req.getName())) {
                ((RequirementImpl) req).setOptional(true);
            }
        }
    }


}
