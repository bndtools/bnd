package bndtools.views.bundlegraph;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import org.bndtools.api.ILogger;
import org.bndtools.api.Logger;
import org.bndtools.core.ui.icons.Icons;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.part.ResourceTransfer;
import org.eclipse.ui.part.ViewPart;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

import bndtools.views.bundlegraph.model.BundleEdge;
import bndtools.views.bundlegraph.model.BundleGraphModel;
import bndtools.views.bundlegraph.model.BundleNode;
import bndtools.views.bundlegraph.model.GraphClosures;
import bndtools.views.bundlegraph.model.SimpleBundleGraphModel;
import bndtools.views.bundlegraph.render.EdgeFilter;
import bndtools.views.bundlegraph.render.MermaidRenderer;

/**
 * Eclipse ViewPart that shows a Mermaid graph of bundle dependencies.
 * <p>
 * The view has two halves:
 * <ul>
 * <li>Top: a dual-list selection builder (Available | Selected) with filter, mode dropdown, and auto-render
 * checkbox.</li>
 * <li>Bottom: an SWT {@link Browser} rendering the Mermaid graph.</li>
 * </ul>
 */
public class BundleGraphView extends ViewPart {

	public static final String	VIEW_ID	= "bndtools.bundleGraphView";

	private static final ILogger logger = Logger.getLogger(BundleGraphView.class);

	/** Expansion mode for the graph. */
	public enum ExpansionMode {
		ONLY_SELECTED("Only selected"),
		SELECTED_AND_DEPENDENCIES("Selected + dependencies"),
		SELECTED_AND_DEPENDANTS("Selected + dependants");

		private final String label;

		ExpansionMode(String label) {
			this.label = label;
		}

		public String getLabel() {
			return label;
		}
	}

	// Model
	private BundleGraphModel			model			= new SimpleBundleGraphModel(Collections.emptySet(),
		Collections.emptySet(), Collections.emptyMap());
	private Supplier<BundleGraphModel>			modelSupplier		= null;
	private final Set<BundleNode>		selected		= new LinkedHashSet<>();
	private ExpansionMode				mode			= ExpansionMode.ONLY_SELECTED;
	private boolean						showFirstPackage	= false;
	private EdgeFilter					edgeFilter				= EdgeFilter.ALL;

	// UI
	private TableViewer					availableViewer;
	private TableViewer					selectedViewer;
	private Text						filterText;
	private Combo						modeCombo;
	private Button						showFirstPackageCheck;
	private Browser						browser;
	private boolean						browserReady;
	private SashForm					sash;
	private Composite					browserPanelComposite;
	private boolean						graphMaximized	= false;

	// Last rendered Mermaid definition (used by "Copy Mermaid" action)
	private String						lastMermaidDef	= "";

	// Filtered available list
	private String						filterString	= "";

	@Override
	public void createPartControl(Composite parent) {
		parent.setLayout(new FillLayout());

		sash = new SashForm(parent, SWT.VERTICAL);

		// ---- Top panel: selection builder ----
		createSelectionPanel(sash);

		// ---- Bottom panel: browser ----
		createBrowserPanel(sash);

		sash.setWeights(new int[] {
			45, 55
		});

		// Toolbar actions
		IToolBarManager toolbar = getViewSite().getActionBars()
			.getToolBarManager();


		Action copyMermaidToClipboard = new Action("Copy Mermaid to Clipboard") {
			@Override
			public void run() {
				copyToClipboard(lastMermaidDef);
			}
		};
		copyMermaidToClipboard.setImageDescriptor(Icons.desc("icons/page_copy.png"));
		toolbar.add(copyMermaidToClipboard);

		Action refreshUniverseAction = new Action("Refresh Universe") {
			@Override
			public void run() {
				refreshUniverse();
			}
		};
		refreshUniverseAction.setImageDescriptor(Icons.desc("icons/arrow_refresh_d.png"));
		toolbar.add(refreshUniverseAction);

		Action togglePanelAction = new Action("Toggle", Action.AS_CHECK_BOX) {
			@Override
			public void run() {
				toggleGraphMaximized();
			}
		};
		togglePanelAction.setToolTipText("Maximize graph (hide selection panel) / Restore split view");
		togglePanelAction.setImageDescriptor(Icons.desc("icons/collapseall.png"));
		toolbar.add(togglePanelAction);

		refreshAvailable();
	}

	private void createSelectionPanel(Composite parent) {
		Composite top = new Composite(parent, SWT.NONE);
		GridLayout topLayout = new GridLayout(3, false);
		topLayout.marginWidth = 5;
		topLayout.marginHeight = 5;
		top.setLayout(topLayout);

		// --- Info banner (DnD hint) ---
		Composite infoBanner = new Composite(top, SWT.NONE);
		GridLayout infoLayout = new GridLayout(2, false);
		infoLayout.marginWidth = 6;
		infoLayout.marginHeight = 4;
		infoBanner.setLayout(infoLayout);

		GridData infoGd = new GridData(SWT.FILL, SWT.CENTER, true, false);
		infoGd.horizontalSpan = 3; // span all 3 columns
		infoBanner.setLayoutData(infoGd);

		Label infoIcon = new Label(infoBanner, SWT.NONE);
		infoIcon.setImage(infoBanner.getDisplay()
			.getSystemImage(SWT.ICON_INFORMATION));

		Label infoText = new Label(infoBanner, SWT.WRAP);
		infoText
			.setText("Drag projects, repositories, or .bndrun files from other Eclipse views into the lists below.");
		infoText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		// Optional: make it look more like a hint than a warning
		infoText.setForeground(infoBanner.getDisplay()
			.getSystemColor(SWT.COLOR_DARK_GRAY));

		// Optional separator under the banner
		Label sep = new Label(top, SWT.SEPARATOR | SWT.HORIZONTAL);
		GridData sepGd = new GridData(SWT.FILL, SWT.CENTER, true, false);
		sepGd.horizontalSpan = 3;
		sep.setLayoutData(sepGd);

		// --- Filter row ---
		filterText = new Text(top, SWT.BORDER | SWT.SEARCH | SWT.ICON_CANCEL);
		filterText.setMessage("Filter available bundles...");
		filterText.setLayoutData(new GridData(SWT.FILL, SWT.LEFT, true, false));
		filterText.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				filterString = filterText.getText()
					.trim()
					.toLowerCase();
				refreshAvailable();
			}
		});

		// placeholder for 3rd column alignment
		new Label(top, SWT.NONE);
		new Label(top, SWT.NONE);

		// --- Dual list area ---

		// Left: available
		Composite leftPanel = new Composite(top, SWT.NONE);
		leftPanel.setLayout(new GridLayout(1, false));
		leftPanel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		// Header row: label + Remove button (common Eclipse pattern)
		Composite availHeaderRow = new Composite(leftPanel, SWT.NONE);
		GridLayout availHeaderLayout = new GridLayout(2, false);
		availHeaderLayout.marginWidth = 0;
		availHeaderLayout.marginHeight = 0;
		availHeaderRow.setLayout(availHeaderLayout);
		availHeaderRow.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		Label availLabel = new Label(availHeaderRow, SWT.NONE);
		availLabel.setText("Available bundles:");
		availLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		Button removeFromUniverseBtn = new Button(availHeaderRow, SWT.PUSH);
		removeFromUniverseBtn.setText("Remove");
		removeFromUniverseBtn.setToolTipText("Remove selected bundles from the universe (Del)");
		removeFromUniverseBtn.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				removeFromUniverse();
			}
		});

		availableViewer = new TableViewer(leftPanel, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL);
		availableViewer.setContentProvider(ArrayContentProvider.getInstance());
		availableViewer.setLabelProvider(new LabelProvider() {
			@Override
			public String getText(Object element) {
				return element instanceof BundleNode ? element.toString() : String.valueOf(element);
			}
		});

		// Double-click adds item to selected
		availableViewer.addDoubleClickListener(new IDoubleClickListener() {
			@Override
			public void doubleClick(DoubleClickEvent event) {
				addSelected();
			}
		});

		// Delete key removes highlighted items from the universe
		availableViewer.getTable()
			.addKeyListener(new KeyAdapter() {
				@Override
				public void keyPressed(KeyEvent e) {
					if (e.keyCode == SWT.DEL) {
						removeFromUniverse();
					}
				}
			});

		Table availTable = availableViewer.getTable();
		availTable.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		// Center: buttons
		Composite btnPanel = new Composite(top, SWT.NONE);
		GridLayout btnLayout = new GridLayout(1, false);
		btnLayout.marginWidth = 3;
		btnPanel.setLayout(btnLayout);
		btnPanel.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, false, true));

		Button addBtn = new Button(btnPanel, SWT.PUSH);
		addBtn.setText("Add >");
		addBtn.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		addBtn.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				addSelected();
			}
		});

		Button removeBtn = new Button(btnPanel, SWT.PUSH);
		removeBtn.setText("< Remove");
		removeBtn.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		removeBtn.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				removeSelected();
			}
		});

		new Label(btnPanel, SWT.SEPARATOR | SWT.HORIZONTAL).setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		Button addDepsBtn = new Button(btnPanel, SWT.PUSH);
		addDepsBtn.setText("Add deps");
		addDepsBtn.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		addDepsBtn.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				addDependencies();
			}
		});

		Button addDependantsBtn = new Button(btnPanel, SWT.PUSH);
		addDependantsBtn.setText("Add dependants");
		addDependantsBtn.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		addDependantsBtn.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				addDependants();
			}
		});

		// Right: selected
		Composite rightPanel = new Composite(top, SWT.NONE);
		rightPanel.setLayout(new GridLayout(1, false));
		rightPanel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		Label selLabel = new Label(rightPanel, SWT.NONE);
		selLabel.setText("Selected input bundles:");

		selectedViewer = new TableViewer(rightPanel, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL);
		selectedViewer.setContentProvider(ArrayContentProvider.getInstance());
		selectedViewer.setLabelProvider(new LabelProvider() {
			@Override
			public String getText(Object element) {
				return element instanceof BundleNode ? element.toString() : String.valueOf(element);
			}
		});
		// Double-click removes item from selection
		selectedViewer.addDoubleClickListener(new IDoubleClickListener() {
			@Override
			public void doubleClick(DoubleClickEvent event) {
				removeSelected();
			}
		});
		// Ctrl/Cmd+C copies highlighted bundle names to clipboard
		selectedViewer.getTable()
			.addKeyListener(new KeyAdapter() {
				@Override
				public void keyPressed(KeyEvent e) {
					if ((e.stateMask & SWT.MOD1) == SWT.MOD1 && e.keyCode == 'c') {
						copySelectedBundlesToClipboard();
					}
				}
			});
		Table selTable = selectedViewer.getTable();
		selTable.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		// --- Options row (mode + auto-render + render button) ---
		Composite optRow = new Composite(top, SWT.NONE);
		GridLayout optLayout = new GridLayout(6, false);
		optLayout.marginWidth = 0;
		optRow.setLayout(optLayout);
		GridData optGd = new GridData(SWT.FILL, SWT.CENTER, true, false);
		optGd.horizontalSpan = 3;
		optRow.setLayoutData(optGd);

		Label modeLabel = new Label(optRow, SWT.NONE);
		modeLabel.setText("Mode:");

		modeCombo = new Combo(optRow, SWT.DROP_DOWN | SWT.READ_ONLY);
		for (ExpansionMode m : ExpansionMode.values()) {
			modeCombo.add(m.getLabel());
		}
		modeCombo.select(0);
		modeCombo.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				mode = ExpansionMode.values()[modeCombo.getSelectionIndex()];
				rerender();
			}
		});

		showFirstPackageCheck = new Button(optRow, SWT.CHECK);
		showFirstPackageCheck.setText("Show edge package");
		showFirstPackageCheck.setToolTipText(
			"Shows the first contributing package on an edge. This is helpful for debugging why two bundles are connected.");
		showFirstPackageCheck.setSelection(false);
		showFirstPackageCheck.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				showFirstPackage = showFirstPackageCheck.getSelection();
				rerender();
			}
		});

		Button renderBtn = new Button(optRow, SWT.PUSH);
		renderBtn.setText("Render");
		renderBtn.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				rerender();
			}
		});

		// Install drag-and-drop support so users can drop .bndrun files or projects onto either list
		Transfer[] dndTransfers = new Transfer[] {
			LocalSelectionTransfer.getTransfer(), ResourceTransfer.getInstance()
		};
		availableViewer.addDropSupport(DND.DROP_COPY | DND.DROP_DEFAULT, dndTransfers,
			new BundleGraphDropAdapter(availableViewer, this, false));
		selectedViewer.addDropSupport(DND.DROP_COPY | DND.DROP_DEFAULT, dndTransfers,
			new BundleGraphDropAdapter(selectedViewer, this, true));

	}

	private void createBrowserPanel(Composite parent) {
		browserPanelComposite = new Composite(parent, SWT.NONE);
		Composite bottomPanel = browserPanelComposite;
		GridLayout bottomLayout = new GridLayout(1, false);
		bottomLayout.marginWidth = 0;
		bottomLayout.marginHeight = 0;
		bottomLayout.verticalSpacing = 0;
		bottomPanel.setLayout(bottomLayout);

		// ---- Zoom toolbar row ----
		Composite zoomRow = new Composite(bottomPanel, SWT.NONE);
		zoomRow.setLayout(new GridLayout(7, false));
		zoomRow.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		Label zoomLabel = new Label(zoomRow, SWT.NONE);
		zoomLabel.setText("Zoom:");

		Button zoomOut = new Button(zoomRow, SWT.PUSH);
		zoomOut.setText("-");

		final Combo zoomCombo = new Combo(zoomRow, SWT.DROP_DOWN | SWT.READ_ONLY);
		final int[] zoomLevels = {
			25, 50, 75, 100, 125, 150, 200
		};
		final int defaultZoomPercent = 100;
		int defaultZoomIndex = 0;
		for (int i = 0; i < zoomLevels.length; i++) {
			zoomCombo.add(zoomLevels[i] + "%");
			if (zoomLevels[i] == defaultZoomPercent) {
				defaultZoomIndex = i;
			}
		}
		zoomCombo.select(defaultZoomIndex);

		Button zoomIn = new Button(zoomRow, SWT.PUSH);
		zoomIn.setText("+");

		Label edgeFilterLabel = new Label(zoomRow, SWT.NONE);
		edgeFilterLabel.setText("Dependencies:");

		Combo edgeFilterCombo = new Combo(zoomRow, SWT.DROP_DOWN | SWT.READ_ONLY);
		edgeFilterCombo.setToolTipText("Controls which dependency edges are shown in the graph");
		EdgeFilter[] edgeFilters = EdgeFilter.values();
		int defaultEdgeFilterIndex = 0;
		for (int i = 0; i < edgeFilters.length; i++) {
			edgeFilterCombo.add(edgeFilters[i].getLabel());
			if (edgeFilters[i] == EdgeFilter.ALL) {
				defaultEdgeFilterIndex = i;
			}
		}
		edgeFilterCombo.select(defaultEdgeFilterIndex);
		edgeFilterCombo.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				edgeFilter = EdgeFilter.values()[edgeFilterCombo.getSelectionIndex()];
				rerender();
			}
		});

		// ---- Browser area ----
		Composite browserArea = new Composite(bottomPanel, SWT.NONE);
		browserArea.setLayout(new FillLayout());
		browserArea.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		try {
			browser = new Browser(browserArea, SWT.NONE);
			String html = buildMermaidHtml();
			browser.setText(html);
			browser.addProgressListener(new org.eclipse.swt.browser.ProgressAdapter() {
				@Override
				public void completed(org.eclipse.swt.browser.ProgressEvent event) {
					browserReady = true;
					rerender();
					applyZoom(currentZoomPercent); // ensures JS scale matches
					// UI state
				}
			});

			// Wire zoom combo
			zoomCombo.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					applyZoom(zoomLevels[zoomCombo.getSelectionIndex()]);
				}
			});
			// Wire zoom-in button
			zoomIn.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					int idx = zoomCombo.getSelectionIndex();
					if (idx < zoomLevels.length - 1) {
						zoomCombo.select(idx + 1);
						applyZoom(zoomLevels[idx + 1]);
					}
				}
			});
			// Wire zoom-out button
			zoomOut.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					int idx = zoomCombo.getSelectionIndex();
					if (idx > 0) {
						zoomCombo.select(idx - 1);
						applyZoom(zoomLevels[idx - 1]);
					}
				}
			});
		} catch (Exception e) {
			// Browser widget not available on this platform; show a label instead
			Label lbl = new Label(browserArea, SWT.WRAP);
			lbl.setText("Browser widget is not available on this platform. Cannot render Mermaid graph.");
			// Disable zoom controls when browser is unavailable
			zoomCombo.setEnabled(false);
			zoomIn.setEnabled(false);
			zoomOut.setEnabled(false);
		}
	}

	private int currentZoomPercent = 100;

	private void applyZoom(int percentValue) {
		if (browser == null || browser.isDisposed() || !browserReady)
			return;
		currentZoomPercent = percentValue;
		double target = percentValue / 100.0;
		browser.execute("window.setZoom(" + target + ");");
	}

	private String buildMermaidHtml() {
		String mermaidScriptTag = resolveMermaidScriptTag();
		return "<!DOCTYPE html>\n" //
			+ "<html>\n" //
			+ "<head>\n" //
			+ "<meta charset=\"UTF-8\">\n" //
			+ "<style>\n" //
			+ "  body { margin: 0; background: #fff; overflow: hidden; }\n" //
			+ "  #viewport { width: 100vw; height: 100vh; overflow: auto; }\n" //
			+ "  #scaler { display: inline-block; transform-origin: top left; }\n" //
			+ "</style>\n" //
			+ mermaidScriptTag + "\n" //
			+ "</head>\n" //
			+ "<body>\n" //
			+ "<div id=\"viewport\"><div id=\"scaler\"><div id=\"graph\"></div></div></div>\n" //
			+ "<script>\n" //
			+ "  mermaid.initialize({ startOnLoad: false, theme: 'default' });\n" //
			+ "  var currentScale = 1.0;\n" //
			+ "  window.setDiagram = function(def) {\n" //
			+ "    var el = document.getElementById('graph');\n" //
			+ "    el.removeAttribute('data-processed');\n" //
			+ "    el.className = 'mermaid';\n" //
			+ "    el.innerHTML = def;\n" //
			+ "    mermaid.init(undefined, el);\n" //
			+ "    applyScale();\n" //
			+ "  };\n" //
			+ "  window.setZoom = function(scale) {\n" //
			+ "    currentScale = scale;\n" //
			+ "    applyScale();\n" //
			+ "  };\n" //
			+ "  function applyScale() {\n" //
			+ "    var scaler = document.getElementById('scaler');\n" //
			+ "    scaler.style.transform = 'scale(' + currentScale + ')';\n" //
			+ "    var graph = document.getElementById('graph');\n" //
			+ "    scaler.style.width = (graph.offsetWidth * currentScale) + 'px';\n" //
			+ "    scaler.style.height = (graph.offsetHeight * currentScale) + 'px';\n" //
			+ "  }\n" //
			+ "</script>\n" //
			+ "</body>\n" //
			+ "</html>\n";
	}

	/**
	 * Returns a {@code <script>} tag that loads Mermaid from the bundled local
	 * resource (preferred) throws IllegalArgumentException if cannot be
	 * resolved.
	 * <p>
	 * The bundled file is at {@code /mermaid/mermaid-{version}.js} inside the
	 * plugin bundle and is included via {@code -includeresource} in
	 * {@code bnd.bnd}.
	 */
	private String resolveMermaidScriptTag() {
		try {
			Bundle bundle = FrameworkUtil.getBundle(BundleGraphView.class);
			if (bundle != null) {
				// source: https://cdn.jsdelivr.net/npm/mermaid@11.12.3/dist/
				URL entry = bundle.getEntry("/mermaid/mermaid-11.12.3.js");
				if (entry != null) {
					URL fileUrl = FileLocator.toFileURL(entry);
					return "<script src=\"" + fileUrl.toExternalForm() + "\"></script>";
				}
			}
		} catch (IOException e) {
			logger.logWarning("Failed to resolve bundled mermaid.min.js", e);
		}
		throw new IllegalArgumentException("Cannot load mermaid.min.js for graph rendering");
	}

	// ---- Public API ----


	/**
	 * Sets the model, seeds the selected set, and stores a model builder to re-create the model on "Refresh Universe".
	 * The model already contains all edges (both mandatory and optional); the "Include optional imports" checkbox
	 * filters at render time without rebuilding the model.
	 *
	 * @param newModel the new graph model
	 * @param initialSelected seed nodes to select (may be null)
	 * @param modelBuilder supplier that produces a fresh model (may be null)
	 */
	public void setInput(BundleGraphModel newModel, Set<BundleNode> initialSelected,
		Supplier<BundleGraphModel> modelBuilder) {
		this.model = newModel != null ? newModel
			: new SimpleBundleGraphModel(Collections.emptySet(), Collections.emptySet(), Collections.emptyMap());
		this.modelSupplier = modelBuilder;
		this.selected.clear();
		if (initialSelected != null) {
			this.selected.addAll(initialSelected);
		}
		refreshAvailable();
		selectedViewer.setInput(new ArrayList<>(selected));
		rerender();
	}

	// ---- Private helpers ----

	private void refreshAvailable() {
		if (availableViewer == null || availableViewer.getControl()
			.isDisposed()) {
			return;
		}
		List<BundleNode> filtered = new ArrayList<>();
		for (BundleNode node : model.nodes()) {
			if (filterString.isEmpty() || node.toString()
				.toLowerCase()
				.contains(filterString)) {
				filtered.add(node);
			}
		}
		filtered.sort((a, b) -> a.toString()
			.compareToIgnoreCase(b.toString()));
		availableViewer.setInput(filtered);
	}

	private void addSelected() {
		IStructuredSelection sel = availableViewer.getStructuredSelection();
		boolean changed = false;
		for (Object o : sel) {
			if (o instanceof BundleNode) {
				changed |= selected.add((BundleNode) o);
			}
		}
		if (changed) {
			selectedViewer.setInput(new ArrayList<>(selected));
			rerender();
		}
	}

	private void removeSelected() {
		IStructuredSelection sel = selectedViewer.getStructuredSelection();
		boolean changed = false;
		for (Object o : sel) {
			if (o instanceof BundleNode) {
				changed |= selected.remove(o);
			}
		}
		if (changed) {
			selectedViewer.setInput(new ArrayList<>(selected));
			rerender();
		}
	}

	/**
	 * Removes the highlighted entries in "Available bundles" from the universe model and (if present) from the selected
	 * set. Edges touching any removed node are also pruned from the model.
	 */
	private void removeFromUniverse() {
		IStructuredSelection sel = availableViewer.getStructuredSelection();
		if (sel.isEmpty()) {
			return;
		}
		Set<BundleNode> toRemove = new LinkedHashSet<>();
		for (Object o : sel) {
			if (o instanceof BundleNode) {
				toRemove.add((BundleNode) o);
			}
		}
		if (toRemove.isEmpty()) {
			return;
		}
		// Rebuild model without the removed nodes and their edges
		Set<BundleNode> newNodes = new LinkedHashSet<>(model.nodes());
		newNodes.removeAll(toRemove);
		Set<BundleEdge> newEdges = new LinkedHashSet<>();
		for (BundleEdge edge : model.edges()) {
			if (!toRemove.contains(edge.from()) && !toRemove.contains(edge.to())) {
				newEdges.add(edge);
			}
		}
		Map<BundleNode, File> newJarMap = new java.util.LinkedHashMap<>(model.nodeToJar());
		newJarMap.keySet().removeAll(toRemove);
		selected.removeAll(toRemove);
		this.model = new SimpleBundleGraphModel(newNodes, newEdges, newJarMap);
		refreshAvailable();
		selectedViewer.setInput(new ArrayList<>(selected));
		rerender();
	}

	private void addDependencies() {
		Set<BundleNode> closure = GraphClosures.dependencyClosure(model, new LinkedHashSet<>(selected));
		boolean changed = selected.addAll(closure);
		if (changed) {
			selectedViewer.setInput(new ArrayList<>(selected));
			rerender();
		}
	}

	private void addDependants() {
		Set<BundleNode> closure = GraphClosures.dependantClosure(model, new LinkedHashSet<>(selected));
		boolean changed = selected.addAll(closure);
		if (changed) {
			selectedViewer.setInput(new ArrayList<>(selected));
			rerender();
		}
	}

	private void rerender() {
		if (browser == null || browser.isDisposed() || !browserReady) {
			return;
		}
		Set<BundleNode> subset;
		switch (mode) {
			case SELECTED_AND_DEPENDENCIES :
				subset = GraphClosures.dependencyClosure(model, new LinkedHashSet<>(selected));
				break;
			case SELECTED_AND_DEPENDANTS :
				subset = GraphClosures.dependantClosure(model, new LinkedHashSet<>(selected));
				break;
			case ONLY_SELECTED :
			default :
				subset = new LinkedHashSet<>(selected);
				break;
		}
		// Pass the user-selected (primary) set so the renderer can style them differently
		lastMermaidDef = MermaidRenderer.toMermaid(model, subset, new LinkedHashSet<>(selected), edgeFilter,
			showFirstPackage);
		// Escape backticks for JS template literal
		String escaped = lastMermaidDef.replace("\\", "\\\\")
			.replace("`", "\\`");
		browser.execute("window.setDiagram(`" + escaped + "`)");
	}

	/**
	 * Re-creates the model from the stored supplier (if any) and updates the view, preserving currently selected nodes
	 * that still exist in the new model.
	 */
	private void refreshUniverse() {
		if (modelSupplier == null) {
			// No supplier stored – just re-apply the current filter
			refreshAvailable();
			return;
		}
		BundleGraphModel newModel = modelSupplier.get();
		if (newModel == null) {
			return;
		}
		this.model = newModel;
		// Keep only selected nodes that still exist in the refreshed universe
		selected.retainAll(newModel.nodes());
		refreshAvailable();
		selectedViewer.setInput(new ArrayList<>(selected));
		rerender();
	}

	/**
	 * Copies text to the system clipboard.
	 */
	private void copyToClipboard(String text) {
		if (text == null || text.isEmpty()) {
			return;
		}
		Clipboard clipboard = new Clipboard(Display.getDefault());
		try {
			clipboard.setContents(new Object[] {
				text
			}, new Transfer[] {
				TextTransfer.getInstance()
			});
		} finally {
			clipboard.dispose();
		}
	}

	/**
	 * Copies the BSNs of currently highlighted items in "Selected input bundles" to the system clipboard.
	 */
	private void copySelectedBundlesToClipboard() {
		IStructuredSelection sel = selectedViewer.getStructuredSelection();
		if (sel.isEmpty()) {
			return;
		}
		List<String> lines = new ArrayList<>();
		for (Iterator<?> it = sel.iterator(); it.hasNext();) {
			Object element = it.next();
			if (element instanceof BundleNode) {
				lines.add(element.toString());
			}
		}
		if (!lines.isEmpty()) {
			copyToClipboard(String.join(System.lineSeparator(), lines));
		}
	}



	/**
	 * Toggles between showing only the graph browser (top panel hidden) and the
	 * normal split view.
	 */
	private void toggleGraphMaximized() {
		graphMaximized = !graphMaximized;
		sash.setMaximizedControl(graphMaximized ? browserPanelComposite : null);
	}

	/**
	 * Merges an incoming model into the current universe. Combines the JAR-file maps of the existing and incoming
	 * models, then recomputes dependency edges over the full merged set so that cross-provider edges (e.g. a workspace
	 * project depending on a repository bundle) are discovered correctly.
	 */
	void mergeIntoUniverse(BundleGraphModel incoming) {
		Map<BundleNode, File> mergedJarMap = new LinkedHashMap<>(model.nodeToJar());
		mergedJarMap.putAll(incoming.nodeToJar());
		Set<BundleNode> mergedNodes = new LinkedHashSet<>(model.nodes());
		mergedNodes.addAll(incoming.nodes());
		Set<BundleEdge> recomputedEdges = ManifestDependencyCalculator.calculateEdges(mergedJarMap);
		this.model = new SimpleBundleGraphModel(mergedNodes, recomputedEdges, mergedJarMap);
		refreshAvailable();
		rerender();
	}

	/**
	 * Adds new nodes to the "Selected input bundles" list. Merges the incoming model into the universe first (which
	 * also recomputes cross-provider edges), then adds all incoming nodes to the selected set.
	 */
	void addNodesToSelected(BundleGraphModel incoming) {
		mergeIntoUniverse(incoming);
		boolean changed = selected.addAll(incoming.nodes());
		if (changed) {
			selectedViewer.setInput(new ArrayList<>(selected));
			rerender();
		}
	}

	@Override
	public void setFocus() {
		if (availableViewer != null) {
			availableViewer.getControl()
				.setFocus();
		}
	}

}