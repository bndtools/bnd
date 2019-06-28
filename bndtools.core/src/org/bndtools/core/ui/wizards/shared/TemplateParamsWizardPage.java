package org.bndtools.core.ui.wizards.shared;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bndtools.templating.Template;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.jface.preference.JFacePreferences;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
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

	private final Set<String>			fixedAttribs	= new HashSet<>();

	private Template					template;
	private Composite					container;
	private Control						currentPanel;

	private boolean						skip			= false;

	private final Map<String, String>	values			= new HashMap<>();

	private ObjectClassDefinition		ocd;

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
			GridLayout layout = new GridLayout(2, false);
			layout.horizontalSpacing = 15;
			panel.setLayout(layout);

			List<Control> fieldControls = new LinkedList<>();
			try {
				ocd = template.getMetadata();
				int count = 0;

				Set<String> requiredIds = new HashSet<>();
				for (AttributeDefinition ad : ocd.getAttributeDefinitions(ObjectClassDefinition.REQUIRED)) {
					requiredIds.add(ad.getID());
				}

				AttributeDefinition[] ads = ocd.getAttributeDefinitions(ObjectClassDefinition.ALL);
				for (AttributeDefinition ad : ads) {
					String attrib = ad.getID();
					if (!fixedAttribs.contains(attrib)) {
						Label label = new Label(panel, SWT.NONE);

						String labelText = ad.getID();
						if (requiredIds.contains(ad.getID())) {
							label.setFont(JFaceResources.getFontRegistry()
								.getBold(JFaceResources.DEFAULT_FONT));
						}
						label.setText(labelText);

						Control fieldControl = createFieldControl(panel, ad);
						fieldControl.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
						fieldControls.add(fieldControl);
						skip = false;
						count++;
					}
				}

				if (count == 0) {
					setMessage("No editable parameters for template: " + ocd.getName(), INFORMATION);
				} else {
					setMessage("Edit parameters for template: " + ocd.getDescription(), INFORMATION);
				}
				setErrorMessage(null);
			} catch (Exception e) {
				setErrorMessage("Error loading template metadata: " + e.getMessage());
				for (Control fieldControl : fieldControls) {
					fieldControl.setEnabled(false);
				}
			}
		}
		currentPanel = panel;
		container.layout(true, true);
		updateValidation();
	}

	private void updateValidation() {
		boolean complete = true;

		// Check required attribs
		AttributeDefinition[] ads = ocd != null ? ocd.getAttributeDefinitions(ObjectClassDefinition.REQUIRED)
			: new AttributeDefinition[0];
		for (AttributeDefinition ad : ads) {
			// Skip attribs provided the wizard
			if (fixedAttribs.contains(ad.getID()))
				continue;
			String value = values.get(ad.getID());
			if (value == null || value.trim()
				.isEmpty()) {
				complete = false;
				break;
			}
		}

		// Validate values
		// TODO

		setPageComplete(complete);
	}

	private Control createFieldControl(Composite parent, final AttributeDefinition ad) {
		switch (ad.getType()) {
			case AttributeDefinition.STRING :
			case AttributeDefinition.INTEGER :
				final Text text = new Text(parent, SWT.BORDER);
				if (ad.getName() != null)
					text.setMessage(ad.getName());

				if (ad.getDescription() != null) {
					ControlDecoration decor = new ControlDecoration(text, SWT.LEFT, parent);
					decor.setImage(FieldDecorationRegistry.getDefault()
						.getFieldDecoration(FieldDecorationRegistry.DEC_INFORMATION)
						.getImage());
					decor.setShowHover(true);
					decor.setDescriptionText(ad.getDescription());
					decor.setMarginWidth(5);
				}

				String[] defaultValue = ad.getDefaultValue();
				if (defaultValue != null && defaultValue.length == 1) {
					text.setText(defaultValue[0]);
					values.put(ad.getID(), defaultValue[0]);
				}
				text.addModifyListener(ev -> {
					values.put(ad.getID(), text.getText());
					updateValidation();
				});

				return text;

			default :
				Label label = new Label(parent, SWT.NONE);
				label.setText("<Unknown Attribute Type>");
				label.setFont(JFaceResources.getFontRegistry()
					.getItalic(JFaceResources.DEFAULT_FONT));
				label.setForeground(JFaceResources.getColorRegistry()
					.get(JFacePreferences.ERROR_COLOR));
				return label;
		}
	}

	@Override
	public boolean shouldSkip() {
		return skip;
	}

	public Map<String, String> getValues() {
		return values;
	}

}
