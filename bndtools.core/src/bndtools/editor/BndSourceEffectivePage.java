package bndtools.editor;

import java.io.File;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Set;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IHandler;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Path;
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
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Table;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.widgets.Form;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.handlers.IHandlerService;
import org.eclipse.ui.part.FileEditorInput;
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
	private SourceViewer	sourceViewer;
	private TableViewer		tableViewer;
	private StyledText	styledText;
	private Button			toggleButton;
	private Composite		viewersComposite;
	private StackLayout		stackLayout;
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

		Composite body = scrolledForm.getBody();
		body.setLayout(new GridLayout(1, false));

		// Create toggle button
		toggleButton = toolkit.createButton(body, "Show as Source", SWT.PUSH);
		toggleButton.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));

		// Create composite for viewers
		viewersComposite = toolkit.createComposite(body);
		viewersComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		stackLayout = new StackLayout() {
			@Override
			protected Point computeSize(Composite composite, int wHint, int hHint, boolean flushCache) {
				Point size = super.computeSize(composite, wHint, hHint, flushCache);
				// hack to make sure styledText never grows outside of body
				size.y = SWT.DEFAULT;
				return size;
			}
		};
		viewersComposite.setLayout(stackLayout);

		createSourceViewer(managedForm, viewersComposite);
		createTableViewer(managedForm, viewersComposite);

		// Set initial view
		stackLayout.topControl = tableViewer.getControl();

		// Add toggle button listener
		toggleButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (stackLayout.topControl == sourceViewer.getControl()) {
					stackLayout.topControl = tableViewer.getControl();
					toggleButton.setText("Show as Source");
				} else {
					stackLayout.topControl = sourceViewer.getControl();
					toggleButton.setText("Show as Table");
				}
				viewersComposite.layout();
			}
		});

		// Initial layout update
		viewersComposite.layout();
		scrolledForm.reflow(true);
	}

	private void createSourceViewer(IManagedForm managedForm, Composite body) {


		// ruler for line numbers
		CompositeRuler ruler = new CompositeRuler();
		LineNumberRulerColumn ln = new LineNumberRulerColumn();
		ln.setForeground(Display.getCurrent()
			.getSystemColor(SWT.COLOR_DARK_GRAY));
		ruler.addDecorator(0, ln);

		this.sourceViewer = new SourceViewer(body, ruler, SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL) {
			@Override
			protected boolean canPerformFind() {
				return true;
			}
		};

		GridData sourceViewerLayoutData = new GridData(SWT.FILL, SWT.FILL, true, false);
		sourceViewerLayoutData.heightHint = 100; // Set to 100 pixels high

		sourceViewer.setDocument(new Document());
		sourceViewer.getControl()
			.setLayoutData(sourceViewerLayoutData);
		sourceViewer.configure(new BndSourceViewerConfiguration(bndEditor, JavaUI.getColorManager()));
		styledText = sourceViewer.getTextWidget();
		styledText.setEditable(false);
		styledText.setFont(JFaceResources.getTextFont());
		styledText.setAlwaysShowScrollBars(true);

		activateFindAndReplace(sourceViewer, getSite(), this);

		sourceViewer.getControl()
			.getParent()
			.layout(true, true);

	}

	private void createTableViewer(IManagedForm managedForm, Composite body) {

		this.tableViewer = new TableViewer(body,
			SWT.BORDER | SWT.FULL_SELECTION | SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);

		Table table = tableViewer.getTable();
		table.setHeaderVisible(true);
		table.setLinesVisible(true);
		table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		createColumns();
		tableViewer.setContentProvider(ArrayContentProvider.getInstance());
		tableViewer.setInput(getTableData());

		tableViewer.addDoubleClickListener(new IDoubleClickListener() {
			@Override
			public void doubleClick(DoubleClickEvent event) {
				IStructuredSelection selection = tableViewer.getStructuredSelection();
				Object firstElement = selection.getFirstElement();

				if(firstElement instanceof PropertyKey prop) {
					String fpath = getPropertyKeyPath(prop);
					if (fpath == null || fpath.isBlank()) {
						return;
					}

					IWorkspaceRoot root = ResourcesPlugin.getWorkspace()
						.getRoot();

					File file = new File(fpath);
					org.eclipse.core.runtime.IPath path = new Path(file.getAbsolutePath());
					IFile iFile = root.getFileForLocation(path);
					if (iFile == null) {
						// File is not in the workspace. You cannot directly get
						// an IFile for it.
					}
					IEditorInput input = new FileEditorInput(iFile);
					try {
						PlatformUI.getWorkbench()
							.getActiveWorkbenchWindow()
							.getActivePage()
							.openEditor(input, BndEditor.WORKSPACE_EDITOR);
					} catch (PartInitException e) {
						throw Exceptions.duck(e);
					}
				}
			}
		});

		tableViewer.getControl()
			.getParent()
			.layout(true, true);
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
			sourceViewer.setDocument(new Document(text));
			styledText.setFocus();
			tableViewer.setInput(getTableData());
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
			return (T) sourceViewer.getFindReplaceTarget();
		}
		return super.getAdapter(adapter);
	}

	// Table
	private void createColumns() {
		String[] titles = {
			"key", "value", "path"
		};
		int[] bounds = {
			200, 300, 300
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
					return String.valueOf(getColumnContent(element, titles[colNum]));
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

	private Object getColumnContent(Object element, String colName) {
		if (element instanceof PropertyKey prop) {
			switch (colName) {
				case "key" : return prop.key();
				case "value" :
					return prop.processor()
						.getProperty(prop.key());
				case "path" : {
					String path = getPropertyKeyPath(prop);
					if (path.isBlank()) {
						return path;
					}

					// cut the beginning to get only e.g. /cnf/build.bnd instead
					// of absolute path
					return path.replaceAll(prop.processor()
						.getBase()
						.getPath(), "")
						.substring(1);
				}

				default :
					throw new IllegalArgumentException("Unknown column: " + colName);
			}

		}
		return null;
	}

	private String getPropertyKeyPath(PropertyKey prop) {
		String path = "";

		File propertiesFile = prop.processor()
			.getPropertiesFile();
		if (propertiesFile != null) {
			path = propertiesFile.getPath();
		}
		return path;
	}

	private Object[] getTableData() {

		try {

			Processor p = new BndEditModel(editModel, true).getProperties();
			List<PropertyKey> propertyKeys = p.getPropertyKeys(k -> true);

			// avoid duplicates because Project is parent of bnd.bnd and also
			// gets the same properties
			// but with higher floor
			List<PropertyKey> visible = PropertyKey.findVisible(propertyKeys);

			Object[] result = new Object[visible.size()];
			int index = 0;

			for (PropertyKey prop : visible) {
				result[index] = prop;
				index++;
			}

			return result;

		} catch (Exception e) {
			throw Exceptions.duck(e);
		}

	}

}
