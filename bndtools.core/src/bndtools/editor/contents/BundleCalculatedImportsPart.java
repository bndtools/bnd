package bndtools.editor.contents;

import java.text.MessageFormat;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.viewers.IOpenListener;
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
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.SectionPart;
import org.eclipse.ui.forms.editor.IFormPage;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.ide.ResourceUtil;
import org.eclipse.ui.plugin.AbstractUIPlugin;

import bndtools.Plugin;
import bndtools.model.clauses.HeaderClause;
import bndtools.model.importanalysis.ImportPackage;
import bndtools.model.importanalysis.ImportTreeContentProvider;
import bndtools.model.importanalysis.ImportsAndExportsViewerSorter;
import bndtools.model.importanalysis.ImportsExportsTreeContentProvider.ImportUsedByClass;
import bndtools.model.importanalysis.ImportsExportsTreeLabelProvider;
import bndtools.tasks.AnalyseImportsJob;

public class BundleCalculatedImportsPart extends SectionPart implements IResourceChangeListener {

    private Image imgRefresh = AbstractUIPlugin.imageDescriptorFromPlugin(Plugin.PLUGIN_ID, "/icons/arrow_refresh.png").createImage(); //$NON-NLS-1$
    private Image imgShowSelfImports = AbstractUIPlugin.imageDescriptorFromPlugin(Plugin.PLUGIN_ID, "/icons/package_folder_impexp.gif").createImage(); //$NON-NLS-1$

    private Tree tree;
    private TreeViewer viewer;

    private ViewerFilter hideSelfImportsFilter;

    public BundleCalculatedImportsPart(Composite parent, FormToolkit toolkit, int style) {
        super(parent, toolkit, style);
        createSection(getSection(), toolkit);
    }

    private void createSection(Section section, FormToolkit toolkit) {
        // CREATE COMPONENTS
        section.setText(Messages.BundleCalculatedImportsPart_title);
        ToolBar toolbar = new ToolBar(section, SWT.FLAT);
        section.setTextClient(toolbar);

        final ToolItem showSelfImportsItem = new ToolItem(toolbar, SWT.CHECK);
        showSelfImportsItem.setImage(imgShowSelfImports);
        showSelfImportsItem.setToolTipText(Messages.BundleCalculatedImportsPart_tooltipShowSelfImports);

        Composite composite = toolkit.createComposite(section);
        section.setClient(composite);

        toolkit.createLabel(composite, Messages.BundleCalculatedImportsPart_description, SWT.WRAP);

        tree = toolkit.createTree(composite, SWT.MULTI | SWT.FULL_SELECTION);
        tree.setHeaderVisible(true);
        tree.setLinesVisible(true);

        TreeColumn col;
        col = new TreeColumn(tree, SWT.NONE);
        col.setText(Messages.BundleCalculatedImportsPart_columnPackage);
        col.setWidth(250);

        col = new TreeColumn(tree, SWT.NONE);
        col.setText(Messages.BundleCalculatedImportsPart_columnAttribs);
        col.setWidth(75);

        viewer = new TreeViewer(tree);
        viewer.setContentProvider(new ImportTreeContentProvider());
        viewer.setSorter(new ImportsAndExportsViewerSorter());
        viewer.setLabelProvider(new ImportsExportsTreeLabelProvider());

        hideSelfImportsFilter = new ViewerFilter() {
            @Override
            public boolean select(Viewer viewer, Object parentElement, Object element) {
                if (element instanceof ImportPackage) {
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
                                builder.append(",\n"); //$NON-NLS-1$
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
                        String className = importUsedBy.getClazz().getFQN();
                        IType type = null;

                        IFile file = getEditorFile();
                        if(file != null) {
                                IJavaProject javaProject = JavaCore.create(file.getProject());
                                try {
                                    type = javaProject.findType(className);
                                } catch (JavaModelException e) {
                                    ErrorDialog.openError(tree.getShell(), Messages.BundleCalculatedImportsPart_error, Messages.BundleCalculatedImportsPart_errorFindingType, new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, MessageFormat.format(Messages.BundleCalculatedImportsPart_errorOpeningClass, className), e));
                                }
                        }
                        try {
                            if(type != null)
                                JavaUI.openInEditor(type, true, true);
                        } catch (PartInitException e) {
                            ErrorDialog.openError(tree.getShell(), Messages.BundleCalculatedImportsPart_error, null, new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, MessageFormat.format(Messages.BundleCalculatedImportsPart_errorOpeningJavaEditor, className), e));
                        } catch (JavaModelException e) {
                            ErrorDialog.openError(tree.getShell(), Messages.BundleCalculatedImportsPart_error, null, new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, MessageFormat.format(Messages.BundleCalculatedImportsPart_errorOpeningClass, className), e));
                        }
                    }
                }
            }
        });


        // LISTENERS
        showSelfImportsItem.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                boolean showSelfImports = showSelfImportsItem.getSelection();
                ViewerFilter[] filters = showSelfImports
                    ? new ViewerFilter[0]
                    : new ViewerFilter[] { hideSelfImportsFilter };
                viewer.setFilters(filters);
            }
        });

        // LAYOUT
        GridLayout layout = new GridLayout();
        layout.marginHeight = 0;
        layout.marginWidth = 0;
        layout.verticalSpacing = 2;
        composite.setLayout(layout);
        GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
        gd.heightHint = 100;
        tree.setLayoutData(gd);
    }

    @Override
    public void initialize(IManagedForm form) {
        super.initialize(form);

        ResourcesPlugin.getWorkspace().addResourceChangeListener(this, IResourceChangeEvent.POST_CHANGE);
    }

    @Override
    public void refresh() {
        super.refresh();

        IFile file = getEditorFile();
        if(file != null) {
            final AnalyseImportsJob job = new AnalyseImportsJob(Messages.BundleCalculatedImportsPart_jobAnalyse, new IFile[] { file });
            final Display display = tree.getDisplay();
            job.addJobChangeListener(new JobChangeAdapter() {
                @Override
                public void done(IJobChangeEvent event) {
                    if(job.getResult().isOK()) {
                        final List<ImportPackage> imports = job.getImportResults();
                        display.asyncExec(new Runnable() {
                            public void run() {
                                if(tree != null && !tree.isDisposed())
                                    viewer.setInput(imports);
                            }
                        });
                    }
                }
            });
            job.schedule();
        }
    }

    private IFile getEditorFile() {
        IFormPage page = (IFormPage) getManagedForm().getContainer();
        IFile file = ResourceUtil.getFile(page.getEditorInput());
        return file;
    }

    @Override
    public void dispose() {
        ResourcesPlugin.getWorkspace().removeResourceChangeListener(this);
        super.dispose();
        imgRefresh.dispose();
        imgShowSelfImports.dispose();

    }

    public void resourceChanged(IResourceChangeEvent event) {
        IFile file = getEditorFile();
        if (file != null) {
            IResourceDelta delta = event.getDelta();
            delta = delta.findMember(file.getFullPath());
            if (delta != null) {
                refresh();
            }
        }
    }
}
