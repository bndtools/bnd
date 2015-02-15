package bndtools.views.repository;

import org.bndtools.core.ui.icons.Icons;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.graphics.Device;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.osgi.framework.Constants;
import org.osgi.namespace.service.ServiceNamespace;
import org.osgi.resource.Requirement;

import aQute.bnd.osgi.resource.CapReqBuilder;

public class ServiceSearchPanel extends SearchPanel {

    private Composite cmpServiceSearch;
    private Text txtServiceSearchName;

    @Override
    public Control createControl(Composite parent) {
        cmpServiceSearch = new Composite(parent, SWT.NONE);

        GridLayout layout = new GridLayout(2, false);
        layout.verticalSpacing = 10;
        cmpServiceSearch.setLayout(layout);

        Label lblInstruction = new Label(cmpServiceSearch, SWT.WRAP | SWT.LEFT);
        lblInstruction.setText("Enter a service interface type name, which may contain wildcard characters (\"*\").");
        lblInstruction.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false, 2, 1));

        new Label(cmpServiceSearch, SWT.NONE).setText("Service Interface:");
        txtServiceSearchName = new Text(cmpServiceSearch, SWT.BORDER);
        txtServiceSearchName.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
        txtServiceSearchName.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
                update();
            }
        });
        return cmpServiceSearch;
    }

    public void update() {
        String serviceClass = txtServiceSearchName.getText();
        if (serviceClass == null || serviceClass.trim().isEmpty()) {
            setError("Service class filter cannot be empty");
            setRequirement(null);
        } else {
            String filter = String.format("(%s=%s)", Constants.OBJECTCLASS, serviceClass);
            Requirement requirement = new CapReqBuilder(ServiceNamespace.SERVICE_NAMESPACE).addDirective(ServiceNamespace.REQUIREMENT_FILTER_DIRECTIVE, filter).buildSyntheticRequirement();
            setError(null);
            setRequirement(requirement);
        }
    }

    @Override
    public void setFocus() {
        txtServiceSearchName.setFocus();
    }

    @Override
    public Image createImage(Device device) {
        return Icons.desc("service").createImage(device);
    }

}
