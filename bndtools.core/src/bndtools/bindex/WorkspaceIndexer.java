package bndtools.bindex;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.net.URL;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import javax.xml.transform.stream.StreamResult;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.osgi.service.bindex.BundleIndexer;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

import aQute.bnd.build.Project;
import aQute.lib.osgi.Builder;
import aQute.libg.sax.SAXUtil;
import bndtools.Central;
import bndtools.Plugin;

public class WorkspaceIndexer implements IRepositoryIndexProvider {

    private final String category;

    private URL url = null;
    private File outputFile = null;

    public WorkspaceIndexer(String category) {
        this.category = category;
    }

    public void initialise(IProgressMonitor monitor) throws Exception {
        int workRemaining = 6;
        SubMonitor progress = SubMonitor.convert(monitor, workRemaining);

        // Create repo file
        File repoFile;
        if (outputFile != null)
            repoFile = outputFile;
        else {
            repoFile = File.createTempFile("repository", ".xml");
            repoFile.deleteOnExit();
        }
        url = repoFile.toURI().toURL();

        progress.worked(1);
        workRemaining --;

        // Gather files to index
        Set<File> fileSet = new HashSet<File>();
        gatherFiles(fileSet, progress.newChild(2, SubMonitor.SUPPRESS_NONE));
        workRemaining -= 2;

        // Run the indexer
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try {
            BundleIndexer indexer = Plugin.getDefault().getBundleIndexer();
            indexer.index(fileSet, output, null);
        } finally {
            output.close();
        }
        progress.worked(2);
        workRemaining -= 2;

        // Post-process to insert category
        XMLReader processor = SAXUtil.buildPipeline(new StreamResult(repoFile), new CategoryInsertionContentFilter(category));
        processor.parse(new InputSource(new ByteArrayInputStream(output.toByteArray())));
        progress.worked(1);
        workRemaining --;
    }

    public File getOutputFile() {
        return outputFile;
    }

    public void setOutputFile(File outputFile) {
        this.outputFile = outputFile;
    }

    public URL[] getUrls() {
        return new URL[] { url };
    }

    protected void gatherFiles(Collection<File> result, IProgressMonitor monitor) throws Exception {
        Collection<Project> projects = Central.getWorkspace().getAllProjects();
        processProjects(projects, result, monitor);
    }

    private void processProjects(Collection<Project> projects, Collection<File> result, IProgressMonitor monitor) throws Exception {
        SubMonitor progress = SubMonitor.convert(monitor, projects.size());
        for (Project project : projects) {
            Collection<? extends Builder> builders = project.getSubBuilders();
            processProjectBuilders(project, builders, result, progress.newChild(1));
        }
    }

    private void processProjectBuilders(Project project, Collection<? extends Builder> builders, Collection<File> result, IProgressMonitor monitor)
            throws Exception {
        SubMonitor progress = SubMonitor.convert(monitor, builders.size());
        for (Builder builder : builders) {
            File bundleFile = new File(project.getTarget(), builder.getBsn() + ".jar");
            if (bundleFile.exists())
                result.add(bundleFile);
            progress.worked(1);
        }
    }

}
