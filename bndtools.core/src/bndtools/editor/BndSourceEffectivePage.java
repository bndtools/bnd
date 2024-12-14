package bndtools.editor;

import java.util.ResourceBundle;
import java.util.Set;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IHandler;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IFindReplaceTarget;
import org.eclipse.jface.text.TextViewer;
import org.eclipse.jface.text.source.CompositeRuler;
import org.eclipse.jface.text.source.LineNumberRulerColumn;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.widgets.Form;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.handlers.IHandlerService;
import org.eclipse.ui.texteditor.FindReplaceAction;

import aQute.bnd.build.model.BndEditModel;
import aQute.bnd.exceptions.Exceptions;
import aQute.bnd.osgi.Processor;
import bndtools.editor.completion.BndSourceViewerConfiguration;

/**
 * The 'Effective source' tab in a Bnd Editor which renders an effective
 * readonly view of all properties of the BndEditModel. This means that it shows
 * all available properties which are pulled in via various include-mechanisms.
 * This it is a useful debugging tool to make things visible to the human eye.
 */
public class BndSourceEffectivePage extends FormPage {


	private final BndEditor		bndEditor;
	private BndEditModel	editModel;
	private SourceViewer	viewer;
	private StyledText	styledText;
	private boolean		loading;

	public BndSourceEffectivePage(FormEditor formEditor, String id, String title) {
		super(formEditor, id, title);
		this.bndEditor = ((BndEditor) formEditor);
		this.editModel = bndEditor.getModel();
	}

	@Override
	protected void createFormContent(IManagedForm managedForm) {
		FormToolkit toolkit = managedForm.getToolkit();
		ScrolledForm scrolledForm = managedForm.getForm();
		Form form = scrolledForm.getForm();
		toolkit.setBorderStyle(SWT.NULL);

		Composite body = form.getBody();
		body.setLayout(new FillLayout());

		// ruler for line numbers
		CompositeRuler ruler = new CompositeRuler();
		LineNumberRulerColumn ln = new LineNumberRulerColumn();
		ln.setForeground(Display.getCurrent()
			.getSystemColor(SWT.COLOR_DARK_GRAY));
		ruler.addDecorator(0, ln);

		this.viewer = new SourceViewer(body, ruler, SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL) {
			@Override
			protected boolean canPerformFind() {
				return true;
			}
		};
		viewer.setDocument(new Document());
		viewer.configure(new BndSourceViewerConfiguration(bndEditor, JavaUI.getColorManager()));
		styledText = viewer.getTextWidget();
		styledText.setEditable(false);
		styledText.setFont(JFaceResources.getTextFont());

		activateFindAndReplace(viewer, getSite(), this);


	}

	/**
	 * Setup Eclipse's built in Search / Replace dialog. Also see
	 * {@link #getAdapter(Class)}
	 *
	 * @param textViewer
	 * @param site
	 * @param page
	 */
	private static void activateFindAndReplace(TextViewer textViewer, IWorkbenchPartSite site,
		IWorkbenchPart page) {
		FindReplaceAction findReplaceAction = new FindReplaceAction(
			ResourceBundle.getBundle("org.eclipse.ui.texteditor.ConstructedEditorMessages"), "Editor.FindReplace.",
			page);
		IHandlerService handlerService = site.getService(IHandlerService.class);
		IHandler handler = new AbstractHandler() {

			@Override
			public Object execute(ExecutionEvent event) throws ExecutionException {
				if (textViewer != null && textViewer.getDocument() != null) {
					findReplaceAction.run();
				}
				return null;
			}
		};

		handlerService.activateHandler("org.eclipse.ui.edit.findReplace", handler);
	}


	@Override
	public boolean isEditor() {
		return true;
	}

	@Override
	public void setActive(boolean active) {
		super.setActive(active);

		IActionBars actionBars = getEditorSite().getActionBars();

		if (active) {
			update();
		} else {
		}
	}

	private static String print(BndEditModel model) throws Exception {
		if (model == null) {
			return "...";
		}

		try {
			StringBuilder sb = new StringBuilder();

			Processor p = new BndEditModel(model, true).getProperties();
			Set<String> propertyKeys = p.getPropertyKeys(true);
			for (String k : propertyKeys) {

				String value = p.getProperty(k);
				sb.append(k)
					.append(": ")
					.append(value)
					.append("\n");
			}

			return sb.toString();
		} catch (Exception e) {
			throw Exceptions.duck(e);
		}

	}


	private void update() {
		if (loading || styledText == null || !isActive()) {
			return;
		}
		loading = true;
		try {
			String text = print(editModel);
			viewer.setDocument(new Document(text));
			styledText.setFocus();
		} catch (Exception e) {
			throw Exceptions.duck(e);
		}

	}

	public void setInput(BndEditModel model) {
		this.editModel = model;
		loading = false;
		update();
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T getAdapter(Class<T> adapter) {
		if (IFindReplaceTarget.class.equals(adapter)) {
			// needed for find/replace via CMD+F / Ctrl+F
			return (T) viewer.getFindReplaceTarget();
		}
		return super.getAdapter(adapter);
	}


}
