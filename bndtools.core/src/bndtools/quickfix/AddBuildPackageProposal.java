package bndtools.quickfix;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.eclipse.ui.texteditor.DocumentProviderRegistry;
import org.eclipse.ui.texteditor.IDocumentProvider;

import aQute.bnd.build.Project;
import aQute.libg.header.Attrs;
import bndtools.Plugin;
import bndtools.editor.model.BndEditModel;
import bndtools.model.clauses.VersionedClause;

public class AddBuildPackageProposal implements IJavaCompletionProposal {

    private final Image image = AbstractUIPlugin.imageDescriptorFromPlugin(Plugin.PLUGIN_ID, "/icons/package_add.png").createImage();
    private final Project project;
    private final String iPackage;

    public AddBuildPackageProposal(Project project, String iPackage) {
        this.project = project;
        this.iPackage = iPackage;
    }

    public int getRelevance()
    {
        return 100;
    }

    public void apply(IDocument source)
    {
        IJavaProject javaProject = Plugin.getDefault().getCentral().getJavaProject(project);
        IFile buildFile = javaProject.getProject().getFile(Project.BNDFILE);

        FileEditorInput input = new FileEditorInput(buildFile);
        IDocumentProvider docProvider = DocumentProviderRegistry.getDefault().getDocumentProvider(input);
        try {
            docProvider.connect(input);
            IDocument document = docProvider.getDocument(input);
            BndEditModel model = new BndEditModel();
            model.loadFrom(document);
            
            List<VersionedClause> packages = model.getBuildPackages();
            if (packages == null) {
                packages = new ArrayList<VersionedClause>(1);
            }
            packages.add(new VersionedClause(iPackage, new Attrs()));
            model.setBuildPackages(packages);

            model.saveChangesTo(document);
            docProvider.saveDocument(null, input, document, true);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (CoreException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } finally {
            docProvider.disconnect(input);
        }
    }

    public String getAdditionalProposalInfo()
    {
        return null;
    }

    public IContextInformation getContextInformation()
    {
        // TODO Auto-generated method stub
        return null;
    }

    public String getDisplayString()
    {
        return "Add package " + iPackage + " to -buildpackages";
    }

    public Image getImage()
    {
        return image;
    }

    public Point getSelection(IDocument document)
    {
        return null;
    }
}
