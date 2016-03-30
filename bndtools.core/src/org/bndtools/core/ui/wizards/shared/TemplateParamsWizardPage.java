package org.bndtools.core.ui.wizards.shared;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.bndtools.templating.Template;
import org.eclipse.jface.preference.JFacePreferences;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.osgi.service.metatype.AttributeDefinition;
import org.osgi.service.metatype.ObjectClassDefinition;

import bndtools.Plugin;

public class TemplateParamsWizardPage extends WizardPage implements ISkippableWizardPage {

    private final Set<String> fixedAttribs = new HashSet<>();

    private Template template;
    private Composite container;
    private Control currentPanel;

    private boolean skip = false;

    private final Map<String,String> values = new HashMap<>();

    public TemplateParamsWizardPage(String[] fixedAttribs) {
        super("templateParams");
        for (String attrib : fixedAttribs) {
            this.fixedAttribs.add(attrib);
        }
    }

    @Override
    public void createControl(Composite parent) {
        setTitle("Template Parameters");
        setImageDescriptor(Plugin.imageDescriptorFromPlugin("icons/bndtools-wizban.png")); //$NON-NLS-1$

        container = new Composite(parent, SWT.NONE);
        setControl(container);

        container.setLayout(new GridLayout(1, false));
        updateUI();
    }

    public void setTemplate(Template template) {
        this.template = template;
        this.values.clear();
        if (container != null && !container.isDisposed())
            updateUI();
    }

    void updateUI() {
        if (currentPanel != null && !currentPanel.isDisposed()) {
            currentPanel.dispose();
            currentPanel = null;
        }

        Composite panel = new Composite(container, SWT.NONE);
        panel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        skip = true;
        if (template == null) {
            setErrorMessage(null);
            setMessage("No template is loaded", WARNING);
        } else {
            panel.setLayout(new GridLayout(2, false));
            try {
                ObjectClassDefinition ocd = template.getMetadata();
                AttributeDefinition[] ads = ocd.getAttributeDefinitions(ObjectClassDefinition.ALL);
                int count = 0;
                for (AttributeDefinition ad : ads) {
                    String attrib = ad.getName();
                    if (!fixedAttribs.contains(attrib)) {
                        Label label = new Label(panel, SWT.NONE);
                        label.setText(ad.getDescription());

                        Control fieldControl = createFieldControl(panel, ad);
                        fieldControl.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
                        count++;
                    }
                }

                if (count == 0) {
                    setMessage("No editable parameters for template: " + ocd.getName(), INFORMATION);
                } else {
                    setMessage("Edit parameters for template: " + ocd.getName(), INFORMATION);
                    skip = false;
                }
                setErrorMessage(null);
            } catch (Exception e) {
                setErrorMessage("Error loading template metadata: " + e.getMessage());
            }
        }
        currentPanel = panel;
        container.layout(true, true);
    }

    private Control createFieldControl(Composite parent, final AttributeDefinition ad) {
        switch (ad.getType()) {
        case AttributeDefinition.STRING :
            final Text text = new Text(parent, SWT.BORDER);
            text.setMessage(ad.getDescription());
            text.addModifyListener(new ModifyListener() {
                @Override
                public void modifyText(ModifyEvent ev) {
                    values.put(ad.getName(), text.getText());
                }
            });
            return text;
        default :
            Label label = new Label(parent, SWT.NONE);
            label.setText("< Unknown Attribute Type >");
            label.setFont(JFaceResources.getFontRegistry().getItalic(JFaceResources.DEFAULT_FONT));
            label.setForeground(JFaceResources.getColorRegistry().get(JFacePreferences.ERROR_COLOR));
            return label;
        }
    }

    @Override
    public boolean shouldSkip() {
        return skip;
    }

    public Map<String,String> getValues() {
        return values;
    }

}
