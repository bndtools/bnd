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
import java.util.LinkedList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.osgi.impl.bundle.obr.resource.BundleInfo;
import org.osgi.impl.bundle.obr.resource.RepositoryImpl;
import org.osgi.impl.bundle.obr.resource.ResourceImpl;
import org.osgi.impl.bundle.obr.resource.Tag;

import aQute.bnd.service.RepositoryPlugin;
import bndtools.Central;
import bndtools.Plugin;

public class LocalRepositoryIndexer implements IRunnableWithProgress {

    private String localCategory = null;
    private IStatus status = Status.OK_STATUS;
    private URL url = null;

    public URL getUrl() {
        return url;
    }

    public IStatus getStatus() {
        return status;
    }

    public void setLocalCategory(String localCategory) {
        this.localCategory = localCategory;
    }

    public void run(IProgressMonitor monitor) {
        int workRemaining = 7;
        SubMonitor progress = SubMonitor.convert(monitor, "Indexing local repository", workRemaining);

        // Setup Bindex
        File tempRepoFile;
        RepositoryImpl bindex;
        try {
            tempRepoFile = File.createTempFile("repository", ".xml");
            url = tempRepoFile.toURI().toURL();
            tempRepoFile.deleteOnExit();
            bindex = new RepositoryImpl(url);

            progress.worked(1);
            workRemaining --;
        } catch (IOException e) {
            status = new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "IO error creating temporary OBR file", e);
            return;
        }

        List<ResourceImpl> resources = new LinkedList<ResourceImpl>();

        // Analyse bundles
        try {
            List<RepositoryPlugin> bndRepos = Central.getWorkspace().getRepositories();
            workRemaining -= 4;
            if (bndRepos != null) {
                processRepos(bndRepos, bindex, resources, progress.newChild(4));
            } else {
                progress.setWorkRemaining(workRemaining);
            }
        } catch (Exception e) {
            status = new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Error indexing local repository", e);
            return;
        }

        // Sort and generate index
        List<ResourceImpl> sorted = new ArrayList<ResourceImpl>(resources);
        Collections.sort(sorted, new Comparator<ResourceImpl>() {
            public int compare(ResourceImpl o1, ResourceImpl o2) {
                String s1 = getResourceName(o1);
                String s2 = getResourceName(o2);
                return s1.compareTo(s2);
            }
        });
        progress.worked(1);
        workRemaining --;
        try {
            Tag tag = doIndex(resources, "dummy");
            PrintWriter printWriter = new PrintWriter(tempRepoFile);
            try {
                tag.print(0, printWriter);
            } finally {
                printWriter.close();
            }
            progress.worked(1);
            workRemaining --;
            status = Status.OK_STATUS;
        } catch (IOException e) {
            status = new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "IO error writing local repository index", e);
            return;
        }
    }

    private void processRepos(List<RepositoryPlugin> bndRepos, RepositoryImpl bindex, List<ResourceImpl> resources, IProgressMonitor monitor) throws Exception {
        SubMonitor progress = SubMonitor.convert(monitor, bndRepos.size());
        for (RepositoryPlugin bndRepo : bndRepos) {
            List<String> bsns = bndRepo.list(null);
            if (bsns != null) {
                processRepoBundles(bndRepo, bsns, bindex, resources, progress.newChild(1));
            }
        }
    }

    private void processRepoBundles(RepositoryPlugin bndRepo, List<String> bsns, RepositoryImpl bindex, List<ResourceImpl> resources, IProgressMonitor monitor) throws Exception {
        SubMonitor progress = SubMonitor.convert(monitor, bsns.size());
        for (String bsn : bsns) {
            File[] files = bndRepo.get(bsn, null);
            if (files != null) {
                processRepoFiles(files, bindex, resources, progress.newChild(1));
            }
        }
    }

    private void processRepoFiles(File[] files, RepositoryImpl bindex, List<ResourceImpl> resources, IProgressMonitor monitor) throws Exception {
        SubMonitor progress = SubMonitor.convert(monitor, files.length);
        for (File bundleFile : files) {
            if (bundleFile.getName().toLowerCase().endsWith(".jar")) {
                BundleInfo info = new BundleInfo(bindex, bundleFile);
                ResourceImpl resource = info.build();
                resource.setURL(bundleFile.toURI().toURL());

                if(localCategory != null)
                    resource.addCategory(localCategory);

                resources.add(resource);
            }
            progress.worked(1);
        }
    }

    String getResourceName(ResourceImpl impl) {
        String s = impl.getSymbolicName();
        if (s != null)
            return s;
        else {
            return "no-symbolic-name";
        }
    }

    Tag doIndex(Collection<ResourceImpl> resources, String name) throws IOException {
        Tag repository = new Tag("repository");
        repository.addAttribute("lastmodified", new Date());
        repository.addAttribute("name", name);

        for (ResourceImpl resource : resources) {
            repository.addContent(resource.toXML());
        }
        return repository;
    }
}
