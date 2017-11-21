package bndtools.editor.project;

import java.util.List;

import org.bndtools.api.ILogger;
import org.bndtools.api.Logger;
import org.bndtools.api.ResolveMode;
import org.bndtools.core.resolve.ResolveJob;
import org.bndtools.core.ui.icons.Icons;
import org.bndtools.utils.swt.SWTConcurrencyUtil;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.editor.IFormPage;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.osgi.resource.Requirement;

import bndtools.BndConstants;
import bndtools.editor.BndEditor;

public class RunRequirementsPart extends AbstractRequirementListPart {

    @SuppressWarnings("deprecation")
    private static final String RUNREQUIRE = BndConstants.RUNREQUIRE;
    private static final ILogger logger = Logger.getLogger(RunRequirementsPart.class);

    private static final String[] SUBSCRIBE_PROPS = new String[] {
            RUNREQUIRE, BndConstants.RUNREQUIRES, BndConstants.RESOLVE_MODE
    };

    private Button btnAutoResolve;
    private ResolveMode resolveMode;

    private final Image resolveIcon = Icons.desc("resolve").createImage();
    private Button btnResolveNow;
    private JobChangeAdapter resolveJobListener;

    public RunRequirementsPart(Composite parent, FormToolkit toolkit, int style) {
        super(parent, toolkit, style);
        createSection(getSection(), toolkit);
    }

    @Override
    protected String[] getProperties() {
        return SUBSCRIBE_PROPS;
    }

    private void createSection(Section section, FormToolkit tk) {
        section.setText("Run Requirements");
        section.setDescription("The specified requirements will be used to resolve a set of runtime bundles from available repositories.");

        // Create toolbar
        createToolBar(section);

        // Create main panel
        Composite composite = tk.createComposite(section);
        section.setClient(composite);

        // Create table
        TableViewer viewer = createViewer(composite, tk);

        // Create resolve and auto-resolve controls
        btnAutoResolve = tk.createButton(composite, "Auto-resolve on save", SWT.CHECK);
        btnResolveNow = tk.createButton(composite, "Resolve", SWT.PUSH);
        btnResolveNow.setImage(resolveIcon);

        // Listeners
        btnAutoResolve.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                ResolveMode old = resolveMode;
                resolveMode = btnAutoResolve.getSelection() ? ResolveMode.auto : ResolveMode.manual;
                if (old != resolveMode)
                    markDirty();
            }
        });
        btnResolveNow.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                doResolve();
            }
        });
        resolveJobListener = new JobChangeAdapter() {
            @Override
            public void running(IJobChangeEvent event) {
                if (event.getJob() instanceof ResolveJob) {
                    SWTConcurrencyUtil.execForControl(btnResolveNow, true, () -> btnResolveNow.setEnabled(false));
                }
            }

            @Override
            public void done(IJobChangeEvent event) {
                if (event.getJob() instanceof ResolveJob) {
                    SWTConcurrencyUtil.execForControl(btnResolveNow, true, () -> btnResolveNow.setEnabled(true));
                }
            }
        };
        Job.getJobManager().addJobChangeListener(resolveJobListener);

        // Layout
        GridLayout layout;
        GridData gd;

        layout = new GridLayout(2, false);
        layout.horizontalSpacing = 0;
        layout.verticalSpacing = 5;
        layout.marginHeight = 0;
        layout.marginWidth = 0;
        composite.setLayout(layout);

        gd = new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1);
        gd.widthHint = 50;
        gd.heightHint = 50;
        viewer.getControl().setLayoutData(gd);

        gd = new GridData(SWT.RIGHT, SWT.CENTER, true, false);
        btnResolveNow.setLayoutData(gd);
    }

    @Override
    public void dispose() {
        super.dispose();
        Job.getJobManager().removeJobChangeListener(resolveJobListener);
        resolveIcon.dispose();
    }

    private void doResolve() {
        IFormPage formPage = (IFormPage) getManagedForm().getContainer();
        BndEditor editor = (BndEditor) formPage.getEditor();
        editor.resolveRunBundles(new NullProgressMonitor(), false);
    }

    @Override
    protected void doCommitToModel(List<Requirement> requires) {
        model.setRunRequires(requires);
        setResolveMode();
    }

    @Override
    public List<Requirement> doRefreshFromModel() {
        resolveMode = getResolveMode();
        btnAutoResolve.setSelection(resolveMode == ResolveMode.auto);

        return model.getRunRequires();
    }

    @Override
    protected String getAddButtonLabel() {
        return "Add Bundle Requirement";
    }

    private void setResolveMode() {
        String formatted;
        if (resolveMode == ResolveMode.manual || resolveMode == null)
            formatted = null;
        else
            formatted = resolveMode.toString();
        model.genericSet(BndConstants.RESOLVE_MODE, formatted);
    }

    private ResolveMode getResolveMode() {
        ResolveMode resolveMode = ResolveMode.manual;
        try {
            String str = (String) model.genericGet(BndConstants.RESOLVE_MODE);
            if (str != null)
                resolveMode = Enum.valueOf(ResolveMode.class, str);
        } catch (Exception e) {
            logger.logError("Error parsing '-resolve' header.", e);
        }
        return resolveMode;
    }

}
