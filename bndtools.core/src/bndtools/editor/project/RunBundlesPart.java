package bndtools.editor.project;

import java.util.ArrayList;
import java.util.List;

import org.bndtools.api.ILogger;
import org.bndtools.api.Logger;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.IFormPage;
import org.eclipse.ui.forms.widgets.FormText;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.ide.ResourceUtil;

import aQute.bnd.build.Project;
import aQute.bnd.build.ProjectBuilder;
import aQute.bnd.build.model.BndEditModel;
import aQute.bnd.osgi.Builder;
import aQute.bnd.osgi.Constants;
import bndtools.central.Central;
import bndtools.model.clauses.VersionedClauseLabelProvider;
import bndtools.model.repo.DependencyPhase;
import bndtools.wizards.repo.RepoBundleSelectionWizard;

public class RunBundlesPart extends RepositoryBundleSelectionPart {
    private static final ILogger logger = Logger.getLogger(RunBundlesPart.class);

    private final List<String> projectBuilders = new ArrayList<>();
    private final Image projectImg = PlatformUI.getWorkbench().getSharedImages().getImage(IDE.SharedImages.IMG_OBJ_PROJECT);

    public RunBundlesPart(Composite parent, FormToolkit toolkit, int style) {
        super(Constants.RUNBUNDLES, DependencyPhase.Run, parent, toolkit, style);
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
            Project model = Central.getProject(project);
            if (model != null) {
                try (ProjectBuilder pb = model.getBuilder(null)) {
                    for (Builder b : pb.getSubBuilders()) {
                        projectBuilders.add(b.getBsn());
                    }
                }
            }
        } catch (Exception e) {
            logger.logError(Messages.RunBundlesPart_errorGettingBuilders, e);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void setBundles(List<aQute.bnd.build.model.clauses.VersionedClause> bundles) {
        this.bundles = bundles;

        @SuppressWarnings("rawtypes")
        List displayList;
        if (projectBuilders.isEmpty())
            displayList = bundles;
        else {
            displayList = new ArrayList<Object>(projectBuilders.size() + bundles.size());
            displayList.addAll(bundles);
            displayList.addAll(projectBuilders);
        }
        viewer.setInput(displayList);
    }

    @Override
    protected IBaseLabelProvider getLabelProvider() {
        return new VersionedClauseLabelProvider() {
            @Override
            public void update(ViewerCell cell) {
                Object element = cell.getElement();

                if (element instanceof String) {
                    String builder = (String) element;
                    StyledString label = new StyledString(builder, StyledString.QUALIFIER_STYLER);
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
        section.setText(Messages.RunBundlesPart_title);
        FormText description = new FormText(section, SWT.READ_ONLY | SWT.WRAP);
        description.setText(Messages.RunBundlesPart_description, true, false);
        section.setDescriptionControl(description);
        super.createSection(section, toolkit);

        Composite composite = (Composite) section.getClient();

        GridLayout layout = (GridLayout) composite.getLayout();
        layout.marginRight = 10;
    }

    @Override
    protected int getTableHeightHint() {
        return 50;
    }

    @Override
    protected void saveToModel(BndEditModel model, List<aQute.bnd.build.model.clauses.VersionedClause> bundles) {
        model.setRunBundles(bundles);
    }

    @Override
    protected List<aQute.bnd.build.model.clauses.VersionedClause> loadFromModel(BndEditModel model) {
        return model.getRunBundles();
    }

    @Override
    protected void setSelectionWizardTitleAndMessage(RepoBundleSelectionWizard wizard) {
        wizard.setSelectionPageTitle(Messages.RunBundlesPart_addWizardTitle);
        wizard.setSelectionPageDescription(Messages.RunBundlesPart_addWizardDescription);
    }

}
