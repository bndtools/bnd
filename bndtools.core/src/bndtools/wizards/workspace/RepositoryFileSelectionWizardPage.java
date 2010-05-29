package bndtools.wizards.workspace;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.jar.Attributes;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.Constants;

import aQute.lib.osgi.Jar;
import bndtools.Plugin;
import bndtools.utils.Pair;

public class RepositoryFileSelectionWizardPage extends WizardPage {

    private final Image jarImg = AbstractUIPlugin.imageDescriptorFromPlugin(Plugin.PLUGIN_ID, "/icons/jar_obj.gif").createImage();
    private final Image warnImg = AbstractUIPlugin.imageDescriptorFromPlugin(Plugin.PLUGIN_ID, "/icons/warning_obj.gif").createImage();
    private final Image errorImg = AbstractUIPlugin.imageDescriptorFromPlugin(Plugin.PLUGIN_ID, "/icons/error.gif").createImage();
    private final Image okayImg = AbstractUIPlugin.imageDescriptorFromPlugin(Plugin.PLUGIN_ID, "/icons/tick.png").createImage();

    private final Map<File, Pair<String, String>> bsnMap = new HashMap<File, Pair<String, String>>();
    private final List<File> files = new ArrayList<File>(1);

    private TableViewer viewer;

    public RepositoryFileSelectionWizardPage(String pageName) {
        super(pageName);
    }

    public void setFiles(File[] files) {
        this.files.clear();
        for (File file : files) {
            analyseFile(file);
            this.files.add(file);
        }

        if(viewer != null && !viewer.getControl().isDisposed()) {
            viewer.refresh();
            validate();
        }
    }

    public List<File> getFiles() {
        return files;
    }

    void analyseFile(File file) {
        Jar jar = null;
        try {
            jar = new Jar(file);
            Attributes attribs = jar.getManifest().getMainAttributes();
            String bsn = attribs.getValue(Constants.BUNDLE_SYMBOLICNAME);
            String version = attribs.getValue(Constants.BUNDLE_VERSION);

            bsnMap.put(file, Pair.newInstance(bsn, version));
        } catch (IOException e) {
            Plugin.logError("Error reading JAR file content", e);
        } finally {
            if(jar != null)
                jar.close();
        }
    }

    public void createControl(Composite parent) {
        setTitle("Add Files to Repository");

        Composite composite = new Composite(parent, SWT.NONE);

        new Label(composite, SWT.NONE).setText("Selected files:");
        new Label(composite, SWT.NONE); // Spacer;
        Table table = new Table(composite, SWT.FULL_SELECTION | SWT.MULTI | SWT.BORDER);
        table.setLinesVisible(true);
        table.setHeaderVisible(true);

        TableColumn col;
        col = new TableColumn(table, SWT.NONE);
        col.setText("Path");
        col.setWidth(300);
        col = new TableColumn(table, SWT.NONE);
        col.setText("Bundle Name/Version");
        col.setWidth(300);

        viewer = new TableViewer(table);
        viewer.setContentProvider(new ArrayContentProvider());
        viewer.setLabelProvider(new StyledCellLabelProvider() {
            @Override
            public void update(ViewerCell cell) {
                File file = (File) cell.getElement();
                Pair<String, String> bundleId = bsnMap.get(file);

                int index = cell.getColumnIndex();
                if(index == 0) {
                    if(bundleId == null) {
                        cell.setImage(errorImg);
                    } else {
                        cell.setImage(jarImg);
                    }
                    cell.setText(file.getName());
                } else if (index == 1) {
                    if(bundleId == null) {
                        cell.setImage(errorImg);
                        cell.setText("Not a JAR file");
                    } else {
                        String bsn = (bundleId != null) ? bundleId.getFirst() : null;
                        String version = (bundleId != null) ? bundleId.getSecond() : null;
                        if(bsn == null) {
                            cell.setImage(warnImg);
                            cell.setText("Not a Bundle JAR");
                        } else {
                            cell.setImage(okayImg);
                            StyledString styledString = new StyledString(bsn);
                            if(version != null) {
                                styledString.append(" [" + version + "]", StyledString.COUNTER_STYLER);
                                cell.setText(styledString.getString());
                                cell.setStyleRanges(styledString.getStyleRanges());
                            }
                        }
                    }
                }
            }
        });
        viewer.setInput(files);
        validate();

        Button btnAdd = new Button(composite, SWT.PUSH);
        btnAdd.setText("Add...");

        final Button btnRemove = new Button(composite, SWT.NONE);
        btnRemove.setText("Remove");
        btnRemove.setEnabled(false);

        // LISTENERS
        viewer.addSelectionChangedListener(new ISelectionChangedListener() {
            public void selectionChanged(SelectionChangedEvent event) {
                btnRemove.setEnabled(!viewer.getSelection().isEmpty());
            }
        });
        btnRemove.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                doRemove();
            }
        });

        // LAYOUT
        composite.setLayout(new GridLayout(2, false));
        table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 3));
        btnAdd.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
        btnRemove.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));

        setControl(composite);
    }

    void doRemove() {
        IStructuredSelection selection = (IStructuredSelection) viewer.getSelection();
        if(!selection.isEmpty()) {
            for(Iterator<?> iter = selection.iterator(); iter.hasNext(); ) {
                Object item = iter.next();
                files.remove(item);
                viewer.remove(item);
            }
            validate();
        }
    }

    void validate() {
        String error = null;
        String warning = null;

        for (File file : files) {
            Pair<String, String> pair = bsnMap.get(file);
            if(pair == null) {
                error = "One or more selected files is not a JAR.";
            } else {
                String bsn = pair.getFirst();
                if(bsn == null) {
                    warning = "One or more selected files is not a Bundle JAR";
                }
            }
        }

        setErrorMessage(error);
        setMessage(warning, WARNING);
        setPageComplete(!files.isEmpty() && error == null);
    }

    @Override
    public void dispose() {
        super.dispose();
        jarImg.dispose();
        warnImg.dispose();
        errorImg.dispose();
        okayImg.dispose();
    }
}