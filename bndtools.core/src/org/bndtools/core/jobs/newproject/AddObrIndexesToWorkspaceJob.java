package org.bndtools.core.jobs.newproject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.text.IDocument;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.texteditor.DocumentProviderRegistry;
import org.eclipse.ui.texteditor.IDocumentProvider;

import aQute.bnd.build.Workspace;
import aQute.libg.header.Attrs;
import bndtools.Plugin;
import bndtools.editor.model.BndEditModel;
import bndtools.editor.model.conversions.CollectionFormatter;
import bndtools.model.clauses.HeaderClause;

public class AddObrIndexesToWorkspaceJob extends WorkspaceJob {

    private final List<String> urls;

    public AddObrIndexesToWorkspaceJob(List<String> urls) {
        super("Add OBR Index(es) to Workspace");
        this.urls = urls;
    }

    @Override
    public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException {
        IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
        IProject cnfProject = root.getProject(Workspace.CNFDIR);
        IFile buildFile = cnfProject.getFile(Workspace.BUILDFILE);

        FileEditorInput input = new FileEditorInput(buildFile);
        IDocumentProvider docProvider = DocumentProviderRegistry.getDefault().getDocumentProvider(input);
        try {
            docProvider.connect(input);
            IDocument document = docProvider.getDocument(input);
            BndEditModel model = new BndEditModel();
            model.loadFrom(document);

            List<HeaderClause> plugins = model.getPlugins();
            List<HeaderClause> newPlugins = plugins == null ? new LinkedList<HeaderClause>() : new ArrayList<HeaderClause>(plugins);

            Attrs attribs = new Attrs();
            attribs.put("locations", new CollectionFormatter<String>(",").convert(urls));

            newPlugins.add(new HeaderClause("aQute.lib.deployer.obr.OBR", attribs));
            model.setPlugins(newPlugins);

            model.saveChangesTo(document);
            docProvider.saveDocument(null, input, document, true);
            return Status.OK_STATUS;
        } catch (IOException e) {
            return new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Failed to save OBR indexes to Workspace.", e);
        } finally {
            docProvider.disconnect(input);
        }
    }

}
