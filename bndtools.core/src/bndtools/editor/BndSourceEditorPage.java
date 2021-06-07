package bndtools.editor;

import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.regex.Pattern;

import org.bndtools.api.ILogger;
import org.bndtools.api.Logger;
import org.eclipse.core.resources.IMarker;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
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

import aQute.bnd.annotation.plugin.InternalPluginDefinition;
import aQute.bnd.build.model.BndEditModel;
import aQute.bnd.help.Syntax;
import aQute.bnd.osgi.Constants;
import aQute.bnd.properties.IDocument;
import bndtools.Plugin;
import bndtools.central.Central;
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
		setSourceViewerConfiguration(new BndSourceViewerConfiguration(bndEditor, JavaUI.getColorManager()));
	}

	@Override
	protected void initializeEditor() {
		super.initializeEditor();
		setDocumentProvider(new BndSourceDocumentProvider());
		setRulerContextMenuId("#BndSourceRulerContext");
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

	@Override
	protected void editorContextMenuAboutToShow(IMenuManager menu) {
		super.editorContextMenuAboutToShow(menu);

		if (editModel != null) {
			List<InternalPluginDefinition> plugins = Central.getInternalPluginDefinitions();
			Collections.sort(plugins, (a, b) -> a.getName()
				.compareTo(b.getName()));
			MenuManager submenu = new MenuManager("Insert Plugin");
			for (InternalPluginDefinition p : plugins) {

				if (p.getImplementation() == null || p.isHidden())
					continue;

				submenu.add(new Action(p.getName()) {
					@Override
					public void run() {
						commit(false);
						String text = editModel.add(Constants.PLUGIN, p.getTemplate());
						refresh();
					}
				});
			}

			submenu.update(true);
			menu.add(submenu);
		}

		doSyntaxMenu(menu, "Insert Instruction", "-");
		doSyntaxMenu(menu, "Insert Header", "[A-Z]");
		doSyntaxMenu(menu, "Insert Macro", "[a-z]");

		menu.update(true);
	}

	private void doSyntaxMenu(IMenuManager menu, String title, String pattern) {
		MenuManager submenu = new MenuManager(title);
		Pattern p = Pattern.compile(pattern);
		for (Entry<String, Syntax> e : new TreeMap<>(Syntax.HELP).entrySet()) {
			String name = e.getKey();
			if (!p.matcher(name)
				.lookingAt())
				continue;

			Syntax syntax = e.getValue();
			String example = syntax.getExample() == null ? "" : syntax.getExample();

			Action action = new Action(name) {
				@Override
				public void run() {
					insert("\n" + syntax.getInsert() + "\n");
				}
			};
			String help = syntax.getLead();
			if (help != null && !help.isEmpty()) {
				action.setToolTipText(help);
			}
			submenu.add(action);
		}

		submenu.update(true);
		menu.add(submenu);
	}

	private void insert(String text) {
		try {
			IDocumentProvider docProvider = getDocumentProvider();
			IEditorInput input = getEditorInput();
			org.eclipse.jface.text.IDocument document = docProvider.getDocument(input);
			ISelection s = getSelectionProvider().getSelection();
			if (s instanceof ITextSelection) {
				int offset = ((ITextSelection) s).getOffset();
				document.replace(offset, 0, text);
			}
		} catch (BadLocationException e) {}
	}
}
