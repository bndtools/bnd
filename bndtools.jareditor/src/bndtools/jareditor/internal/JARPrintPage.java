package bndtools.jareditor.internal;

import java.net.URI;

import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.editor.FormPage;

import aQute.bnd.osgi.Jar;
import aQute.bnd.print.JarPrinter;

public class JARPrintPage extends FormPage {

	private Text	text;
	private URI		uri;
	private boolean	loading;

	public JARPrintPage(FormEditor formEditor, String id, String title) {
		super(formEditor, id, title);
	}

	@Override
	public void createPartControl(Composite parent) {
		text = new Text(parent, SWT.NONE | SWT.H_SCROLL | SWT.V_SCROLL);
		text.setEditable(false);
		text.setFont(JFaceResources.getTextFont());
	}

	@Override
	public Control getPartControl() {
		return text;
	}

	private static String print(URI uri) throws Exception {
		try (JarPrinter printer = new JarPrinter()) {
			int options = -1;
			try (Jar jar = new Jar(uri.toString(), uri.toURL()
				.openStream())) {
				printer.doPrint(jar, options, false, false);
				return printer.toString();
			}
		}
	}

	@Override
	public void setActive(boolean active) {
		super.setActive(active);
		if (active)
			update();
	}

	private void update() {
		if (loading || text == null || !isActive()) {
			return;
		}
		loading = true;
		JAREditor.background("Printing ZIP file", monitor -> {
			return print(uri);
		}, text::setText);
	}

	public void setInput(URI uri) {
		this.uri = uri;
		loading = false;
		update();
	}
}
