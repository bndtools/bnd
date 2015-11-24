package org.bndtools.core.ui.wizards.index;

import java.io.File;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bndtools.core.jobs.GenerateIndexJob;
import org.eclipse.core.resources.IFolder;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.osgi.service.indexer.ResourceIndexer;

public class NewIndexWizard extends Wizard implements INewWizard {

    private final IndexerWizardPage indexPage = new IndexerWizardPage();

    @Override
    public void init(IWorkbench workbench, IStructuredSelection selection) {
        Object firstSelElem = selection.getFirstElement();
        if (firstSelElem instanceof IFolder) {
            File dir = ((IFolder) firstSelElem).getLocation().toFile();
            indexPage.setBaseDir(dir);
        }
    }

    @Override
    public void addPages() {
        addPage(indexPage);
    }

    @Override
    public boolean performFinish() {
        File baseDir = indexPage.getBaseDir();
        Path basePath = baseDir.toPath();
        List<Path> paths = indexPage.getInputPaths();
        Set<File> inputFiles = new HashSet<>(paths.size());
        for (Path path : paths)
            inputFiles.add(basePath.resolve(path).toFile());

        // Setup index config
        Map<String,String> config = new HashMap<>();
        Boolean pretty = Boolean.valueOf(indexPage.getOutputStyle() == IndexFormatStyle.PRETTY_UNCOMPRESSED);
        Boolean compressed = Boolean.valueOf(indexPage.getOutputStyle() == IndexFormatStyle.COMPRESSED);
        config.put(ResourceIndexer.PRETTY, pretty.toString());
        config.put(ResourceIndexer.COMPRESSED, compressed.toString());
        config.put(ResourceIndexer.ROOT_URL, baseDir.toURI().toASCIIString());

        // Create the job
        GenerateIndexJob job = new GenerateIndexJob(inputFiles, indexPage.getOutputFile(), config);
        job.setUser(true);
        job.schedule();

        return true;
    }

}
