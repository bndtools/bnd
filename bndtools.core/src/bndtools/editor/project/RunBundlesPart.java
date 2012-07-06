package bndtools.editor.project;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.IFormPart;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.IFormPage;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.FormText;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.ide.ResourceUtil;
import org.eclipse.ui.plugin.AbstractUIPlugin;

import aQute.bnd.build.Project;
import aQute.bnd.build.Workspace;
import aQute.lib.osgi.Builder;
import aQute.lib.osgi.Constants;
import bndtools.Plugin;
import bndtools.editor.model.BndtoolsEditModel;
import bndtools.model.clauses.VersionedClauseLabelProvider;
import bndtools.utils.EditorUtils;
import bndtools.wizards.repo.RepoBundleSelectionWizard;

public class RunBundlesPart extends RepositoryBundleSelectionPart {

    private final List<Builder> projectBuilders = new ArrayList<Builder>();
    private Image projectImg = PlatformUI.getWorkbench().getSharedImages().getImage(IDE.SharedImages.IMG_OBJ_PROJECT);

    private Image warningImg;
    private ControlDecoration warningDecor = null;

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
            Plugin.getDefault().getLogger().logError(Messages.RunBundlesPart_errorGettingBuilders, e);
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
        section.setText(Messages.RunBundlesPart_title);

        FormText description = new FormText(section, SWT.READ_ONLY | SWT.WRAP);
        description.setText(Messages.RunBundlesPart_description, true, false);
        warningImg = AbstractUIPlugin.imageDescriptorFromPlugin(Plugin.PLUGIN_ID, "icons/warning_obj.gif").createImage(); //$NON-NLS-1$
        description.setImage(Messages.RunBundlesPart_warningKey, warningImg);
        description.addHyperlinkListener(new HyperlinkAdapter() {
            @Override
            public void linkActivated(HyperlinkEvent e) {
                if ("requirements".equals(e.data)) {
                    IFormPart part = EditorUtils.findPartByClass(getManagedForm(), RunRequirementsPart.class);
                    if (part != null)
                        part.setFocus();
                }
            }
        });

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
    protected void saveToModel(BndtoolsEditModel model, List<aQute.bnd.build.model.clauses.VersionedClause> bundles) {
        model.setRunBundles(bundles);
    }

    @Override
    protected List<aQute.bnd.build.model.clauses.VersionedClause> loadFromModel(BndtoolsEditModel model) {
        return model.getRunBundles();
    }

    @Override
    protected void setSelectionWizardTitleAndMessage(RepoBundleSelectionWizard wizard) {
        wizard.setSelectionPageTitle(Messages.RunBundlesPart_addWizardTitle);
        wizard.setSelectionPageDescription(Messages.RunBundlesPart_addWizardDescription);
    }

    @Override
    public void dispose() {
        super.dispose();
        warningImg.dispose();
    }

    @Override
    public void markDirty() {
        super.markDirty();
        updateWarning();
    }

    @Override
    public void refresh() {
        super.refresh();
        updateWarning();
    }

    @Override
    public void commit(boolean onSave) {
        super.commit(onSave);
        updateWarning();
    }

    private void updateWarning() {
        if (isDirty()) {
            if (warningDecor != null) {
                warningDecor.show();
            } else {
                warningDecor = new ControlDecoration(viewer.getControl(), SWT.RIGHT | SWT.TOP, (Composite) getSection().getClient());
                warningDecor.setImage(FieldDecorationRegistry.getDefault().getFieldDecoration(FieldDecorationRegistry.DEC_WARNING).getImage());
                warningDecor.setDescriptionText(Messages.RunBundlesPart_warningHover);
                warningDecor.setMarginWidth(2);
                warningDecor.setShowHover(true);
            }
        } else {
            if (warningDecor != null) {
                warningDecor.hide();
            }
        }
    }

}
