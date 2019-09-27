package bndtools.shared;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;

import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

public class URLDialog extends TitleAreaDialog {

	private String			locationStr	= null;
	private URI				location	= null;
	private String			name		= null;
	private final String	title;
	private final boolean	named;

	public URLDialog(Shell parentShell, String title) {
		this(parentShell, title, true);
	}

	public URLDialog(Shell parentShell, String title, boolean named) {
		super(parentShell);
		this.title = title;
		this.named = named;
		setShellStyle(getShellStyle() | SWT.RESIZE);
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		setTitle(title);

		Composite area = (Composite) super.createDialogArea(parent);
		Composite container = new Composite(area, SWT.NONE);
		container.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		GridLayout gl = new GridLayout(3, false);
		container.setLayout(gl);

		Label lblUrl = new Label(container, SWT.NONE);
		lblUrl.setText("URL:");

		final Text txtUrl = new Text(container, SWT.BORDER);
		txtUrl.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		txtUrl.addModifyListener(ev -> {
			locationStr = txtUrl.getText();
			updateFromInput();
		});

		Button btnBrowseFile = new Button(container, SWT.PUSH);
		btnBrowseFile.setText("Local File");
		btnBrowseFile.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				FileDialog fileDialog = new FileDialog(getShell(), SWT.OPEN);
				String path = fileDialog.open();
				if (path != null) {
					File file = new File(path);
					URI fileUri = file.toURI();
					txtUrl.setText(fileUri.toString());
				}
			}
		});

		if (named) {
			new Label(container, SWT.NONE).setText("Name:");
			final Text txtName = new Text(container, SWT.BORDER);
			txtName.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
			@SuppressWarnings("unused")
			Label lblSpacer1 = new Label(container, SWT.NONE); // spacer

			txtName.addModifyListener(ev -> name = txtName.getText());
		}

		// Listeners
		// Load from state
		if (location != null)
			txtUrl.setText(location.toString());

		return area;
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		super.createButtonsForButtonBar(parent);

		Button ok = getButton(OK);
		ok.setText("Add");
		ok.setEnabled(location != null);
	}

	private void updateFromInput() {
		try {
			location = new URI(locationStr);
			setErrorMessage(null);
			getButton(OK).setEnabled(true);
		} catch (URISyntaxException e) {
			setErrorMessage(e.getMessage());
			getButton(OK).setEnabled(false);
		}
	}

	public URI getLocation() {
		return location;
	}

	public String getName() {
		return name;
	}

}
