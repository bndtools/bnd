package bndtools.editor.pages;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.bndtools.api.BndtoolsConstants;
import org.bndtools.api.ILogger;
import org.bndtools.api.Logger;
import org.bndtools.build.api.BuildErrorDetailsHandler;
import org.bndtools.build.api.BuildErrorDetailsHandlers;
import org.bndtools.core.ui.ExtendedFormEditor;
import org.bndtools.core.ui.IFormPageFactory;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IMarkerResolution;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.IMessageManager;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.editor.IFormPage;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.ide.ResourceUtil;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.eclipse.ui.texteditor.ITextEditor;

import aQute.bnd.build.model.BndEditModel;
import bndtools.Plugin;
import bndtools.editor.BndEditor;
import bndtools.editor.common.IPriority;
import bndtools.editor.common.MDSashForm;
import bndtools.editor.project.BuildOperationsPart;
import bndtools.editor.project.BuildPathPart;
import bndtools.editor.project.SubBundlesPart;
import bndtools.utils.MessageHyperlinkAdapter;

public class ProjectBuildPage extends FormPage implements IPriority, IResourceChangeListener {
    private static final ILogger logger = Logger.getLogger(ProjectBuildPage.class);

    private final BndEditModel model;
    private final Image imgError = PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJS_ERROR_TSK);
    private final Image imgWarning = PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJS_WARN_TSK);

    private final ImageDescriptor imgErrorOverlay = AbstractUIPlugin.imageDescriptorFromPlugin(Plugin.PLUGIN_ID, "icons/error_co.gif");
    private final ImageDescriptor imgWarningOverlay = AbstractUIPlugin.imageDescriptorFromPlugin(Plugin.PLUGIN_ID, "icons/warning_co.gif");

    private final Map<String,Integer> messageSeverityMap = new LinkedHashMap<String,Integer>();
    private final Map<String,IAction[]> messageFixesMap = new HashMap<String,IAction[]>();
    private int problemSeverity = 0;

    private Image pageImage = null;
    private final ExtendedFormEditor editor;

    public static final IFormPageFactory FACTORY = new IFormPageFactory() {
        @Override
        public IFormPage createPage(ExtendedFormEditor editor, BndEditModel model, String id) throws IllegalArgumentException {
            return new ProjectBuildPage(editor, model, id, "Build");
        }

        @Override
        public boolean supportsMode(Mode mode) {
            return mode == Mode.project;
        }
    };

    private ProjectBuildPage(ExtendedFormEditor editor, BndEditModel model, String id, String title) {
        super(editor, id, title);
        this.editor = editor;
        this.model = model;
    }

    @Override
    protected void createFormContent(IManagedForm managedForm) {
        managedForm.setInput(model);

        FormToolkit tk = managedForm.getToolkit();
        ScrolledForm form = managedForm.getForm();
        form.setText("Project Build");
        tk.decorateFormHeading(form.getForm());
        form.getForm().addMessageHyperlinkListener(new MessageHyperlinkAdapter(getEditor()));

        GridLayout layout;
        GridData gd;

        // Create Controls
        Composite body = form.getBody();
        body.setLayout(new FillLayout());

        MDSashForm sashForm = new MDSashForm(body, SWT.HORIZONTAL, managedForm);
        sashForm.setSashWidth(6);
        tk.adapt(sashForm, false, false);
        sashForm.hookResizeListener();

        Composite leftPanel = tk.createComposite(sashForm);
        gd = new GridData(SWT.FILL, SWT.FILL, true, true);
        leftPanel.setLayoutData(gd);

        layout = new GridLayout(1, false);
        leftPanel.setLayout(layout);

        SubBundlesPart subBundlesPart = new SubBundlesPart(leftPanel, tk, Section.TITLE_BAR | Section.EXPANDED | Section.DESCRIPTION);
        managedForm.addPart(subBundlesPart);
        gd = new GridData(SWT.FILL, SWT.FILL, true, false);
        subBundlesPart.getSection().setLayoutData(gd);

        BuildPathPart buildPathPart = new BuildPathPart(leftPanel, tk, Section.TITLE_BAR | Section.EXPANDED | Section.DESCRIPTION);
        managedForm.addPart(buildPathPart);
        gd = new GridData(SWT.FILL, SWT.FILL, true, true);
        gd.widthHint = 50;
        gd.heightHint = 50;
        buildPathPart.getSection().setLayoutData(gd);

        Composite rightPanel = tk.createComposite(sashForm);
        gd = new GridData(SWT.FILL, SWT.FILL, true, true);
        rightPanel.setLayoutData(gd);

        BuildOperationsPart buildOpsPart = new BuildOperationsPart(rightPanel, tk, Section.TITLE_BAR | Section.EXPANDED | Section.DESCRIPTION);
        managedForm.addPart(buildOpsPart);

        gd = new GridData(SWT.FILL, SWT.FILL, true, false);
        buildOpsPart.getSection().setLayoutData(gd);

        layout = new GridLayout(1, false);
        rightPanel.setLayout(layout);

        reportProblemsInHeader();
    }

    @Override
    public void init(IEditorSite site, IEditorInput input) {
        super.init(site, input);

        loadProblems();
    }

    void loadProblems() {
        IResource resource = ResourceUtil.getResource(getEditorInput());
        problemSeverity = 0;
        messageSeverityMap.clear();

        if (resource != null) {
            try {
                IMarker[] markers;

                markers = resource.findMarkers(BndtoolsConstants.MARKER_BND_PROBLEM, true, 0);
                loadMarkers(markers);

                markers = resource.findMarkers(BndtoolsConstants.MARKER_BND_PATH_PROBLEM, true, 0);
                loadMarkers(markers);

                markers = resource.findMarkers(BndtoolsConstants.MARKER_BND_MISSING_WORKSPACE, true, 0);
                loadMarkers(markers);
            } catch (CoreException e) {
                logger.logError("Error retrieving problem markers", e);
            }
        }

        ImageDescriptor editorImgOverlay;
        if (problemSeverity >= IMarker.SEVERITY_ERROR) {
            pageImage = imgError;
            editorImgOverlay = imgErrorOverlay;
        } else if (problemSeverity >= IMarker.SEVERITY_WARNING) {
            pageImage = imgWarning;
            editorImgOverlay = imgWarningOverlay;
        } else {
            pageImage = null;
            editorImgOverlay = null;
        }
        editor.updatePageTitle(this);
        editor.setOverlayTitleImage(editorImgOverlay);
    }

    private void loadMarkers(IMarker[] markers) {
        for (IMarker marker : markers) {
            int severity = marker.getAttribute(IMarker.SEVERITY, IMarker.SEVERITY_INFO);
            String message = marker.getAttribute(IMarker.MESSAGE, "");

            messageSeverityMap.put(message, severity);

            generateFixes(message, marker);

            problemSeverity = Math.max(problemSeverity, severity);
        }
    }

    // look for available quick-fixes from the bnd build error details
    private void generateFixes(String message, final IMarker marker) {
        boolean fixable = marker.getAttribute(BuildErrorDetailsHandler.PROP_HAS_RESOLUTIONS, false);
        if (!fixable)
            return;

        ITextEditor sourcePage = editor.getSourcePage();
        if (sourcePage == null)
            return;

        String type = marker.getAttribute("$bndType", (String) null);
        if (type == null)
            return;

        BuildErrorDetailsHandler handler = BuildErrorDetailsHandlers.INSTANCE.findHandler(type);
        if (handler == null)
            return;

        List<IAction> fixes = new LinkedList<>();
        List<ICompletionProposal> proposals = handler.getProposals(marker);
        if (proposals != null) {
            for (ICompletionProposal proposal : proposals) {
                fixes.add(new ApplyCompletionProposalAction(proposal, editor.getSourcePage(), editor, BndEditor.SOURCE_PAGE));
            }
        }

        // If no source completion fixes were found, add the marker resolution actions.
        if (fixes.isEmpty()) {
            List<IMarkerResolution> resolutions = handler.getResolutions(marker);
            for (final IMarkerResolution resolution : resolutions) {
                Action action = new Action(resolution.getLabel()) {
                    @Override
                    public void run() {
                        resolution.run(marker);
                    }
                };
                fixes.add(action);
            }
        }

        messageFixesMap.put(message, fixes.toArray(new IAction[fixes.size()]));
    }

    void reportProblemsInHeader() {
        IManagedForm mform = getManagedForm();
        if (mform == null)
            return;

        if (mform.getForm().isDisposed())
            // We were called asynchronously after dialog was closed
            return;

        IMessageManager manager = mform.getMessageManager();
        manager.removeMessages();

        for (Entry<String,Integer> entry : messageSeverityMap.entrySet()) {
            // severities in IMessageProvider are 1 higher than in IMarker
            int mappedSeverity = entry.getValue() + 1;

            IAction[] fixes = messageFixesMap.get(entry.getKey()); // may be null

            manager.addMessage(entry.getKey(), entry.getKey(), fixes, mappedSeverity);
        }
    }

    @Override
    public int getPriority() {
        if (problemSeverity >= IMarker.SEVERITY_ERROR)
            return 10;
        if (problemSeverity >= IMarker.SEVERITY_WARNING)
            return 5;
        return 0;
    }

    @Override
    public Image getTitleImage() {
        return pageImage;
    }

    @Override
    public void resourceChanged(IResourceChangeEvent event) {
        IResource myResource = ResourceUtil.getResource(getEditorInput());

        IResourceDelta delta = event.getDelta();
        if (delta == null)
            return;
        IPath fullPath = myResource.getFullPath();
        delta = delta.findMember(fullPath);
        if (delta == null)
            return;

        if ((delta.getKind() & IResourceDelta.CHANGED) != 0 && (delta.getFlags() & IResourceDelta.MARKERS) != 0) {
            getEditorSite().getShell().getDisplay().asyncExec(new Runnable() {
                @Override
                public void run() {
                    loadProblems();
                    reportProblemsInHeader();
                }
            });
        }
    }

}