package bndtools.bindex;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.osgi.impl.bundle.obr.resource.RepositoryImpl;
import org.osgi.impl.bundle.obr.resource.ResourceImpl;
import org.osgi.impl.bundle.obr.resource.Tag;
import org.osgi.service.obr.Resource;

public abstract class AbstractIndexer {

    private URL url = null;

    protected abstract String getTaskLabel();

    protected abstract void generateResources(RepositoryImpl bindex, List<Resource> result, IProgressMonitor monitor) throws Exception;

    public abstract String getCategory();

    public void initialise(IProgressMonitor monitor) throws Exception {
        int workRemaining = 9;
        SubMonitor progress = SubMonitor.convert(monitor, getTaskLabel(), workRemaining);

        // Early exit if possible!
        if (url != null)
            return;

        // Setup Bindex
        File tempRepoFile;
        RepositoryImpl bindex;
        tempRepoFile = File.createTempFile("repository", ".xml");
        tempRepoFile.deleteOnExit();
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
        PrintWriter printWriter = new PrintWriter(tempRepoFile);
        try {
            tag.print(0, printWriter);
        } finally {
            printWriter.close();
        }
        progress.worked(1);
        workRemaining--;
        url = tempRepoFile.toURI().toURL();
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

}
