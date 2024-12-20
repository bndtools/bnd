package bndtools.editor;

import java.io.File;
import java.util.List;
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
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Table;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.widgets.Form;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.handlers.IHandlerService;
import org.eclipse.ui.texteditor.FindReplaceAction;

import aQute.bnd.build.model.BndEditModel;
import aQute.bnd.exceptions.Exceptions;
import aQute.bnd.osgi.Processor;
import aQute.bnd.osgi.Processor.PropertyKey;
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
	private TableViewer		tableViewer;
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
		scrolledForm.setExpandHorizontal(true);
		scrolledForm.setExpandVertical(true);

		Form form = scrolledForm.getForm();
		toolkit.setBorderStyle(SWT.NULL);

		Composite body = form.getBody();
		body.setLayout(new FillLayout());

		// Create a SashForm for horizontal resizing
		SashForm sashForm = new SashForm(body, SWT.HORIZONTAL);
		sashForm.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		createSourceViewer(managedForm, sashForm);
		createTableViewer(managedForm, sashForm);

		// Set the initial weights so each section takes half the space
		sashForm.setWeights(new int[] {
			1, 1
		});

	}

	private void createSourceViewer(IManagedForm managedForm, Composite body) {

		Section section = managedForm.getToolkit()
			.createSection(body, Section.TITLE_BAR | Section.EXPANDED);
		section.setText("Effective Source");
		section.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		Composite sectionClient = managedForm.getToolkit()
			.createComposite(section, SWT.NONE);
		sectionClient.setLayout(new org.eclipse.swt.layout.GridLayout(1, false));
		managedForm.getToolkit()
			.paintBordersFor(sectionClient);
		section.setClient(sectionClient);

		// ruler for line numbers
		CompositeRuler ruler = new CompositeRuler();
		LineNumberRulerColumn ln = new LineNumberRulerColumn();
		ln.setForeground(Display.getCurrent()
			.getSystemColor(SWT.COLOR_DARK_GRAY));
		ruler.addDecorator(0, ln);

		this.viewer = new SourceViewer(sectionClient, ruler, SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL) {
			@Override
			protected boolean canPerformFind() {
				return true;
			}
		};
		viewer.setDocument(new Document());
		viewer.getControl()
			.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		viewer.configure(new BndSourceViewerConfiguration(bndEditor, JavaUI.getColorManager()));
		styledText = viewer.getTextWidget();
		styledText.setEditable(false);
		styledText.setFont(JFaceResources.getTextFont());

		activateFindAndReplace(viewer, getSite(), this);

	}

	private void createTableViewer(IManagedForm managedForm, Composite body) {
		Section section = managedForm.getToolkit()
			.createSection(body, Section.TITLE_BAR | Section.EXPANDED);
		section.setText("Sortable Table");
		section.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		Composite sectionClient = managedForm.getToolkit()
			.createComposite(section, SWT.NONE);
		sectionClient.setLayout(new org.eclipse.swt.layout.GridLayout(1, false));
		managedForm.getToolkit()
			.paintBordersFor(sectionClient);
		section.setClient(sectionClient);

		this.tableViewer = new TableViewer(sectionClient,
			SWT.BORDER | SWT.FULL_SELECTION | SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);

		Table table = tableViewer.getTable();
		table.setHeaderVisible(true);
		table.setLinesVisible(true);
		table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		createColumns();
		tableViewer.setContentProvider(ArrayContentProvider.getInstance());
		tableViewer.setInput(getTableData());

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

	// Table
	private void createColumns() {
		String[] titles = {
			"key", "value", "path"
		};
		int[] bounds = {
			100, 200, 200
		};

		for (int i = 0; i < titles.length; i++) {
			final TableViewerColumn column = new TableViewerColumn(tableViewer, SWT.NONE);
			column.getColumn()
				.setText(titles[i]);
			column.getColumn()
				.setWidth(bounds[i]);
			column.getColumn()
				.setResizable(true);
			column.getColumn()
				.setMoveable(true);

			final int colNum = i;
			column.setLabelProvider(new ColumnLabelProvider() {
				@Override
				public String getText(Object element) {
					return String.valueOf(getColumnValue(element, colNum));
				}
			});

			final int columnIndex = i;
			column.getColumn()
				.addListener(SWT.Selection, e -> {
					// Comparator<Object> comparator = Comparator.comparing(obj
					// -> getColumnValue(obj, columnIndex));
					// tableViewer.setComparator(new
					// ViewerComparator(comparator));
				});
		}
	}

	private Object getColumnValue(Object element, int columnIndex) {
		if (element instanceof String[]) {
			String[] row = (String[]) element;
			return row[columnIndex];
		}
		return null;
	}

	private Object[] getTableData() {

		try {

			Processor p = new BndEditModel(editModel, true).getProperties();
			// Set<String> propertyKeys = p.getPropertyKeys(true);
			List<PropertyKey> propertyKeys = p.getPropertyKeys(k -> true);

			Object[] result = new Object[propertyKeys.size()];
			int index = 0;

			for (PropertyKey prop : propertyKeys) {
				String key = prop.key();
				String value = p.getProperty(key);
				String path = "";
				if (!prop.isLocalTo(p)) {
					File propertiesFile = prop.processor()
						.getPropertiesFile();
					if (propertiesFile != null) {
						path = propertiesFile.getAbsolutePath();

					}
				}
				result[index] = new String[] {
					key, value, path
				};
				index++;
			}

			return result;

		} catch (Exception e) {
			throw Exceptions.duck(e);
		}

	}

}
