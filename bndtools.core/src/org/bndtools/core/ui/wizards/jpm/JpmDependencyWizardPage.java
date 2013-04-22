package org.bndtools.core.ui.wizards.jpm;

import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.util.Set;

import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ShellAdapter;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import aQute.bnd.service.repository.SearchableRepository.ResourceDescriptor;

public class JpmDependencyWizardPage extends WizardPage {

    private final URI originUri;

    private TableViewer viewerDirect;
    private CheckboxTableViewer viewerIndirect;
    private boolean queried = false;

    private String errorText;
    private Set<ResourceDescriptor> directResources;
    private Set<ResourceDescriptor> indirectResources;

    /**
     * Create the wizard.
     */
    public JpmDependencyWizardPage(URI originUri) {
        super("jpmDependencies");
        this.originUri = originUri;

        setTitle("Add Dependencies from JPM4J");
        setDescription("Review the following dependencies supplied by JPM4J before adding them.");
    }

    /**
     * Create contents of the wizard.
     * 
     * @param parent
     */
    public void createControl(Composite parent) {
        // CREATE CONTROLS
        Composite container = new Composite(parent, SWT.NULL);
        setControl(container);

        Label lblDirect = new Label(container, SWT.NONE);
        lblDirect.setFont(JFaceResources.getBannerFont());
        lblDirect.setText("Direct Dependencies:");

        Table tblDirect = new Table(container, SWT.MULTI | SWT.FULL_SELECTION | SWT.BORDER | SWT.V_SCROLL);
        viewerDirect = new TableViewer(tblDirect);
        viewerDirect.setContentProvider(ArrayContentProvider.getInstance());
        viewerDirect.setLabelProvider(new ResourceDescriptorLabelProvider());

        createHelpLabel(container, "The above dependencies will be added to the project and, if necessary, to the JPM4J local index.");

        Label separator = new Label(container, SWT.SEPARATOR | SWT.HORIZONTAL);

        Label lblIndirect = new Label(container, SWT.NONE);
        lblIndirect.setFont(JFaceResources.getBannerFont());
        lblIndirect.setText("Transitive Dependencies:");

        Table tblIndirect = new Table(container, SWT.CHECK | SWT.MULTI | SWT.FULL_SELECTION | SWT.BORDER | SWT.V_SCROLL);
        viewerIndirect = new CheckboxTableViewer(tblIndirect);
        viewerIndirect.setContentProvider(ArrayContentProvider.getInstance());
        viewerIndirect.setLabelProvider(new ResourceDescriptorLabelProvider());

        createHelpLabel(container, "The above dependencies will be added to the JPM4J local index. Checked dependencies will also be added directly to the project.");

        // LAYOUT
        GridLayout layout;
        GridData gd;

        layout = new GridLayout(1, true);
        container.setLayout(layout);

        gd = new GridData(SWT.FILL, SWT.FILL, true, false);
        gd.heightHint = 30;
        tblDirect.setLayoutData(gd);

        gd = new GridData(SWT.FILL, SWT.FILL, true, false);
        separator.setLayoutData(gd);

        gd = new GridData(SWT.FILL, SWT.FILL, true, true);
        gd.heightHint = 100;
        tblIndirect.setLayoutData(gd);

        getContainer().getShell().addShellListener(new ShellAdapter() {
            @Override
            public void shellActivated(ShellEvent e) {
                runQuery();
            }
        });
    }

    private Control createHelpLabel(Composite container, String text) {
        Label label = new Label(container, SWT.WRAP);
        label.setText(text);

        ControlDecoration decoration = new ControlDecoration(label, SWT.LEFT, container);
        Image imgInfo = FieldDecorationRegistry.getDefault().getFieldDecoration(FieldDecorationRegistry.DEC_INFORMATION).getImage();
        decoration.setImage(imgInfo);

        GridData gd = new GridData(SWT.FILL, SWT.FILL, true, false);
        gd.horizontalIndent = imgInfo.getBounds().width;
        gd.widthHint = 100;
        label.setLayoutData(gd);

        return label;
    }

    private void runQuery() {
        if (!queried) {
            errorText = null;
            try {
                QueryJpmDependenciesRunnable query = new QueryJpmDependenciesRunnable(originUri);
                getContainer().run(true, true, query);
                queried = true;
                errorText = query.getError();
                directResources = query.getDirectResources();
                indirectResources = query.getIndirectResources();
            } catch (InvocationTargetException e) {
                errorText = e.getCause().getMessage();
            } catch (InterruptedException e) {
                // ignore
            } finally {
                updateUi();
            }
        }
    }

    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);
        if (visible && !queried) {}
    }

    public void updateUi() {
        viewerDirect.setInput(directResources);
        viewerIndirect.setInput(indirectResources);
        getContainer().updateButtons();
        getContainer().updateMessage();
    }

    @Override
    public boolean isPageComplete() {
        return queried && errorText == null;
    }

    @Override
    public String getErrorMessage() {
        return errorText;
    }

    private static class ResourceDescriptorLabelProvider extends StyledCellLabelProvider {
        @Override
        public void update(ViewerCell cell) {
            ResourceDescriptor descriptor = (ResourceDescriptor) cell.getElement();

            StyledString label = new StyledString(descriptor.bsn + " ");
            if (descriptor.version != null)
                label.append(descriptor.version.toString(), StyledString.COUNTER_STYLER);

            cell.setText(label.toString());
            cell.setStyleRanges(label.getStyleRanges());
        }
    }

}
