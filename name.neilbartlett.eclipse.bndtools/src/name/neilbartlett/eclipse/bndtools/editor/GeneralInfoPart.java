/*******************************************************************************
 * Copyright (c) 2009 Neil Bartlett.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Neil Bartlett - initial API and implementation
 ******************************************************************************/
package name.neilbartlett.eclipse.bndtools.editor;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import name.neilbartlett.eclipse.bndtools.Plugin;
import name.neilbartlett.eclipse.bndtools.editor.model.BndEditModel;
import name.neilbartlett.eclipse.bndtools.editor.model.ExportedPackage;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.core.search.SearchMatch;
import org.eclipse.jdt.core.search.SearchPattern;
import org.eclipse.jdt.core.search.SearchRequestor;
import org.eclipse.jdt.internal.corext.util.SearchUtils;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.bindings.keys.KeyStroke;
import org.eclipse.jface.bindings.keys.ParseException;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.fieldassist.ContentProposalAdapter;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.jface.fieldassist.IContentProposal;
import org.eclipse.jface.fieldassist.IContentProposalListener;
import org.eclipse.jface.fieldassist.IContentProposalProvider;
import org.eclipse.jface.fieldassist.TextContentAdapter;
import org.eclipse.jface.viewers.LabelProvider;
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
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.IMessageManager;
import org.eclipse.ui.forms.SectionPart;
import org.eclipse.ui.forms.editor.IFormPage;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Hyperlink;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.Constants;
import org.osgi.framework.Version;

public class GeneralInfoPart extends SectionPart implements PropertyChangeListener {
	
	private static final String AUTO_ACTIVATE_CHARS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ01234567890._";
	private static final String[] INTERESTED_PROPERTIES = new String[] {
		Constants.BUNDLE_SYMBOLICNAME,
		Constants.BUNDLE_VERSION,
		Constants.BUNDLE_ACTIVATOR,
		Constants.EXPORT_PACKAGE,
		aQute.lib.osgi.Constants.PRIVATE_PACKAGE,
		aQute.lib.osgi.Constants.SOURCES,
	};
	private static final String UNKNOWN_ACTIVATOR_ERROR_KEY = "ERROR_" + Constants.BUNDLE_ACTIVATOR + "_UNKNOWN";
	private static final String UNINCLUDED_ACTIVATOR_WARNING_KEY = "WARNING_" + Constants.BUNDLE_ACTIVATOR + "_UNINCLUDED";

	private final Set<String> interestedPropertySet;
	private final Set<String> dirtySet = new HashSet<String>();
	
	private BndEditModel model;
	private IJavaProject javaProject;
	
	private Text txtBSN;
	private Text txtVersion;
	private Text txtActivator;
	private Button btnSources;
	
	private AtomicInteger refreshers = new AtomicInteger(0);

	public GeneralInfoPart(Composite parent, FormToolkit toolkit, int style) {
		super(parent, toolkit, style);
		createSection(getSection(), toolkit);
		
		interestedPropertySet = new HashSet<String>();
		for (String prop : INTERESTED_PROPERTIES) {
			interestedPropertySet.add(prop);
		}
	}

	private void createSection(Section section, FormToolkit toolkit) {
		section.setText("General Information");
		
		KeyStroke assistKeyStroke = null;
		try {
			assistKeyStroke = KeyStroke.getInstance("Ctrl+Space");
		} catch (ParseException x) {
			// Ignore
		}
		FieldDecoration contentAssistDecoration = FieldDecorationRegistry.getDefault().getFieldDecoration(FieldDecorationRegistry.DEC_CONTENT_PROPOSAL);
		FieldDecoration infoDecoration = FieldDecorationRegistry.getDefault().getFieldDecoration(FieldDecorationRegistry.DEC_INFORMATION);
		
		Composite composite = toolkit.createComposite(section);
		section.setClient(composite);
		
		toolkit.createLabel(composite, "Symbolic Name:");
		txtBSN = toolkit.createText(composite, "");
		
		toolkit.createLabel(composite, "Version:");
		txtVersion = toolkit.createText(composite, "");
		
		Hyperlink linkActivator = toolkit.createHyperlink(composite, "Activator:", SWT.NONE);
		txtActivator = toolkit.createText(composite, "");
		ControlDecoration decorActivator = new ControlDecoration(txtActivator, SWT.LEFT | SWT.TOP, composite);
		decorActivator.setImage(contentAssistDecoration.getImage());
		decorActivator.setDescriptionText(MessageFormat.format("Content Assist is available. Press {0} or start typing to activate", assistKeyStroke.format()));
		decorActivator.setShowOnlyOnFocus(true);
		decorActivator.setShowHover(true);
		
		ContentProposalAdapter activatorProposalAdapter = new ContentProposalAdapter(txtActivator, new TextContentAdapter(), new ActivatorProposalProvider(), assistKeyStroke, AUTO_ACTIVATE_CHARS.toCharArray());
		activatorProposalAdapter.setProposalAcceptanceStyle(ContentProposalAdapter.PROPOSAL_REPLACE);
		activatorProposalAdapter.setLabelProvider(new ActivatorLabelProvider());
		activatorProposalAdapter.setAutoActivationDelay(500);
		
		btnSources = toolkit.createButton(composite, "Include source files.", SWT.CHECK);
		ControlDecoration decorSources = new ControlDecoration(btnSources, SWT.RIGHT, composite);
		decorSources.setImage(infoDecoration.getImage());
		decorSources.setDescriptionText("If checked, the source code files will be included in the bundle under 'OSGI-OPT/src'.");
		decorSources.setShowHover(true);
		
		// Listeners
		txtBSN.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				addDirtyProperty(Constants.BUNDLE_SYMBOLICNAME);
				IMessageManager msgs = getManagedForm().getMessageManager();
				String text = txtBSN.getText();
				if(text == null || text.length() == 0)
					msgs.addMessage("INFO_" + Constants.BUNDLE_SYMBOLICNAME, "The symbolic name of the bundle will default to the filename, without the .bnd extension.", null, IMessageProvider.INFORMATION, txtBSN);
				else
					msgs.removeMessage("INFO_" + Constants.BUNDLE_SYMBOLICNAME, txtBSN);
			}
		});
		txtVersion.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				addDirtyProperty(Constants.BUNDLE_VERSION);
				IMessageManager msgs = getManagedForm().getMessageManager();
				try {
					String text = txtVersion.getText();
					if(text.length() > 0)
						new Version(text);
					msgs.removeMessage("ERROR_" + Constants.BUNDLE_VERSION, txtVersion);
				} catch (IllegalArgumentException x) {
					msgs.addMessage("ERROR_" + Constants.BUNDLE_VERSION, "Invalid version format.", null, IMessageProvider.ERROR, txtVersion);
				}
			}
		});
		txtActivator.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent ev) {
				addDirtyProperty(Constants.BUNDLE_ACTIVATOR);
				IMessageManager msgs = getManagedForm().getMessageManager();
				String unknownError = null;
				
				String activatorClassName = txtActivator.getText();
				if(activatorClassName != null && activatorClassName.length() > 0) {
					try {
						IType activatorType = javaProject.findType(activatorClassName);
						if(activatorType == null) {
							unknownError = "The activator class name is not known in this project.";
						}
					} catch (JavaModelException e) {
						// TODO: report exception
						e.printStackTrace();
					}
				}
				
				if(unknownError != null) {
					msgs.addMessage(UNKNOWN_ACTIVATOR_ERROR_KEY, unknownError, null, IMessageProvider.ERROR, txtActivator);
				} else {
					msgs.removeMessage(UNKNOWN_ACTIVATOR_ERROR_KEY, txtActivator);
				}
				
				checkActivatorIncluded();
			}
		});
		linkActivator.addHyperlinkListener(new HyperlinkAdapter() {
			@Override
			public void linkActivated(HyperlinkEvent ev) {
				String activatorClassName = txtActivator.getText();
				if(activatorClassName != null && activatorClassName.length() > 0) {
					try {
						IType activatorType = javaProject.findType(activatorClassName);
						if(activatorType != null) {
							JavaUI.openInEditor(activatorType, true, true);
						}
					} catch (PartInitException e) {
						ErrorDialog.openError(getManagedForm().getForm().getShell(), "Error", null, new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, MessageFormat.format("Error opening an editor for activator class '{0}'.", activatorClassName), e));
					} catch (JavaModelException e) {
						ErrorDialog.openError(getManagedForm().getForm().getShell(), "Error", null, new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, MessageFormat.format("Error searching for activator class '{0}'.", activatorClassName), e));
					}
				}
			}
		});
		activatorProposalAdapter.addContentProposalListener(new IContentProposalListener() {
			public void proposalAccepted(IContentProposal proposal) {
				if(proposal instanceof JavaClassContentProposal) {
					IType selectedType = ((JavaClassContentProposal) proposal).element;
					String selectedPackageName = selectedType.getPackageFragment().getElementName();
					
					if(!isIncludedPackage(selectedPackageName)) {
						Collection<String> privatePackages = model.getPrivatePackages();
						privatePackages.add(selectedPackageName);
						commit(false);
						model.setPrivatePackages(privatePackages);
					}
				}
			}
		});
		btnSources.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				addDirtyProperty(aQute.lib.osgi.Constants.SOURCES);
			}
		});
		
		// Layout
		GridLayout layout = new GridLayout(2, false);
		composite.setLayout(layout);
		
		GridData gd;
		gd = new GridData(SWT.FILL, SWT.TOP, true, false);
		gd.widthHint = 150;
		gd.horizontalIndent = 5;
		txtBSN.setLayoutData(gd);

		gd = new GridData(SWT.FILL, SWT.TOP, true, false);
		gd.widthHint = 150;
		gd.horizontalIndent = 5;
		txtVersion.setLayoutData(gd);
		
		gd = new GridData(SWT.FILL, SWT.TOP, true, false);
		gd.widthHint = 150;
		gd.horizontalIndent = 5;
		txtActivator.setLayoutData(gd);
		
		gd = new GridData(SWT.LEFT, SWT.TOP, false, false, 2, 1);
		btnSources.setLayoutData(gd);
	}
	
	boolean isIncludedPackage(String packageName) {
		final Collection<String> privatePackages = model.getPrivatePackages();
		for (String pkg : privatePackages) {
			if(packageName.equals(pkg)) {
				return true;
			}
		}
		final Collection<ExportedPackage> exportedPackages = model.getExportedPackages();
		for (ExportedPackage pkg : exportedPackages) {
			if(packageName.equals(pkg.getPackageName())) {
				return true;
			}
		}
		return false;
	}
	
	void checkActivatorIncluded() {
		String warningMessage = null;
		
		String activatorClassName = txtActivator.getText();
		if(activatorClassName != null && activatorClassName.length() > 0) {
			int dotIndex = activatorClassName.lastIndexOf('.');
			if(dotIndex == -1) {
				warningMessage = "Cannot use an activator in the default package.";
			} else {
				String packageName = activatorClassName.substring(0, dotIndex);
				if(!isIncludedPackage(packageName)) {
					warningMessage = "Activator package is not included in the bundle. It will be imported instead.";
				}
			}
		}
		
		IMessageManager msgs = getManagedForm().getMessageManager();
		if(warningMessage != null)
			msgs.addMessage(UNINCLUDED_ACTIVATOR_WARNING_KEY, warningMessage, null, IMessageProvider.WARNING, txtActivator);
		else
			msgs.removeMessage(UNINCLUDED_ACTIVATOR_WARNING_KEY, txtActivator);
	}
	
	protected void addDirtyProperty(String property) {
		if(refreshers.get() == 0) {
			dirtySet.add(property);
			getManagedForm().dirtyStateChanged();
		}
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
			if(dirtySet.contains(Constants.BUNDLE_SYMBOLICNAME)) {
				String bsn = txtBSN.getText();
				if(bsn != null && bsn.length() == 0) bsn = null;
				model.setBundleSymbolicName(bsn);
			}
			if(dirtySet.contains(Constants.BUNDLE_VERSION)) {
				String version = txtVersion.getText();
				if(version != null && version.length() == 0) version = null;
				model.setBundleVersion(version);
			}
			if(dirtySet.contains(Constants.BUNDLE_ACTIVATOR)) {
				String activator = txtActivator.getText();
				if(activator != null && activator.length() == 0) activator = null;
					model.setBundleActivator(activator);
			}
			if(dirtySet.contains(aQute.lib.osgi.Constants.SOURCES)) {
				model.setIncludeSources(btnSources.getSelection());
			}
		} finally {
			// Restore property change listening
			model.addPropertyChangeListener(this);
			dirtySet.clear();
		}
	}
	
	@Override
	public void refresh() {
		super.refresh();
		try {
			refreshers.incrementAndGet();
			String bsn = model.getBundleSymbolicName();
			txtBSN.setText(bsn != null ? bsn : ""); //$NON-NLS-1$
			
			String bundleVersion = model.getBundleVersionString();
			txtVersion.setText(bundleVersion != null ? bundleVersion : ""); //$NON-NLS-1$
			
			String bundleActivator = model.getBundleActivator();
			txtActivator.setText(bundleActivator != null ? bundleActivator : ""); //$NON-NLS-1$
			
			btnSources.setSelection(model.isIncludeSources());
		} finally {
			refreshers.decrementAndGet();
		}
		dirtySet.clear();
	}
	
	@Override
	public void initialize(IManagedForm form) {
		super.initialize(form);
		
		this.model = (BndEditModel) form.getInput();
		this.model.addPropertyChangeListener(this);
		
		IEditorInput editorInput = ((IFormPage) getManagedForm().getContainer()).getEditorInput();
		if(editorInput instanceof IFileEditorInput) {
			IFile file = ((IFileEditorInput) editorInput).getFile();
			IProject project = file.getProject();
			this.javaProject = JavaCore.create(project);
		} else {
			this.javaProject = null;
		}

	}
	
	public void propertyChange(PropertyChangeEvent evt) {
		if(interestedPropertySet.contains(evt.getPropertyName())) {
			IFormPage page = (IFormPage) getManagedForm().getContainer();
			if(page.isActive()) {
				refresh();
			} else {
				markStale();
			}
		}
	}
	
	@Override
	public void dispose() {
		super.dispose();
		if(this.model != null)
			this.model.removePropertyChangeListener(this);
	}
	
	private class ActivatorProposalProvider implements IContentProposalProvider {
		public IContentProposal[] getProposals(String contents, int position) {
			String substring = contents.substring(0, position).toLowerCase();
			
			final List<IContentProposal> proposals = new LinkedList<IContentProposal>();
			try {
				if(javaProject == null) {
					proposals.add(new ErrorContentProposal("Not a java project"));
				} else {
					IType activatorType = javaProject.findType(BundleActivator.class.getName());
					
					final SearchRequestor requestor = new SearchRequestor() {
						public void acceptSearchMatch(SearchMatch match) throws CoreException {
							IType element = (IType) match.getElement();
							proposals.add(new JavaClassContentProposal(element));
						}
					};
					
					SearchPattern pattern = SearchPattern.createPattern(activatorType, IJavaSearchConstants.IMPLEMENTORS);
					try {
						NullProgressMonitor monitor = new NullProgressMonitor();
						IJavaSearchScope searchScope = javaProject != null ? SearchEngine.createJavaSearchScope(new IJavaElement[] { javaProject }) : null;
						if(searchScope != null) {
							new SearchEngine().search(pattern, SearchUtils.getDefaultSearchParticipants(), searchScope,
									requestor, monitor);
						}
					} catch (CoreException e) {
						e.printStackTrace();
					}
				}
			} catch (JavaModelException e) {
				proposals.clear();
				proposals.add(new ErrorContentProposal("Error"));
			}
			
			// Remove proposals that don't match what the user has typed
			for (Iterator<IContentProposal> iter = proposals.iterator(); iter.hasNext();) {
				IContentProposal proposal = iter.next();
				if(proposal.getContent().toLowerCase().indexOf(substring) == -1) {
					iter.remove();
				}
			}
			
			return proposals
					.toArray(new IContentProposal[proposals.size()]);
		}
	}
	private class JavaClassContentProposal implements IContentProposal {
		
		private final IType element;

		public JavaClassContentProposal(IType element) {
			this.element = element;
		}

		public String getContent() {
			return element.getFullyQualifiedName();
		}

		public int getCursorPosition() {
			return getContent().length();
		}

		public String getDescription() {
			return null;
		}

		public String getLabel() {
			return element.getElementName() + " - " + element.getPackageFragment().getElementName();
		}
		
		public IType getType() {
			return element;
		}
		
	}
	private class ErrorContentProposal implements IContentProposal {

		private final String message;

		public ErrorContentProposal(String message) {
			this.message = message;
		}

		public String getContent() {
			return "";
		}

		public String getDescription() {
			return null;
		}

		public String getLabel() {
			return message;
		}

		public int getCursorPosition() {
			return 0;
		}
	}
	
	private class ActivatorLabelProvider extends LabelProvider {
		
		private Image classImg = AbstractUIPlugin.imageDescriptorFromPlugin(Plugin.PLUGIN_ID, "icons/class_obj.gif").createImage();
		
		@Override
		public Image getImage(Object element) {
			Image result = null;
			
			if(element instanceof JavaClassContentProposal) {
				result = classImg;
			}
			
			return result;
		}
		
		@Override
		public String getText(Object element) {
			IContentProposal proposal = (IContentProposal) element;
			
			return proposal.getLabel();
		}
		
		@Override
		public void dispose() {
			super.dispose();
			classImg.dispose();
		}
	}
	
	
}
