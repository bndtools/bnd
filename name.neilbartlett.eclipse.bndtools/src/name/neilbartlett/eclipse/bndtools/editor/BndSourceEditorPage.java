package name.neilbartlett.eclipse.bndtools.editor;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.eclipse.core.resources.IMarker;
import org.eclipse.jdt.internal.ui.propertiesfileeditor.PropertiesFileEditor;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentExtension2;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.editor.IFormPage;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.texteditor.IDocumentProvider;

@SuppressWarnings("restriction")
public class BndSourceEditorPage extends PropertiesFileEditor implements IFormPage {

	private static final String ISO_8859_1 = "ISO-8859-1";
	private final BndEditor formEditor;
	private final String id;
	
	private int index;
	private boolean stale = false;
	
	private final PropertyChangeListener propChangeListener = new PropertyChangeListener() {
		public void propertyChange(PropertyChangeEvent evt) {
			stale = true;
		}
	};

	private Control control;
	
	public BndSourceEditorPage(String id, BndEditor formEditor) {
		this.id = id;
		this.formEditor = formEditor;
		this.formEditor.getBndModel().addPropertyChangeListener(propChangeListener);
	}
	
	@Override
	public void dispose() {
		this.formEditor.getBndModel().removePropertyChangeListener(propChangeListener);
		super.dispose();
	}

	public boolean canLeaveThePage() {
		return true;
	}

	public FormEditor getEditor() {
		return formEditor;
	}

	public String getId() {
		return id;
	}

	public void setIndex(int index) {
		this.index = index;
	}
	
	public int getIndex() {
		return index;
	}

	public IManagedForm getManagedForm() {
		return null;
	}
	
	@Override
	public void createPartControl(Composite parent) {
		super.createPartControl(parent);

		Control[] children = parent.getChildren();
		control = children[children.length - 1];
	}

	public Control getPartControl() {
		return control;
	}

	public void initialize(FormEditor formEditor) {
	}

	public boolean isActive() {
		return this.equals(formEditor.getActivePageInstance());
	}

	public boolean isEditor() {
		return true;
	}

	public boolean selectReveal(Object object) {
		if (object instanceof IMarker) {
			IDE.gotoMarker(this, (IMarker) object);
			return true;
		}
		return false;
	}

	public void setActive(boolean active) {
		if(active) {
			System.out.println("!! Setting edit page ACTIVE");
			if(stale)
				refresh();
		} else {
			System.out.println("!! Setting edit page INACTIVE");
			if(isDirty())
				commit(false);
		}
	}
	
	void commit(boolean onSave) {
		try {
			String text = getDocument().get();
			ByteArrayInputStream stream = new ByteArrayInputStream(text
					.getBytes(ISO_8859_1));
			formEditor.getBndModel().loadFrom(stream);
		} catch (IOException e) {
			// TODO
			e.printStackTrace();
		}
	}

	void refresh() {
		IDocument document = null;
		try {
			ByteArrayOutputStream buffer = new ByteArrayOutputStream();
			formEditor.getBndModel().saveTo(buffer);
			String text = buffer.toString(ISO_8859_1);
			
			Point selected = getSourceViewer().getSelectedRange();
			document = getDocument();
			if(document instanceof IDocumentExtension2) {
				((IDocumentExtension2) document).stopListenerNotification();
			}
			document.set(text);
			getSourceViewer().setSelectedRange(selected.x, 0);
			stale = false;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			if(document != null && document instanceof IDocumentExtension2) {
				((IDocumentExtension2) document).resumeListenerNotification();
			}
		}
	}

	private IDocument getDocument() {
		IDocumentProvider docProvider = getDocumentProvider();
		IEditorInput input = getEditorInput();
		IDocument doc = docProvider.getDocument(input);
		return doc;
	}
}
