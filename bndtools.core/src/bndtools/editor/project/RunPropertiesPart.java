package bndtools.editor.project;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.bndtools.utils.swt.AddRemoveButtonBarPart;
import org.bndtools.utils.swt.AddRemoveButtonBarPart.AddRemoveListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;

import aQute.bnd.osgi.Constants;
import bndtools.editor.common.BndEditorPart;
import bndtools.editor.common.MapContentProvider;
import bndtools.editor.common.MapEntryCellModifier;
import bndtools.editor.common.PropertiesTableLabelProvider;
import bndtools.editor.utils.ToolTips;
import bndtools.utils.ModificationLock;

public class RunPropertiesPart extends BndEditorPart {

	private final ModificationLock					lock					= new ModificationLock();

	private Map<String, String>						runProperties;
	private String									programArgs				= null;
	private String									vmArgs					= null;

	private final AddRemoveButtonBarPart			createRemovePropsPart	= new AddRemoveButtonBarPart();

	private Table									tblRunProperties;
	private TableViewer								viewRunProperties;
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

	private void createSection(Section section, FormToolkit toolkit) {
		section.setText("Runtime Properties");

		final Composite composite = toolkit.createComposite(section);
		section.setClient(composite);

		// Create controls: Run Properties
		Label lblRunProperties = toolkit.createLabel(composite, "OSGi Framework properties:");
		tblRunProperties = toolkit.createTable(composite, SWT.FULL_SELECTION | SWT.MULTI | SWT.BORDER);
		viewRunProperties = new TableViewer(tblRunProperties);
		runPropertiesModifier = new MapEntryCellModifier<>(viewRunProperties);

		tblRunProperties.setHeaderVisible(true);
		final TableColumn tblRunPropsCol1 = new TableColumn(tblRunProperties, SWT.NONE);
		tblRunPropsCol1.setText("Name");
		tblRunPropsCol1.setWidth(100);
		final TableColumn tblRunPropsCol2 = new TableColumn(tblRunProperties, SWT.NONE);
		tblRunPropsCol1.setText("Value");
		tblRunPropsCol1.setWidth(100);

		viewRunProperties.setUseHashlookup(true);
		viewRunProperties.setColumnProperties(MapEntryCellModifier.getColumnProperties());
		runPropertiesModifier.addCellEditorsToViewer();
		viewRunProperties.setCellModifier(runPropertiesModifier);

		viewRunProperties.setContentProvider(new MapContentProvider());
		viewRunProperties.setLabelProvider(new PropertiesTableLabelProvider());
		Control createRemovePropsToolBar = createRemovePropsPart.createControl(composite, SWT.FLAT | SWT.VERTICAL);

		// Create controls: program args
		Label lblProgramArgs = toolkit.createLabel(composite, "Launcher Arguments:");
		txtProgramArgs = toolkit.createText(composite, "", SWT.MULTI | SWT.BORDER);
		ToolTips.setupMessageAndToolTipFromSyntax(txtProgramArgs, Constants.RUNPROGRAMARGS);

		// Create controls: vm args
		Label lblVmArgs = toolkit.createLabel(composite, "JVM Arguments:");
		txtVmArgs = toolkit.createText(composite, "", SWT.MULTI | SWT.BORDER);
		ToolTips.setupMessageAndToolTipFromSyntax(txtVmArgs, Constants.RUNVM);

		// Layout
		GridLayout gl;
		GridData gd;

		gl = new GridLayout(2, false);
		gl.marginWidth = 0;
		gl.marginHeight = 0;
		composite.setLayout(gl);

		lblRunProperties.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1));

		gd = new GridData(SWT.FILL, SWT.FILL, true, false);
		gd.heightHint = 50;
		gd.widthHint = 50;
		tblRunProperties.setLayoutData(gd);

		gd = new GridData(SWT.FILL, SWT.TOP, false, true);
		createRemovePropsToolBar.setLayoutData(gd);

		lblProgramArgs.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1));
		gd = new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1);
		gd.heightHint = 40;
		gd.widthHint = 50;
		txtProgramArgs.setLayoutData(gd);

		lblVmArgs.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1));
		gd = new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1);
		gd.heightHint = 40;
		gd.widthHint = 50;
		txtVmArgs.setLayoutData(gd);

		// Listeners
		viewRunProperties.addSelectionChangedListener(
			event -> createRemovePropsPart.setRemoveEnabled(!viewRunProperties.getSelection()
				.isEmpty()));
		createRemovePropsPart.addListener(new AddRemoveListener() {
			@Override
			public void addSelected() {
				runProperties.put("name", "");
				viewRunProperties.add("name");
				markDirty();
				viewRunProperties.editElement("name", 0);
			}

			@Override
			public void removeSelected() {
				@SuppressWarnings("rawtypes")
				Iterator iter = ((IStructuredSelection) viewRunProperties.getSelection()).iterator();
				while (iter.hasNext()) {
					Object item = iter.next();
					runProperties.remove(item);
					viewRunProperties.remove(item);
				}
				markDirty();
			}
		});
		runPropertiesModifier.addPropertyChangeListener(evt -> markDirty());
		txtProgramArgs.addModifyListener(ev -> lock.ifNotModifying(() -> {
			markDirty();
			programArgs = txtProgramArgs.getText();
			if (programArgs.length() == 0)
				programArgs = null;
		}));
		txtVmArgs.addModifyListener(ev -> lock.ifNotModifying(() -> {
			markDirty();
			vmArgs = txtVmArgs.getText();
			if (vmArgs.length() == 0)
				vmArgs = null;
		}));
		composite.addControlListener(new ControlAdapter() {
			@Override
			public void controlResized(ControlEvent e) {
				Rectangle area = composite.getClientArea();
				Point preferredSize = tblRunProperties.computeSize(SWT.DEFAULT, SWT.DEFAULT);
				int width = area.width - 2 * tblRunProperties.getBorderWidth();
				if (preferredSize.y > area.height + tblRunProperties.getHeaderHeight()) {
					// Subtract the scrollbar width from the total column width
					// if a vertical scrollbar will be required
					Point vBarSize = tblRunProperties.getVerticalBar()
						.getSize();
					width -= vBarSize.x;
				}
				Point oldSize = tblRunProperties.getSize();
				if (oldSize.x > area.width) {
					// table is getting smaller so make the columns
					// smaller first and then resize the table to
					// match the client area width
					tblRunPropsCol1.setWidth(width / 3);
					tblRunPropsCol2.setWidth(width - tblRunPropsCol1.getWidth());
					tblRunProperties.setSize(area.width, area.height);
				} else {
					// table is getting bigger so make the table
					// bigger first and then make the columns wider
					// to match the client area width
					tblRunProperties.setSize(area.width, area.height);
					tblRunPropsCol1.setWidth(width / 3);
					tblRunPropsCol2.setWidth(width - tblRunPropsCol1.getWidth());
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
		Map<String, String> tmp = model.getRunProperties();
		if (tmp == null)
			this.runProperties = new HashMap<>();
		else
			this.runProperties = new HashMap<>(tmp);
		viewRunProperties.setInput(runProperties);

		lock.modifyOperation(() -> {
			programArgs = model.getRunProgramArgs();
			if (programArgs == null)
				programArgs = ""; //$NON-NLS-1$
			txtProgramArgs.setText(programArgs);

			vmArgs = model.getRunVMArgs();
			if (vmArgs == null)
				vmArgs = "";
			txtVmArgs.setText(vmArgs);
		});
	}

	@Override
	protected void commitToModel(boolean onSave) {
		model.setRunProperties(runProperties);
		model.setRunProgramArgs(emptyToNull(programArgs));
		model.setRunVMArgs(emptyToNull(vmArgs));
	}

	private String emptyToNull(String s) {
		if (s != null && s.isEmpty())
			return null;
		return s;
	}
}
