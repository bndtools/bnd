package bndtools.editor.contents;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.reflect.InvocationTargetException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.bndtools.api.ILogger;
import org.bndtools.api.Logger;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.bindings.keys.KeyStroke;
import org.eclipse.jface.bindings.keys.ParseException;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.fieldassist.ContentProposalAdapter;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.jface.fieldassist.IContentProposal;
import org.eclipse.jface.fieldassist.TextContentAdapter;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.SectionPart;
import org.eclipse.ui.forms.editor.IFormPage;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Hyperlink;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.ide.ResourceUtil;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.Constants;

import aQute.bnd.build.model.BndEditModel;
import aQute.lib.exceptions.Exceptions;
import bndtools.Plugin;
import bndtools.UIConstants;
import bndtools.editor.utils.ToolTips;
import bndtools.utils.CachingContentProposalProvider;
import bndtools.utils.JavaContentProposal;
import bndtools.utils.JavaContentProposalLabelProvider;
import bndtools.utils.JavaTypeContentProposal;
import bndtools.utils.ModificationLock;

public class GeneralInfoPart extends SectionPart implements PropertyChangeListener {
	private static final ILogger	logger				= Logger.getLogger(GeneralInfoPart.class);

	private static final String[]	EDITABLE_PROPERTIES	= new String[] {
		Constants.BUNDLE_VERSION, Constants.BUNDLE_ACTIVATOR, aQute.bnd.osgi.Constants.SERVICE_COMPONENT,
		aQute.bnd.osgi.Constants.DSANNOTATIONS
	};

	private final Set<String>		editablePropertySet;
	private final Set<String>		dirtySet			= new HashSet<>();

	private BndEditModel			model;

	private Text					txtVersion;
	private Text					txtActivator;

	private final ModificationLock	lock				= new ModificationLock();

	public GeneralInfoPart(Composite parent, FormToolkit toolkit, int style) {
		super(parent, toolkit, style);
		createSection(getSection(), toolkit);

		editablePropertySet = new HashSet<>();
		for (String prop : EDITABLE_PROPERTIES) {
			editablePropertySet.add(prop);
		}
	}

	private void createSection(Section section, FormToolkit toolkit) {
		section.setText("Basic Information");

		KeyStroke assistKeyStroke = null;
		try {
			assistKeyStroke = KeyStroke.getInstance("Ctrl+Space");
		} catch (ParseException x) {
			// Ignore
		}
		FieldDecoration contentAssistDecoration = FieldDecorationRegistry.getDefault()
			.getFieldDecoration(FieldDecorationRegistry.DEC_CONTENT_PROPOSAL);

		Composite composite = toolkit.createComposite(section);
		section.setClient(composite);

		toolkit.createLabel(composite, "Version:");
		txtVersion = toolkit.createText(composite, "", SWT.BORDER);
		ToolTips.setupMessageAndToolTipFromSyntax(txtVersion, Constants.BUNDLE_VERSION);

		Hyperlink linkActivator = toolkit.createHyperlink(composite, "Activator:", SWT.NONE);
		txtActivator = toolkit.createText(composite, "", SWT.BORDER);
		ToolTips.setupMessageAndToolTipFromSyntax(txtActivator, Constants.BUNDLE_ACTIVATOR);

		// Content Proposal for the Activator field
		ContentProposalAdapter activatorProposalAdapter = null;

		ActivatorClassProposalProvider proposalProvider = new ActivatorClassProposalProvider();
		activatorProposalAdapter = new ContentProposalAdapter(txtActivator, new TextContentAdapter(), proposalProvider,
			assistKeyStroke, UIConstants.autoActivationCharacters());
		activatorProposalAdapter.addContentProposalListener(proposalProvider);
		activatorProposalAdapter.setProposalAcceptanceStyle(ContentProposalAdapter.PROPOSAL_REPLACE);
		activatorProposalAdapter.setLabelProvider(new JavaContentProposalLabelProvider());
		activatorProposalAdapter.setAutoActivationDelay(1000);

		// Decorator for the Activator field
		ControlDecoration decorActivator = new ControlDecoration(txtActivator, SWT.LEFT | SWT.CENTER, composite);
		decorActivator.setImage(contentAssistDecoration.getImage());
		decorActivator.setMarginWidth(3);
		if (assistKeyStroke == null) {
			decorActivator.setDescriptionText("Content Assist is available. Start typing to activate");
		} else {
			decorActivator.setDescriptionText(MessageFormat.format(
				"Content Assist is available. Press {0} or start typing to activate", assistKeyStroke.format()));
		}
		decorActivator.setShowOnlyOnFocus(true);
		decorActivator.setShowHover(true);

		// Listeners
		txtVersion.addModifyListener(e -> lock.ifNotModifying(() -> addDirtyProperty(Constants.BUNDLE_VERSION)));
		txtActivator.addModifyListener(ev -> lock.ifNotModifying(() -> addDirtyProperty(Constants.BUNDLE_ACTIVATOR)));
		linkActivator.addHyperlinkListener(new HyperlinkAdapter() {
			@Override
			public void linkActivated(HyperlinkEvent ev) {
				String activatorClassName = txtActivator.getText();
				if (activatorClassName != null && activatorClassName.length() > 0) {
					try {
						IJavaProject javaProject = getJavaProject();
						if (javaProject == null)
							return;

						IType activatorType = javaProject.findType(activatorClassName);
						if (activatorType != null) {
							JavaUI.openInEditor(activatorType, true, true);
						}
					} catch (PartInitException e) {
						ErrorDialog.openError(getManagedForm().getForm()
							.getShell(), "Error", null,
							new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, MessageFormat
								.format("Error opening an editor for activator class '{0}'.", activatorClassName), e));
					} catch (JavaModelException e) {
						ErrorDialog.openError(getManagedForm().getForm()
							.getShell(), "Error", null,
							new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0,
								MessageFormat.format("Error searching for activator class '{0}'.", activatorClassName),
								e));
					}
				}
			}
		});
		activatorProposalAdapter.addContentProposalListener(proposal -> {
			if (proposal instanceof JavaContentProposal) {
				String selectedPackageName = ((JavaContentProposal) proposal).getPackageName();
				if (!model.isIncludedPackage(selectedPackageName)) {
					model.addPrivatePackage(selectedPackageName);
				}
			}
		});

		// Layout
		GridLayout layout = new GridLayout(2, false);
		layout.horizontalSpacing = 15;

		composite.setLayout(layout);

		GridData gd = new GridData(SWT.FILL, SWT.TOP, true, false);

		txtVersion.setLayoutData(gd);
		txtActivator.setLayoutData(gd);
	}

	protected void addDirtyProperty(final String property) {
		lock.ifNotModifying(() -> {
			dirtySet.add(property);
			getManagedForm().dirtyStateChanged();
		});
	}

	@Override
	public void markDirty() {
		throw new UnsupportedOperationException("Do not call markDirty directly, instead call addDirtyProperty.");
	}

	@Override
	public boolean isDirty() {
		return !dirtySet.isEmpty();
	}

	@Override
	public void commit(boolean onSave) {
		try {
			// Stop listening to property changes during the commit only
			model.removePropertyChangeListener(this);
			if (dirtySet.contains(Constants.BUNDLE_VERSION)) {
				String version = txtVersion.getText();
				if (version != null && version.length() == 0)
					version = null;
				model.setBundleVersion(version);
			}
			if (dirtySet.contains(Constants.BUNDLE_ACTIVATOR)) {
				String activator = txtActivator.getText();
				if (activator != null && activator.length() == 0)
					activator = null;
				model.setBundleActivator(activator);
			}
		} finally {
			// Restore property change listening
			model.addPropertyChangeListener(this);
			dirtySet.clear();
			getManagedForm().dirtyStateChanged();
		}
	}

	@Override
	public void refresh() {
		super.refresh();
		lock.modifyOperation(() -> {
			String bundleVersion = model.getBundleVersionString();
			txtVersion.setText(bundleVersion != null ? bundleVersion : ""); //$NON-NLS-1$

			String bundleActivator = model.getBundleActivator();
			txtActivator.setText(bundleActivator != null ? bundleActivator : ""); //$NON-NLS-1$
		});
		dirtySet.clear();
		getManagedForm().dirtyStateChanged();
	}

	@Override
	public void initialize(IManagedForm form) {
		super.initialize(form);

		this.model = (BndEditModel) form.getInput();
		this.model.addPropertyChangeListener(this);
	}

	@Override
	public void propertyChange(PropertyChangeEvent evt) {
		if (editablePropertySet.contains(evt.getPropertyName())) {
			IFormPage page = (IFormPage) getManagedForm().getContainer();
			if (page.isActive()) {
				refresh();
			} else {
				markStale();
			}
		}
	}

	@Override
	public void dispose() {
		super.dispose();
		if (this.model != null)
			this.model.removePropertyChangeListener(this);
	}

	IJavaProject getJavaProject() {
		IFormPage formPage = (IFormPage) getManagedForm().getContainer();
		IFile file = ResourceUtil.getFile(formPage.getEditorInput());
		return file != null ? JavaCore.create(file.getProject()) : null;
	}

	private class ActivatorClassProposalProvider extends CachingContentProposalProvider {
		@Override
		protected List<IContentProposal> doGenerateProposals(String contents, int position) {
			final String prefix = contents.substring(0, position);
			final IJavaProject javaProject = getJavaProject();
			if (javaProject == null)
				return Collections.emptyList();

			try {
				final List<IContentProposal> result = new ArrayList<>();

				final IRunnableWithProgress runnable = monitor -> {
					try {
						IType activatorType = javaProject.findType(BundleActivator.class.getName());
						if (activatorType != null) {
							ITypeHierarchy hierarchy = activatorType.newTypeHierarchy(javaProject, monitor);
							for (IType subType : hierarchy.getAllSubtypes(activatorType)) {
								if (!Flags.isAbstract(subType.getFlags()) && subType.getElementName()
									.toLowerCase()
									.contains(prefix.toLowerCase())) {
									result.add(new JavaTypeContentProposal(subType));
								}
							}
						}
					} catch (JavaModelException e) {
						throw new InvocationTargetException(e);
					}
				};

				IWorkbenchWindow window = ((IFormPage) getManagedForm().getContainer()).getEditorSite()
					.getWorkbenchWindow();
				window.run(false, false, runnable);
				return result;
			} catch (InvocationTargetException e) {
				logger.logError("Error searching for BundleActivator types",
					Exceptions.unrollCause(e, InvocationTargetException.class));
				return Collections.emptyList();
			} catch (InterruptedException e) {
				Thread.currentThread()
					.interrupt();
				return Collections.emptyList();
			}
		}

		@Override
		protected boolean match(String contents, int position, IContentProposal proposal) {
			String prefix = contents.substring(0, position);
			return ((JavaTypeContentProposal) proposal).getTypeName()
				.toLowerCase()
				.startsWith(prefix.toLowerCase());
		}
	}
}
