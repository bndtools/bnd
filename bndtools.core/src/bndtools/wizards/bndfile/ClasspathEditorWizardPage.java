/*******************************************************************************
 * Copyright (c) 2010 Neil Bartlett.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Neil Bartlett - initial API and implementation
 *******************************************************************************/
package bndtools.wizards.bndfile;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.ViewerDropAdapter;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.FileTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.dnd.TransferData;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.ui.dialogs.ElementTreeSelectionDialog;
import org.eclipse.ui.dialogs.ISelectionStatusValidator;
import org.eclipse.ui.model.WorkbenchContentProvider;
import org.eclipse.ui.model.WorkbenchLabelProvider;

import bndtools.Plugin;
import bndtools.utils.ClassPathLabelProvider;
import bndtools.utils.FileExtensionFilter;

public class ClasspathEditorWizardPage extends WizardPage {

    public static final String PROP_PATHS = "paths";

    private final PropertyChangeSupport propertySupport = new PropertyChangeSupport(this);
	private final Collection<IPath> paths = new LinkedList<IPath>();

	private TableViewer viewer;
	private Button btnAdd;
	private Button btnAddExternal;
	private Button btnRemove;

	public ClasspathEditorWizardPage(final String pageName) {
		super(pageName);
	}

	public void createControl(final Composite parent) {
		setTitle("Select JARs");
		final Composite composite = new Composite(parent, SWT.NONE);

		Label lblHint = new Label(composite, SWT.WRAP);
		lblHint.setText("Selected files (hint: drag files from an external application into this list):");

		final Table table = new Table(composite, SWT.FULL_SELECTION | SWT.MULTI | SWT.BORDER);
		viewer = new TableViewer(table);
		viewer.setContentProvider(new ArrayContentProvider());
		viewer.setLabelProvider(new ClassPathLabelProvider());

		btnAdd = new Button(composite, SWT.PUSH);
		btnAdd.setText("Add");
		btnAddExternal = new Button(composite, SWT.PUSH);
		btnAddExternal.setText("Add External");
		btnRemove = new Button(composite, SWT.PUSH);
		btnRemove.setText("Remove");

		viewer.setInput(paths);
		update();

		// Listeners
		ViewerDropAdapter dropAdapter = new ViewerDropAdapter(viewer) {
            @Override
            public void dragEnter(DropTargetEvent event) {
                super.dragEnter(event);
                event.detail = DND.DROP_COPY;
            }

            @Override
            public boolean validateDrop(Object target, int operation, TransferData transferType) {
                return true;
            }

            @Override
            public boolean performDrop(Object data) {
                if(data instanceof String[]) {
                    String[] newPaths = (String[]) data;
                    if (newPaths != null) {
                        List<IPath> added = new ArrayList<IPath>(newPaths.length);
                        for (String path : newPaths) {
                            added.add(new Path(path));
                        }

                        if (!added.isEmpty()) {
                            addToPaths(added);
                            viewer.add(added.toArray(new IPath[added.size()]));

                            update();
                        }
                    }
                }

                return true;
            }
        };
        dropAdapter.setFeedbackEnabled(false);
        dropAdapter.setSelectionFeedbackEnabled(false);
        viewer.addDropSupport(DND.DROP_COPY | DND.DROP_MOVE, new Transfer[] { FileTransfer.getInstance() }, dropAdapter);
		viewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(final SelectionChangedEvent event) {
				update();
			}
		});
		btnAdd.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				//IResource newFile = ResourcesPlugin.getWorkspace().getRoot();
				//if(newFile != null) {
					ElementTreeSelectionDialog dialog = new ElementTreeSelectionDialog(getShell(), new WorkbenchLabelProvider(), new WorkbenchContentProvider());
					dialog.setValidator(new ISelectionStatusValidator() {
						public IStatus validate(Object[] selection) {
							if (selection.length > 0 && selection[0] instanceof IFile) {
								return new Status(IStatus.OK, Plugin.PLUGIN_ID, IStatus.OK, "", null); //$NON-NLS-1$
							}
							return new Status(IStatus.ERROR, Plugin.PLUGIN_ID, IStatus.ERROR, "", null); //$NON-NLS-1$
						}
					});
					dialog.setAllowMultiple(true);
					dialog.setTitle("JAR File Selection");
					dialog.setMessage("Select one or more JAR files.");
					dialog.addFilter(new FileExtensionFilter("jar")); //$NON-NLS-1$
					dialog.setInput(ResourcesPlugin.getWorkspace());

					if(dialog.open() == Window.OK) {
						Object[] files = dialog.getResult();
						List<IPath> added = new ArrayList<IPath>(files.length);
						for (Object file : files) {
							added.add(((IResource) file).getFullPath().makeRelative());
						}
						if(!added.isEmpty()) {
						    addToPaths(added);
							viewer.add(added.toArray(new IPath[added.size()]));
						}
					}
				//}
				update();
			}
		});
		btnAddExternal.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				FileDialog dialog = new FileDialog(getShell(), SWT.OPEN | SWT.MULTI);
				dialog.setFilterExtensions(new String[] {"*.jar"}); //$NON-NLS-1$
				String res = dialog.open();
				if (res != null) {
					IPath filterPath = new Path(dialog.getFilterPath());

					String[] fileNames = dialog.getFileNames();
					List<IPath> added = new ArrayList<IPath>(fileNames.length);
					for (String fileName : fileNames) {
						added.add(filterPath.append(fileName));
					}
					if(!added.isEmpty()) {
					    addToPaths(added);
						viewer.add(added.toArray(new IPath[added.size()]));
					}
				}
				update();
			}
		});
		btnRemove.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
			    removeFromPaths(((IStructuredSelection) viewer.getSelection()).toList());
				viewer.remove(((IStructuredSelection) viewer.getSelection()).toArray());
				update();
			}
		});

		// Layout
		composite.setLayout(new GridLayout(2, false));
		table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 3));
		btnAdd.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
		btnAddExternal.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
		btnRemove.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
		lblHint.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false, 2, 1));

		setControl(composite);
	}

    private void addToPaths(List<IPath> added) {
	    List<IPath> oldPaths = new ArrayList<IPath>(paths.size());
	    oldPaths.addAll(paths);

	    paths.addAll(added);
	    propertySupport.firePropertyChange(PROP_PATHS, oldPaths, paths);
    }

    private void removeFromPaths(List<IPath> removed) {
        List<IPath> oldPaths = new ArrayList<IPath>(paths.size());
        oldPaths.addAll(paths);

        paths.removeAll(removed);
        propertySupport.firePropertyChange(PROP_PATHS, oldPaths, paths);
    }

    @Override
	public boolean isPageComplete() {
		return !paths.isEmpty();
	}

	private void update() {
		btnRemove.setEnabled(!viewer.getSelection().isEmpty());
		getContainer().updateButtons();
		getContainer().updateMessage();
	}

	public Collection<IPath> getPaths() {
		return paths;
	}

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        propertySupport.addPropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        propertySupport.removePropertyChangeListener(listener);
    }

    public void addPropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        propertySupport.addPropertyChangeListener(propertyName, listener);
    }

    public void removePropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        propertySupport.removePropertyChangeListener(propertyName, listener);
    }


}
