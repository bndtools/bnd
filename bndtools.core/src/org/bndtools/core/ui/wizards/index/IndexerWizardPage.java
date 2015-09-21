package org.bndtools.core.ui.wizards.index;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.PatternSyntaxException;

import org.bndtools.core.ui.icons.Icons;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.ContainerSelectionDialog;
import bndtools.Plugin;

public class IndexerWizardPage extends WizardPage {

    private File baseDir;
    private String resourcePattern = "**.jar"; //$NON-NLS-1$
    private List<Path> inputPaths = Collections.emptyList();
    private String outputFileName = "index.xml.gz"; //$NON-NLS-1$

    private IndexFormatStyle outputStyle = IndexFormatStyle.PRETTY_UNCOMPRESSED;

    private Text txtBaseDir;
    private Text txtResourcePattern;
    private Text txtOutputPrefix;
    private Button btnOutputCompressed;
    private Button btnOutputPretty;
    private Label lblOutputName;

    private SearchFilesJob updateInputFilesJob;
    private TableViewer vwrInputs;
    private Label lblInputCount;

    private final Image imgFile = Icons.desc("file").createImage(); //$NON-NLS-1$
    private final Image imgWarning = Icons.desc("warning").createImage(); //$NON-NLS-1$
    private final Image imgError = Icons.desc("error").createImage(); //$NON-NLS-1$

    public IndexerWizardPage() {
        super("index"); //$NON-NLS-1$

        setTitle(Messages.IndexerWizardPage_title);
        setDescription(Messages.IndexerWizardPage_description);
    }

    @Override
    public void createControl(Composite parent) {
        Composite composite = new Composite(parent, SWT.NULL);
        setControl(composite);

        // Create controls
        new Label(composite, SWT.NONE).setText(Messages.IndexerWizardPage_baseDir);
        txtBaseDir = new Text(composite, SWT.BORDER);

        newSpacer(composite);
        Composite cmpButtonBar = new Composite(composite, SWT.NONE);
        Button btnBrowseWorkspace = new Button(cmpButtonBar, SWT.PUSH);
        btnBrowseWorkspace.setText(Messages.IndexerWizardPage_browse);
        Button btnBrowseExternal = new Button(cmpButtonBar, SWT.PUSH);
        btnBrowseExternal.setText(Messages.IndexerWizardPage_browseExternal);

        new Label(composite, SWT.NONE).setText(Messages.IndexerWizardPage_resourcePattern);
        txtResourcePattern = new Text(composite, SWT.BORDER);
        ControlDecoration decorResourcePattern = new ControlDecoration(txtResourcePattern, SWT.LEFT | SWT.TOP, composite);
        decorResourcePattern.setImage(FieldDecorationRegistry.getDefault().getFieldDecoration(FieldDecorationRegistry.DEC_INFORMATION).getImage());
        decorResourcePattern.setMarginWidth(3);
        decorResourcePattern.setDescriptionText(Messages.IndexerWizardPage_resourcePatternHelp);
        decorResourcePattern.setShowHover(true);
        decorResourcePattern.setShowOnlyOnFocus(false);

        Label lblInputs = new Label(composite, SWT.NONE);
        lblInputs.setText(Messages.IndexerWizardPage_inputs);
        Table tblInputs = new Table(composite, SWT.MULTI | SWT.BORDER);
        vwrInputs = new TableViewer(tblInputs);
        vwrInputs.setContentProvider(ArrayContentProvider.getInstance());
        vwrInputs.setLabelProvider(new StyledCellLabelProvider() {
            @Override
            public void update(ViewerCell cell) {
                Object elem = cell.getElement();
                if (elem instanceof Path) {
                    cell.setText(elem.toString());
                    cell.setImage(imgFile);
                } else if (elem instanceof IStatus) {
                    IStatus status = (IStatus) elem;
                    cell.setText(status.getMessage());
                    if (status.getSeverity() == IStatus.ERROR)
                        cell.setImage(imgError);
                    else if (status.getSeverity() == IStatus.WARNING)
                        cell.setImage(imgWarning);
                }
            }
        });
        newSpacer(composite);
        lblInputCount = new Label(composite, SWT.NONE);

        Label lblOutputs = new Label(composite, SWT.NONE);
        lblOutputs.setText(Messages.IndexerWizardPage_output);
        Group grpOutput = new Group(composite, SWT.NONE);

        new Label(grpOutput, SWT.NONE).setText(Messages.IndexerWizardPage_prefix);
        txtOutputPrefix = new Text(grpOutput, SWT.BORDER);

        newSpacer(grpOutput);
        btnOutputPretty = new Button(grpOutput, SWT.RADIO);
        btnOutputPretty.setText(Messages.IndexerWizardPage_prettyPrint);

        newSpacer(grpOutput);
        btnOutputCompressed = new Button(grpOutput, SWT.RADIO);
        btnOutputCompressed.setText(Messages.IndexerWizardPage_compressed);

        newSpacer(grpOutput);
        lblOutputName = new Label(grpOutput, SWT.NONE);

        // LAYOUT
        GridLayout gl;
        GridData gd;
        gl = new GridLayout(2, false);
        gl.horizontalSpacing = 10;
        composite.setLayout(gl);

        txtBaseDir.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        cmpButtonBar.setLayoutData(new GridData(SWT.RIGHT, SWT.FILL, false, false));
        gl = new GridLayout(2, false);
        gl.marginHeight = 0;
        gl.marginWidth = 0;
        cmpButtonBar.setLayout(gl);
        btnBrowseWorkspace.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
        btnBrowseExternal.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));

        txtResourcePattern.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

        lblInputs.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false));
        gd = new GridData(SWT.FILL, SWT.FILL, true, false);
        gd.heightHint = 120;
        gd.widthHint = 100;
        tblInputs.setLayoutData(gd);
        lblInputCount.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, true, false));

        lblOutputs.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false));
        grpOutput.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
        grpOutput.setLayout(new GridLayout(2, false));
        txtOutputPrefix.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        btnOutputCompressed.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        btnOutputPretty.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        lblOutputName.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

        // LOAD DATA
        if (baseDir != null)
            txtBaseDir.setText(baseDir.getAbsolutePath());
        if (resourcePattern != null)
            txtResourcePattern.setText(resourcePattern);
        txtOutputPrefix.setText("index"); //$NON-NLS-1$
        btnOutputCompressed.setSelection(outputStyle == IndexFormatStyle.COMPRESSED);
        btnOutputPretty.setSelection(outputStyle == IndexFormatStyle.PRETTY_UNCOMPRESSED);
        updateOutputFileName();
        updateInputs();

        // LISTENERS
        txtBaseDir.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent me) {
                String baseDirStr = txtBaseDir.getText();
                baseDir = baseDirStr.isEmpty() ? null : new File(baseDirStr);
                validate();
                updateInputs();
            }
        });
        txtResourcePattern.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent me) {
                resourcePattern = txtResourcePattern.getText();
                updateInputs();
            }
        });
        btnBrowseWorkspace.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
                ContainerSelectionDialog containerDlg = new ContainerSelectionDialog(getShell(), root, false, Messages.IndexerWizardPage_selectBaseDir);
                containerDlg.showClosedProjects(false);
                if (containerDlg.open() == Window.OK) {
                    Object[] selection = containerDlg.getResult();
                    if (selection != null && selection.length > 0) {
                        IPath workspacePath = (IPath) selection[0];
                        IResource resource = root.findMember(workspacePath);
                        if (resource == null)
                            MessageDialog.openError(getShell(), "Error", "Could not find resource for path " + workspacePath);
                        else
                            txtBaseDir.setText(resource.getLocation().toString());
                    }
                }
            }
        });
        btnBrowseExternal.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                DirectoryDialog dirDialog = new DirectoryDialog(getShell());
                dirDialog.setMessage(Messages.IndexerWizardPage_selectBaseDir);
                String selectedPath = dirDialog.open();
                if (selectedPath != null)
                    txtBaseDir.setText(selectedPath);
            }
        });
        txtOutputPrefix.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent me) {
                updateOutputFileName();
                validate();
            }
        });
        Listener outputRadioListener = new Listener() {
            @Override
            public void handleEvent(Event ev) {
                outputStyle = btnOutputCompressed.getSelection() ? IndexFormatStyle.COMPRESSED : IndexFormatStyle.PRETTY_UNCOMPRESSED;
                updateOutputFileName();
                validate();
            }
        };
        btnOutputCompressed.addListener(SWT.Selection, outputRadioListener);
        btnOutputPretty.addListener(SWT.Selection, outputRadioListener);

        validate();
    }

    private Label newSpacer(Composite composite) {
        // Wrapping this in a method makes it easier to ignore the unused warning
        return new Label(composite, SWT.NONE); // spacer
    }

    private void updateInputs() {
        if (updateInputFilesJob != null) {
            updateInputFilesJob.cancel();
        }
        inputPaths = Collections.emptyList();
        setPageComplete(false);
        vwrInputs.setInput(inputPaths);
        lblInputCount.setText(String.format("%d resources found", inputPaths.size()));
        lblInputCount.getParent().layout(new Control[] {
                lblInputCount
        });
        final String resourcePattern = this.resourcePattern;
        final Display display = getShell().getDisplay();

        if (baseDir == null)
            return;
        updateInputFilesJob = new SearchFilesJob(baseDir, resourcePattern);
        updateInputFilesJob.addJobChangeListener(new JobChangeAdapter() {
            @Override
            public void done(IJobChangeEvent event) {
                final IStatus status = updateInputFilesJob.getSearchResult();
                display.syncExec(new Runnable() {
                    @Override
                    public void run() {
                        inputPaths = updateInputFilesJob.getPaths();
                        if (inputPaths == null)
                            inputPaths = Collections.emptyList();
                        if (status.isOK())
                            vwrInputs.setInput(updateInputFilesJob.getPaths());
                        else
                            // The error/warning status is displayed in the table instead of the path list
                            vwrInputs.setInput(Collections.singleton(status));
                        lblInputCount.setText(String.format("%d resources found", inputPaths.size()));
                        lblInputCount.getParent().layout(new Control[] {
                                lblInputCount
                        });
                        validate();
                    }
                });
            }
        });
        updateInputFilesJob.setSystem(true);
        updateInputFilesJob.schedule(500);
        vwrInputs.setInput(Collections.singleton(new Status(IStatus.INFO, Plugin.PLUGIN_ID, 0, Messages.IndexerWizardPage_checking, null)));
        lblInputCount.setText("...");
        lblInputCount.getParent().layout(new Control[] {
                lblInputCount
        });
    }

    private void updateOutputFileName() {
        String prefix = txtOutputPrefix.getText();
        String suffix = outputStyle == IndexFormatStyle.COMPRESSED ? "xml.gz" : "xml"; //$NON-NLS-1$ //$NON-NLS-2$

        StringBuilder sb = new StringBuilder().append(prefix);
        if (sb.charAt(sb.length() - 1) != '.')
            sb.append('.');
        sb.append(suffix);
        outputFileName = sb.toString();
        lblOutputName.setText(String.format(Messages.IndexerWizardPage_outputFileMessage, outputFileName));
    }

    private void validate() {
        String warning = null;

        if (baseDir == null) {
            setErrorMessage(null);
            setPageComplete(false);
            return;
        }

        if (!baseDir.exists()) {
            setErrorMessage(Messages.IndexerWizardPage_error_noSuchDir + baseDir.getAbsolutePath());
            setPageComplete(false);
            return;
        }

        if (!baseDir.isDirectory()) {
            setErrorMessage(Messages.IndexerWizardPage_error_notDir + baseDir.getAbsolutePath());
            setPageComplete(false);
            return;
        }

        try {
            FileSystem fs = FileSystems.getDefault();
            fs.getPathMatcher("glob:" + resourcePattern); //$NON-NLS-1$
        } catch (Exception e) {
            setErrorMessage(e.getMessage());
            setPageComplete(false);
            return;
        }

        if (outputFileName.indexOf('/') > -1) {
            setErrorMessage(Messages.IndexerWizardPage_error_noSlashes);
            setPageComplete(false);
        }
        File outputFile = new File(baseDir, outputFileName);
        if (outputFile.exists()) {
            if (outputFile.isFile())
                warning = Messages.IndexerWizardPage_warn_fileOverwrite + outputFile.getAbsolutePath();
            else {
                setErrorMessage(Messages.IndexerWizardPage_error_fileExists + outputFile.getAbsolutePath());
                setPageComplete(false);
            }
        }

        setErrorMessage(null);
        setMessage(warning, WARNING);
        setPageComplete(inputPaths != null && !inputPaths.isEmpty());
    }

    public void setBaseDir(File baseDir) {
        this.baseDir = baseDir;
        if (isControlCreated() && !getControl().isDisposed())
            txtBaseDir.setText(baseDir.getAbsolutePath());
    }

    public File getBaseDir() {
        return baseDir;
    }

    public void setResourcePattern(String pattern) {
        this.resourcePattern = pattern;
        if (isControlCreated() && !getControl().isDisposed())
            txtResourcePattern.setText(resourcePattern);
    }

    public String getResourcePattern() {
        return resourcePattern;
    }

    public IndexFormatStyle getOutputStyle() {
        return outputStyle;
    }

    public File getOutputFile() {
        return new File(baseDir, outputFileName);
    }

    public List<Path> getInputPaths() {
        try {
            if (updateInputFilesJob != null)
                updateInputFilesJob.join();
            return inputPaths;
        } catch (InterruptedException e) {
            return Collections.emptyList();
        }
    }

    @Override
    public void dispose() {
        super.dispose();
        imgFile.dispose();
        imgError.dispose();
        imgWarning.dispose();
    }

    private static class SearchFilesJob extends Job {

        private final File baseDir;
        private final String resourcePattern;
        private IStatus searchResult = Status.OK_STATUS;
        private List<Path> paths = Collections.emptyList();

        private SearchFilesJob(File baseDir, String resourcePattern) {
            super(Messages.IndexerWizardPage_updateInputs);
            if (baseDir == null)
                throw new NullPointerException("baseDir cannot be null"); //$NON-NLS-1$
            this.baseDir = baseDir;
            this.resourcePattern = resourcePattern;
            setSystem(true);
            setUser(false);
        }

        @Override
        protected IStatus run(IProgressMonitor monitor) {
            try {
                final List<Path> result = new LinkedList<>();

                // Collect the files
                final Path basePath = baseDir.toPath();
                FileSystem fs = FileSystems.getDefault();
                final PathMatcher matcher = fs.getPathMatcher("glob:" + resourcePattern); //$NON-NLS-1$
                Files.walkFileTree(basePath, new SimpleFileVisitor<Path>() {
                    @Override
                    public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) throws IOException {
                        Path relative = basePath.relativize(path);
                        if (matcher.matches(relative))
                            result.add(relative);
                        return FileVisitResult.CONTINUE;
                    }
                });
                if (result.isEmpty())
                    searchResult = new Status(IStatus.WARNING, Plugin.PLUGIN_ID, 0, Messages.IndexerWizardPage_warn_noMatchingFiles, null);
                else
                    searchResult = Status.OK_STATUS;
                paths = result;
            } catch (PatternSyntaxException e) {
                searchResult = new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, Messages.IndexerWizardPage_error_invalidPattern + e.getMessage(), e);
            } catch (Exception e) {
                searchResult = new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, Messages.IndexerWizardPage_error_fileSearch, e);
            }

            return Status.OK_STATUS;
        }

        public IStatus getSearchResult() {
            return searchResult;
        }

        public List<Path> getPaths() {
            return paths;
        }
    }

}
