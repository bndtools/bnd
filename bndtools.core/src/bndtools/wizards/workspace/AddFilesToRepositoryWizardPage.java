package bndtools.wizards.workspace;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.jar.Attributes;

import org.bndtools.api.ILogger;
import org.bndtools.api.Logger;
import org.bndtools.core.ui.icons.Icons;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.ui.dialogs.ElementTreeSelectionDialog;
import org.eclipse.ui.model.WorkbenchContentProvider;
import org.eclipse.ui.model.WorkbenchLabelProvider;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.Constants;

import aQute.bnd.osgi.Jar;
import bndtools.Plugin;
import bndtools.types.Pair;
import bndtools.utils.FileExtensionFilter;

public class AddFilesToRepositoryWizardPage extends WizardPage {
	private static final ILogger					logger		= Logger
		.getLogger(AddFilesToRepositoryWizardPage.class);

	private final Image								jarImg		= Icons.desc("jar")
		.createImage();
	private final Image								warnImg		= AbstractUIPlugin
		.imageDescriptorFromPlugin(Plugin.PLUGIN_ID, "/icons/warning_obj.gif")
		.createImage();
	private final Image								errorImg	= AbstractUIPlugin
		.imageDescriptorFromPlugin(Plugin.PLUGIN_ID, "/icons/error.gif")
		.createImage();
	private final Image								okayImg		= AbstractUIPlugin
		.imageDescriptorFromPlugin(Plugin.PLUGIN_ID, "/icons/tick.png")
		.createImage();

	private final Map<File, Pair<String, String>>	bsnMap		= new HashMap<>();
	private final List<File>						files		= new ArrayList<>(1);

	private TableViewer								viewer;

	public AddFilesToRepositoryWizardPage(String pageName) {
		super(pageName);
	}

	public void setFiles(File[] files) {
		this.files.clear();
		for (File file : files) {
			analyseFile(file);
			this.files.add(file);
		}

		if (viewer != null && !viewer.getControl()
			.isDisposed()) {
			viewer.refresh();
			validate();
		}
	}

	public List<File> getFiles() {
		return files;
	}

	void analyseFile(File file) {
		try (Jar jar = new Jar(file)) {
			Attributes attribs = jar.getManifest()
				.getMainAttributes();
			String bsn = attribs.getValue(Constants.BUNDLE_SYMBOLICNAME);
			String version = attribs.getValue(Constants.BUNDLE_VERSION);

			bsnMap.put(file, Pair.newInstance(bsn, version));
		} catch (Exception e) {
			logger.logError("Error reading JAR file content", e);
		}
	}

	@Override
	@SuppressWarnings("unused")
	public void createControl(Composite parent) {
		setTitle("Add Files to Repository");

		Composite composite = new Composite(parent, SWT.NONE);

		new Label(composite, SWT.NONE).setText("Selected files:");
		new Label(composite, SWT.NONE); // Spacer;
		Table table = new Table(composite, SWT.FULL_SELECTION | SWT.MULTI | SWT.BORDER);
		table.setLinesVisible(true);
		table.setHeaderVisible(true);

		TableColumn col;
		col = new TableColumn(table, SWT.NONE);
		col.setText("Path");
		col.setWidth(300);
		col = new TableColumn(table, SWT.NONE);
		col.setText("Bundle Name/Version");
		col.setWidth(300);

		viewer = new TableViewer(table);
		viewer.setContentProvider(new ArrayContentProvider());
		viewer.setLabelProvider(new StyledCellLabelProvider() {
			@Override
			public void update(ViewerCell cell) {
				File file = (File) cell.getElement();
				Pair<String, String> bundleId = bsnMap.get(file);

				int index = cell.getColumnIndex();
				if (index == 0) {
					if (bundleId == null) {
						cell.setImage(errorImg);
					} else {
						cell.setImage(jarImg);
					}
					StyledString label = new StyledString(file.getName());
					String parentPath = file.getParent();
					if (parentPath != null) {
						label.append(" (" + parentPath + ")", StyledString.QUALIFIER_STYLER);
					}
					cell.setText(label.getString());
					cell.setStyleRanges(label.getStyleRanges());
				} else if (index == 1) {
					if (bundleId == null) {
						cell.setImage(errorImg);
						cell.setText("Not a JAR file");
					} else {
						String bsn = bundleId.getFirst();
						String version = bundleId.getSecond();
						if (bsn == null) {
							cell.setImage(warnImg);
							cell.setText("Not a Bundle JAR");
						} else {
							cell.setImage(okayImg);
							StyledString styledString = new StyledString(bsn);
							if (version != null) {
								styledString.append(" [" + version + "]", StyledString.COUNTER_STYLER);
								cell.setText(styledString.getString());
								cell.setStyleRanges(styledString.getStyleRanges());
							}
						}
					}
				}
			}
		});
		viewer.setInput(files);
		validate();

		final Button btnAdd = new Button(composite, SWT.PUSH);
		btnAdd.setText("Add JARs...");

		final Button btnAddExternal = new Button(composite, SWT.PUSH);
		btnAddExternal.setText("Add External JARs...");

		final Button btnRemove = new Button(composite, SWT.NONE);
		btnRemove.setText("Remove");
		btnRemove.setEnabled(false);

		// LISTENERS
		viewer.addSelectionChangedListener(event -> btnRemove.setEnabled(!viewer.getSelection()
			.isEmpty()));
		btnAdd.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				doAdd();
			}
		});
		btnAddExternal.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				doAddExternal();
			}
		});
		btnRemove.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				doRemove();
			}
		});

		// LAYOUT
		composite.setLayout(new GridLayout(2, false));
		table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 4));
		btnAdd.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		btnRemove.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));

		setControl(composite);
	}

	void doAdd() {
		ElementTreeSelectionDialog dialog = new ElementTreeSelectionDialog(getShell(), new WorkbenchLabelProvider(),
			new WorkbenchContentProvider());
		dialog.setValidator(selection -> {
			if (selection.length > 0 && selection[0] instanceof IFile) {
				return new Status(IStatus.OK, Plugin.PLUGIN_ID, IStatus.OK, "", null); //$NON-NLS-1$
			}
			return new Status(IStatus.ERROR, Plugin.PLUGIN_ID, IStatus.ERROR, "", null); //$NON-NLS-1$
		});
		dialog.setAllowMultiple(true);
		dialog.setTitle("JAR File Selection");
		dialog.addFilter(new FileExtensionFilter("jar")); //$NON-NLS-1$
		dialog.setInput(ResourcesPlugin.getWorkspace()
			.getRoot());

		if (dialog.open() == Window.OK) {
			Object[] result = dialog.getResult();
			List<File> added = new ArrayList<>(result.length);
			for (Object fileObj : result) {
				IFile ifile = (IFile) fileObj;
				File file = ifile.getLocation()
					.toFile();
				analyseFile(file);
				files.add(file);
				added.add(file);
			}
			if (!added.isEmpty()) {
				viewer.add(added.toArray());
				validate();
			}
		}
	}

	void doAddExternal() {
		FileDialog dialog = new FileDialog(getShell(), SWT.OPEN | SWT.MULTI);
		dialog.setFilterExtensions(new String[] {
			"*.jar" //$NON-NLS-1$
		});
		String res = dialog.open();
		if (res != null) {
			IPath filterPath = new Path(dialog.getFilterPath());

			String[] fileNames = dialog.getFileNames();
			List<File> added = new ArrayList<>(fileNames.length);
			for (String fileName : fileNames) {
				added.add(filterPath.append(fileName)
					.toFile());
			}
			if (!added.isEmpty()) {
				for (File addedFile : added) {
					analyseFile(addedFile);
					files.add(addedFile);
				}
				viewer.add(added.toArray());
				validate();
			}
		}
	}

	void doRemove() {
		IStructuredSelection selection = (IStructuredSelection) viewer.getSelection();
		if (!selection.isEmpty()) {
			for (Iterator<?> iter = selection.iterator(); iter.hasNext();) {
				Object item = iter.next();
				files.remove(item);
				viewer.remove(item);
			}
			validate();
		}
	}

	void validate() {
		String error = null;
		String warning = null;

		for (File file : files) {
			Pair<String, String> pair = bsnMap.get(file);
			if (pair == null) {
				error = "One or more selected files is not a JAR.";
			} else {
				String bsn = pair.getFirst();
				if (bsn == null) {
					warning = "One or more selected files is not a Bundle JAR";
				}
			}
		}

		setErrorMessage(error);
		setMessage(warning, WARNING);
		setPageComplete(!files.isEmpty() && error == null);
	}

	@Override
	public void dispose() {
		super.dispose();
		jarImg.dispose();
		warnImg.dispose();
		errorImg.dispose();
		okayImg.dispose();
	}
}
