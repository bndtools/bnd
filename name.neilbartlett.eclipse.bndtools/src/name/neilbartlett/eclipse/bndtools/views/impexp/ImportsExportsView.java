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
package name.neilbartlett.eclipse.bndtools.views.impexp;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;

import name.neilbartlett.eclipse.bndtools.Plugin;
import name.neilbartlett.eclipse.bndtools.editor.model.HeaderClause;
import name.neilbartlett.eclipse.bndtools.utils.PartAdapter;
import name.neilbartlett.eclipse.bndtools.utils.Predicate;
import name.neilbartlett.eclipse.bndtools.utils.SWTConcurrencyUtil;
import name.neilbartlett.eclipse.bndtools.utils.SelectionUtils;
import name.neilbartlett.eclipse.bndtools.views.impexp.ImportsExportsTreeContentProvider.ImportUsedByClass;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.viewers.IOpenListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.OpenEvent;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.DragSourceListener;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IPartListener;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.ide.ResourceUtil;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.plugin.AbstractUIPlugin;

public class ImportsExportsView extends ViewPart implements ISelectionListener, IResourceChangeListener {
	
	public static String VIEW_ID = "name.neilbartlett.eclipse.bndtools.impExpView";

	private Display display = null;
	private Tree tree = null;
	private TreeViewer viewer;
	private ViewerFilter hideSelfImportsFilter;
	
	private IFile[] selectedFiles;
	private Job analysisJob = null;
	
	private IPartListener partAdapter = new PartAdapter() {
		public void partActivated(IWorkbenchPart part) {
			if(part instanceof IEditorPart) {
				IEditorInput editorInput = ((IEditorPart) part).getEditorInput();
				IFile file = ResourceUtil.getFile(editorInput);
				if(file != null && file.getName().endsWith(".bnd")) {
					selectedFiles = new IFile[] { file };
					executeAnalysis();
				}
			}
		}
	};

	
	@Override
	public void createPartControl(Composite parent) {
		this.display = parent.getDisplay();
		
		tree = new Tree(parent, SWT.FULL_SELECTION | SWT.MULTI);
		tree.setHeaderVisible(true);
		tree.setLinesVisible(true);
		
		TreeColumn col;
		col = new TreeColumn(tree, SWT.NONE);
		col.setText("Package");
		col.setWidth(400);
		
		col = new TreeColumn(tree, SWT.NONE);
		col.setText("Attribs");
		col.setWidth(100);
		
		viewer = new TreeViewer(tree);
		viewer.setContentProvider(new ImportsExportsTreeContentProvider());
		viewer.setSorter(new ImportsAndExportsViewerSorter());
		viewer.setLabelProvider(new ImportsExportsTreeLabelProvider());
		viewer.setAutoExpandLevel(2);
		
		hideSelfImportsFilter = new ViewerFilter() {
			public boolean select(Viewer viewer, Object parentElement, Object element) {
				if(element instanceof ImportPackage) {
					return !((ImportPackage) element).isSelfImport();
				}
				return true;
			}
		};
		viewer.setFilters(new ViewerFilter[] { hideSelfImportsFilter });
		
		viewer.addDragSupport(DND.DROP_MOVE | DND.DROP_COPY, new Transfer[] { TextTransfer.getInstance() }, new DragSourceListener() {
			public void dragStart(DragSourceEvent event) {
			}
			public void dragSetData(DragSourceEvent event) {
				if (TextTransfer.getInstance().isSupportedType(event.dataType)) {
					StringBuilder builder = new StringBuilder();
					Iterator<?> iterator = ((IStructuredSelection) viewer.getSelection()).iterator();
					while(iterator.hasNext()) {
						Object item = iterator.next();
						if(item instanceof HeaderClause) {
							HeaderClause clause = (HeaderClause) item;
							builder.append(clause.getName());
							if(iterator.hasNext()) {
								builder.append(",\n");
							}
						}
					}
					event.data = builder.toString();
				}
			}
			public void dragFinished(DragSourceEvent event) {
			}
		});
		
		viewer.addOpenListener(new IOpenListener() {
			public void open(OpenEvent event) {
				IStructuredSelection selection = (IStructuredSelection) event.getSelection();
				for(Iterator<?> iter = selection.iterator(); iter.hasNext(); ) {
					Object item = iter.next();
					if(item instanceof ImportUsedByClass) {
						ImportUsedByClass importUsedBy = (ImportUsedByClass) item;
						String className = importUsedBy.clazz.getFQN();
						IType type = null;
						if(selectedFiles != null) {
							for (IFile selectedFile : selectedFiles) {
								IJavaProject javaProject = JavaCore.create(selectedFile.getProject());
								try {
									type = javaProject.findType(className);
									if(type != null)
										break;
								} catch (JavaModelException e) {
									ErrorDialog.openError(getSite().getShell(), "Error", "", new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, MessageFormat.format("Error opening Java class '{0}'.", className), e));
								}
							}
						}
						try {
							if(type != null)
								JavaUI.openInEditor(type, true, true);
						} catch (PartInitException e) {
							ErrorDialog.openError(getSite().getShell(), "Error", "", new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, MessageFormat.format("Error opening Java editor for class '{0}'.", className), e));
						} catch (JavaModelException e) {
							ErrorDialog.openError(getSite().getShell(), "Error", "", new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, MessageFormat.format("Error opening Java class '{0}'.", className), e));
						}
					}
				}
			}
		});
		
		fillActionBars();
		
		getSite().getPage().addPostSelectionListener(this);
		getSite().getPage().addPartListener(partAdapter);
		ResourcesPlugin.getWorkspace().addResourceChangeListener(this, IResourceChangeEvent.POST_CHANGE);

		// Current selection & part
		IWorkbenchPart activePart = getSite().getPage().getActivePart();
		ISelection activeSelection = getSite().getWorkbenchWindow().getSelectionService().getSelection();
		selectionChanged(activePart, activeSelection);
	}

	void fillActionBars() {
		IAction toggleShowSelfImports = new Action("showSelfImports", IAction.AS_CHECK_BOX) {
			@Override
			public void runWithEvent(Event event) {
				if(isChecked()) {
					viewer.removeFilter(hideSelfImportsFilter);
				} else {
					viewer.addFilter(hideSelfImportsFilter);
				}
			}
		};
		toggleShowSelfImports.setChecked(false);
		toggleShowSelfImports.setImageDescriptor(AbstractUIPlugin.imageDescriptorFromPlugin(Plugin.PLUGIN_ID, "/icons/package_folder_impexp.gif"));
		toggleShowSelfImports.setToolTipText("Show self-imported packages");
		
		IToolBarManager toolBarManager = getViewSite().getActionBars().getToolBarManager();
		toolBarManager.add(toggleShowSelfImports);
	}

	@Override
	public void setFocus() {
	}
	
	@Override
	public void dispose() {
		getSite().getPage().removeSelectionListener(this);
		ResourcesPlugin.getWorkspace().removeResourceChangeListener(this);
		getSite().getPage().removePartListener(partAdapter);
		super.dispose();
	};
	
	public void setInput(IFile[] sourceFiles, Collection<? extends ImportPackage> imports, Collection<? extends ExportPackage> exports) {
		selectedFiles = sourceFiles;
		if(tree != null && !tree.isDisposed()) {
			viewer.setInput(new ImportsAndExports(imports, exports));
			
			String label;
			if(sourceFiles != null) {
				StringBuilder builder = new StringBuilder();
				for(int i = 0; i < sourceFiles.length; i++) {
					if(i > 0) builder.append(", ");
					builder.append(sourceFiles[i].getFullPath().toString());
				}
				label = builder.toString();
			} else {
				label = "<no input>";
			}
			setContentDescription(label);
		}
	}
	public void selectionChanged(IWorkbenchPart part, ISelection selection) {
		if(selection instanceof IStructuredSelection) {
			Collection<IFile> fileList = SelectionUtils.getSelectionMembers(selection, IFile.class, new Predicate<IFile>() {
				public boolean select(IFile item) {
					return item.getName().endsWith(".bnd") || item.getName().endsWith(".jar");
				}
			});
			IFile[] files = fileList.toArray(new IFile[fileList.size()]);
			if(!Arrays.equals(files, selectedFiles)) {
				this.selectedFiles = files;
				executeAnalysis();
			}
		}
	}
	void executeAnalysis() {
		if(analysisJob != null) {
			analysisJob.cancel();
		}
		if(selectedFiles != null) {
			analysisJob = new AnalyseImportsJob("importExportAnalysis", selectedFiles, getSite().getPage());
			analysisJob.setSystem(true);
			analysisJob.schedule(500);
			analysisJob.addJobChangeListener(new JobChangeAdapter() {
				@Override
				public void done(IJobChangeEvent event) {
					analysisJob = null;
				}
			});
		} else {
			SWTConcurrencyUtil.execForDisplay(display, new Runnable() {;
				public void run() {
					setInput(null, Collections.<ImportPackage>emptyList(), Collections.<ExportPackage>emptyList());
				}
			});
		}
	}
	public void resourceChanged(IResourceChangeEvent event) {
		if(selectedFiles != null) {
			for (IFile file : selectedFiles) {
				if(event.getDelta().findMember(file.getFullPath()) != null) {
					executeAnalysis();
					break;
				}
			}
		}
	}
}
