package bndtools.wizards.workspace;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.bndtools.templating.Resource;
import org.bndtools.templating.ResourceMap;
import org.bndtools.templating.Template;
import org.bndtools.utils.jface.ItalicStyler;
import org.bndtools.utils.swt.SWTConcurrencyUtil;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.jface.wizard.IWizardContainer;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.ui.plugin.AbstractUIPlugin;

import aQute.lib.io.IO;
import bndtools.Plugin;
import bndtools.utils.ModificationLock;

public class WorkspacePreviewPage extends WizardPage {

	private final Object				lock					= new Object();
	private final ModificationLock		modifyLock				= new ModificationLock();

	private final Set<String>			existingFiles			= new HashSet<>();
	private final Map<String, String>	resourceErrors			= new HashMap<>();
	private final Set<String>			checkedPaths			= new HashSet<>();

	private static final String			MSG_NOTHING_SELECTED	= "Select an entry to view details";

	private File						targetDir;
	private Template					template;
	private ResourceMap					templateOutputs			= null;
	private String						errorMessage			= null;

	private boolean						seen					= false;
	private Table						tblOutputs;
	private CheckboxTableViewer			vwrOutputs;

	private Image						imgAdded;
	private Image						imgOverwrite;
	private Image						imgError;

	private final Runnable				updateDisplayTask		= () -> {
																	synchronized (lock) {
																		if (errorMessage != null) {
																			setErrorMessage(errorMessage);
																		} else if (!resourceErrors.isEmpty()) {
																			setErrorMessage(
																				"Cannot expand the template due to errors on some resources.");
																		} else {
																			setErrorMessage(null);
																			if (!existingFiles.isEmpty()) {
																				setMessage(
																					"Some files will be overwritten",
																					WARNING);
																			} else {
																				setMessage(null, WARNING);
																			}
																		}

																		List<String> viewerInput;
																		if (templateOutputs != null) {
																			viewerInput = new ArrayList<>(
																				templateOutputs.entries()
																					.size());
																			for (Entry<String, Resource> entry : templateOutputs
																				.entries())
																				viewerInput.add(entry.getKey());
																			seen = true;
																		} else {
																			viewerInput = Collections.emptyList();
																			seen = false;
																		}
																		checkedPaths.addAll(viewerInput);

																		if (tblOutputs != null
																			&& !tblOutputs.isDisposed()
																			&& vwrOutputs != null) {
																			vwrOutputs.setInput(viewerInput);
																			vwrOutputs.setAllChecked(true);
																		}

																		IWizardContainer container = getContainer();
																		if (container != null)
																			container.updateButtons();
																	}
																};

	private final IRunnableWithProgress	calculatePreviewTask	= monitor -> {
																	synchronized (lock) {
																		try {
																			if (templateOutputs == null) {
																				errorMessage = null;
																				existingFiles.clear();
																				resourceErrors.clear();

																				templateOutputs = template
																					.generateOutputs(Collections
																						.<String, List<Object>> emptyMap(),
																						monitor);

																				IWorkspaceRoot workspaceRoot = ResourcesPlugin
																					.getWorkspace()
																					.getRoot();

																				for (Entry<String, Resource> entry : templateOutputs
																					.entries()) {
																					// Check
																					// for
																					// existing
																					// files
																					File file = new File(targetDir,
																						entry.getKey());
																					switch (entry.getValue()
																						.getType()) {
																						case Folder :
																							if (file.exists()
																								&& !file.isDirectory())
																								resourceErrors.put(
																									entry.getKey(),
																									String.format(
																										"Path already exists and is not a directory: %s",
																										entry
																											.getKey()));
																							break;
																						case File :
																							if (file.exists()
																								&& !isEqualContent(file,
																									entry.getValue()
																										.getContent())) {

																								existingFiles.add(
																									entry.getKey());
																								if (!file.isFile())
																									resourceErrors.put(
																										entry.getKey(),
																										String.format(
																											"Path already exists and is not a plain file: %s",
																											entry
																												.getKey()));
																							}
																							break;
																						default :
																							// ignore
																					}

																					// If
																					// the
																					// base
																					// folder
																					// has
																					// the
																					// same
																					// name
																					// as
																					// a
																					// project,
																					// but
																					// the
																					// project
																					// has
																					// a
																					// different
																					// location
																					// in
																					// the
																					// filesystem,
																					// then
																					// we
																					// can't
																					// import
																					// it.
																					String projectFolderName;
																					int slashIndex = entry.getKey()
																						.indexOf('/');
																					if (slashIndex < 0)
																						projectFolderName = entry
																							.getKey();
																					else
																						projectFolderName = entry
																							.getKey()
																							.substring(0, slashIndex);
																					File projectFolder = new File(
																						targetDir, projectFolderName);
																					IProject project = workspaceRoot
																						.getProject(projectFolderName);
																					if (project.exists()) {
																						File existingProjectLoc = project
																							.getLocation()
																							.toFile();
																						if (!existingProjectLoc
																							.equals(projectFolder))
																							resourceErrors.put(
																								entry.getKey(),
																								String.format(
																									"Cannot import project directory %s as it clashes with an existing project '%s' at location %s",
																									projectFolder
																										.getAbsolutePath(),
																									project.getName(),
																									existingProjectLoc
																										.getAbsolutePath()));
																					}

																				}
																			}
																		} catch (Exception e) {
																			errorMessage = e.getMessage();
																		}
																	}

																	SWTConcurrencyUtil.execForControl(tblOutputs, true,
																		updateDisplayTask);
																};

	public WorkspacePreviewPage() {
		super("preview");
	}

	@Override
	public void createControl(Composite parent) {
		setTitle("Preview Changes");
		setImageDescriptor(Plugin.imageDescriptorFromPlugin("icons/bndtools-wizban.png")); //$NON-NLS-1$

		imgAdded = AbstractUIPlugin.imageDescriptorFromPlugin(Plugin.PLUGIN_ID, "icons/incoming.gif")
			.createImage(parent.getDisplay());
		imgOverwrite = AbstractUIPlugin.imageDescriptorFromPlugin(Plugin.PLUGIN_ID, "icons/conflict.gif")
			.createImage(parent.getDisplay());
		imgError = AbstractUIPlugin.imageDescriptorFromPlugin(Plugin.PLUGIN_ID, "icons/error_obj.gif")
			.createImage(parent.getDisplay());

		int columns = 4;

		Composite composite = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout(columns, false);
		composite.setLayout(layout);
		setControl(composite);

		Label lblTitle = new Label(composite, SWT.NONE);
		lblTitle.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, columns, 1));

		// Table
		tblOutputs = new Table(composite, SWT.BORDER | SWT.CHECK);
		tblOutputs.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, columns, 1));
		vwrOutputs = new CheckboxTableViewer(tblOutputs);
		vwrOutputs.setContentProvider(ArrayContentProvider.getInstance());
		vwrOutputs.setLabelProvider(new StyledCellLabelProvider() {
			@Override
			public void update(ViewerCell cell) {
				StyledString label;
				Image icon;

				String path = (String) cell.getElement();
				String error = resourceErrors.get(path);
				if (error != null) {
					label = new StyledString(path, ItalicStyler.INSTANCE_ERROR);
					icon = imgError;
				} else {
					label = new StyledString(path);
					icon = existingFiles.contains(path) ? imgOverwrite : imgAdded;
				}

				cell.setText(path);
				cell.setStyleRanges(label.getStyleRanges());
				cell.setImage(icon);
			}
		});
		vwrOutputs.setComparator(new ViewerComparator(Collator.getInstance()));

		// Details display
		final Label lblDetails = new Label(composite, SWT.NONE);
		lblDetails.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, columns, 1));
		lblDetails.setText(MSG_NOTHING_SELECTED);

		// Button Panel

		Label spacer1 = new Label(composite, SWT.NONE);
		spacer1.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		Button btnSelectNonConflict = new Button(composite, SWT.PUSH);
		btnSelectNonConflict.setText("Select Non-Conflicting");
		btnSelectNonConflict.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				modifyLock.modifyOperation(() -> {
					checkedPaths.clear();
					for (Entry<String, Resource> entry : templateOutputs.entries()) {
						String path = entry.getKey();
						if (!existingFiles.contains(path))
							checkedPaths.add(path);
					}
					vwrOutputs.setCheckedElements(checkedPaths.toArray());
				});
			}
		});
		Button btnSelectAll = new Button(composite, SWT.PUSH);
		btnSelectAll.setText("Select All");
		btnSelectAll.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				vwrOutputs.setAllChecked(true);
			}
		});
		Button btnSelectNone = new Button(composite, SWT.PUSH);
		btnSelectNone.setText("Select None");
		btnSelectNone.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				vwrOutputs.setAllChecked(false);
			}
		});

		// Listeners
		vwrOutputs.addSelectionChangedListener(event -> {
			IStructuredSelection sel = (IStructuredSelection) vwrOutputs.getSelection();
			if (sel.isEmpty()) {
				lblDetails.setText(MSG_NOTHING_SELECTED);
			} else {
				String path = (String) sel.getFirstElement();
				String resourceError = resourceErrors.get(path);
				if (resourceError != null) {
					lblDetails.setText(resourceError);
				} else if (existingFiles.contains(path)) {
					lblDetails.setText("This file already exists and will be overwritten");
				} else {
					lblDetails.setText("This file will be created");
				}
			}
		});
		vwrOutputs.addCheckStateListener(event -> modifyLock.ifNotModifying(() -> {
			final String updatedPath = (String) event.getElement();
			if (event.getChecked()) {
				checkedPaths.add(updatedPath);
				// Check any directories that are parents of this path
				modifyLock.modifyOperation(() -> {
					for (Entry<String, Resource> entry : templateOutputs.entries()) {
						String path = entry.getKey();
						if (path.endsWith("/") && updatedPath.startsWith(path)) {
							checkedPaths.add(path);
							vwrOutputs.setChecked(path, true);
						}
					}
				});
			} else {
				checkedPaths.remove(updatedPath);
				// Uncheck any paths that are descendants of this path
				if (updatedPath.endsWith("/")) {
					modifyLock.modifyOperation(() -> {
						for (Entry<String, Resource> entry : templateOutputs.entries()) {
							String path = entry.getKey();
							if (path.startsWith(updatedPath)) {
								checkedPaths.remove(path);
								vwrOutputs.setChecked(path, false);
							}
						}
					});
				}
			}
		}));
	}

	@Override
	public void dispose() {
		super.dispose();

		imgAdded.dispose();
		imgOverwrite.dispose();
		imgError.dispose();
	}

	@Override
	public boolean isPageComplete() {
		return super.isPageComplete() && seen && errorMessage == null && resourceErrors.isEmpty();
	}

	@Override
	public void setVisible(boolean visible) {
		super.setVisible(visible);
		if (visible) {
			try {
				getContainer().run(true, true, calculatePreviewTask);
			} catch (InvocationTargetException e) {
				errorMessage = e.getTargetException()
					.getMessage();
			} catch (InterruptedException e) {
				errorMessage = e.getMessage();
			}
			updateDisplayTask.run();
		}
	}

	public void setTemplate(Template template) {
		// set the template and remove the resource lock
		synchronized (lock) {
			this.template = template;
			this.templateOutputs = null;
		}
		updateDisplayTask.run();
	}

	public void setTargetDir(File file) {
		synchronized (lock) {
			this.targetDir = file;
			this.templateOutputs = null;
		}
		updateDisplayTask.run();
	}

	public Template getTemplate() {
		return template;
	}

	public ResourceMap getTemplateOutputs() {
		synchronized (lock) {
			return templateOutputs;
		}
	}

	public File getTargetDir() {
		return targetDir;
	}

	public Set<String> getCheckedPaths() {
		return Collections.unmodifiableSet(checkedPaths);
	}

	private boolean isEqualContent(File file, InputStream content) throws IOException {
		byte[] a = IO.read(file);
		byte[] b = IO.read(content);
		return Arrays.equals(a, b);
	}
}
