package bndtools.views.repository;

import org.bndtools.core.ui.icons.Icons;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Device;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IMemento;
import org.osgi.namespace.service.ServiceNamespace;
import org.osgi.resource.Namespace;
import org.osgi.resource.Requirement;

import aQute.bnd.osgi.resource.CapReqBuilder;

public class ServiceSearchPanel extends SearchPanel {

	private String	serviceClass;
	private Control	focusControl;

	@Override
	public Control createControl(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);

		GridLayout layout = new GridLayout(2, false);
		layout.verticalSpacing = 10;
		container.setLayout(layout);

		Label lblInstruction = new Label(container, SWT.WRAP | SWT.LEFT);
		lblInstruction.setText("Enter a service interface type name, which may contain wildcard characters (\"*\").");
		lblInstruction.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false, 2, 1));

		new Label(container, SWT.NONE).setText("Service Interface:");
		final Text txtName = new Text(container, SWT.BORDER);
		if (serviceClass != null)
			txtName.setText(serviceClass);
		txtName.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		txtName.addModifyListener(e -> {
			serviceClass = txtName.getText()
				.trim();
			validate();
		});
		this.focusControl = txtName;
		validate();
		return container;
	}

	public void validate() {
		if (serviceClass == null || serviceClass.trim()
			.isEmpty()) {
			setError(null);
			setRequirement(null);
		} else {
			String filter = String.format("(%s=%s)", ServiceNamespace.CAPABILITY_OBJECTCLASS_ATTRIBUTE, serviceClass);
			Requirement requirement = new CapReqBuilder(ServiceNamespace.SERVICE_NAMESPACE)
				.addDirective(Namespace.REQUIREMENT_FILTER_DIRECTIVE, filter)
				.buildSyntheticRequirement();
			setError(null);
			setRequirement(requirement);
		}
	}

	@Override
	public void setFocus() {
		focusControl.setFocus();
	}

	@Override
	public Image createImage(Device device) {
		return Icons.desc("service")
			.createImage(device);
	}

	@Override
	public void saveState(IMemento memento) {
		memento.putString("serviceClass", serviceClass);
	}

	@Override
	public void restoreState(IMemento memento) {
		serviceClass = memento.getString("serviceClass");
	}

}
