package bndtools.editor.project;

import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.bndtools.utils.swt.AddRemoveButtonBarPart;
import org.bndtools.utils.swt.AddRemoveButtonBarPart.AddRemoveListener;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Path;
import org.bndtools.core.ui.StickyToolTipSupport;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;
import org.eclipse.ui.forms.editor.IFormPage;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.part.FileEditorInput;

import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Processor.PropertyKey;
import bndtools.Plugin;
import bndtools.editor.BndEditor;
import bndtools.editor.common.BndEditorPart;
import bndtools.editor.common.MapContentProvider;
import bndtools.editor.common.MapEntryCellModifier;
import bndtools.editor.common.PropertiesTableLabelProvider;
import bndtools.editor.utils.ToolTips;
import bndtools.utils.ModificationLock;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;

public class RunPropertiesPart extends BndEditorPart {

	private final ModificationLock					lock					= new ModificationLock();

	/** Local properties that are written to this file. */
	private final Map<String, String>				localProperties			= new LinkedHashMap<>();
	/** Keys present in included files but absent locally (shown gray, non-editable). */
	private final Map<String, String>				inheritedProperties		= new LinkedHashMap<>();
	/** Combined view used as viewer input: inherited entries first, then local. */
	private final Map<String, String>				displayProperties		= new LinkedHashMap<>();
	/** Keys from inheritedProperties (for label provider and modifier). */
	private final Set<String>						inheritedKeys			= new java.util.LinkedHashSet<>();
	/** Per-key provenance: maps each inherited property name to the file path that defines it. */
	private Map<String, String>						inheritedProvenances	= new java.util.LinkedHashMap<>();

	private String									programArgs				= null;
	private String									inheritedProgramArgs	= null;
	private String									vmArgs					= null;
	private String									inheritedVmArgs			= null;

	/** Key used to write local properties; may be plain or a .local suffix. */
	private String									localPropertiesKey		= Constants.RUNPROPERTIES;
	/** Key used to write local programArgs. */
	private String									localProgramArgsKey		= Constants.RUNPROGRAMARGS;
	/** Key used to write local vmArgs. */
	private String									localVmArgsKey			= Constants.RUNVM;

	private final AddRemoveButtonBarPart			createRemovePropsPart	= new AddRemoveButtonBarPart();

	private Table									tblRunProperties;
	private TableViewer								viewRunProperties;
	private final TableColumn[]						tblCols					= new TableColumn[2];
	private MapEntryCellModifier<String, String>	runPropertiesModifier;

	private Text									txtProgramArgs;
	private Text									txtVmArgs;

	private static final String[]					SUBCRIBE_PROPS			= new String[] {
		Constants.RUNPROPERTIES, Constants.RUNPROGRAMARGS, Constants.RUNVM
	};

	public RunPropertiesPart(Composite parent, FormToolkit toolkit, int style) {
		super(parent, toolkit, style);
		createSection(getSection(), toolkit);
	}

	/** Colors inherited table rows gray; local rows use the default foreground. */
	private class MixedPropertiesLabelProvider extends PropertiesTableLabelProvider {
		private final Color grey;

		MixedPropertiesLabelProvider(Display display) {
			grey = display.getSystemColor(SWT.COLOR_DARK_GRAY);
		}

		@Override
		public void update(ViewerCell cell) {
			super.update(cell);
			if (inheritedKeys.contains(cell.getElement())) {
				cell.setForeground(grey);
			}
		}

		@Override
		public String getToolTipText(Object element) {
			if (!(element instanceof String) || !inheritedKeys.contains(element))
				return null;
			String path = inheritedProvenances.get(element);
			return path != null
				? "Inherited from " + BndEditModelAccessor.getDisplayProvenance(model, path)
					+ "\nDouble-click to open source."
				: "Inherited from an included file.";
		}
	}

	/** Prevents editing of inherited (gray) entries. */
	private class LocalOnlyModifier extends MapEntryCellModifier<String, String> {
		LocalOnlyModifier(TableViewer viewer) {
			super(viewer);
		}

		@Override
		public boolean canModify(Object element, String property) {
			return !inheritedKeys.contains(element) && super.canModify(element, property);
		}
	}

	private void createSection(Section section, FormToolkit toolkit) {
		section.setText("Runtime Properties");

		final Composite composite = toolkit.createComposite(section);
		section.setClient(composite);

		Label lblRunProperties = toolkit.createLabel(composite, "OSGi Framework properties:");
		tblRunProperties = toolkit.createTable(composite, SWT.FULL_SELECTION | SWT.MULTI | SWT.BORDER);
		viewRunProperties = new TableViewer(tblRunProperties);
		runPropertiesModifier = new LocalOnlyModifier(viewRunProperties);

		tblRunProperties.setHeaderVisible(true);
		tblCols[0] = new TableColumn(tblRunProperties, SWT.NONE);
		tblCols[0].setText("Name");
		tblCols[0].setWidth(100);
		tblCols[1] = new TableColumn(tblRunProperties, SWT.NONE);
		tblCols[1].setText("Value");
		tblCols[1].setWidth(100);

		viewRunProperties.setUseHashlookup(true);
		viewRunProperties.setColumnProperties(MapEntryCellModifier.getColumnProperties());
		runPropertiesModifier.addCellEditorsToViewer();
		viewRunProperties.setCellModifier(runPropertiesModifier);
		viewRunProperties.setContentProvider(new MapContentProvider());
		viewRunProperties.setLabelProvider(new MixedPropertiesLabelProvider(tblRunProperties.getDisplay()));
		StickyToolTipSupport.enableFor(viewRunProperties);
		Control createRemovePropsToolBar = createRemovePropsPart.createControl(composite, SWT.FLAT | SWT.VERTICAL);

		Label lblProgramArgs = toolkit.createLabel(composite, "Launcher Arguments:");
		txtProgramArgs = toolkit.createText(composite, "", SWT.MULTI | SWT.BORDER);
		ToolTips.setupMessageAndToolTipFromSyntax(txtProgramArgs, Constants.RUNPROGRAMARGS);
		txtProgramArgs.addMouseListener(inheritedArgsNavigator(txtProgramArgs, Constants.RUNPROGRAMARGS));

		Label lblVmArgs = toolkit.createLabel(composite, "JVM Arguments:");
		txtVmArgs = toolkit.createText(composite, "", SWT.MULTI | SWT.BORDER);
		ToolTips.setupMessageAndToolTipFromSyntax(txtVmArgs, Constants.RUNVM);
		txtVmArgs.addMouseListener(inheritedArgsNavigator(txtVmArgs, Constants.RUNVM));

		// Layout
		GridLayout gl;
		GridData gd;

		gl = new GridLayout(2, false);
		gl.marginWidth = 0;
		gl.marginHeight = 0;
		composite.setLayout(gl);

		lblRunProperties.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1));

		// All three content areas grab vertical space equally (equal heightHint = equal base share).
		gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		gd.heightHint = 50;
		gd.widthHint = 50;
		tblRunProperties.setLayoutData(gd);

		gd = new GridData(SWT.FILL, SWT.TOP, false, false);
		createRemovePropsToolBar.setLayoutData(gd);

		lblProgramArgs.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1));
		gd = new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1);
		gd.heightHint = 50;
		gd.widthHint = 50;
		txtProgramArgs.setLayoutData(gd);

		lblVmArgs.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1));
		gd = new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1);
		gd.heightHint = 50;
		gd.widthHint = 50;
		txtVmArgs.setLayoutData(gd);

		// Listeners
		viewRunProperties.addSelectionChangedListener(event -> {
			IStructuredSelection sel = (IStructuredSelection) viewRunProperties.getSelection();
			boolean hasLocal = !sel.isEmpty() && sel.toList().stream().anyMatch(k -> !inheritedKeys.contains(k));
			createRemovePropsPart.setRemoveEnabled(hasLocal);
		});
		// Double-click on an inherited row opens the file that defines that specific key.
		tblRunProperties.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseDoubleClick(MouseEvent e) {
				IStructuredSelection sel = (IStructuredSelection) viewRunProperties.getSelection();
				if (sel.isEmpty()) return;
				String propKey = (String) sel.getFirstElement();
				if (!inheritedKeys.contains(propKey)) return;
				String path = inheritedProvenances.get(propKey);
				if (path != null) openProvenanceByPath(path);
			}
		});
		createRemovePropsPart.addListener(new AddRemoveListener() {
			@Override
			public void addSelected() {
				// New entries always go into local display; local key will be resolved on commit.
				String newKey = "name";
				// Avoid collision with existing keys
				int n = 1;
				while (displayProperties.containsKey(newKey))
					newKey = "name" + n++;
				displayProperties.put(newKey, "");
				viewRunProperties.add(newKey);
				markDirty();
				viewRunProperties.editElement(newKey, 0);
			}

			@Override
			public void removeSelected() {
				@SuppressWarnings("unchecked")
				Iterator<Object> iter = ((IStructuredSelection) viewRunProperties.getSelection()).iterator();
				while (iter.hasNext()) {
					Object item = iter.next();
					if (!inheritedKeys.contains(item)) {
						displayProperties.remove(item);
						viewRunProperties.remove(item);
					}
				}
				markDirty();
			}
		});
		runPropertiesModifier.addPropertyChangeListener(evt -> markDirty());
		txtProgramArgs.addModifyListener(ev -> lock.ifNotModifying(() -> {
			markDirty();
			programArgs = txtProgramArgs.getText();
			if (programArgs.isEmpty())
				programArgs = null;
		}));
		txtVmArgs.addModifyListener(ev -> lock.ifNotModifying(() -> {
			markDirty();
			vmArgs = txtVmArgs.getText();
			if (vmArgs.isEmpty())
				vmArgs = null;
		}));
		composite.addControlListener(new ControlAdapter() {
			@Override
			public void controlResized(ControlEvent e) {
				Rectangle area = composite.getClientArea();
				Point preferredSize = tblRunProperties.computeSize(SWT.DEFAULT, SWT.DEFAULT);
				int width = area.width - 2 * tblRunProperties.getBorderWidth();
				if (preferredSize.y > area.height + tblRunProperties.getHeaderHeight()) {
					Point vBarSize = tblRunProperties.getVerticalBar()
						.getSize();
					width -= vBarSize.x;
				}
				Point oldSize = tblRunProperties.getSize();
				if (oldSize.x > area.width) {
					tblCols[0].setWidth(width / 3);
					tblCols[1].setWidth(width - tblCols[0].getWidth());
					tblRunProperties.setSize(area.width, area.height);
				} else {
					tblRunProperties.setSize(area.width, area.height);
					tblCols[0].setWidth(width / 3);
					tblCols[1].setWidth(width - tblCols[0].getWidth());
				}
			}
		});
	}

	@Override
	protected String[] getProperties() {
		return SUBCRIBE_PROPS;
	}

	@Override
	protected void refreshFromModel() {
		// --- Properties table ------------------------------------------------
		Map<String, String> mergedProps = BndEditModelAccessor.getMergedProperties(model, Constants.RUNPROPERTIES);
		Map<String, String> localProps = BndEditModelAccessor.getLocalProperties(model, Constants.RUNPROPERTIES);
		if (mergedProps == null) mergedProps = new LinkedHashMap<>();
		if (localProps == null) localProps = new LinkedHashMap<>();

		inheritedProperties.clear();
		localProperties.clear();
		inheritedKeys.clear();
		for (Map.Entry<String, String> e : mergedProps.entrySet()) {
			if (localProps.containsKey(e.getKey())) {
				localProperties.put(e.getKey(), e.getValue());
			} else {
				inheritedProperties.put(e.getKey(), e.getValue());
				inheritedKeys.add(e.getKey());
			}
		}
		// Local-only keys (not in merged) are also local
		for (Map.Entry<String, String> e : localProps.entrySet()) {
			if (!mergedProps.containsKey(e.getKey()))
				localProperties.put(e.getKey(), e.getValue());
		}

		displayProperties.clear();
		displayProperties.putAll(inheritedProperties);
		displayProperties.putAll(localProperties);

		// Per-key provenance for the double-click handler.
		inheritedProvenances = BndEditModelAccessor.getInheritedPropertiesProvenance(model, Constants.RUNPROPERTIES);

		// Table tooltip.
		tblRunProperties.setToolTipText(
			inheritedProperties.isEmpty() ? null
				: "Some entries are inherited from included files. Double-click an inherited row to open its source.");
		viewRunProperties.setInput(displayProperties);

		// Determine local key for properties.
		String existingKey = BndEditModelAccessor.findLocalMergeKey(model, Constants.RUNPROPERTIES);
		localPropertiesKey = (existingKey != null) ? existingKey
			: (!inheritedProperties.isEmpty() ? Constants.RUNPROPERTIES + ".local" : Constants.RUNPROPERTIES);

		// --- Launcher Arguments text field -----------------------------------
		refreshTextArg(txtProgramArgs, Constants.RUNPROGRAMARGS,
			s -> programArgs = s, this::setLocalProgramArgsKey);

		// --- JVM Arguments text field ----------------------------------------
		refreshTextArg(txtVmArgs, Constants.RUNVM,
			s -> vmArgs = s, this::setLocalVmArgsKey);
	}

	@FunctionalInterface
	private interface StringSetter { void set(String v); }
	@FunctionalInterface
	private interface StringKeySetter { void set(String key); }

	private void refreshTextArg(Text txt, String stem,
		StringSetter localSetter, StringKeySetter keySetter) {

		String merged = BndEditModelAccessor.getMergedString(model, stem);
		boolean hasLocal = BndEditModelAccessor.hasLocalMergeProperty(model, stem);
		String existing = BndEditModelAccessor.findLocalMergeKey(model, stem);
		String key = (existing != null) ? existing
			: (!hasLocal && merged != null && !merged.isBlank()
				? stem + ".local" : stem);
		keySetter.set(key);

		lock.modifyOperation(() -> {
			if (hasLocal) {
				// Show the local value, editable.
				String localVal = model.getTypedProperty(existing != null ? existing : stem);
				String display = localVal != null ? localVal : "";
				txt.setText(display);
				localSetter.set(localVal);
				txt.setEditable(true);
				txt.setForeground(null);
				txt.setToolTipText(null);
			} else if (merged != null && !merged.isBlank()) {
				// Show inherited value grayed out; editable=false keeps mouse events (enabled=false would swallow them).
				txt.setText(merged);
				localSetter.set(null);
				txt.setEditable(false);
				Color grey = txt.getDisplay().getSystemColor(SWT.COLOR_DARK_GRAY);
				txt.setForeground(grey);
				List<PropertyKey> inherited = BndEditModelAccessor.getInheritedPropertyKeys(model, stem);
				String tip;
				if (inherited.size() > 1) {
					tip = "Inherited from multiple included files. Double-click to choose and open a source.";
				} else {
					tip = BndEditModelAccessor.getPropertyProvenance(model, stem)
						.map(p -> "Inherited from " + BndEditModelAccessor.getDisplayProvenance(model, p)
							+ ". Double-click to open source.")
						.orElse("Inherited from an included file.");
				}
				txt.setToolTipText(tip);
			} else {
				txt.setText("");
				localSetter.set(null);
				txt.setEditable(true);
				txt.setForeground(null);
				txt.setToolTipText(null);
			}
		});
	}

	private void setLocalProgramArgsKey(String key) { localProgramArgsKey = key; }
	private void setLocalVmArgsKey(String key) { localVmArgsKey = key; }

	@Override
	protected void commitToModel(boolean onSave) {
		// Properties: local entries = displayProperties minus inherited keys.
		Map<String, String> toSave = new LinkedHashMap<>(displayProperties);
		toSave.keySet().removeAll(inheritedKeys);
		String propsKey = localPropertiesKey != null ? localPropertiesKey : Constants.RUNPROPERTIES;
		if (Constants.RUNPROPERTIES.equals(propsKey)) {
			model.setRunProperties(toSave);
		} else {
			BndEditModelAccessor.setPropertiesByKey(model, propsKey, toSave);
		}

		// getEditable(): inherited fields are editable=false; enabled state is unreliable during save
		if (txtProgramArgs.getEditable()) {
			String value = emptyToNull(txtProgramArgs.getText());
			String key = localProgramArgsKey != null ? localProgramArgsKey : Constants.RUNPROGRAMARGS;
			if (Constants.RUNPROGRAMARGS.equals(key)) {
				model.setRunProgramArgs(value);
			} else {
				model.setTypedProperty(key, value);
			}
		}
		if (txtVmArgs.getEditable()) {
			String value = emptyToNull(txtVmArgs.getText());
			String key = localVmArgsKey != null ? localVmArgsKey : Constants.RUNVM;
			if (Constants.RUNVM.equals(key)) {
				model.setRunVMArgs(value);
			} else {
				model.setTypedProperty(key, value);
			}
		}
	}

	private MouseAdapter inheritedArgsNavigator(Text txt, String stem) {
		return new MouseAdapter() {
			@Override
			public void mouseDoubleClick(MouseEvent e) {
				if (txt.getEditable())
					return;
				openInheritedArgsProvenance(stem);
			}
		};
	}

	/** Opens the defining file of an inherited args value; multiple sources show a chooser. */
	private void openInheritedArgsProvenance(String stem) {
		List<PropertyKey> inherited = BndEditModelAccessor.getInheritedPropertyKeys(model, stem);
		if (inherited.isEmpty())
			return;
		if (inherited.size() == 1) {
			inherited.get(0)
				.getProvenance()
				.ifPresent(this::openProvenanceByPath);
			return;
		}
		ElementListSelectionDialog dialog = new ElementListSelectionDialog(getSection().getShell(),
			new LabelProvider() {
				@Override
				public String getText(Object element) {
					PropertyKey pk = (PropertyKey) element;
					String value = pk.getRawValue() != null ? pk.getRawValue() : "";
					String file = pk.getProvenance()
						.map(p -> new File(p).getName())
						.orElse("?");
					return pk.key() + " = " + value + "  \u2014  " + file;
				}
			});
		dialog.setTitle("Inherited " + stem);
		dialog.setMessage("Select an entry to open its defining file:");
		dialog.setElements(inherited.toArray());
		dialog.setMultipleSelection(false);
		if (dialog.open() == Window.OK) {
			PropertyKey pk = (PropertyKey) dialog.getFirstResult();
			if (pk != null)
				pk.getProvenance()
					.ifPresent(this::openProvenanceByPath);
		}
	}

	private String emptyToNull(String s) {
		return (s != null && !s.isEmpty()) ? s : null;
	}

	private void openProvenanceByPath(String absolutePath) {
		File file = new File(absolutePath);
		if (!file.isFile())
			return;
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		IFile iFile = root.getFileForLocation(new Path(absolutePath));
		if (iFile == null || !iFile.exists())
			return;
		try {
			PlatformUI.getWorkbench()
				.getActiveWorkbenchWindow()
				.getActivePage()
				.openEditor(new FileEditorInput(iFile), BndEditor.WORKSPACE_EDITOR);
		} catch (PartInitException e) {
			ErrorDialog.openError(getSection().getShell(), "Error", null,
				new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Failed to open source file.", e));
		}
	}
}
