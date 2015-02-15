package bndtools.views.repository;

import org.bndtools.core.ui.icons.Icons;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Device;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;
import org.osgi.framework.namespace.PackageNamespace;
import aQute.bnd.osgi.resource.CapReqBuilder;
import aQute.bnd.version.VersionRange;

public class PackageSearchPanel extends SearchPanel {

    private Composite cmpPackageSearch;
    private Text txtPackageSearchName;
    private Text txtPackageSearchVersion;
    private Label lblPackageSearchVersionHint;

    @Override
    public Control createControl(Composite parent) {

        Listener modifyListener = new Listener() {
            @Override
            public void handleEvent(Event event) {
                update();
            }
        };

        cmpPackageSearch = new Composite(parent, SWT.NONE);
        GridLayout layout = new GridLayout(3, false);
        layout.verticalSpacing = 10;
        cmpPackageSearch.setLayout(layout);

        Label lblInstruction = new Label(cmpPackageSearch, SWT.WRAP | SWT.LEFT);
        lblInstruction.setText("Enter a package name, which may contain wildcard characters (\"*\").");
        lblInstruction.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false, 3, 1));

        new Label(cmpPackageSearch, SWT.NONE).setText("Package Name:");
        txtPackageSearchName = new Text(cmpPackageSearch, SWT.BORDER);
        txtPackageSearchName.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
        txtPackageSearchName.addListener(SWT.Modify, modifyListener);

        new Label(cmpPackageSearch, SWT.NONE).setText("Version Range:");
        txtPackageSearchVersion = new Text(cmpPackageSearch, SWT.BORDER);
        txtPackageSearchVersion.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
        txtPackageSearchVersion.addListener(SWT.Modify, modifyListener);

        lblPackageSearchVersionHint = new Label(cmpPackageSearch, SWT.NONE);
        lblPackageSearchVersionHint.setText("Example: [1.0, 2.0)");

        return cmpPackageSearch;
    }

    private void update() {
        try {
            String filter = null;

            String packageName = txtPackageSearchName.getText();
            if (packageName == null || packageName.trim().isEmpty())
                throw new IllegalArgumentException("Package filter cannot be empty");

            VersionRange versionRange = null;
            String versionRangeStr = txtPackageSearchVersion.getText();
            if (versionRangeStr != null && versionRangeStr.trim().length() > 0) {
                try {
                    versionRange = new VersionRange(versionRangeStr);
                } catch (Exception e) {
                    throw new IllegalArgumentException("Invalid version range: " + e.getMessage());
                }
            }
            filter = formatPackageRequirement(packageName, versionRange);
            if (filter != null)
                setRequirement(new CapReqBuilder(PackageNamespace.PACKAGE_NAMESPACE).addDirective(PackageNamespace.REQUIREMENT_FILTER_DIRECTIVE, filter).buildSyntheticRequirement());
            setError(null);
        } catch (Exception e) {
            setError(e.getMessage());
            setRequirement(null);
        }
    }

    private String formatPackageRequirement(String packageName, VersionRange versionRange) {
        String filter;

        if (versionRange == null) {
            filter = String.format("(%s=%s)", PackageNamespace.PACKAGE_NAMESPACE, packageName.trim());
        } else {
            StringBuilder builder = new StringBuilder();
            builder.append("(&(").append(PackageNamespace.PACKAGE_NAMESPACE).append('=').append(packageName).append(')');
            if (versionRange.includeLow())
                builder.append("(version>=").append(versionRange.getLow()).append(')');
            else
                builder.append("(!(version<=").append(versionRange.getLow()).append("))");
            if (versionRange.isRange()) {
                if (versionRange.includeHigh())
                    builder.append("(version<=").append(versionRange.getHigh()).append(')');
                else
                    builder.append("(!(version>=").append(versionRange.getHigh()).append("))");
            }
            builder.append(')');
            filter = builder.toString();
        }

        return filter;
    }

    @Override
    public void setFocus() {
        txtPackageSearchName.setFocus();
    }

    @Override
    public Image createImage(Device device) {
        return Icons.desc("package").createImage(device);
    }

}
