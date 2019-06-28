package bndtools.wizards.bndfile;

import java.text.MessageFormat;
import java.util.Collection;
import java.util.Collections;

import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.ui.PlatformUI;

import bndtools.Plugin;

public class EnableSubBundlesDialog extends TitleAreaDialog {

	private boolean						enableSubBundles	= true;
	private final Collection<String>	allProperties;
	private final Collection<String>	selectedProperties;

	private Table						propsTable;

	private CheckboxTableViewer			viewer;
	private Button						btnCheckAll;
	private Button						btnUncheckAll;
	private Link						link;
	private Button						btnEnableSubbundles;
	private Label						lblHeaderCount;

	/**
	 * Create the dialog.
	 *
	 * @param parentShell
	 */
	public EnableSubBundlesDialog(Shell parentShell, Collection<String> allProperties,
		Collection<String> selectedProperties) {
		super(parentShell);
		setShellStyle(SWT.CLOSE | SWT.MAX | SWT.TITLE | SWT.BORDER | SWT.APPLICATION_MODAL | SWT.RESIZE
			| getDefaultOrientation());
		this.allProperties = allProperties;
		this.selectedProperties = selectedProperties;

		setHelpAvailable(true);
		setDialogHelpAvailable(true);
	}

	/**
	 * Create contents of the dialog.
	 *
	 * @param parent
	 */
	@SuppressWarnings("unused")
	@Override
	protected Control createDialogArea(Composite parent) {
		setMessage(Messages.EmptyBndFileWizard_questionSubBundlesNotEnabled);
		setTitle("Sub-bundles not enabled");
		Composite area = (Composite) super.createDialogArea(parent);
		Composite container = new Composite(area, SWT.NONE);
		GridLayout layout = new GridLayout(2, false);
		layout.marginTop = 20;
		layout.marginWidth = 10;
		layout.verticalSpacing = 10;
		container.setLayout(layout);
		container.setLayoutData(new GridData(GridData.FILL_BOTH));

		btnEnableSubbundles = new Button(container, SWT.CHECK);
		btnEnableSubbundles.setText(Messages.EnableSubBundlesDialog_btnEnableSubbundles_text_3);
		btnEnableSubbundles.setSelection(enableSubBundles);
		new Label(container, SWT.NONE);

		link = new Link(container, SWT.NONE);
		link.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1));
		link.setText(Messages.EnableSubBundlesDialog_link_text);
		link.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				getShell().notifyListeners(SWT.Help, new Event());
			}
		});

		propsTable = new Table(container, SWT.BORDER | SWT.CHECK | SWT.FULL_SELECTION);
		GridData gd_propsTable = new GridData(SWT.FILL, SWT.FILL, true, true, 1, 2);
		gd_propsTable.heightHint = 100;
		gd_propsTable.widthHint = 175;
		propsTable.setLayoutData(gd_propsTable);

		viewer = new CheckboxTableViewer(propsTable);
		viewer.setContentProvider(ArrayContentProvider.getInstance());
		viewer.setInput(allProperties);
		viewer.setCheckedElements(selectedProperties.toArray());

		btnCheckAll = new Button(container, SWT.NONE);
		btnCheckAll.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false, 1, 1));
		btnCheckAll.setText(Messages.EnableSubBundlesDialog_btnCheckAll_text);

		btnUncheckAll = new Button(container, SWT.NONE);
		btnUncheckAll.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false, 1, 1));
		btnUncheckAll.setText(Messages.EnableSubBundlesDialog_btnUncheckAll_text);

		lblHeaderCount = new Label(container, SWT.NONE);
		lblHeaderCount.setText(MessageFormat.format("", allProperties.size()));

		new Label(container, SWT.NONE); // Spacer

		btnEnableSubbundles.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				enableSubBundles = btnEnableSubbundles.getSelection();
				updateEnablement();
			}
		});
		viewer.addCheckStateListener(event -> {
			String property = (String) event.getElement();
			if (event.getChecked())
				selectedProperties.add(property);
			else
				selectedProperties.remove(property);
		});
		btnCheckAll.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				selectedProperties.clear();
				selectedProperties.addAll(allProperties);
				viewer.setCheckedElements(selectedProperties.toArray());
			}
		});
		btnUncheckAll.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				selectedProperties.clear();
				viewer.setCheckedElements(selectedProperties.toArray());
			}
		});

		PlatformUI.getWorkbench()
			.getHelpSystem()
			.setHelp(getShell(), Plugin.PLUGIN_ID + ".enableSubBundles");

		return area;
	}

	void updateEnablement() {
		btnCheckAll.setEnabled(enableSubBundles);
		btnUncheckAll.setEnabled(enableSubBundles);
		propsTable.setEnabled(enableSubBundles);
	}

	@Override
	protected Point getInitialSize() {
		return new Point(475, 330);
	}

	public boolean isEnableSubBundles() {
		return enableSubBundles;
	}

	public Collection<String> getSelectedProperties() {
		return Collections.unmodifiableCollection(selectedProperties);
	}
}
