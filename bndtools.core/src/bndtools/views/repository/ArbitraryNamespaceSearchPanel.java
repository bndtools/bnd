package bndtools.views.repository;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.namespace.service.ServiceNamespace;
import org.osgi.resource.Namespace;
import aQute.bnd.osgi.resource.CapReqBuilder;

public class ArbitraryNamespaceSearchPanel extends SearchPanel {

    private Composite grpArbitrarySearch;
    private Text txtArbitrarySearchNamespace;
    private Text txtArbitrarySearchFilter;
    private Label lblArbitrarySearchFilterHint;

    @Override
    public Control createControl(Composite parent) {

        ModifyListener validationListener = new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
                validate();
            }
        };

        grpArbitrarySearch = new Composite(parent, SWT.NONE);
        GridLayout layout = new GridLayout(2, false);
        grpArbitrarySearch.setLayout(layout);

        Label lblInstruction = new Label(grpArbitrarySearch, SWT.WRAP | SWT.LEFT);
        lblInstruction.setText("Enter a capability namespace and filter expression in OSGi standard format. Refer to OSGi Core specification, section 3.2.7 \"Filter Syntax\".");
        lblInstruction.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false, 2, 1));

        new Label(grpArbitrarySearch, SWT.NONE).setText("Namespace:");
        txtArbitrarySearchNamespace = new Text(grpArbitrarySearch, SWT.BORDER);
        txtArbitrarySearchNamespace.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
        txtArbitrarySearchNamespace.addModifyListener(validationListener);

        new Label(grpArbitrarySearch, SWT.NONE).setText("Filter Expression:");
        @SuppressWarnings("unused")
        Label lblSpacer2 = new Label(grpArbitrarySearch, SWT.NONE); // spacer

        txtArbitrarySearchFilter = new Text(grpArbitrarySearch, SWT.MULTI | SWT.BORDER);
        txtArbitrarySearchFilter.setMessage("enter OSGi-style filter");
        GridData gdArbitrarySearchFilter = new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1);
        gdArbitrarySearchFilter.heightHint = 50;
        txtArbitrarySearchFilter.setLayoutData(gdArbitrarySearchFilter);
        txtArbitrarySearchFilter.addModifyListener(validationListener);

        lblArbitrarySearchFilterHint = new Label(grpArbitrarySearch, SWT.NONE);
        lblArbitrarySearchFilterHint.setText("Example: (&&(name=value)(version>=1.0))");
        lblArbitrarySearchFilterHint.setLayoutData(new GridData(SWT.FILL, SWT.NONE, true, false, 2, 1));

        return grpArbitrarySearch;
    }

    private void validate() {
        try {
            String namespace = txtArbitrarySearchNamespace.getText();
            if (namespace == null || namespace.length() == 0)
                throw new IllegalArgumentException("Namespace cannot be empty");

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
            String filterStr = txtArbitrarySearchFilter.getText();
            if (filterStr == null || filterStr.trim().length() == 0)
                throw new IllegalArgumentException("Filter cannot be empty");

            try {
                Filter filter = FrameworkUtil.createFilter(filterStr.trim());
                setRequirement(new CapReqBuilder(namespace).addDirective(Namespace.REQUIREMENT_FILTER_DIRECTIVE, filter.toString()).buildSyntheticRequirement());
            } catch (InvalidSyntaxException e) {
                throw new IllegalArgumentException("Invalid filter string: " + e.getMessage());
            }
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

        lblArbitrarySearchFilterHint.setText("Example: " + hint);
    }

    @Override
    public void setFocus() {
        txtArbitrarySearchNamespace.setFocus();
    }

}
