package org.bndtools.builder.classpath;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;

import org.bndtools.api.BndtoolsConstants;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.ui.wizards.IClasspathContainerPage;
import org.eclipse.jdt.ui.wizards.IClasspathContainerPageExtension;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;

import aQute.bnd.build.Container;
import aQute.bnd.build.Project;
import aQute.lib.io.IO;
import bndtools.central.Central;

public class BndContainerPage extends WizardPage implements IClasspathContainerPage, IClasspathContainerPageExtension {

	// private Activator activator = Activator.getActivator();

	private Table			table;
	private Project			model;
	private File			basedir;
	private IJavaProject	javaProject;

	/**
	 * Default Constructor - sets title, page name, description
	 */
	public BndContainerPage() {
		super("bnd", "bnd - classpath", null);
		setDescription(
			"Ensures that bnd sees the same classpath as eclipse. The table will show the current contents. If there is no bnd file, you can create it with the button");
		setPageComplete(true);
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.eclipse.jdt.ui.wizards.IClasspathContainerPageExtension#initialize
	 * (org.eclipse.jdt.core.IJavaProject,
	 * org.eclipse.jdt.core.IClasspathEntry[])
	 */
	@Override
	public void initialize(IJavaProject project, IClasspathEntry[] currentEntries) {
		javaProject = project;
		model = Central.getInstance()
			.getModel(project);
		basedir = project.getProject()
			.getLocation()
			.makeAbsolute()
			.toFile();
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.
	 * widgets .Composite)
	 */
	@Override
	public void createControl(Composite parent) {
		Composite composite = new Composite(parent, SWT.NULL);
		composite.setLayout(new FormLayout());
		composite.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_FILL | GridData.HORIZONTAL_ALIGN_FILL));
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

			@Override
			public Object[] getElements(Object inputElement) {
				if (model != null)
					try {

						return model.getBuildpath()
							.toArray();
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				return new Object[0];

			}

			@Override
			public void dispose() {
				// TODO Auto-generated method stub

			}

			@Override
			public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {

			}

		});
		tableViewer.setLabelProvider(new ITableLabelProvider() {

			@Override
			public Image getColumnImage(Object element, int columnIndex) {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public String getColumnText(Object element, int columnIndex) {
				Container c = (Container) element;
				switch (columnIndex) {
					case 0 :
						return c.getBundleSymbolicName();
					case 1 :
						return c.getVersion();
					case 2 :
						return c.getError();
					case 3 :
						return c.getFile() + " (" + (c.getFile() != null && c.getFile()
							.exists() ? "exists" : "?") + ")";
					default :
						break;
				}
				return null;
			}

			@Override
			public void addListener(ILabelProviderListener listener) {
				// TODO Auto-generated method stub

			}

			@Override
			public void dispose() {
				// TODO Auto-generated method stub

			}

			@Override
			public boolean isLabelProperty(Object element, String property) {
				// TODO Auto-generated method stub
				return false;
			}

			@Override
			public void removeListener(ILabelProviderListener listener) {
				// TODO Auto-generated method stub

			}

		});
		tableViewer.setInput(model);
		wCreate.addSelectionListener(new SelectionListener() {

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				System.out.println("defw selected");
			}

			@Override
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
				try (PrintStream ps = new PrintStream(IO.outputStream(bnd), false, "UTF-8")) {
					ps.println("# Auto generated by bnd, please adapt");
					ps.println();
					ps.println("Export-Package:                    ");
					ps.println("Private-Package:                   ");
					ps.println("Bundle-Name:                       ${Bundle-SymbolicName}");
					ps.println("Bundle-Version:                    1.0");
					ps.println();
					ps.println("#Example buildpath");
					ps.println(
						"-buildpath:                        osgi;                                        version=4.0, \\");
					ps.println(
						"                                   com.springsource.junit;                      version=\"[3.8,4)\"");
				}
				javaProject.getResource()
					.refreshLocal(IResource.DEPTH_ONE, null);
				model = Central.getInstance()
					.getModel(javaProject);
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
	 * @see org.eclipse.jdt.ui.wizards.IClasspathContainerPage#finish()
	 */
	@Override
	public boolean finish() {
		return true;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.jdt.ui.wizards.IClasspathContainerPage#getSelection()
	 */
	@Override
	public IClasspathEntry getSelection() {
		IPath containerPath = BndtoolsConstants.BND_CLASSPATH_ID;
		IClasspathEntry cpe = JavaCore.newContainerEntry(containerPath);
		return cpe;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.jdt.ui.wizards.IClasspathContainerPage#setSelection(org.
	 * eclipse.jdt.core.IClasspathEntry)
	 */
	@Override
	public void setSelection(IClasspathEntry containerEntry) {
		if (containerEntry != null) {
			// initPath = containerEntry.getPath();
		}
	}
}
