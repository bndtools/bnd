package bndtools.editor.workspace;

import java.text.MessageFormat;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import aQute.lib.osgi.Constants;
import aQute.libg.header.Attrs;

public class PluginPropertiesPage extends WizardPage {

    private IConfigurationElement configElement;
    private Attrs properties = new Attrs();
    private boolean changed = false;

    private Composite mainComposite;
    private Composite fieldContainer;
    private Text txtPath;

    public PluginPropertiesPage() {
        super("pluginProperties");
    }

    public void createControl(Composite parent) {
        setTitle("Plug-in Configuration");
        setDescription("Set configuration properties for the plug-in.");

        // Create controls
        mainComposite = new Composite(parent, SWT.NONE);

        Group group = new Group(mainComposite, SWT.NONE);
        group.setText("Properties");
        group.setLayout(new FillLayout());
        ScrolledComposite scroller = new ScrolledComposite(group, SWT.V_SCROLL);
        fieldContainer = new Composite(scroller, SWT.NONE);
        scroller.setMinSize(200, 200);
        scroller.setExpandVertical(true);
        scroller.setExpandHorizontal(true);
        scroller.setContent(fieldContainer);

        Label separator = new Label(mainComposite, SWT.SEPARATOR | SWT.HORIZONTAL);

        Composite classpathComposite = new Composite(mainComposite, SWT.NONE);
        new Label(classpathComposite, SWT.NONE).setText("Classpath:");
        txtPath = new Text(classpathComposite, SWT.BORDER);

        resetPropertyFields();

        String path = properties.get(Constants.PATH_DIRECTIVE);
        if (path != null)
            txtPath.setText(path);
        txtPath.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                String path = txtPath.getText();
                if (path == null || path.length() == 0)
                    properties.remove(Constants.PATH_DIRECTIVE);
                else
                    properties.put(Constants.PATH_DIRECTIVE, path);
                changed = true;
            }
        });

        // Layout
        GridLayout layout;
        GridData gd;

        layout = new GridLayout(1, false);
        mainComposite.setLayout(layout);

        gd = new GridData(SWT.FILL, SWT.FILL, true, true);
        group.setLayoutData(gd);
        gd = new GridData(SWT.FILL, SWT.CENTER, true, false);
        separator.setLayoutData(gd);
        gd = new GridData(SWT.FILL, SWT.FILL, true, false);
        classpathComposite.setLayoutData(gd);

        layout = new GridLayout(2, false);
        layout.verticalSpacing = 10;
        layout.horizontalSpacing = 10;
        fieldContainer.setLayout(layout);

        layout = new GridLayout(2, false);
        classpathComposite.setLayout(layout);
        gd = new GridData(SWT.FILL, SWT.CENTER, true, false);
        txtPath.setLayoutData(gd);

        setControl(mainComposite);
    }

    void resetPropertyFields() {
        // Remove existing controls
        Control[] children = fieldContainer.getChildren();
        for (Control child : children)
            child.dispose();

        // Add new ones
        if (configElement != null) {
            IConfigurationElement[] propertyElements = configElement.getChildren("property");
            String path = configElement.getAttribute("path");
            String className = configElement.getAttribute("class");

            String summaryMessage = MessageFormat.format("Found {0,choice,0#no properties|1#one property|1<{0} properties} for plug-in class {1}.", propertyElements.length, className);

            if (path != null) {
                properties.put(Constants.PATH_DIRECTIVE, path);
                txtPath.setText(path);
            }

            for (IConfigurationElement propertyElement : propertyElements) {
                final String name = propertyElement.getAttribute("name");
                String value = properties.get(name);

                String propertyType = propertyElement.getAttribute("type");
                String defaultStr = propertyElement.getAttribute("default");
                if (value == null && defaultStr != null) {
                    value = defaultStr;
                    properties.put(name, defaultStr);
                }

                Label label = new Label(fieldContainer, SWT.NONE);
                label.setText(name);

                if ("boolean".equals(propertyType)) {
                    final Button button = new Button(fieldContainer, SWT.CHECK);
                    button.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false));

                    button.setSelection("true".equalsIgnoreCase(value));

                    button.addSelectionListener(new SelectionAdapter() {
                        @Override
                        public void widgetSelected(SelectionEvent e) {
                            properties.put(name, button.getSelection() ? "true" : "false");
                            changed = true;
                        }
                    });
                } else {
                    final Text text = new Text(fieldContainer, SWT.BORDER);
                    text.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

                    if (value != null) text.setText(value);

                    text.addModifyListener(new ModifyListener() {
                        public void modifyText(ModifyEvent e) {
                            String value = text.getText();
                            if (value == null || value.length() == 0)
                                properties.remove(name);
                            else
                                properties.put(name, value);
                            changed = true;
                        }
                    });
                }

                String description = propertyElement.getAttribute("description");
                if (description != null) {
                    ControlDecoration decoration = new ControlDecoration(label, SWT.RIGHT | SWT.CENTER);
                    decoration.setShowHover(true);
                    decoration.setDescriptionText(description);
                    decoration.setImage(FieldDecorationRegistry.getDefault().getFieldDecoration(FieldDecorationRegistry.DEC_INFORMATION).getImage());
                }
                
                String deprecation = propertyElement.getAttribute("deprecated");
                if (deprecation != null) {
                    ControlDecoration decoration = new ControlDecoration(label, SWT.LEFT | SWT.CENTER);
                    decoration.setShowHover(true);
                    decoration.setDescriptionText("Property deprecated: " + deprecation);
                    decoration.setImage(FieldDecorationRegistry.getDefault().getFieldDecoration(FieldDecorationRegistry.DEC_WARNING).getImage());
                }
            }
            Label summaryLabel = new Label(fieldContainer, SWT.NONE);
            summaryLabel.setText(summaryMessage);
            summaryLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1));
        }
        mainComposite.layout(true, true);
    }

    public IConfigurationElement getConfigElement() {
        return configElement;
    }

    public void setConfigElement(IConfigurationElement configElement) {
        this.configElement = configElement;
        if (Display.getCurrent() != null && fieldContainer != null && !fieldContainer.isDisposed()) {
            resetPropertyFields();
        }
    }

    public void setProperties(Attrs properties) {
        this.properties = properties;
        if (Display.getCurrent() != null && fieldContainer != null && !fieldContainer.isDisposed()) {
            resetPropertyFields();
        }
    }

    public Attrs getProperties() {
        return properties;
    }

    public boolean isChanged() {
        return changed;
    }

}
