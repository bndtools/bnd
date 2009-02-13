/*******************************************************************************
 * Copyright (c) 2009 Neil Bartlett.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Neil Bartlett - initial API and implementation
 ******************************************************************************/
package name.neilbartlett.eclipse.bndtools.wizards;

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import name.neilbartlett.eclipse.bndtools.internal.libs.MutableRefCell;
import name.neilbartlett.eclipse.bndtools.internal.libs.RefCell;
import name.neilbartlett.eclipse.bndtools.internal.pkgselection.IPackageLister;
import name.neilbartlett.eclipse.bndtools.internal.pkgselection.PackageNameLabelProvider;
import name.neilbartlett.eclipse.bndtools.internal.pkgselection.PackageSelectionDialog;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;

public class PackageListGroup {
	
	private final Set<String> packages;
	private final MutableRefCell<Boolean> allPackagesRef;
	private final RefCell<IPackageLister> packageListerRef;
	private final boolean includeNonSource;
	private final Set<String> excludes;
	
	private Table table;
	private Button addButton;
	private Button removeButton;
	private TableViewer viewer;
	private Button btnAllPackages = null;

	public PackageListGroup(Set<String> packages, MutableRefCell<Boolean> allPackagesRef, RefCell<IPackageLister> packageListerRef, boolean includeNonSource, Set<String> excludes) {
		this.packages = packages;
		this.allPackagesRef = allPackagesRef;
		this.packageListerRef = packageListerRef;
		this.includeNonSource = includeNonSource;
		this.excludes = excludes;
	}

	public void createControl(Composite composite) {
		if(allPackagesRef != null) {
			btnAllPackages = new Button(composite, SWT.CHECK);
			btnAllPackages.setText("Use all available packages");
			btnAllPackages.setSelection(allPackagesRef.getValue());
			btnAllPackages.setLayoutData(new GridData(SWT.LEFT, SWT.NONE, true, false, 2, 1));
		}
		
		table = new Table(composite, SWT.FULL_SELECTION | SWT.BORDER | SWT.MULTI);
		viewer = new TableViewer(table);
		viewer.setContentProvider(new ArrayContentProvider());
		viewer.setLabelProvider(new PackageNameLabelProvider());
		viewer.setInput(packages);
		
		addButton = new Button(composite, SWT.PUSH);
		addButton.setText("Add");
		
		removeButton = new Button(composite, SWT.PUSH);
		removeButton.setText("Remove");
		

		// Initial values
		updateEnablement();
		
		// Layout
		composite.setLayout(new GridLayout(2, false));
		table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 3));
		addButton.setLayoutData(new GridData(SWT.FILL, SWT.NONE, false, false));
		removeButton.setLayoutData(new GridData(SWT.FILL, SWT.NONE, false, false));
		
		// Listeners
		if(btnAllPackages != null) {
			btnAllPackages.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					allPackagesRef.setValue(btnAllPackages.getSelection());
					updateEnablement();
				}
			});
		}
		addButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				Set<String> excludeFromAdd = packages;
				if(excludes != null && excludes.size() > 0) {
					excludeFromAdd = new HashSet<String>();
					excludeFromAdd.addAll(packages);
					excludeFromAdd.addAll(excludes);
				}
				PackageSelectionDialog dialog = new PackageSelectionDialog(addButton.getShell(), packageListerRef.getValue(), includeNonSource, excludeFromAdd);
				dialog.setMultipleSelection(true);
				
				if(dialog.open() == Window.OK) {
					Object[] results = dialog.getResult();
					List<String> added = new LinkedList<String>();
					
					for (Object result : results) {
						String pkg = (String) result;
						if(packages.add(pkg)) {
							added.add(pkg);
						}
					}
					
					viewer.add(added.toArray(new String[added.size()]));
				}
			}
		});
		
		removeButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				Iterator<?> iterator = ((IStructuredSelection) viewer.getSelection()).iterator();
				List<String> removed = new LinkedList<String>();
				
				while(iterator.hasNext()) {
					String export = (String) iterator.next();
					if(packages.remove(export)) {
						removed.add(export);
					}
				}
				
				viewer.remove(removed.toArray(new String[removed.size()]));
			}
		});

		viewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				updateEnablement();
			}
		});
	}

	private void updateEnablement() {
		boolean allPackages = allPackagesRef == null ? false : allPackagesRef.getValue();
		table.setEnabled(!allPackages);
		addButton.setEnabled(!allPackages);
		removeButton.setEnabled(!allPackages && !viewer.getSelection().isEmpty());
	}

}
