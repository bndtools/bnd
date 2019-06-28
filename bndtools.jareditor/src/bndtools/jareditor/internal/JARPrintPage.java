package bndtools.jareditor.internal;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.net.URI;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.editor.FormPage;

public class JARPrintPage extends FormPage {

	private Text	text;

	private boolean	loaded	= false;

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

	@Override
	public void setActive(boolean active) {
		super.setActive(active);
		if (active && !loaded) {
			refresh();
		}
	}

	public void refresh() {
		URI uri = null;
		try {
			uri = URIHelper.retrieveFileURI(getEditorInput());
			if (uri != null) {
				text.setText(print(new File(uri)));
			}
		} catch (Exception e) {
			Plugin.getDefault()
				.getLog()
				.log(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Error outputing JAR content display.", e));
		} finally {
			loaded = true;
		}
	}

	private static String print(File file) throws Exception {
		try (Printer printer = new Printer()) {
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			printer.setOut(new PrintStream(bos, false, "UTF-8"));

			/* Printer.METATYPE throws an NPE */
			int options = Printer.VERIFY | Printer.MANIFEST | Printer.LIST | Printer.IMPEXP | Printer.USES
				| Printer.USEDBY | Printer.COMPONENT;

			printer.doPrint(file.getAbsolutePath(), options);
			return bos.toString("UTF-8");
		}
	}
}
