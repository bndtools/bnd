package bndtools.editor;

import static aQute.bnd.help.Syntax.isInstruction;
import static aQute.bnd.osgi.Constants.MERGED_HEADERS;
import static bndtools.editor.completion.BndHover.lookupSyntax;
import static bndtools.editor.completion.BndHover.syntaxHoverText;
import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toCollection;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IHandler;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IFindReplaceTarget;
import org.eclipse.jface.text.TextViewer;
import org.eclipse.jface.text.source.CompositeRuler;
import org.eclipse.jface.text.source.LineNumberRulerColumn;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.window.ToolTip;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.ToolBar;
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

import aQute.bnd.build.Workspace;
import aQute.bnd.build.model.BndEditModel;
import aQute.bnd.exceptions.Exceptions;
import aQute.bnd.help.Syntax;
import aQute.bnd.osgi.Processor;
import aQute.bnd.osgi.Processor.PropertyKey;
import bndtools.central.Central;
import bndtools.editor.completion.BndSourceViewerConfiguration;
import bndtools.editor.completion.TooltipInput;
import bndtools.utils.EditorUtils;

/**
 * The 'Effective source' tab in a Bnd Editor which renders an effective
 * readonly view of all properties of the BndEditModel. This means that it shows
 * all available properties which are pulled in via various include-mechanisms.
 * This it is a useful debugging tool to make things visible to the human eye.
 */
public class BndSourceEffectivePage extends FormPage {

	private final BndEditor	bndEditor;
	private BndEditModel	editModel;
	private SourceViewer	sourceViewer;
	private TableViewer		tableViewer;
	private StyledText		styledText;
	private Button			toggleButton;
	private Button			showExpandedValuesButton;
	private Button			showMergedPropertiesButton;
	private Composite		viewersComposite;
	private StackLayout		stackLayout;
	private boolean			loading;

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

		this.showExpandedValuesButton = toolkit.createButton(body, "Show expanded values", SWT.CHECK);
		showExpandedValuesButton.setSelection(true);
		showExpandedValuesButton.setToolTipText("Shows the actual values instead of placeholders.");

		this.showMergedPropertiesButton = toolkit.createButton(body, "Show as merged properties", SWT.CHECK);
		showMergedPropertiesButton.setToolTipText(
			"Groups all instructions by the stem of the property, which previews how bnd sees the instructions under the hood.");
		showMergedPropertiesButton.setSelection(true);
		showMergedPropertiesButton.setEnabled(true);

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
		showExpandedValuesButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (showExpandedValuesButton.getSelection()) {
					showMergedPropertiesButton.setEnabled(true);
				} else {
					showMergedPropertiesButton.setEnabled(false);
					showMergedPropertiesButton.setSelection(false);
				}

				loading = false;
				update();
				tableViewer.refresh();
				viewersComposite.layout();

			}
		});

		showMergedPropertiesButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				loading = false;
				update();
				tableViewer.refresh();
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

		int[] widths = createColumns(tableViewer);

		tableViewer.setContentProvider(ArrayContentProvider.getInstance());
		tableViewer.setInput(getTableData());

		tableViewer.addDoubleClickListener(new IDoubleClickListener() {
			@Override
			public void doubleClick(DoubleClickEvent event) {
				IStructuredSelection selection = tableViewer.getStructuredSelection();
				for (Object element : selection.toList()) {
					open(element);
				}
			}
		});

		tableViewer.getControl()
			.addControlListener(new ControlListener() {

				@Override
				public void controlResized(ControlEvent arg0) {
					Rectangle rect = tableViewer.getTable()
						.getClientArea();
					if (rect.width > 0) {
						int total = rect.width;
						int selected = -1;

						for (int i = 0; i < widths.length; i++) {
							TableColumn column = tableViewer.getTable()
								.getColumn(i);
							int w = widths[i];
							if (w > 0) {
								total -= column.getWidth();
								continue;
							}
							selected = i;
						}
						if (selected >= 0) {
							TableColumn column = tableViewer.getTable()
								.getColumn(selected);
							int minwidth = -widths[selected];
							if (minwidth < total) {
								column.setWidth(total);
							} else {
								column.setWidth(minwidth);
							}
						}
					}
				}

				@Override
				public void controlMoved(ControlEvent arg0) {}
			});
		tableViewer.getControl()
			.getParent()
			.layout(true, true);
	}

	private void open(Object element) {
		if (element instanceof PropertyRow prop) {
			List<String> fpaths = prop.provenances();

			if (fpaths == null || fpaths.isEmpty()) {
				return;
			}

			// we can only open one path
			String fpath = fpaths.get(0);

			if (fpath == null || fpath.isBlank()) {
				return;
			}
			IWorkspaceRoot root = ResourcesPlugin.getWorkspace()
				.getRoot();

			File file = new File(fpath);
			if (!file.isFile()) {
				return;
			}

			org.eclipse.core.runtime.IPath path = new Path(file.getAbsolutePath());
			IFile iFile = root.getFileForLocation(path);
			if (iFile == null || !iFile.exists()) {
				return;
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

	/**
	 * Setup Eclipse's built in Search / Replace dialog. Also see
	 * {@link #getAdapter(Class)}
	 *
	 * @param textViewer
	 * @param site
	 * @param page
	 */
	private static void activateFindAndReplace(TextViewer textViewer, IWorkbenchPartSite site, IWorkbenchPart page) {
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
		} else {}
	}

	private String printSourceViewerContent() throws Exception {
		if (editModel == null) {
			return "...";
		}

		try {
			boolean showExpandedValues = showExpandedValuesButton.getSelection();
			boolean showMerged = showMergedPropertiesButton.getSelection();

			StringBuilder sb = new StringBuilder();
			Processor properties = getProperties();
			List<PropertyKey> propertyKeys = properties.getPropertyKeys(k -> true);
			List<PropertyKey> visible = PropertyKey.findVisible(propertyKeys);

			if (showMerged) {

				Collection<String> stems = visible.stream()
					.map(k -> {
						String stem = BndEditModel.getStem(k.key());
						if (isInstruction(stem) || MERGED_HEADERS.contains(stem)) {
							return stem;
						}
						return k.key();
					})
					.collect(Collectors.toCollection(LinkedHashSet::new));

				for (String stem : stems) {

					String value = isInstruction(stem) || MERGED_HEADERS.contains(stem)
						? properties.decorated(stem)
							.toString()
						: properties.get(stem);

					sb.append(stem)
						.append(": ")
						.append(BndEditModel.format(stem, value))
						.append("\n");
				}

			} else {

				for (PropertyKey k : visible) {
					Processor p = getProperties();
					String value = showExpandedValues ? getExpandedValue(k, p) : k.getRawValue();
					sb.append(k.key())
						.append(": ")
						.append(BndEditModel.format(k.key(), value))
						.append("\n");
				}
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
			String sourceViewerContent = printSourceViewerContent();
			sourceViewer.setDocument(new Document(sourceViewerContent));
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

	private record ColSpec(String title, int width, Function<PropertyRow, String> label,
		Function<PropertyRow, String> tooltip,
		Function<PropertyRow, String> tooltipHelpUrl) {}

	private int[] createColumns(TableViewer tableViewer) {


		boolean showMerged = showMergedPropertiesButton.getSelection();

		ColSpec[] specs = new ColSpec[] {
			new ColSpec("Key", 150, PropertyRow::title, (pr -> syntaxHoverText(pr.title, getProperties())),
				pr -> {
					Syntax syntax = lookupSyntax(pr.title);
					return syntax != null ? syntax.autoHelpUrl() : null;
				}), //
			new ColSpec("Value", -250, PropertyRow::value, PropertyRow::tooltip, null), //
			new ColSpec("Provenance", 150, (pr -> {
				return pr.provenances()
					.stream()
					.map(prov -> relativizeProvenancePath(prov))
					.collect(Collectors.joining(","));
			}),
				null, null), //
			new ColSpec("Errors", 150, (pr -> {
				List<String> errors = pr.errors();
				return errors.isEmpty() ? "" : errors.toString();
			}
			), (pr -> {
				List<String> errors = pr.errors();
				return errors.isEmpty() ? "" : errors.toString();
			}), null), //
		};

		int[] widths = new int[specs.length];

		List<CellLabelProvider> columnLabelProviders = new ArrayList<>();


		int colIndex = 0;
		for (ColSpec spec : specs) {
			final TableViewerColumn column = new TableViewerColumn(tableViewer, SWT.NONE);
			TableColumn tc = column.getColumn();
			tc.setText(spec.title);
			tc.setResizable(true);
			tc.setMoveable(true);

			CellLabelProvider labelProvider = new CustomTooltipCellLabelProvider(spec);
			column.setLabelProvider(labelProvider);

			// mapping needed for tooltip later
			columnLabelProviders.add(labelProvider);

			tc.setWidth(spec.width < 0 ? -spec.width : spec.width);
			widths[colIndex] = spec.width;
			colIndex++;
		}

		createCustomToolTipSupport(tableViewer, columnLabelProviders);

		return widths;
	}

	private void createCustomToolTipSupport(TableViewer tableViewer, List<CellLabelProvider> columnLabelProviders) {
		// 3) Subclass ColumnViewerToolTipSupport to create a selectable Text
		// control with scrollbars
		ColumnViewerToolTipSupport tooltipSupport = new ColumnViewerToolTipSupport(tableViewer, ToolTip.NO_RECREATE,
			false) {
			@Override
			protected Composite createToolTipContentArea(Event event, Composite parent) {
				// Define tooltip size constants
				final int TOOLTIP_WIDTH = 700;
				final int TOOLTIP_HEIGHT = 300;

				Composite container = new Composite(parent, SWT.NONE);

				if (parent instanceof Shell) {
					parent.setSize(TOOLTIP_WIDTH, TOOLTIP_HEIGHT);
				}
				container.setLayout(new GridLayout(1, false));

				// unfortunatelly lots of ceremony to
				Table table = tableViewer.getTable();
				Point pt = new Point(event.x, event.y);
				TableItem item = table.getItem(pt);
				Object element = item != null ? item.getData() : null;

				if (item == null)
					return container;

				int columnIndex = -1;
				for (int i = 0; i < table.getColumnCount(); i++) {
					if (item.getBounds(i)
						.contains(pt)) {
						columnIndex = i;
						break;
					}
				}

				//
				TooltipInput tooltipInfo = null;
				if (columnIndex >= 0 && columnIndex < columnLabelProviders.size()) {
					CellLabelProvider provider = columnLabelProviders.get(columnIndex);
					if (provider instanceof CustomTooltipCellLabelProvider myProvider) {
						tooltipInfo = myProvider.getTooltipInfo(element);
					}
				}

				String tooltipText = getText(event);

				if (tooltipText == null) {
					tooltipText = "";
				}

				StyledText text = new StyledText(container,
					SWT.READ_ONLY | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);
				text.setMargins(5, 5, 5, 5); // left, top, right, bottom

				GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
				gd.widthHint = TOOLTIP_WIDTH - 50; // Account for margins and scrollbars
				gd.heightHint = TOOLTIP_HEIGHT - 80; // Account for margins, scrollbars, and toolbar
				text.setLayoutData(gd);
				text.setText(tooltipText);

				if (tooltipInfo != null && tooltipInfo.url != null) {
					ToolBarManager toolBarManager = new ToolBarManager(SWT.FLAT);
					ToolBar toolBar = toolBarManager.createControl(container);
					toolBar.setLayoutData(new GridData(SWT.END, SWT.CENTER, false, false));

					// Create the Action using your utility
					Action helpButton = EditorUtils.createHelpButton(tooltipInfo.url, "Open help for more information");

					// Add it to the manager
					toolBarManager.add(helpButton);
					toolBarManager.update(true);
				}

				Display display = parent.getDisplay();
				text.setFocus();

				return container;
			}


			@Override
			public boolean isHideOnMouseDown() {
				return false;
			}

			@Override
			public Point getLocation(Point tipSize, Event event) {
				Display display = Display.getCurrent();
				Control control = (Control) event.widget;
				Point cursor = display.map(control, null, new Point(event.x, event.y));

				// move the tooltip a little bit more left.
				// makes it easier to move the mouse into the tooltip
				// without accidentally closing the tooltip
				int offsetX = -10;
				int offsetY = 0;

				return new Point(cursor.x + offsetX, cursor.y + offsetY);
			}


		};
	}

	private String expandedOrRawValue(PropertyKey k, Processor p) {
		boolean showExpandedValues = showExpandedValuesButton.getSelection();
		return showExpandedValues ? getExpandedValue(k, p) : k.getRawValue();
	}

	private String getExpandedValue(PropertyKey k, Processor p) {
		return p.getProperty(k.key());
	}

	private String relativizeProvenancePath(String path) {

		if (path == null || path.isBlank()) {
			return "";
		}

		File file = new File(path);
		if (!file.isFile())
			return path;

		Workspace workspace = Central.getWorkspaceIfPresent();
		if (workspace == null)
			return path;

		URI wsbase = workspace.getBase()
			.toURI();
		URI fbase = file.toURI();
		URI relative = wsbase.relativize(fbase);
		return relative.getPath();
	}

	Object[] getTableData() {

		try {
			Processor p1 = getProperties();
			List<PropertyKey> propertyKeys = p1.getPropertyKeys(k -> true);
			List<PropertyKey> visible = PropertyKey.findVisible(propertyKeys);

			boolean showMerged = showMergedPropertiesButton.getSelection();

			if (showMerged) {

				Collection<String> stems = visible.stream()
					.map(k -> {
						String stem = BndEditModel.getStem(k.key());
						if (isInstruction(stem) || MERGED_HEADERS.contains(stem)) {
							return stem;
						}
						return k.key();
					})
					.collect(Collectors.toCollection(LinkedHashSet::new));

				Map<String, List<String>> stemProvenances = visible.stream()
					.collect(Collectors.groupingBy(k -> {
						String stem = BndEditModel.getStem(k.key());
						if (isInstruction(stem) || MERGED_HEADERS.contains(stem)) {
							return stem;
						}
						return k.key();
					}, mapping(k -> {
						String prov = k.getProvenance()
							.orElse(null);
						return prov;
					}, //
						// remove duplicates
						collectingAndThen(toCollection(LinkedHashSet::new),
							// convert back to List
							ArrayList::new
					))));

				return stems.stream()
					.map(stem -> {
						Processor p = getProperties();

						String value = isInstruction(stem) || MERGED_HEADERS.contains(stem)
							? p.decorated(stem)
								.toString()
							: p.get(stem);

						List<String> provList = stemProvenances.get(stem);

						String tooltip = BndEditModel.format(stem, value);
						return new PropertyRow(stem, value, provList, tooltip,
							p.getErrors());
					})
					.toArray();

			} else {

				return visible.stream()
					.map(k -> {
						String provenance = k.getProvenance()
							.orElse(null);
						Processor p = getProperties();
						String tooltip = BndEditModel.format(k.key(), expandedOrRawValue(k, p));
						return new PropertyRow(k.key(), expandedOrRawValue(k, p), List.of(provenance), tooltip,
							p.getErrors());
					})
					.toArray();
			}

		} catch (Exception e) {
			throw Exceptions.duck(e);
		}

	}

	Processor getProperties() {
		Map<String, String> changes = editModel.getDocumentChanges();
		Processor p = new Processor(editModel.getOwner()) {
			@Override
			public String getUnexpandedProperty(String key) {
				if (changes.containsKey(key)) {
					return changes.get(key);
				}
				return super.getUnexpandedProperty(key);
			}
		};
		p.setBase(editModel.getOwner()
			.getBase());
		return p;
	}

	private final class CustomTooltipCellLabelProvider extends CellLabelProvider {
		private final ColSpec spec;

		private CustomTooltipCellLabelProvider(ColSpec spec) {
			this.spec = spec;
		}

		@Override
		public void update(ViewerCell cell) {
			Object element = cell.getElement();
			if (element instanceof PropertyRow pkey) {
				String text = spec.label != null ? spec.label.apply(pkey) : null;
				cell.setText(text);
			}
		}

		@Override
		public boolean useNativeToolTip(Object object) {
			if (object instanceof PropertyRow) {
				return false;
			}
			return true;
		}

		@Override
		public String getToolTipText(Object element) {
			if ("Provenance".equals(spec.title) && element instanceof PropertyRow prow) {
				return prow.provenances()
					.stream()
					.collect(Collectors.joining("\n"));
			}
			if (spec.tooltip != null && element instanceof PropertyRow prow) {
				return spec.tooltip.apply(prow);
			}

			return null;
		}

		public TooltipInput getTooltipInfo(Object element) {
			if (spec.tooltip != null && element instanceof PropertyRow pkey) {
				String text = getToolTipText(element);
				String url = spec.tooltipHelpUrl != null ? spec.tooltipHelpUrl.apply(pkey) : null;
				return new TooltipInput(text, url);
			}
			return null;
		}
	}

	/**
	 * represents a row in the Effective view table.
	 */
	record PropertyRow(String title, String value, List<String> provenances, String tooltip,
		List<String> errors) {}
}
