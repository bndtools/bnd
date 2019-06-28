package bndtools.views.repository;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IMemento;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.namespace.service.ServiceNamespace;
import org.osgi.resource.Namespace;

import aQute.bnd.osgi.resource.CapReqBuilder;

public class ArbitraryNamespaceSearchPanel extends SearchPanel {

	private String	namespace;
	private String	filterStr	= "";

	private Control	focusControl;
	private Label	lblFilterHint;

	@Override
	public Control createControl(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout(2, false);
		container.setLayout(layout);

		Label lblInstruction = new Label(container, SWT.WRAP | SWT.LEFT);
		lblInstruction.setText(
			"Enter a capability namespace and filter expression in OSGi standard format. Refer to OSGi Core specification, section 3.2.7 \"Filter Syntax\".");
		lblInstruction.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false, 2, 1));

		new Label(container, SWT.NONE).setText("Namespace:");
		final Text txtNamespace = new Text(container, SWT.BORDER);
		if (namespace != null)
			txtNamespace.setText(namespace);
		txtNamespace.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		txtNamespace.addModifyListener(e -> {
			namespace = txtNamespace.getText()
				.trim();
			validate();
		});

		new Label(container, SWT.NONE).setText("Filter Expression:");
		@SuppressWarnings("unused")
		Label lblSpacer2 = new Label(container, SWT.NONE); // spacer

		final Text txtFilter = new Text(container, SWT.MULTI | SWT.BORDER);
		txtFilter.setMessage("enter OSGi-style filter");
		if (filterStr != null)
			txtFilter.setText(filterStr);
		GridData gdArbitrarySearchFilter = new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1);
		gdArbitrarySearchFilter.heightHint = 50;
		txtFilter.setLayoutData(gdArbitrarySearchFilter);
		txtFilter.addModifyListener(e -> {
			filterStr = txtFilter.getText()
				.trim();
			validate();
		});

		lblFilterHint = new Label(container, SWT.NONE);
		lblFilterHint.setText("Example: (&&(name=value)(version>=1.0))");
		lblFilterHint.setLayoutData(new GridData(SWT.FILL, SWT.NONE, true, false, 2, 1));

		validate();

		focusControl = txtNamespace;
		return container;
	}

	private void validate() {
		try {
			if (namespace == null || namespace.length() == 0) {
				setError(null);
				setRequirement(null);
				return;
			}

			for (int i = 0; i < namespace.length(); i++) {
				char c = namespace.charAt(i);
				if ('.' == c) {
					if (i == 0 || i == namespace.length() - 1)
						throw new IllegalArgumentException("Namespace cannot have leading or trailing '.' character");
					else if ('.' == namespace.charAt(i - 1))
						throw new IllegalArgumentException("Namespace cannot have repeated '.' characters");
				} else if (!Character.isLetterOrDigit(c) && c != '-' && c != '_')
					throw new IllegalArgumentException(String.format("Invalid character in namespace: '%c'", c));
			}
			updateFilterExpressionHint(namespace);

			CapReqBuilder builder = new CapReqBuilder(namespace);
			if (filterStr != null && filterStr.trim()
				.length() > 0) {
				try {
					Filter filter = FrameworkUtil.createFilter(filterStr.trim());
					builder.addDirective(Namespace.REQUIREMENT_FILTER_DIRECTIVE, filter.toString());
				} catch (InvalidSyntaxException e) {
					throw new IllegalArgumentException("Invalid filter string: " + e.getMessage());
				}
			}
			setRequirement(builder.buildSyntheticRequirement());
			setError(null);
		} catch (Exception e) {
			setError(e.getMessage());
			setRequirement(null);
		}
	}

	private void updateFilterExpressionHint(String namespace) {
		String hint;
		if (ServiceNamespace.SERVICE_NAMESPACE.equals(namespace))
			hint = String.format("(%s=fully-qualified-classname)", Constants.OBJECTCLASS);
		else
			// double ampersand because it's a mnemonic in SWT... FFS!
			hint = String.format("(&&(%s=value)(version>=1.0))", namespace);

		lblFilterHint.setText("Example: " + hint);
	}

	@Override
	public void setFocus() {
		focusControl.setFocus();
	}

	@Override
	public void saveState(IMemento memento) {
		memento.putString("namespace", namespace);
		memento.putString("filter", filterStr);
	}

	@Override
	public void restoreState(IMemento memento) {
		namespace = memento.getString("namespace");
		filterStr = memento.getString("filter");
	}

}
