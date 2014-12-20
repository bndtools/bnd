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
package bndtools.views;

import java.io.File;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
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
import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.viewers.IOpenListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.OpenEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.DragSourceListener;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IPartListener;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.ide.ResourceUtil;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.namespace.HostNamespace;
import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.resource.Capability;

import aQute.bnd.osgi.Clazz;
import bndtools.Plugin;
import bndtools.model.resolution.CapReqMapContentProvider;
import bndtools.model.resolution.CapabilityLabelProvider;
import bndtools.model.resolution.RequirementWrapper;
import bndtools.model.resolution.RequirementWrapperLabelProvider;
import bndtools.tasks.AnalyseBundleResolutionJob;
import bndtools.utils.PartAdapter;
import bndtools.utils.SelectionUtils;

public class ResolutionView extends ViewPart implements ISelectionListener, IResourceChangeListener {

    private static String[] FILTERED_CAPABILITY_NAMESPACES = {
            IdentityNamespace.IDENTITY_NAMESPACE, HostNamespace.HOST_NAMESPACE
    };

    private Display display = null;

    private Tree reqsTree = null;
    private Table capsTable = null;

    private TreeViewer reqsViewer;
    private TableViewer capsViewer;

    private ViewerFilter hideSelfImportsFilter;

    private boolean outOfDate = false;
    private File[] selectedFiles;
    private Job analysisJob;

    private final Set<String> filteredCapabilityNamespaces;

    public ResolutionView() {
        filteredCapabilityNamespaces = new HashSet<String>(Arrays.asList(FILTERED_CAPABILITY_NAMESPACES));
    }

    private final IPartListener partAdapter = new PartAdapter() {
        @Override
        public void partActivated(IWorkbenchPart part) {
            if (part == ResolutionView.this) {
                if (outOfDate) {
                    executeAnalysis();
                }
            } else if (part instanceof IEditorPart) {
                IEditorInput editorInput = ((IEditorPart) part).getEditorInput();
                IFile file = ResourceUtil.getFile(editorInput);
                if (file != null) {
                    if (file.getName().toLowerCase().endsWith(".bnd") || file.getName().toLowerCase().endsWith(".jar")) {
                        selectedFiles = new File[] {
                            file.getLocation().toFile()
                        };

                        if (getSite().getPage().isPartVisible(ResolutionView.this)) {
                            executeAnalysis();
                        } else {
                            outOfDate = true;
                        }
                    }
                }
            }
        }
    };

    @Override
    public void createPartControl(Composite parent) {
        this.display = parent.getDisplay();

        SashForm splitPanel = new SashForm(parent, SWT.HORIZONTAL);
        splitPanel.setLayout(new FillLayout());

        Composite reqsPanel = new Composite(splitPanel, SWT.NONE);
        reqsPanel.setBackground(parent.getBackground());

        GridLayout reqsLayout = new GridLayout(1, false);
        reqsLayout.marginWidth = 0;
        reqsLayout.marginHeight = 0;
        reqsLayout.verticalSpacing = 2;
        reqsPanel.setLayout(reqsLayout);
        new Label(reqsPanel, SWT.NONE).setText("Requirements:");
        reqsTree = new Tree(reqsPanel, SWT.FULL_SELECTION | SWT.MULTI | SWT.BORDER);
        reqsTree.setHeaderVisible(false);
        reqsTree.setLinesVisible(false);
        reqsTree.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        reqsViewer = new TreeViewer(reqsTree);
        reqsViewer.setLabelProvider(new RequirementWrapperLabelProvider(true));
        reqsViewer.setContentProvider(new CapReqMapContentProvider());

        Composite capsPanel = new Composite(splitPanel, SWT.NONE);
        capsPanel.setBackground(parent.getBackground());

        GridLayout capsLayout = new GridLayout(1, false);
        capsLayout.marginWidth = 0;
        capsLayout.marginHeight = 0;
        capsLayout.verticalSpacing = 2;
        capsPanel.setLayout(capsLayout);
        new Label(capsPanel, SWT.NONE).setText("Capabilities:");
        capsTable = new Table(capsPanel, SWT.FULL_SELECTION | SWT.MULTI | SWT.BORDER);
        capsTable.setHeaderVisible(false);
        capsTable.setLinesVisible(false);
        capsTable.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        capsViewer = new TableViewer(capsTable);
        capsViewer.setLabelProvider(new CapabilityLabelProvider(true));
        capsViewer.setContentProvider(new CapReqMapContentProvider());
        capsViewer.setFilters(new ViewerFilter[] {
            new ViewerFilter() {
                @Override
                public boolean select(Viewer viewer, Object parent, Object element) {
                    return !filteredCapabilityNamespaces.contains(((Capability) element).getNamespace());
                }
            }
        });

        hideSelfImportsFilter = new ViewerFilter() {
            @Override
            public boolean select(Viewer viewer, Object parentElement, Object element) {
                if (element instanceof RequirementWrapper) {
                    RequirementWrapper rw = (RequirementWrapper) element;
                    return !rw.resolved;
                }
                return true;
            }
        };
        reqsViewer.setFilters(new ViewerFilter[] {
            hideSelfImportsFilter
        });

        reqsViewer.addDragSupport(DND.DROP_MOVE | DND.DROP_COPY, new Transfer[] {
            LocalSelectionTransfer.getTransfer()
        }, new LocalTransferDragListener(reqsViewer));

        capsViewer.addDragSupport(DND.DROP_MOVE | DND.DROP_COPY, new Transfer[] {
            LocalSelectionTransfer.getTransfer()
        }, new LocalTransferDragListener(capsViewer));

        reqsViewer.addOpenListener(new IOpenListener() {
            @Override
            public void open(OpenEvent event) {
                IStructuredSelection selection = (IStructuredSelection) event.getSelection();
                for (Iterator< ? > iter = selection.iterator(); iter.hasNext();) {
                    Object item = iter.next();
                    if (item instanceof Clazz) {
                        Clazz clazz = (Clazz) item;
                        String className = clazz.getFQN();
                        IType type = null;
                        if (selectedFiles != null) {
                            IWorkspaceRoot wsroot = ResourcesPlugin.getWorkspace().getRoot();
                            for (File selectedFile : selectedFiles) {
                                IFile[] wsfiles = wsroot.findFilesForLocationURI(selectedFile.toURI());
                                for (IFile wsfile : wsfiles) {
                                    IJavaProject javaProject = JavaCore.create(wsfile.getProject());
                                    try {
                                        type = javaProject.findType(className);
                                        if (type != null)
                                            break;
                                    } catch (JavaModelException e) {
                                        ErrorDialog.openError(getSite().getShell(), "Error", "", new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, MessageFormat.format("Error opening Java class '{0}'.", className), e));
                                    }
                                }
                            }
                        }
                        try {
                            if (type != null)
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
                if (isChecked()) {
                    reqsViewer.removeFilter(hideSelfImportsFilter);
                } else {
                    reqsViewer.addFilter(hideSelfImportsFilter);
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
    public void setFocus() {}

    @Override
    public void dispose() {
        getSite().getPage().removeSelectionListener(this);
        ResourcesPlugin.getWorkspace().removeResourceChangeListener(this);
        getSite().getPage().removePartListener(partAdapter);
        super.dispose();
    }

    public void setInput(File[] sourceFiles, Map<String,List<Capability>> capabilities, Map<String,List<RequirementWrapper>> requirements) {
        selectedFiles = sourceFiles;
        if (reqsTree != null && !reqsTree.isDisposed() && capsTable != null && !capsTable.isDisposed()) {
            reqsViewer.setInput(requirements);
            capsViewer.setInput(capabilities);

            String label;
            if (sourceFiles != null) {
                StringBuilder builder = new StringBuilder();
                for (int i = 0; i < sourceFiles.length; i++) {
                    if (i > 0)
                        builder.append(", ");
                    builder.append(sourceFiles[i].getAbsolutePath());
                }
                label = builder.toString();
            } else {
                label = "<no input>";
            }
            setContentDescription(label);
        }
    }

    @Override
    public void selectionChanged(IWorkbenchPart part, ISelection selection) {
        if (selection == null || selection.isEmpty())
            return;

        Collection<File> fileList = getFilesFromSelection(selection);

        // Filter out non-bundle files/dirs
        for (Iterator<File> iter = fileList.iterator(); iter.hasNext();) {
            File file = iter.next();
            if (file.isDirectory()) {
                File manifestFile = new File(file, "META-INF/MANIFEST.MF");
                if (!manifestFile.isFile())
                    iter.remove();
            } else {
                String fileName = file.getName().toLowerCase();
                if (!fileName.endsWith(".bnd") && !fileName.endsWith(".jar"))
                    iter.remove();
            }
        }

        if (fileList.isEmpty())
            return;

        File[] files = fileList.toArray(new File[fileList.size()]);
        if (!Arrays.equals(files, selectedFiles)) {
            this.selectedFiles = files;

            if (getSite().getPage().isPartVisible(this)) {
                executeAnalysis();
            } else {
                outOfDate = true;
            }
        }
    }

    private static Collection<File> getFilesFromSelection(ISelection selection) {
        if (selection.isEmpty() || !(selection instanceof IStructuredSelection)) {
            return Collections.emptyList();
        }

        IStructuredSelection structSel = (IStructuredSelection) selection;
        Collection<File> result = new ArrayList<File>();
        Iterator< ? > iter = structSel.iterator();
        while (iter.hasNext()) {
            Object element = iter.next();
            File file = SelectionUtils.adaptObject(element, File.class);
            if (file != null) {
                result.add(file);
            } else {
                IResource resource = SelectionUtils.adaptObject(element, IResource.class);
                if (resource != null) {
                    IPath location = resource.getLocation();
                    if (location != null)
                        result.add(location.toFile());
                }
            }
        }
        return result;
    }

    void executeAnalysis() {
        outOfDate = false;
        synchronized (this) {
            Job oldJob = analysisJob;
            if (oldJob != null && oldJob.getState() != Job.NONE)
                oldJob.cancel();

            if (selectedFiles != null) {
                final AnalyseBundleResolutionJob tmp = new AnalyseBundleResolutionJob("importExportAnalysis", selectedFiles);
                tmp.setSystem(true);

                tmp.addJobChangeListener(new JobChangeAdapter() {
                    @Override
                    public void done(IJobChangeEvent event) {
                        IStatus result = tmp.getResult();
                        if (result != null && result.isOK()) {
                            if (display != null && !display.isDisposed())
                                display.asyncExec(new Runnable() {
                                    @Override
                                    public void run() {
                                        setInput(tmp.getResultFileArray(), tmp.getCapabilities(), tmp.getRequirements());
                                    }
                                });
                        }
                    }
                });

                analysisJob = tmp;
                analysisJob.schedule(500);
            } else {
                analysisJob = null;
            }
        }
    }

    @Override
    public void resourceChanged(IResourceChangeEvent event) {
        if (selectedFiles != null) {
            IWorkspaceRoot wsroot = ResourcesPlugin.getWorkspace().getRoot();
            for (File file : selectedFiles) {
                IFile[] wsfiles = wsroot.findFilesForLocationURI(file.toURI());
                for (IFile wsfile : wsfiles) {
                    if (event.getDelta().findMember(wsfile.getFullPath()) != null) {
                        executeAnalysis();
                        break;
                    }
                }
            }
        }
    }

    static class LocalTransferDragListener implements DragSourceListener {

        private final Viewer viewer;

        public LocalTransferDragListener(Viewer viewer) {
            this.viewer = viewer;
        }

        @Override
        public void dragStart(DragSourceEvent event) {}

        @Override
        public void dragSetData(DragSourceEvent event) {
            LocalSelectionTransfer transfer = LocalSelectionTransfer.getTransfer();
            if (transfer.isSupportedType(event.dataType))
                transfer.setSelection(viewer.getSelection());
        }

        @Override
        public void dragFinished(DragSourceEvent event) {}
    }

}