/*******************************************************************************
 * Copyright (c) 2010 Neil Bartlett.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Neil Bartlett - initial API and implementation
 ******************************************************************************/
package bndtools.editor;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;


import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.ElementTreeSelectionDialog;
import org.eclipse.ui.dialogs.ISelectionStatusValidator;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.SectionPart;
import org.eclipse.ui.forms.editor.IFormPage;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.ide.ResourceUtil;
import org.eclipse.ui.model.WorkbenchContentProvider;
import org.eclipse.ui.model.WorkbenchLabelProvider;
import org.eclipse.ui.plugin.AbstractUIPlugin;

import bndtools.Plugin;
import bndtools.editor.model.BndEditModel;
import bndtools.utils.ClassFolderFilter;
import bndtools.utils.ClassPathLabelProvider;
import bndtools.utils.FileExtensionFilter;

import aQute.lib.osgi.Constants;

public class ClassPathPart extends SectionPart implements PropertyChangeListener {
	
	private final Image imgAddJar = AbstractUIPlugin.imageDescriptorFromPlugin(Plugin.PLUGIN_ID, "/icons/jar_add.gif").createImage();
	private final Image imgAddFolder = AbstractUIPlugin.imageDescriptorFromPlugin(Plugin.PLUGIN_ID, "/icons/folder_add.gif").createImage();

	private List<IPath> classPath;
	private boolean refreshing;
	
	private TableViewer viewer;
	private BndEditModel model;

	public ClassPathPart(Composite parent, FormToolkit toolkit, int style) {
		super(parent, toolkit, style);
		createSection(getSection(), toolkit);
	}

	private void createSection(Section section, FormToolkit toolkit) {
		section.setText("Classpath");
		section.setDescription("Define paths for classes that may appear in the bundle. If empty, the Eclipse project classpath will be used.");
		
		// Section toolbar buttons
		ToolBar toolbar = new ToolBar(section, SWT.FLAT);
		section.setTextClient(toolbar);
		final ToolItem addJarItem = new ToolItem(toolbar, SWT.PUSH);
		addJarItem.setImage(imgAddJar);
		addJarItem.setToolTipText("Add JAR");

		final ToolItem addFolderItem = new ToolItem(toolbar, SWT.PUSH);
		addFolderItem.setImage(imgAddFolder);
		addFolderItem.setToolTipText("Add Class Folder");
		
		final ToolItem removeItem = new ToolItem(toolbar, SWT.PUSH);
		removeItem.setImage(PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_TOOL_DELETE));
		removeItem.setDisabledImage(PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_TOOL_DELETE_DISABLED));
		removeItem.setToolTipText("Remove");
		removeItem.setEnabled(false);
		
		// Contents
		Composite composite = toolkit.createComposite(section, SWT.NONE);
		section.setClient(composite);
		Table table = toolkit.createTable(composite, SWT.FULL_SELECTION | SWT.MULTI);
		
		viewer = new TableViewer(table);
		viewer.setContentProvider(new ArrayContentProvider());
		viewer.setLabelProvider(new ClassPathLabelProvider());
		
		// Actions
		viewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				removeItem.setEnabled(!viewer.getSelection().isEmpty());
			}
		});
		addJarItem.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				doAddJar();
			}
		});
		addFolderItem.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				doAddFolder();
			}
		});
		removeItem.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				doRemove();
			}
		});
		
		// Layout
		GridLayout layout = new GridLayout(1, false);
		layout.horizontalSpacing = 0; layout.verticalSpacing = 0;
		layout.marginHeight = 0; layout.marginWidth = 0;
		composite.setLayout(layout);
		
		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, false);
		gd.heightHint = 75;
		table.setLayoutData(gd);
	}
	

	private void doAddJar() {
		ElementTreeSelectionDialog dialog = new ElementTreeSelectionDialog(getSection().getShell(), new WorkbenchLabelProvider(), new WorkbenchContentProvider());
		dialog.setAllowMultiple(true);
		dialog.setTitle("JAR Selection");
		dialog.setMessage("Select JAR Files to add to the Classpath.");
		dialog.addFilter(new FileExtensionFilter("jar")); //$NON-NLS-1$
		
		IResource resource = getInputResource();
		dialog.setInput(resource.getProject());
		
		if(dialog.open() == Window.OK) {
			Object[] files = dialog.getResult();
			List<IPath> added = new ArrayList<IPath>(files.length);
			for (Object file : files) {
				IPath newPath = ((IResource) file).getFullPath().makeRelative();
				// Remove the first segment (project name)
				newPath = newPath.removeFirstSegments(1);
				added.add(newPath);
			}
			if(!added.isEmpty()) {
				classPath.addAll(added);
				viewer.add(added.toArray(new IPath[added.size()]));
				markDirty();
			}
		}
	}

	private void doAddFolder() {
		ElementTreeSelectionDialog dialog = new ElementTreeSelectionDialog(getSection().getShell(), new WorkbenchLabelProvider(), new WorkbenchContentProvider());
		dialog.setAllowMultiple(true);
		dialog.setTitle("Class Folder Selection");
		dialog.setMessage("Select Class Folders to add to the Classpath.");
		dialog.addFilter(new ClassFolderFilter());
		
		IResource resource = getInputResource();
		dialog.setInput(resource.getProject());
		
		if(dialog.open() == Window.OK) {
			Object[] folders = dialog.getResult();
			List<IPath> added = new ArrayList<IPath>(folders.length);
			for (Object folder : folders) {
				IPath newPath = ((IResource) folder).getFullPath().makeRelative().addTrailingSeparator();
				// Remove the first segment (project name)
				newPath = newPath.removeFirstSegments(1);
				added.add(newPath);
			}
			if(!added.isEmpty()) {
				classPath.addAll(added);
				viewer.add(added.toArray(new IPath[added.size()]));
				markDirty();
			}
		}
	}
	private void doRemove() {
		IStructuredSelection selection = (IStructuredSelection) viewer.getSelection();
		classPath.removeAll(selection.toList());
		viewer.remove(selection.toArray());
		markDirty();
	}

	private IResource getInputResource() {
		IFormPage formPage = (IFormPage) getManagedForm().getContainer();
		IResource resource = ResourceUtil.getResource(formPage.getEditorInput());
		return resource;
	}
	
	
	@Override
	public void refresh() {
		try {
			this.refreshing = true;
			List<String> temp = model.getClassPath();
			classPath = new ArrayList<IPath>(temp != null ? temp.size() : 5);
			if(temp != null) {
				for (String path : temp) {
					classPath.add(new Path(path));
				}
			}
			viewer.setInput(classPath);
		} finally {
			this.refreshing = false;
		}
		super.refresh();
	}
	
	@Override
	public void commit(boolean onSave) {
		super.commit(onSave);
		
		List<String> strings = new ArrayList<String>(classPath.size());
		for (IPath path : classPath) {
			strings.add(path.toString());
		}
		model.setClassPath(strings);
	}

	public void propertyChange(PropertyChangeEvent evt) {
		if(Constants.CLASSPATH.equals(evt.getPropertyName())) {
			IFormPage page = (IFormPage) getManagedForm().getContainer();
			if(page.isActive()) {
				refresh();
			} else {
				markStale();
			}
		}
	}
	
	@Override
	public void initialize(IManagedForm form) {
		super.initialize(form);
		model = (BndEditModel) form.getInput();
		model.addPropertyChangeListener(Constants.CLASSPATH, this);
	}
	
	@Override
	public void dispose() {
		model.removePropertyChangeListener(Constants.CLASSPATH, this);
		super.dispose();
		imgAddJar.dispose();
		imgAddFolder.dispose();
	}
}
