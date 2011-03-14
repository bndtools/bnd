package bndtools.editor.contents;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
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
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.IOpenListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.OpenEvent;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.viewers.ViewerDropAdapter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.dnd.TransferData;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.SectionPart;
import org.eclipse.ui.forms.editor.IFormPage;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.ide.ResourceUtil;
import org.eclipse.ui.part.ResourceTransfer;
import org.eclipse.ui.plugin.AbstractUIPlugin;

import aQute.lib.osgi.Constants;
import bndtools.BndConstants;
import bndtools.Plugin;
import bndtools.editor.components.Messages;
import bndtools.editor.model.BndEditModel;

public class TestSuitesPart extends SectionPart implements PropertyChangeListener {

    private BndEditModel model;
    private List<String> testSuites;

    private TableViewer viewer;

    public TestSuitesPart(Composite parent, FormToolkit toolkit, int style) {
        super(parent, toolkit, style);

        createSection(getSection(), toolkit);
    }

    private void createSection(Section section, FormToolkit toolkit) {
        section.setText("Test Suites");

        // Section toolbar buttons
        ToolBar toolbar = new ToolBar(section, SWT.FLAT);
        section.setTextClient(toolbar);
        final ToolItem addItem = new ToolItem(toolbar, SWT.PUSH);
        addItem.setImage(PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJ_ADD));
        addItem.setToolTipText("Add");

        final ToolItem removeItem = new ToolItem(toolbar, SWT.PUSH);
        removeItem.setImage(PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_TOOL_DELETE));
        removeItem.setDisabledImage(PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_TOOL_DELETE_DISABLED));
        removeItem.setToolTipText("Remove");
        removeItem.setEnabled(false);

        Composite composite = toolkit.createComposite(section);
        section.setClient(composite);

        Table table = toolkit.createTable(composite, SWT.FULL_SELECTION | SWT.MULTI);

        viewer = new TableViewer(table);
        viewer.setContentProvider(new ArrayContentProvider());
        viewer.setLabelProvider(new TestSuiteLabelProvider());

        // LISTENERS
        viewer.addSelectionChangedListener(new ISelectionChangedListener() {
            public void selectionChanged(SelectionChangedEvent event) {
                ISelection selection = event.getSelection();
                removeItem.setEnabled(selection != null && !selection.isEmpty());
                getManagedForm().fireSelectionChanged(TestSuitesPart.this, selection);
            }
        });
        viewer.addOpenListener(new IOpenListener() {
            public void open(OpenEvent event) {
                String name = (String) ((IStructuredSelection) event.getSelection()).getFirstElement();
                if(name != null)
                    doOpenSource(name);
            }
        });
        viewer.addDropSupport(DND.DROP_COPY | DND.DROP_MOVE, new Transfer[] { ResourceTransfer.getInstance() }, new TestSuiteListDropAdapter());
        addItem.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                doAdd();
            }
        });
        removeItem.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                doRemove();
            }
        });
        table.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                if(e.character == SWT.DEL) {
                    doRemove();
                } else if(e.character == '+') {;
                    doAdd();
                }
            }
        });


        // LAYOUT
        GridLayout layout;

        layout = new GridLayout();
        layout.marginHeight = 0;
        layout.marginWidth = 0;
        composite.setLayout(layout);
        table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
    }

    void doOpenSource(String name) {
        IJavaProject javaProj = getJavaProject();
        if(javaProj != null) {
            try {
                IType type = javaProj.findType(name);
                if(type != null)
                    JavaUI.openInEditor(type, true, true);
            } catch (PartInitException e) {
                e.printStackTrace();
            } catch (JavaModelException e) {
                e.printStackTrace();
            }
        }
    }

    void doAdd() {
    }

    void doRemove() {
        IStructuredSelection sel = (IStructuredSelection) viewer.getSelection();
        if(!sel.isEmpty()) {
            testSuites.removeAll(sel.toList());
            viewer.remove(sel.toArray());
            markDirty();
            validate();
        }
    }

    @Override
    public void initialize(IManagedForm form) {
        super.initialize(form);

        this.model = (BndEditModel) form.getInput();
        this.model.addPropertyChangeListener(BndConstants.TESTSUITES, this);
        this.model.addPropertyChangeListener(Constants.TESTCASES, this);
    }

    @Override
    public void refresh() {
        List<String> modelList = model.getTestSuites();
        testSuites = (modelList == null) ? new ArrayList<String>() : new ArrayList<String>(modelList);
        viewer.setInput(testSuites);
        validate();
    }

    private void validate() {
    }

    @Override
    public void commit(boolean onSave) {
        try {
            model.removePropertyChangeListener(BndConstants.TESTSUITES, this);
            model.removePropertyChangeListener(Constants.TESTCASES, this);
            model.setTestSuites(testSuites.isEmpty() ? null : testSuites);
        } finally {
            model.addPropertyChangeListener(BndConstants.TESTSUITES, this);
            model.addPropertyChangeListener(Constants.TESTCASES, this);
            super.commit(onSave);
        }
    }

    public void propertyChange(PropertyChangeEvent evt) {
        String propertyName = evt.getPropertyName();
        if(BndConstants.TESTSUITES.equals(propertyName) || Constants.TESTCASES.equals(propertyName)) {
            IFormPage page = (IFormPage) getManagedForm().getContainer();
            if(page.isActive()) {
                refresh();
            } else {
                markStale();
            }
        } else if(Constants.PRIVATE_PACKAGE.equals(propertyName) || Constants.EXPORT_PACKAGE.equals(propertyName)) {
            validate();
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
    private class TestSuiteListDropAdapter extends ViewerDropAdapter {

        protected TestSuiteListDropAdapter() {
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
                insertionIndex = testSuites.indexOf(target);
                if(insertionIndex > -1 && loc == LOCATION_ON || loc == LOCATION_AFTER)
                    insertionIndex ++;
            }

            List<String> addedNames = new ArrayList<String>();
            if(data instanceof IResource[]) {
                IResource[] resources = (IResource[]) data;
                for (IResource resource : resources) {
                    IJavaElement javaElement = JavaCore.create(resource);
                    if(javaElement != null) {
                        try {
                            if(javaElement instanceof IType) {
                                IType type = (IType) javaElement;
                                if(type.isClass() && Flags.isPublic(type.getFlags())) {
                                    String typeName = type.getPackageFragment().getElementName() + "." + type.getElementName(); //$NON-NLS-1$
                                    addedNames.add(typeName);
                                }
                            } else if(javaElement instanceof ICompilationUnit) {
                                IType[] allTypes = ((ICompilationUnit) javaElement).getAllTypes();
                                for (IType type : allTypes) {
                                    if(type.isClass() && Flags.isPublic(type.getFlags())) {
                                        String typeName = type.getPackageFragment().getElementName() + "." + type.getElementName(); //$NON-NLS-1$
                                        addedNames.add(typeName);
                                    }
                                }
                            }
                        } catch (JavaModelException e) {
                            Plugin.log(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, Messages.ComponentListPart_errorJavaType, e));
                        }
                    }
                }
            }

            if(!addedNames.isEmpty()) {
                if(insertionIndex == -1 || insertionIndex == testSuites.size()) {
                    testSuites.addAll(addedNames);
                    viewer.add(addedNames.toArray(new String[addedNames.size()]));
                } else {
                    testSuites.addAll(insertionIndex, addedNames);
                    viewer.refresh();
                }
                viewer.setSelection(new StructuredSelection(addedNames), true);
                validate();
                markDirty();
            }
            return true;
        }
    }
}


class TestSuiteLabelProvider extends StyledCellLabelProvider {
    private Image suiteImg = AbstractUIPlugin.imageDescriptorFromPlugin(Plugin.PLUGIN_ID, "/icons/tsuite.gif").createImage();
    @Override
    public void update(ViewerCell cell) {
        String fqName = (String) cell.getElement();
        cell.setText(fqName);
        cell.setImage(suiteImg);
    }
    @Override
    public void dispose() {
        super.dispose();
        suiteImg.dispose();
    }

}

