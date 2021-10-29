package org.bndtools.remoteinstall.wizard;

import static org.bndtools.remoteinstall.helper.MessageDialogHelper.showMessage;
import static org.bndtools.remoteinstall.nls.Messages.InstallBundleWizardPage_ButtonAdd_Title;
import static org.bndtools.remoteinstall.nls.Messages.InstallBundleWizardPage_ButtonEdit_Title;
import static org.bndtools.remoteinstall.nls.Messages.InstallBundleWizardPage_ButtonRemove_Title;
import static org.bndtools.remoteinstall.nls.Messages.InstallBundleWizardPage_Description;
import static org.bndtools.remoteinstall.nls.Messages.InstallBundleWizardPage_Dialog_Title;
import static org.bndtools.remoteinstall.nls.Messages.InstallBundleWizardPage_Error_NoConfigSelected;
import static org.bndtools.remoteinstall.nls.Messages.InstallBundleWizardPage_Name;
import static org.bndtools.remoteinstall.nls.Messages.TableColumn_Host;
import static org.bndtools.remoteinstall.nls.Messages.TableColumn_Name;
import static org.bndtools.remoteinstall.nls.Messages.TableColumn_Port;
import static org.bndtools.remoteinstall.nls.Messages.TableColumn_Timeout;
import static org.eclipse.core.runtime.IStatus.OK;
import static org.eclipse.swt.layout.GridData.BEGINNING;
import static org.eclipse.swt.layout.GridData.CENTER;
import static org.eclipse.swt.layout.GridData.FILL;

import org.bndtools.remoteinstall.dialog.RemoteRuntimeConfigurationDialog;
import org.bndtools.remoteinstall.dto.RemoteRuntimeConfiguration;
import org.bndtools.remoteinstall.store.RemoteRuntimeConfigurationStore;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.ui.PlatformUI;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(service = InstallBundleWizardPage.class)
public final class InstallBundleWizardPage extends WizardPage {

	private Composite						composite;
	private TableViewer						listViewer;

	private static final int				TABLE_COLUMN_WIDTH		= 100;
	private static final int				FORM_SELECT_AREA_WIDTH	= 400;
	private static final int				FORM_SELECT_AREA_HEIGHT	= 150;

	@Reference
	private RemoteRuntimeConfigurationStore	store;

	public InstallBundleWizardPage() {
		super(InstallBundleWizardPage_Name);
		setDescription(InstallBundleWizardPage_Description);
	}

	@Override
	public void createControl(final Composite parent) {
		composite = parent;

		initComposite();
		initForm();
		initList();
	}

	private void addConfiguration() {
		final RemoteRuntimeConfigurationDialog dialog = new RemoteRuntimeConfigurationDialog(
				PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell());

		if (dialog.open() == OK) {
			final RemoteRuntimeConfiguration config = dialog.getConfiguration();
			store.addConfiguration(config);
			initList();
		}
	}

	private void modifyConfiguration() {
		int selectedIndex = listViewer.getTable().getSelectionIndex();
		if (selectedIndex < 0) {
			if (listViewer.getTable().getItemCount() > 0) {
				listViewer.getTable().select(0);
				selectedIndex = 0;
			} else {
				showMessage(InstallBundleWizardPage_Error_NoConfigSelected, InstallBundleWizardPage_Dialog_Title);
				return;
			}
		}

		final RemoteRuntimeConfiguration configuration = store.getConfiguration(selectedIndex);
		if (configuration != null) {
			final RemoteRuntimeConfigurationDialog dialog = new RemoteRuntimeConfigurationDialog(
					PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell());

			dialog.setConfiguration(configuration);

			if (dialog.open() == OK) {
				final RemoteRuntimeConfiguration c = dialog.getConfiguration();

				configuration.name = c.name;
				configuration.host = c.host;
				configuration.port = c.port;
				configuration.timeout = c.timeout;

				store.updateConfiguration(selectedIndex, configuration);
				initList();
			}
		}
	}

	private void deleteConfiguration() {
		final int selectedIndex = listViewer.getTable().getSelectionIndex();
		if (selectedIndex < 0) {
			showMessage(InstallBundleWizardPage_Error_NoConfigSelected, InstallBundleWizardPage_Dialog_Title);
			return;
		}
		store.removeConfiguration(selectedIndex);
		initList();
	}

	public RemoteRuntimeConfiguration getSelectedConfiguration() {
		final IStructuredSelection selection = listViewer.getStructuredSelection();
		return (RemoteRuntimeConfiguration) selection.getFirstElement();
	}

	private void initComposite() {
		final GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		composite.setLayout(layout);

		setControl(composite);
	}

	private void initForm() {
		initTable();
		initAddButton();
		initModifyButton();
		initDeleteButton();
	}

	private void initAddButton() {
		final Button buttonAdd = new Button(composite, SWT.NONE);

		buttonAdd.setText(InstallBundleWizardPage_ButtonAdd_Title);
		buttonAdd.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(final SelectionEvent event) {
				addConfiguration();
			}
		});
		buttonAdd.setLayoutData(new GridData(FILL, CENTER, true, false));
	}

	private void initModifyButton() {
		final Button buttonModify = new Button(composite, SWT.NONE);
		buttonModify.setText(InstallBundleWizardPage_ButtonEdit_Title);
		buttonModify.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(final SelectionEvent event) {
				modifyConfiguration();
			}
		});
		buttonModify.setLayoutData(new GridData(FILL, BEGINNING, true, false));
	}

	private void initDeleteButton() {
		final Button buttonDelete = new Button(composite, SWT.NONE);

		buttonDelete.setText(InstallBundleWizardPage_ButtonRemove_Title);
		buttonDelete.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(final SelectionEvent event) {
				deleteConfiguration();
			}
		});
		buttonDelete.setLayoutData(new GridData(FILL, BEGINNING, true, false));
	}

	private void initTable() {
		final GridData configsListData = new GridData();

		configsListData.widthHint = FORM_SELECT_AREA_WIDTH;
		configsListData.heightHint = FORM_SELECT_AREA_HEIGHT;
		configsListData.verticalSpan = 3;
		configsListData.grabExcessHorizontalSpace = false;
		configsListData.grabExcessVerticalSpace = true;

		listViewer = new TableViewer(composite,
				SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL | SWT.FULL_SELECTION | SWT.BORDER);

		final Table table = listViewer.getTable();
		table.setLayoutData(configsListData);
		table.setHeaderVisible(true);
		table.setLinesVisible(true);

		listViewer.setContentProvider(ArrayContentProvider.getInstance());

		final TableViewerColumn colName = new TableViewerColumn(listViewer, SWT.NONE);
		colName.getColumn().setWidth(TABLE_COLUMN_WIDTH);
		colName.getColumn().setText(TableColumn_Name);
		colName.setLabelProvider(new ColumnLabelProvider() {

			@Override
			public String getText(final Object element) {
				final RemoteRuntimeConfiguration config = (RemoteRuntimeConfiguration) element;
				return config.name;
			}
		});

		final TableViewerColumn colHost = new TableViewerColumn(listViewer, SWT.NONE);
		colHost.getColumn().setWidth(TABLE_COLUMN_WIDTH);
		colHost.getColumn().setText(TableColumn_Host);
		colHost.setLabelProvider(new ColumnLabelProvider() {

			@Override
			public String getText(final Object element) {
				final RemoteRuntimeConfiguration config = (RemoteRuntimeConfiguration) element;
				return config.host;
			}
		});

		final TableViewerColumn colPort = new TableViewerColumn(listViewer, SWT.NONE);
		colPort.getColumn().setWidth(TABLE_COLUMN_WIDTH);
		colPort.getColumn().setText(TableColumn_Port);
		colPort.setLabelProvider(new ColumnLabelProvider() {

			@Override
			public String getText(final Object element) {
				final RemoteRuntimeConfiguration config = (RemoteRuntimeConfiguration) element;
				return String.valueOf(config.port);
			}
		});

		final TableViewerColumn colTimeout = new TableViewerColumn(listViewer, SWT.NONE);
		colTimeout.getColumn().setWidth(TABLE_COLUMN_WIDTH);
		colTimeout.getColumn().setText(TableColumn_Timeout);
		colTimeout.setLabelProvider(new ColumnLabelProvider() {

			@Override
			public String getText(final Object element) {
				final RemoteRuntimeConfiguration config = (RemoteRuntimeConfiguration) element;
				return String.valueOf(config.timeout);
			}
		});
	}

	private void initList() {
		listViewer.getTable().removeAll();
		RemoteRuntimeConfiguration[] array = store.getConfigurations().toArray(new RemoteRuntimeConfiguration[0]);
		listViewer.setInput(array);
		listViewer.getTable().redraw();
		if (array != null && array.length > 0) {
			listViewer.getTable().select(0);
		}
	}

}
