package bndtools.editor;

import java.beans.PropertyChangeListener;
import java.io.IOException;

import org.bndtools.api.ILogger;
import org.bndtools.api.Logger;
import org.eclipse.core.resources.IMarker;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.editors.text.TextEditor;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.editor.IFormPage;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.eclipse.ui.texteditor.IDocumentProvider;

import aQute.bnd.build.model.BndEditModel;
import aQute.bnd.properties.IDocument;
import bndtools.Plugin;
import bndtools.editor.completion.BndSourceViewerConfiguration;
import bndtools.editor.model.IDocumentWrapper;

public class BndSourceEditorPage extends TextEditor implements IFormPage {
	private static final ILogger			logger				= Logger.getLogger(BndSourceEditorPage.class);

	private final Image						icon;

	private final String					id;
	private final FormEditor				editor;

	private String							lastLoaded;
	private BndEditModel					editModel;

	private int								index;

	private final PropertyChangeListener	propChangeListener	= evt -> {
																	refresh();
																	lastLoaded = getDocument().get();
																};

	private Control							control;

	public BndSourceEditorPage(String id, FormEditor editor) {
		this.id = id;
		this.editor = editor;
		ImageDescriptor iconDescriptor = AbstractUIPlugin.imageDescriptorFromPlugin(Plugin.PLUGIN_ID,
			"icons/page_white_text.png");
		icon = iconDescriptor.createImage();
	}

	@Override
	public void dispose() {
		editModel.removePropertyChangeListener(propChangeListener);
		super.dispose();
		icon.dispose();
	}

	@Override
	public boolean canLeaveThePage() {
		return true;
	}

	@Override
	public FormEditor getEditor() {
		return editor;
	}

	@Override
	public String getId() {
		return id;
	}

	@Override
	public void setIndex(int index) {
		this.index = index;
	}

	@Override
	public int getIndex() {
		return index;
	}

	@Override
	public IManagedForm getManagedForm() {
		return null;
	}

	@Override
	public void createPartControl(Composite parent) {
		super.createPartControl(parent);

		Control[] children = parent.getChildren();
		control = children[children.length - 1];
	}

	@Override
	public Control getPartControl() {
		return control;
	}

	@Override
	public void initialize(FormEditor formEditor) {
		BndEditor bndEditor = (BndEditor) formEditor;
		editModel = bndEditor.getEditModel();
		editModel.addPropertyChangeListener(propChangeListener);
	}

	@Override
	protected void initializeEditor() {
		super.initializeEditor();
		setDocumentProvider(new BndSourceDocumentProvider());
		setRulerContextMenuId("#BndSourceRulerContext");
		setSourceViewerConfiguration(new BndSourceViewerConfiguration(JavaUI.getColorManager()));
	}

	@Override
	public boolean isActive() {
		return this.equals(editor.getActivePageInstance());
	}

	@Override
	public boolean isEditor() {
		return true;
	}

	@Override
	public boolean selectReveal(Object object) {
		if (object instanceof IMarker) {
			IDE.gotoMarker(this, (IMarker) object);
			return true;
		}
		return false;
	}

	@Override
	public void setActive(boolean active) {
		if (!active) {
			commit(false);
		}
	}

	void commit(@SuppressWarnings("unused") boolean onSave) {
		try {
			// Only commit changes to the model if the document text has
			// actually changed since we switched to the page; this prevents us
			// losing selection in the Components and Imports tabs.
			// We can't use the dirty flag for this because "undo" will clear
			// the dirty flag.
			IDocument doc = getDocument();
			String currentContent = doc.get();
			if (!currentContent.equals(lastLoaded))
				editModel.loadFrom(getDocument());
		} catch (IOException e) {
			logger.logError("Error loading model from document.", e);
		}
	}

	void refresh() {
		IDocument document = getDocument();
		editModel.saveChangesTo(document);
	}

	private IDocument getDocument() {
		IDocumentProvider docProvider = getDocumentProvider();
		IEditorInput input = getEditorInput();
		return new IDocumentWrapper(docProvider.getDocument(input));
	}

	@Override
	public Image getTitleImage() {
		return icon;
	}
}
