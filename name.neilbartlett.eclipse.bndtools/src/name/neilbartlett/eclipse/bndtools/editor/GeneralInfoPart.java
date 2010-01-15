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
import java.lang.reflect.InvocationTargetException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import name.neilbartlett.eclipse.bndtools.Plugin;
import name.neilbartlett.eclipse.bndtools.UIConstants;
import name.neilbartlett.eclipse.bndtools.editor.model.BndEditModel;
import name.neilbartlett.eclipse.bndtools.editor.model.ExportedPackage;
import name.neilbartlett.eclipse.bndtools.utils.JavaContentProposal;
import name.neilbartlett.eclipse.bndtools.utils.JavaContentProposalLabelProvider;
import name.neilbartlett.eclipse.bndtools.utils.JavaTypeContentProposal;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
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
import org.eclipse.jdt.core.search.SearchParticipant;
import org.eclipse.jdt.core.search.SearchPattern;
import org.eclipse.jdt.core.search.SearchRequestor;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
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
import org.eclipse.jface.fieldassist.TextContentAdapter;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbenchWindow;
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
import org.eclipse.ui.ide.ResourceUtil;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.Constants;
import org.osgi.framework.Version;

import aQute.bnd.plugin.Activator;

public class GeneralInfoPart extends SectionPart implements PropertyChangeListener {
	
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
		section.setText("Basic Information");
		
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
		
		// Content Proposal for the Activator field
		ContentProposalAdapter activatorProposalAdapter = null;
		
		ActivatorClassProposalProvider proposalProvider = new ActivatorClassProposalProvider();
		activatorProposalAdapter = new ContentProposalAdapter(txtActivator, new TextContentAdapter(), proposalProvider, assistKeyStroke, UIConstants.AUTO_ACTIVATION_CLASSNAME);
		activatorProposalAdapter.addContentProposalListener(proposalProvider);
		activatorProposalAdapter.setProposalAcceptanceStyle(ContentProposalAdapter.PROPOSAL_REPLACE);
		activatorProposalAdapter.setLabelProvider(new JavaContentProposalLabelProvider());
		activatorProposalAdapter.setAutoActivationDelay(500);
		
		ControlDecoration decorActivator = new ControlDecoration(txtActivator, SWT.LEFT | SWT.TOP, composite);
		decorActivator.setImage(contentAssistDecoration.getImage());
		decorActivator.setDescriptionText(MessageFormat.format("Content Assist is available. Press {0} or start typing to activate", assistKeyStroke.format()));
		decorActivator.setShowOnlyOnFocus(true);
		decorActivator.setShowHover(true);
		
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
						IType activatorType = getJavaProject().findType(activatorClassName);
						if(activatorType == null) {
							unknownError = "The activator class name is not known in this project.";
						}
					} catch (JavaModelException e) {
						Activator.getDefault().getLog().log(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Error looking up activator class name: " + activatorClassName, e));
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
						IType activatorType = getJavaProject().findType(activatorClassName);
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
		if(activatorProposalAdapter != null) {
			activatorProposalAdapter.addContentProposalListener(new IContentProposalListener() {
				public void proposalAccepted(IContentProposal proposal) {
					if(proposal instanceof JavaContentProposal) {
						String selectedPackageName = ((JavaContentProposal) proposal).getPackageName();
						if(!model.isIncludedPackage(selectedPackageName)) {
							commit(false);
							model.addPrivatePackage(selectedPackageName);
						}
					}
				}
			});
		}
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
	
	void checkActivatorIncluded() {
		String warningMessage = null;
		IAction[] fixes = null;
		
		String activatorClassName = txtActivator.getText();
		if(activatorClassName != null && activatorClassName.length() > 0) {
			int dotIndex = activatorClassName.lastIndexOf('.');
			if(dotIndex == -1) {
				warningMessage = "Cannot use an activator in the default package.";
			} else {
				final String packageName = activatorClassName.substring(0, dotIndex);
				if(!model.isIncludedPackage(packageName)) {
					warningMessage = "Activator package is not included in the bundle. It will be imported instead.";
					fixes = new Action[] {
						new Action(MessageFormat.format("Add \"{0}\" to Private Packages.", packageName)) {
							public void run() {
								model.addPrivatePackage(packageName);
								addDirtyProperty(aQute.lib.osgi.Constants.PRIVATE_PACKAGE);
							};
						},
						new Action(MessageFormat.format("Add \"{0}\" to Exported Packages.", packageName)) {
							public void run() {
								model.addExportedPackage(new ExportedPackage(packageName, null));
								addDirtyProperty(aQute.lib.osgi.Constants.PRIVATE_PACKAGE);
							};
						}
					};
				}
			}
		}
		
		IMessageManager msgs = getManagedForm().getMessageManager();
		if(warningMessage != null)
			msgs.addMessage(UNINCLUDED_ACTIVATOR_WARNING_KEY, warningMessage, fixes, IMessageProvider.WARNING, txtActivator);
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
	
	IJavaProject getJavaProject() {
		IFormPage formPage = (IFormPage) getManagedForm().getContainer();
		IFile file = ResourceUtil.getFile(formPage.getEditorInput());
		return JavaCore.create(file.getProject());
	}
	
	private class ActivatorClassProposalProvider extends CachingContentProposalProvider {
		@Override
		protected List<IContentProposal> doGenerateProposals(String contents, int position) {
			final String prefix = contents.substring(0, position);
			IJavaProject javaProject = getJavaProject();
			try {
				IType activatorType = javaProject.findType(BundleActivator.class.getName());
				final SearchPattern searchPattern = SearchPattern.createPattern(activatorType, IJavaSearchConstants.IMPLEMENTORS);
				final IJavaSearchScope scope = SearchEngine.createJavaSearchScope(new IJavaElement[] { javaProject });
				final List<IContentProposal> result = new ArrayList<IContentProposal>();
				final SearchRequestor requestor = new SearchRequestor() {
					@Override
					public void acceptSearchMatch(SearchMatch match) throws CoreException {
						IType type = (IType) match.getElement();
						if(type.getElementName().toLowerCase().contains(prefix.toLowerCase()))
							result.add(new JavaTypeContentProposal(type));
					}
				};
				final IRunnableWithProgress runnable = new IRunnableWithProgress() {
					public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
						try {
							new SearchEngine().search(searchPattern, new SearchParticipant[] { SearchEngine.getDefaultSearchParticipant() }, scope, requestor, monitor);
						} catch (CoreException e) {
							throw new InvocationTargetException(e);
						}
					}
				};
				
				IWorkbenchWindow window = ((IFormPage) getManagedForm().getContainer()).getEditorSite().getWorkbenchWindow();
				window.run(false, false, runnable);
				return result;
			} catch (JavaModelException e) {
				IStatus status = new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Error searching for BundleActivator types", e);
				Activator.getDefault().getLog().log(status);
				return Collections.emptyList();
			} catch (InvocationTargetException e) {
				IStatus status = new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Error searching for BundleActivator types", e.getTargetException());
				Activator.getDefault().getLog().log(status);
				return Collections.emptyList();
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				return Collections.emptyList();
			}
		}

		@Override
		protected boolean match(String contents, int position, IContentProposal proposal) {
			String prefix = contents.substring(0, position);
			return ((JavaTypeContentProposal) proposal).getTypeName().toLowerCase().startsWith(prefix.toLowerCase());
		}
	}
}
