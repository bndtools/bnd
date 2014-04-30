package org.bndtools.core.resolve.ui;

import java.util.List;
import java.util.Map.Entry;

import org.bndtools.core.ui.resource.R5LabelFormatter;
import org.bndtools.utils.jface.ImageCachingLabelProvider;
import org.bndtools.utils.resources.ResourceUtils;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.osgi.resource.Capability;
import org.osgi.resource.Namespace;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;

import bndtools.Plugin;
import bndtools.UIConstants;

public class ResolutionChoiceSelectionDialog extends TitleAreaDialog {

    private final Requirement requirement;
    private final List<Capability> candidates;

    private CheckboxTableViewer viewer;
    private Button btnUp;
    private Button btnDown;

    private Button btnSavePreference;
    private StyledText txtSavePreference;

    public ResolutionChoiceSelectionDialog(Shell shell, Requirement requirement, List<Capability> candidates) {
        super(shell);
        this.requirement = requirement;
        this.candidates = candidates;
    }

    @SuppressWarnings("unused")
    @Override
    protected Control createDialogArea(Composite parent) {
        setTitle("Multiple Provider Candidates");
        setMessage("Use the candidate list to specify your preferences. Candidates at the top of the list will be preferred by the resolver.");

        // Create controls
        Composite outer = (Composite) super.createDialogArea(parent);
        Composite contents = new Composite(outer, SWT.NONE);

        Label lblRequirement = new Label(contents, SWT.NONE);
        lblRequirement.setText("Requirement Info");
        lblRequirement.setFont(JFaceResources.getFontRegistry().getBold(JFaceResources.DIALOG_FONT));

        StyledText txtRequirement = new StyledText(contents, SWT.WRAP | SWT.BORDER);
        txtRequirement.setEditable(false);
        txtRequirement.setCaret(null);
        //        txtRequirement.setBackground(contents.getBackground());
        txtRequirement.setCursor(parent.getDisplay().getSystemCursor(SWT.CURSOR_ARROW));

        new Label(contents, SWT.NONE);

        Label lblCandidates = new Label(contents, SWT.NONE);
        lblCandidates.setText("Candidates");
        lblCandidates.setFont(JFaceResources.getFontRegistry().getBold(JFaceResources.DIALOG_FONT));

        Composite lowerPanel = new Composite(contents, SWT.NONE);
        Table tbl = new Table(lowerPanel, SWT.SINGLE | SWT.FULL_SELECTION | SWT.BORDER);

        viewer = new CheckboxTableViewer(tbl);
        viewer.setContentProvider(ArrayContentProvider.getInstance());
        viewer.setLabelProvider(new CapabilityResourceLabelProvider());

        btnUp = new Button(lowerPanel, SWT.PUSH);
        btnUp.setText("Move Up");
        btnUp.setEnabled(false);

        btnDown = new Button(lowerPanel, SWT.PUSH);
        btnDown.setText("Move Down");
        btnDown.setEnabled(false);

        Composite cmpPreferences = new Composite(contents, SWT.NONE);
        btnSavePreference = new Button(cmpPreferences, SWT.CHECK | SWT.WRAP);
        txtSavePreference = new StyledText(cmpPreferences, SWT.WRAP);
        txtSavePreference.setEditable(false);
        txtSavePreference.setCaret(null);
        txtSavePreference.setBackground(contents.getBackground());
        txtSavePreference.setCursor(parent.getDisplay().getSystemCursor(SWT.CURSOR_ARROW));

        // Events
        txtSavePreference.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseDown(MouseEvent e) {
                btnSavePreference.setSelection(!btnSavePreference.getSelection());
            }
        });

        // Load data
        StyledString label = createRequirementText();
        txtRequirement.setText(label.getString());
        txtRequirement.setStyleRanges(label.getStyleRanges());

        viewer.setInput(candidates);

        updateSavePreferenceText();

        // Layout
        GridLayout layout;
        GridData gd;

        gd = new GridData(SWT.FILL, SWT.FILL, true, true);
        contents.setLayoutData(gd);
        layout = new GridLayout(1, false);
        contents.setLayout(layout);

        gd = new GridData(SWT.FILL, SWT.NONE, true, false);
        gd.horizontalIndent = 5;
        txtRequirement.setLayoutData(gd);
        gd = new GridData(SWT.FILL, SWT.NONE, true, false);

        gd = new GridData(SWT.FILL, SWT.FILL, true, true);
        lowerPanel.setLayoutData(gd);

        layout = new GridLayout(2, false);
        layout.marginHeight = 0;
        layout.marginWidth = 0;
        lowerPanel.setLayout(layout);

        gd = new GridData(SWT.FILL, SWT.FILL, true, true, 1, 3);
        gd.widthHint = 450;
        gd.heightHint = 250;
        tbl.setLayoutData(gd);

        gd = new GridData(SWT.FILL, SWT.NONE, false, false);
        btnUp.setLayoutData(gd);
        gd = new GridData(SWT.FILL, SWT.NONE, false, false);
        btnDown.setLayoutData(gd);

        gd = new GridData(SWT.FILL, SWT.NONE, true, false);
        cmpPreferences.setLayoutData(gd);

        layout = new GridLayout(2, false);
        layout.marginHeight = 0;
        layout.marginWidth = 0;
        cmpPreferences.setLayout(layout);

        gd = new GridData(SWT.LEFT, SWT.CENTER, false, false);
        btnSavePreference.setLayoutData(gd);
        gd = new GridData(SWT.FILL, SWT.CENTER, true, false);
        txtSavePreference.setLayoutData(gd);

        return contents;
    }

    private void updateSavePreferenceText() {
        Resource resource = candidates.get(0).getResource();
        Capability identity = ResourceUtils.getIdentityCapability(resource);
        String name = ResourceUtils.getIdentity(identity);

        StyledString label = new StyledString("Save top candidate (");
        label.append(name, UIConstants.BOLD_STYLER);
        label.append(") as a ");
        label.append("preferred resource.", UIConstants.ITALIC_STYLER);

        txtSavePreference.setText(label.getString());
        txtSavePreference.setStyleRanges(label.getStyleRanges());
    }

    protected StyledString createRequirementText() {
        StyledString label = new StyledString();
        label.append("Namespace: ");
        label.append(requirement.getNamespace() + "\n", UIConstants.BOLD_STYLER);

        label.append("Filter: ");
        R5LabelFormatter.appendRequirementLabel(label, requirement);
        label.append("\n");

        for (Entry<String,String> entry : requirement.getDirectives().entrySet()) {
            String key = entry.getKey();
            if (!Namespace.REQUIREMENT_FILTER_DIRECTIVE.equals(key) && !Namespace.REQUIREMENT_RESOLUTION_DIRECTIVE.equals(key))
                label.append("    " + key + ":=" + entry.getValue() + "\n");
        }

        if (Namespace.RESOLUTION_OPTIONAL.equals(requirement.getDirectives().get(Namespace.REQUIREMENT_RESOLUTION_DIRECTIVE)))
            label.append("Optionally ", UIConstants.ITALIC_STYLER);
        label.append("Required by Resource: ");
        R5LabelFormatter.appendResourceLabel(label, requirement.getResource());

        return label;
    }

    @Override
    protected void createButtonsForButtonBar(Composite parent) {
        createButton(parent, IDialogConstants.OK_ID, IDialogConstants.NEXT_LABEL, true);
        createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
    }

    private static class CapabilityResourceLabelProvider extends ImageCachingLabelProvider {

        public CapabilityResourceLabelProvider() {
            super(Plugin.PLUGIN_ID);
        }

        @Override
        public void update(ViewerCell cell) {
            Capability capability = (Capability) cell.getElement();

            StyledString label = new StyledString();
            R5LabelFormatter.appendResourceLabel(label, capability.getResource());

            label.append(" (provides ", StyledString.QUALIFIER_STYLER);
            R5LabelFormatter.appendCapability(label, capability);
            label.append(")", StyledString.QUALIFIER_STYLER);

            cell.setText(label.getString());
            cell.setStyleRanges(label.getStyleRanges());
        }
    }

}
