package aQute.bnd.classpath;

import java.io.*;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.jdt.core.*;
import org.eclipse.jdt.ui.wizards.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.jface.wizard.*;
import org.eclipse.swt.*;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;

import aQute.bnd.build.*;
import aQute.bnd.plugin.*;

public class BndContainerPage extends WizardPage implements
        IClasspathContainerPage, IClasspathContainerPageExtension {

    // private Activator activator = Activator.getActivator();

    private Table        table;
    private Project      model;
    private File         basedir;
    private IJavaProject javaProject;

    /**
     * Default Constructor - sets title, page name, description
     */
    public BndContainerPage() {
        super("bnd", "bnd - classpath", null);
        setDescription("Ensures that bnd sees the same classpath as eclipse. The table will show the current contents. If there is no bnd file, you can create it with the button");
        setPageComplete(true);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.jdt.ui.wizards.IClasspathContainerPageExtension#initialize(org.eclipse.jdt.core.IJavaProject,
     *      org.eclipse.jdt.core.IClasspathEntry[])
     */
    public void initialize(IJavaProject project, IClasspathEntry[] currentEntries) {
        javaProject = project;
        model = Activator.getDefault().getCentral().getModel(project);
        basedir = project.getProject().getLocation().makeAbsolute().toFile();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
     */
    public void createControl(Composite parent) {
        Composite composite = new Composite(parent, SWT.NULL);
        composite.setLayout(new FormLayout());
        composite.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_FILL
                | GridData.HORIZONTAL_ALIGN_FILL));
        composite.setFont(parent.getFont());

        setControl(composite);

        final Button wCreate = new Button(composite, SWT.NONE);
        wCreate.setEnabled(model == null);
        final FormData fd_wCreate = new FormData();
        fd_wCreate.bottom = new FormAttachment(100, -5);
        fd_wCreate.right = new FormAttachment(100, -4);
        wCreate.setLayoutData(fd_wCreate);
        wCreate.setText("Create bnd.bnd");

        final TableViewer tableViewer = new TableViewer(composite, SWT.BORDER);
        table = tableViewer.getTable();
        final FormData fd_table = new FormData();
        fd_table.top = new FormAttachment(0, 3);
        fd_table.left = new FormAttachment(0, 3);
        fd_table.right = new FormAttachment(100, -4);
        fd_table.bottom = new FormAttachment(100, -37);
        table.setLayoutData(fd_table);
        table.setLinesVisible(true);
        table.setHeaderVisible(true);

        final TableColumn wBsn = new TableColumn(table, SWT.NONE);
        wBsn.setWidth(200);
        wBsn.setText("Bundle Symbolic Name");

        final TableColumn wVersion = new TableColumn(table, SWT.NONE);
        wVersion.setWidth(100);
        wVersion.setText("Version");

        final TableColumn wOptions = new TableColumn(table, SWT.NONE);
        wOptions.setWidth(200);
        wOptions.setText("Options");

        final TableColumn wFile = new TableColumn(table, SWT.NONE);
        wFile.setWidth(100);
        wFile.setText("File");

        tableViewer.setContentProvider(new IStructuredContentProvider() {

            public Object[] getElements(Object inputElement) {
                if (model != null)
                    try {
                        
                        return model.getBuildpath().toArray();
                    } catch (Exception e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                return new Object[0];

            }

            public void dispose() {
                // TODO Auto-generated method stub

            }

            public void inputChanged(Viewer viewer, Object oldInput,
                    Object newInput) {

            }

        });
        tableViewer.setLabelProvider(new ITableLabelProvider() {

            public Image getColumnImage(Object element, int columnIndex) {
                // TODO Auto-generated method stub
                return null;
            }

            public String getColumnText(Object element, int columnIndex) {
                Container c = (Container) element;
                switch (columnIndex) {
                case 0:
                    return c.getBundleSymbolicName();
                case 1:
                    return c.getVersion();
                case 2:
                    return c.getError();
                case 3:
                    return c.getFile() + " (" + (c.getFile()!=null && c.getFile().exists() ? "exists" : "?") + ")";
                }
                return null;
            }

            public void addListener(ILabelProviderListener listener) {
                // TODO Auto-generated method stub

            }

            public void dispose() {
                // TODO Auto-generated method stub

            }

            public boolean isLabelProperty(Object element, String property) {
                // TODO Auto-generated method stub
                return false;
            }

            public void removeListener(ILabelProviderListener listener) {
                // TODO Auto-generated method stub

            }

        });
        tableViewer.setInput(model);
        wCreate.addSelectionListener(new SelectionListener() {

            public void widgetDefaultSelected(SelectionEvent e) {
                System.out.println("defw selected");
            }

            public void widgetSelected(SelectionEvent e) {
                wCreate.setEnabled(!createBnd());
                tableViewer.setInput(model);
            }

        });
    }

    protected boolean createBnd() {
        if (basedir != null && basedir.isDirectory()) {
            File bnd = new File(basedir, "bnd.bnd");
            try {
                FileOutputStream out = new FileOutputStream(bnd);
                PrintStream ps = new PrintStream(out);
                try {
                    ps.println("# Auto generated by bnd, please adapt");
                    ps.println();
                    ps.println("Export-Package:                    ");
                    ps.println("Private-Package:                   ");
                    ps
                            .println("Bundle-Name:                       ${Bundle-SymbolicName}");
                    ps.println("Bundle-Version:                    1.0");
                    ps.println();
                    ps.println("#Example buildpath");
                    ps
                            .println("-buildpath:                        osgi;                                        version=4.0, \\");
                    ps
                            .println("                                   com.springsource.junit;                      version=\"[3.8,4)\"");
                } finally {
                    out.close();
                    ps.close();
                }
                javaProject.getResource().refreshLocal(IResource.DEPTH_ONE, null);
                model = Activator.getDefault().getCentral().getModel(javaProject);
                return model != null;
            } catch (IOException e) {
                e.printStackTrace();
            } catch (CoreException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        return false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.jdt.ui.wizards.IClasspathContainerPage#finish()
     */
    public boolean finish() {
        return true;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.jdt.ui.wizards.IClasspathContainerPage#getSelection()
     */
    public IClasspathEntry getSelection() {
        IPath containerPath = BndContainerInitializer.ID;
        IClasspathEntry cpe = JavaCore.newContainerEntry(containerPath);
        return cpe;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.jdt.ui.wizards.IClasspathContainerPage#setSelection(org.eclipse.jdt.core.IClasspathEntry)
     */
    public void setSelection(IClasspathEntry containerEntry) {
        if (containerEntry != null) {
            // initPath = containerEntry.getPath();
        }
    }
}
