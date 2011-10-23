package bndtools.editor.project;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.IFormPage;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.ide.ResourceUtil;

import aQute.bnd.build.Project;
import aQute.bnd.build.Workspace;
import aQute.lib.osgi.Builder;
import aQute.lib.osgi.Constants;
import bndtools.Plugin;
import bndtools.editor.model.BndEditModel;
import bndtools.model.clauses.VersionedClause;
import bndtools.model.clauses.VersionedClauseLabelProvider;
import bndtools.wizards.repo.RepoBundleSelectionWizard;

public class RunBundlesPart extends RepositoryBundleSelectionPart {

    private final List<Builder> projectBuilders = new ArrayList<Builder>();
    private Image projectImg = PlatformUI.getWorkbench().getSharedImages().getImage(IDE.SharedImages.IMG_OBJ_PROJECT);

    public RunBundlesPart(Composite parent, FormToolkit toolkit, int style) {
        super(Constants.RUNBUNDLES, parent, toolkit, style);
    }

    @Override
    public void initialize(IManagedForm form) {
        super.initialize(form);

        IFormPage page = (IFormPage) form.getContainer();
        IFile file = ResourceUtil.getFile(page.getEditorInput());
        if (file != null) {
            if (Project.BNDFILE.equals(file.getName())) {
                loadBuilders(file.getProject());
            }
        }
    }

    private void loadBuilders(IProject project) {
        try {
            Project model = Workspace.getProject(project.getLocation().toFile());
            if (model != null)
                projectBuilders.addAll(model.getSubBuilders());
        } catch (Exception e) {
            Plugin.logError("Error getting project builders", e);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void setBundles(List<VersionedClause> bundles) {
        this.bundles = bundles;

        @SuppressWarnings("rawtypes")
        List displayList;
        if (projectBuilders.isEmpty())
            displayList = bundles;
        else {
            displayList = new ArrayList<Object>(projectBuilders.size() + bundles.size());
            displayList.addAll(projectBuilders);
            displayList.addAll(bundles);
        }
        viewer.setInput(displayList);
    }

    @Override
    protected IBaseLabelProvider getLabelProvider() {
        return new VersionedClauseLabelProvider() {
            @Override
            public void update(ViewerCell cell) {
                Object element = cell.getElement();

                if (element instanceof Builder) {
                    Builder builder = (Builder) element;
                    StyledString label = new StyledString(builder.getBsn(), StyledString.QUALIFIER_STYLER);
                    cell.setText(label.getString());
                    cell.setStyleRanges(label.getStyleRanges());
                    cell.setImage(projectImg);
                } else {
                    super.update(cell);
                }
            }
        };
    }

    @Override
    void createSection(Section section, FormToolkit toolkit) {
        section.setText("Run Bundles");
        section.setDescription("The listed bundles will be added to the runtime.");
        super.createSection(section, toolkit);

        // Composite composite = (Composite) section.getClient();
    }

    @Override
    protected int getTableHeightHint() {
        return 100;
    }

    @Override
    protected void saveToModel(BndEditModel model, List<VersionedClause> bundles) {
        model.setRunBundles(bundles);
    }

    @Override
    protected List<VersionedClause> loadFromModel(BndEditModel model) {
        return model.getRunBundles();
    }

    @Override
    protected void setSelectionWizardTitleAndMessage(RepoBundleSelectionWizard wizard) {
        wizard.setSelectionPageTitle("Project Run Path");
        wizard.setSelectionPageDescription("Select bundles to be added to the project build path for compilation.");
    }

}
