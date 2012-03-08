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
package bndtools.editor.components;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.IOpenListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.OpenEvent;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerDropAdapter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.dnd.TransferData;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.IMessageManager;
import org.eclipse.ui.forms.SectionPart;
import org.eclipse.ui.forms.editor.IFormPage;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.ide.ResourceUtil;
import org.eclipse.ui.part.ResourceTransfer;

import aQute.lib.osgi.Constants;
import aQute.libg.header.Attrs;
import bndtools.Plugin;
import bndtools.editor.model.BndEditModel;
import bndtools.model.clauses.ExportedPackage;
import bndtools.model.clauses.ServiceComponent;
import bndtools.utils.PathUtils;

public class ComponentListPart extends SectionPart implements PropertyChangeListener {

	private static final String XML_SUFFIX = ".xml"; //$NON-NLS-1$
	private List<String> componentNames;
	private Map<String, ServiceComponent> componentMap;

	private IManagedForm managedForm;
	private Table table;
	private TableViewer viewer;
	private BndEditModel model;

	public ComponentListPart(Composite parent, FormToolkit toolkit, int style) {
		super(parent, toolkit, style);
		createSection(getSection(), toolkit);
	}
	void createSection(Section section, FormToolkit toolkit) {
		section.setText(Messages.ComponentListPart_listSectionTitle);

		Composite composite = toolkit.createComposite(section);
		section.setClient(composite);

		table = toolkit.createTable(composite, SWT.MULTI | SWT.FULL_SELECTION | SWT.BORDER);

		viewer = new TableViewer(table);
		viewer.setUseHashlookup(true);
		viewer.setContentProvider(new ArrayContentProvider());
		viewer.setLabelProvider(new ServiceComponentLabelProvider());

		final Button btnAdd = toolkit.createButton(composite, Messages.ComponentListPart_addButton, SWT.PUSH);
		final Button btnRemove = toolkit.createButton(composite, Messages.ComponentListPart_RemoveButton, SWT.PUSH);

		toolkit.paintBordersFor(section);

		// Listeners
		viewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				IStructuredSelection selection = (IStructuredSelection) event.getSelection();
				ArrayList<ServiceComponent> selectedComponents = new ArrayList<ServiceComponent>(selection.size());

				@SuppressWarnings("rawtypes")
				Iterator iterator = selection.iterator();
				while(iterator.hasNext()) {
					String name = (String) iterator.next();
					ServiceComponent component = componentMap.get(name);
					if(component != null)
						selectedComponents.add(component);
				}

				managedForm.fireSelectionChanged(ComponentListPart.this, new StructuredSelection(selectedComponents));
				btnRemove.setEnabled(!selection.isEmpty());
			}
		});
		viewer.addOpenListener(new IOpenListener() {
			public void open(OpenEvent event) {
				String name = (String) ((IStructuredSelection) event.getSelection()).getFirstElement();
				if(name != null) {
					doOpenComponent(name);
				}
			}
		});
		viewer.addDropSupport(DND.DROP_COPY | DND.DROP_MOVE, new Transfer[] { ResourceTransfer.getInstance() }, new ComponentListDropAdapter(viewer));
		btnAdd.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				try {
                    doAdd();
                } catch (Exception x) {
                    Plugin.logError("Error adding component", x);
                }
			}
		});
		btnRemove.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				doRemove();
			}
		});

		// Layout
		GridData gd;
		section.setLayoutData(new GridData(GridData.FILL_BOTH));
		composite.setLayout(new GridLayout(2, false));
		gd = new GridData(SWT.FILL, SWT.FILL, true, true, 1, 3);
		gd.widthHint = 250;
		table.setLayoutData(gd);
		btnAdd.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
		btnRemove.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
	}
	void doAdd() throws Exception {
		String name;
		ServiceComponent component;
		int i = 0;
		while(true) {
		    if (i == 0)
		        name = ""; //$NON-NLS-1$
		    else
		        name = String.format("%d", i); //$NON-NLS-1$
			if(!componentMap.containsKey(name)) {
				component = new ServiceComponent(name, new Attrs());
				componentMap.put(name, component);
				componentNames.add(name);
				break;
			}
			if (i > 100) throw new Exception("Unable to find a free component name after 100 tries!");
		}
		viewer.add(name);
		viewer.setSelection(new StructuredSelection(name), true);
		checkComponentPackagesIncluded();
		markDirty();
	}
	void doRemove() {
		Iterator<?> iter = ((IStructuredSelection) viewer.getSelection()).iterator();
		while(iter.hasNext()) {
			Object item = iter.next();
			componentMap.remove(item);
			componentNames.remove(item);
			viewer.remove(item);
		}
		checkComponentPackagesIncluded();
		markDirty();
	}
	void doOpenComponent(String componentName) {
		ServiceComponent component = componentMap.get(componentName);
		if(component == null)
			return;

		if(component.isPath()) {
			IFormPage page = (IFormPage) getManagedForm().getContainer();
			IResource resource = ResourceUtil.getResource(page.getEditorInput());
			IProject project = resource.getProject();
			IResource member = project.findMember(component.getName());
			if(member != null && member.getType() == IResource.FILE) {
				try {
					IDE.openEditor(page.getEditorSite().getPage(), (IFile) member);
				} catch (PartInitException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		} else if(!component.getName().endsWith("*")) { //$NON-NLS-1$
			try {
				IType type = getJavaProject().findType(component.getName());
				if(type != null) {
					JavaUI.openInEditor(type, true, true);
				}
			} catch (JavaModelException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (PartInitException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	void checkComponentPackagesIncluded() {
		IMessageManager msgs = getManagedForm().getMessageManager();
		msgs.setDecorationPosition(SWT.TOP | SWT.RIGHT);
		msgs.removeMessages();

		int index = 0;

		if(componentMap != null) {
			for (Entry<String, ServiceComponent> entry : componentMap.entrySet()) {
				ServiceComponent component = entry.getValue();
				if(component.getName().length() > 0 && !component.isPath()) {
					String classOrWildcard = entry.getKey();
					if(classOrWildcard != null && classOrWildcard.length() > 0) {
						int dotIndex = classOrWildcard.lastIndexOf('.');
						if(dotIndex == -1) {
							msgs.addMessage("_comp_default_pkg" + index, Messages.ComponentListPart_warningDefaultPkg, null, IMessageProvider.WARNING); //$NON-NLS-1$
						} else {
							final String packageName = classOrWildcard.substring(0, dotIndex);
							final BndEditModel model = (BndEditModel) getManagedForm().getInput();
							if(!model.isIncludedPackage(packageName)) {
                                String message = MessageFormat.format(Messages.ComponentListPart_warningPkgNotIncluded, packageName);
								IAction[] fixes = new Action[] {
									new Action(MessageFormat.format(Messages.ComponentListPart_fixAddToPrivatePkgs, packageName)) {
										@Override
                                        public void run() {
											model.addPrivatePackage(packageName);
											markDirty();
										}
									},
									new Action(MessageFormat.format(Messages.ComponentListPart_fixAddToExportedPkgs, packageName)) {
										@Override
                                        public void run() {
											model.addExportedPackage(new ExportedPackage(packageName, null));
											markDirty();
										}
									}
								};
								msgs.addMessage("_comp_nonincluded_pkg" + index, message, fixes, IMessageProvider.WARNING); //$NON-NLS-1$
							}
						}
					}
				}
				index++;
			}
		}
	}
	@Override
	public void initialize(IManagedForm form) {
		super.initialize(form);
		this.managedForm = form;
		this.model = (BndEditModel) form.getInput();
		this.model.addPropertyChangeListener(this);
	}
	@Override
	public void dispose() {
		super.dispose();
		this.model.removePropertyChangeListener(this);
	}
	@Override
	public void refresh() {
		super.refresh();

		// Deep-copy the model
		Collection<ServiceComponent> componentList = model.getServiceComponents();
		if(componentList != null) {
			componentNames = new ArrayList<String>(componentList.size());
			componentMap = new HashMap<String, ServiceComponent>(componentList.size());
			for (ServiceComponent component : componentList) {
				componentNames.add(component.getName());
				componentMap.put(component.getName(), component.clone());
			}
		} else {
			componentNames = new LinkedList<String>();
			componentMap = new HashMap<String, ServiceComponent>();
		}

		viewer.setInput(componentNames);
		checkComponentPackagesIncluded();
	}
	@Override
	public void commit(boolean onSave) {
		try {
			model.removePropertyChangeListener(this);
			model.setServiceComponents(componentMap.isEmpty() ? null : new ArrayList<ServiceComponent>(componentMap.values()));
		} finally {
		    model.addPropertyChangeListener(this);
			super.commit(onSave);
		}
	}
	public void propertyChange(PropertyChangeEvent evt) {
		if(Constants.SERVICE_COMPONENT.equals(evt.getPropertyName())) {
			// Refresh content if ServiceComponents changed
			IFormPage page = (IFormPage) managedForm.getContainer();
			if(page.isActive())
				refresh();
			else
				markStale();
		} else if(Constants.PRIVATE_PACKAGE.equals(evt.getPropertyName()) || Constants.EXPORT_PACKAGE.equals(evt.getPropertyName())) {
			// Content of bundle has changed, re-validate component's for included packages
			checkComponentPackagesIncluded();
		}
	}
	public void updateLabel(String oldName, String newName) {
		int index = componentNames.indexOf(oldName);

		if(index > -1) {
			componentNames.remove(index);
			ServiceComponent component = componentMap.remove(oldName);
			if(component != null) {
				componentNames.add(index, newName);
				componentMap.put(newName, component);
				viewer.replace(newName, index);
			} else {
				viewer.remove(oldName);
			}
		}
	}
	IJavaProject getJavaProject() {
		IFormPage page = (IFormPage) getManagedForm().getContainer();
		IEditorInput input = page.getEditorInput();

		IFile file = ResourceUtil.getFile(input);
		if(file != null) {
			return JavaCore.create(file.getProject());
		} else {
			return null;
		}
	}

	private class ComponentListDropAdapter extends ViewerDropAdapter {

		protected ComponentListDropAdapter(Viewer viewer) {
			super(viewer);
		}

		@Override
		public void dragEnter(DropTargetEvent event) {
			event.detail = DND.DROP_COPY;
			super.dragEnter(event);
		}
		@Override
		public boolean validateDrop(Object target, int operation, TransferData transferType) {
			return ResourceTransfer.getInstance().isSupportedType(transferType);
		}
		@Override
		public boolean performDrop(Object data) {
			Object target = getCurrentTarget();
			int loc = getCurrentLocation();

			int insertionIndex = -1;
			if(target != null) {
				insertionIndex = componentNames.indexOf(target);
				if(insertionIndex > -1 && loc == LOCATION_ON || loc == LOCATION_AFTER)
					insertionIndex ++;
			}

			List<String> addedNames = new ArrayList<String>();
			Map<String, ServiceComponent> addedMap = new HashMap<String, ServiceComponent>();
			if(data instanceof IResource[]) {
				IResource[] resources = (IResource[]) data;
				for (IResource resource : resources) {
					IJavaElement javaElement = JavaCore.create(resource);
					if(javaElement != null) {
						try {
							if(javaElement instanceof IType) {
								IType type = (IType) javaElement;
								if(type.isClass() && Flags.isPublic(type.getFlags())) {
									String compName = type.getPackageFragment().getElementName() + "." + type.getElementName(); //$NON-NLS-1$
									ServiceComponent component = new ServiceComponent(compName, new Attrs());
									addedNames.add(compName);
									addedMap.put(compName, component);
								}
							} else if(javaElement instanceof ICompilationUnit) {
								IType[] allTypes = ((ICompilationUnit) javaElement).getAllTypes();
								for (IType type : allTypes) {
									if(type.isClass() && Flags.isPublic(type.getFlags())) {
										String compName = type.getPackageFragment().getElementName() + "." + type.getElementName(); //$NON-NLS-1$
										ServiceComponent component = new ServiceComponent(compName, new Attrs());
										addedNames.add(compName);
										addedMap.put(compName, component);
									}
								}
							}
						} catch (JavaModelException e) {
							Plugin.log(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, Messages.ComponentListPart_errorJavaType, e));
						}
					} else if(resource.getName().endsWith(XML_SUFFIX)) {
						IFormPage formPage = (IFormPage) getManagedForm().getContainer();
						IFile bndFile = ResourceUtil.getFile(formPage.getEditorInput());

						IPath relativePath = PathUtils.makeRelativeTo(resource.getFullPath(), bndFile.getFullPath());

						String compName = relativePath.toString();
						ServiceComponent component = new ServiceComponent(compName, new Attrs());
						addedNames.add(compName);
						addedMap.put(compName, component);
					}
				}
			}

			if(!addedNames.isEmpty()) {
				componentMap.putAll(addedMap);
				if(insertionIndex == -1 || insertionIndex == componentNames.size()) {
					componentNames.addAll(addedNames);
					viewer.add(addedNames.toArray(new String[addedNames.size()]));
				} else {
					componentNames.addAll(insertionIndex, addedNames);
					viewer.refresh();
				}
			}
			viewer.setSelection(new StructuredSelection(addedNames), true);
			checkComponentPackagesIncluded();
			markDirty();
			return true;
		}
	}

	void setSelectedComponent(ServiceComponent component) {
		viewer.setSelection(new StructuredSelection(component.getName()));
	}
}
