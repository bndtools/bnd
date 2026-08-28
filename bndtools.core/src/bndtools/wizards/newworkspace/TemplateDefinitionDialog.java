package bndtools.wizards.newworkspace;

import java.net.URI;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import bndtools.util.ui.UI;

/**
 * Asks for a url or file path
 */
class TemplateDefinitionDialog extends Dialog {
	final UI<TemplateDefinitionDialog>	ui	= new UI<>(this);
	private static final String			DEFAULT_TEMPLATE_DEFINITION = "https://raw.githubusercontent.com/bndtools/workspace-templates/refs/heads/master/index.bnd";
	String								path;

	public TemplateDefinitionDialog(Shell parentShell) {
		super(parentShell);
	}

	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText("Template Definitions");
	}

	@Override
	protected Composite createDialogArea(Composite parent) {
		Composite container = (Composite) super.createDialogArea(parent);
		GridLayout layout = new GridLayout(12, false);
		container.setLayout(layout);

		Label label = new Label(container, SWT.NONE);
		label.setText(
			"Template definitions. You can enter a URL or a file path");
		label.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 12, 1));

		Text textField = new Text(container, SWT.BORDER);
		String clipboardUrl = getClipboardUrl();
		path = clipboardUrl != null ? clipboardUrl : DEFAULT_TEMPLATE_DEFINITION;
		textField.setText(path);
		GridData textFieldLayoutData = new GridData(SWT.FILL, SWT.CENTER, true, false, 11, 1);
		textFieldLayoutData.minimumWidth = 200;
		textField.setLayoutData(textFieldLayoutData);

		Button browseButton = new Button(container, SWT.PUSH);
		browseButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
		browseButton.setText("Browse...");
		ui.u("path", path, UI.text(textField));
		browseButton.addSelectionListener(UI.onSelect(x -> browseForFile()));
		return container;
	}

	private String getClipboardUrl() {
		Clipboard clipboard = new Clipboard(getShell().getDisplay());
		try {
			Object contents = clipboard.getContents(TextTransfer.getInstance());
			if (contents instanceof String text) {
				String value = text.trim();
				URI uri = URI.create(value);
				if (uri.isAbsolute() && uri.getHost() != null) {
					return value;
				}
			}
		} catch (IllegalArgumentException e) {
		} finally {
			clipboard.dispose();
		}
		return null;
	}

	private void browseForFile() {
		FileDialog dialog = new FileDialog(getShell());
		String path = dialog.open();
		ui.write(() -> this.path = path);
	}

	public String getSelectedPath() {
		return path;
	}

}
